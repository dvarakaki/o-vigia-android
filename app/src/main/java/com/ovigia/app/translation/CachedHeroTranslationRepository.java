package com.ovigia.app.translation;

import android.util.Log;

import com.google.gson.Gson;
import com.ovigia.app.model.CharacterDetail;
import com.ovigia.app.util.AtomicFiles;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/**
 * Traduz a ficha com o tradutor do aparelho e guarda o resultado em
 * {@code files/hero_translations/{id}_{idioma}.json}: da segunda vez a ficha já
 * abre traduzida, sem gastar bateria de novo.
 *
 * A biografia de um herói famoso passa de 200 mil caracteres e leva minutos:
 * ela é traduzida trecho a trecho, o jogador vê o andamento e cada trecho
 * pronto fica salvo — sair da tela no meio não joga o trabalho fora, a próxima
 * visita continua de onde parou.
 */
public final class CachedHeroTranslationRepository implements HeroTranslationRepository {

    private static final String TAG = "HeroTranslation";

    /** Maior pedaço mandado ao tradutor de uma vez, em frases inteiras. */
    private static final int CHUNK_CHARS = 600;
    /** Quanto da biografia traduzir antes de mostrá-la pela primeira vez (o que cabe na prévia). */
    private static final int PREVIEW_CHARS = 2_000;
    /** Intervalo entre avisos de andamento — neles só o número muda, a biografia não é remontada. */
    private static final long PROGRESS_INTERVAL_MILLIS = 1_000;

    private static final int CACHE_VERSION = 1;
    private static final char[] HEX = "0123456789abcdef".toCharArray();

    private final TextTranslator translator;
    private final Supplier<File> directory;
    private final Executor ioExecutor;
    private final Executor translationExecutor;
    private final Executor mainExecutor;
    private final LongSupplier clock;
    private final Gson gson = new Gson();

    /**
     * @param ioExecutor lê o que já estava traduzido e mostra na hora — de propósito
     *     fora do {@code translationExecutor}, que pode estar preso num download de
     *     dezenas de MB enquanto o jogador abre outra ficha
     * @param translationExecutor uma thread só, com a tradução em si
     * @param directory resolvido só fora da main thread
     */
    public CachedHeroTranslationRepository(TextTranslator translator, Supplier<File> directory,
                                           Executor ioExecutor, Executor translationExecutor,
                                           Executor mainExecutor, LongSupplier clock) {
        this.translator = translator;
        this.directory = directory;
        this.ioExecutor = ioExecutor;
        this.translationExecutor = translationExecutor;
        this.mainExecutor = mainExecutor;
        this.clock = clock;
    }

    @Override
    public boolean supports(String language) {
        return !ComicVineTexts.isSourceLanguage(language) && translator.supports(language);
    }

    @Override
    public Job translate(CharacterDetail detail, String language, boolean allowMeteredDownload,
                         Callback callback) {
        Work work = new Work(detail, language, allowMeteredDownload, callback);
        ioExecutor.execute(work::prepare);
        return work::cancel;
    }

    /** Chave curta do texto original: o cache guarda as traduções, não o inglês de novo. */
    private static String key(String text) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(24);
            for (int i = 0; i < 12; i++) {
                hex.append(HEX[(digest[i] >> 4) & 0xF]).append(HEX[digest[i] & 0xF]);
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError("SHA-256 existe em qualquer JVM", e);
        }
    }

    private Map<String, String> read(File file) {
        if (!file.exists()) return Collections.emptyMap();
        try {
            Cache cache = gson.fromJson(AtomicFiles.readUtf8(file), Cache.class);
            if (cache != null && cache.version == CACHE_VERSION && cache.texts != null) return cache.texts;
        } catch (IOException | RuntimeException e) {
            Log.w(TAG, "Tradução salva ilegível", e);
        }
        return Collections.emptyMap();
    }

    private void write(File file, Map<String, String> translations, Set<String> keep) {
        Cache cache = new Cache();
        cache.version = CACHE_VERSION;
        cache.texts = new HashMap<>();
        // Trechos que a ficha não tem mais (a Comic Vine reescreveu a biografia) saem do arquivo.
        for (String key : keep) {
            String translated = translations.get(key);
            if (translated != null) cache.texts.put(key, translated);
        }
        try {
            AtomicFiles.writeUtf8(file, gson.toJson(cache, Cache.class));
        } catch (IOException e) {
            Log.w(TAG, "Falha ao salvar a tradução", e);
        }
    }

    /** Uma ficha sendo traduzida, do cache lido até o último trecho. */
    private final class Work {

        private final int characterId;
        private final String language;
        private final String description;
        private final List<String> shortTexts;
        private final boolean allowMeteredDownload;
        private final Callback callback;
        private final AtomicBoolean cancelled = new AtomicBoolean(false);

        /** Traduções conhecidas, por {@link #key(String)}. */
        private final Map<String, String> translations = new HashMap<>();
        private List<String> biography = Collections.emptyList();
        private int biographyChars;
        private int translatedChars;
        private String rendered;
        private boolean changed;
        private File file;

        Work(CharacterDetail detail, String language, boolean allowMeteredDownload, Callback callback) {
            // A ficha na tela pode ser trocada enquanto isto roda: o que interessa vai copiado.
            this.characterId = detail.id;
            this.language = language;
            this.description = detail.description;
            this.shortTexts = ComicVineTexts.shortTexts(detail);
            this.allowMeteredDownload = allowMeteredDownload;
            this.callback = callback;
        }

        void cancel() {
            cancelled.set(true);
        }

        /** Mostra na hora o que já estava traduzido e, se faltar algo, chama o tradutor. */
        void prepare() {
            try {
                if (cancelled.get()) return;
                file = new File(directory.get(), characterId + "_" + language + ".json");
                translations.putAll(read(file));
                biography = HtmlTextRuns.sources(description);
                for (String run : biography) {
                    biographyChars += run.length();
                    if (translations.containsKey(key(run))) translatedChars += run.length();
                }
                if (translatedChars > 0) rendered = render();
                boolean done = isDone();
                publish(snapshot(done));
                if (!done && !cancelled.get()) translationExecutor.execute(this::translateMissing);
            } catch (RuntimeException e) {
                Log.w(TAG, "Falha ao ler a tradução da ficha " + characterId, e);
                publishError(TranslationException.Reason.FAILED);
            }
        }

        private void translateMissing() {
            try {
                translate();
            } catch (RuntimeException e) {
                Log.w(TAG, "Falha inesperada ao traduzir a ficha " + characterId, e);
                publishError(TranslationException.Reason.FAILED);
            }
        }

        private void translate() {
            if (cancelled.get()) return;
            TextTranslator.Session session;
            try {
                session = translator.open(language, allowMeteredDownload);
            } catch (TranslationException e) {
                publishError(e.reason);
                return;
            }
            TranslationException.Reason failure = null;
            try (TextTranslator.Session open = session) {
                translateShortTexts(open);
                translateBiography(open);
            } catch (TranslationException e) {
                failure = e.reason;
            }
            if (changed) write(file, translations, currentKeys());
            if (cancelled.get()) return;
            if (failure != null) {
                publishError(failure);
                return;
            }
            if (changed && biographyChars > 0) rendered = render();
            publish(snapshot(true));
        }

        private void translateShortTexts(TextTranslator.Session session) throws TranslationException {
            boolean translated = false;
            for (String text : shortTexts) {
                if (cancelled.get()) return;
                if (translations.containsKey(key(text))) continue;
                String result = translate(session, text);
                if (result == null) return;
                translations.put(key(text), result);
                translated = true;
                changed = true;
            }
            // O resumo já aparece traduzido enquanto a biografia demora.
            if (translated && !cancelled.get()) publish(snapshot(false));
        }

        private void translateBiography(TextTranslator.Session session) throws TranslationException {
            long lastProgress = clock.getAsLong();
            for (String run : biography) {
                if (cancelled.get()) return;
                if (translations.containsKey(key(run))) continue;
                String result = translate(session, run);
                if (result == null) return;
                translations.put(key(run), result);
                translatedChars += run.length();
                changed = true;

                if (rendered == null && translatedChars >= Math.min(PREVIEW_CHARS, biographyChars)) {
                    // Primeira versão da biografia: o suficiente para ler a prévia.
                    rendered = render();
                } else if (clock.getAsLong() - lastProgress < PROGRESS_INTERVAL_MILLIS) {
                    continue;
                }
                publish(snapshot(false));
                lastProgress = clock.getAsLong();
            }
        }

        /** {@code null} quando o trabalho foi cancelado no meio — tradução pela metade não entra no cache. */
        private String translate(TextTranslator.Session session, String text) throws TranslationException {
            StringBuilder translated = new StringBuilder();
            for (String chunk : SentenceChunks.split(text, CHUNK_CHARS)) {
                if (cancelled.get()) return null;
                if (translated.length() > 0) translated.append(' ');
                translated.append(session.translate(chunk));
            }
            return translated.toString();
        }

        private boolean isDone() {
            for (String text : shortTexts) {
                if (!translations.containsKey(key(text))) return false;
            }
            return translatedChars >= biographyChars;
        }

        private String render() {
            return HtmlTextRuns.apply(description, source -> translations.get(key(source)));
        }

        private Set<String> currentKeys() {
            Set<String> keys = new HashSet<>();
            for (String text : shortTexts) keys.add(key(text));
            for (String run : biography) keys.add(key(run));
            return keys;
        }

        private HeroTranslation snapshot(boolean complete) {
            Map<String, String> texts = new HashMap<>();
            for (String text : shortTexts) {
                String translated = translations.get(key(text));
                if (translated != null) texts.put(text, translated);
            }
            float progress = biographyChars == 0 ? 1f
                    : Math.min(1f, (float) translatedChars / biographyChars);
            return new HeroTranslation(language, texts, rendered, progress, complete);
        }

        private void publish(HeroTranslation translation) {
            mainExecutor.execute(() -> {
                if (!cancelled.get()) callback.onUpdate(translation);
            });
        }

        private void publishError(TranslationException.Reason reason) {
            if (changed && biographyChars > 0) rendered = render();
            HeroTranslation partial = snapshot(false);
            mainExecutor.execute(() -> {
                if (!cancelled.get()) callback.onError(reason, partial);
            });
        }
    }

    /** Formato do arquivo de cache. */
    private static final class Cache {
        int version;
        Map<String, String> texts;
    }
}
