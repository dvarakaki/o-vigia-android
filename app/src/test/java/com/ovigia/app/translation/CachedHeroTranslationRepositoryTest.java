package com.ovigia.app.translation;

import com.ovigia.app.model.CharacterDetail;
import com.ovigia.app.translation.TranslationException.Reason;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** Cache em disco, andamento, cancelamento e falhas da tradução — sem tradutor de verdade. */
public class CachedHeroTranslationRepositoryTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private final Executor direct = Runnable::run;
    private FakeTranslator translator;
    private File directory;
    private Result result;

    @Before
    public void setUp() {
        translator = new FakeTranslator();
        directory = new File(tmp.getRoot(), "hero_translations");
        result = new Result();
    }

    private CachedHeroTranslationRepository repository() {
        return repository(direct);
    }

    /** @param translationExecutor onde a tradução em si roda (os testes de cancelamento a seguram) */
    private CachedHeroTranslationRepository repository(Executor translationExecutor) {
        return new CachedHeroTranslationRepository(translator, () -> directory, direct, translationExecutor,
                direct, () -> 0L);
    }

    private static CharacterDetail lizard() {
        CharacterDetail detail = new CharacterDetail();
        detail.id = 1488;
        detail.deck = "Curtis Connors became a reptile.";
        detail.description = "<h2>Origin</h2><p>He was a scientist.</p>";
        return detail;
    }

    private HeroTranslationRepository.Job translate(CharacterDetail detail) {
        return repository().translate(detail, "pt", false, result);
    }

    @Test
    public void traduzResumoEBiografia() {
        translate(lizard());

        HeroTranslation translation = result.last();
        assertTrue(translation.complete);
        assertEquals("[Curtis Connors became a reptile.]", translation.text("Curtis Connors became a reptile."));
        assertEquals("<h2>[Origin]</h2><p>[He was a scientist.]</p>", translation.description);
        assertEquals(1f, translation.biographyProgress, 0.001f);
        assertEquals(1, translator.opened);
        assertEquals(1, translator.closed);
    }

    @Test
    public void naSegundaVezTudoVemDoCacheSemAbrirOTradutor() {
        translate(lizard());
        int calls = translator.translated;

        result = new Result();
        translator.opened = 0;
        translate(lizard());

        assertEquals(1, result.updates.size());
        assertTrue(result.last().complete);
        assertEquals("<h2>[Origin]</h2><p>[He was a scientist.]</p>", result.last().description);
        assertEquals("nenhum texto novo traduzido", calls, translator.translated);
        assertEquals("tradutor nem foi aberto", 0, translator.opened);
    }

    @Test
    public void semNadaParaTraduzirNaoChamaOTradutor() {
        CharacterDetail empty = new CharacterDetail();
        empty.id = 7;

        translate(empty);

        assertTrue(result.last().complete);
        assertTrue(result.last().isEmpty());
        assertEquals(0, translator.opened);
    }

    @Test
    public void cancelarNoMeioSalvaOQueJaFoiTraduzidoEContinuaDepois() {
        CharacterDetail detail = lizard();
        detail.description = "<p>First part.</p><p>Second part.</p>";
        // A tradução fica segurada até o teste soltá-la, para cancelar no meio dela.
        List<Runnable> queued = new ArrayList<>();
        HeroTranslationRepository.Job job = repository(queued::add).translate(detail, "pt", false, result);
        // Cancela assim que o primeiro trecho da biografia for traduzido.
        translator.onTranslate = text -> {
            if (text.startsWith("First")) job.cancel();
        };

        assertEquals(1, queued.size());
        queued.get(0).run();

        assertFalse("nada de anunciar tradução pela metade como pronta", result.last().complete);
        result = new Result();
        translator.onTranslate = null;
        translate(detail);

        HeroTranslation translation = result.last();
        assertTrue(translation.complete);
        assertEquals("<p>[First part.]</p><p>[Second part.]</p>", translation.description);
        // "First part." ficou salvo do trabalho anterior: só o que faltava foi traduzido de novo.
        assertEquals(1, count(translator.texts, "First part."));
    }

    @Test
    public void semPacoteDoIdiomaEmRedeMovelEsperaOJogador() {
        translator.failOnOpen = Reason.NEEDS_DOWNLOAD;

        translate(lizard());

        assertEquals(Reason.NEEDS_DOWNLOAD, result.reason);
        assertNotNull(result.partial);
        assertTrue(result.partial.isEmpty());

        // Com o aval do jogador, o download acontece e a ficha é traduzida.
        result = new Result();
        repository().translate(lizard(), "pt", true, result);
        assertNull(result.reason);
        assertTrue(result.last().complete);
    }

    @Test
    public void falhaNoMeioDevolveOQueJaTinhaSidoTraduzido() {
        CharacterDetail detail = lizard();
        translator.failOnTranslate = "He was a scientist.";

        translate(detail);

        assertEquals(Reason.FAILED, result.reason);
        assertEquals("[Curtis Connors became a reptile.]",
                result.partial.text("Curtis Connors became a reptile."));
        assertFalse(result.partial.complete);
    }

    @Test
    public void oIdiomaDaApiNaoEhTraduzido() {
        assertFalse(repository().supports("en"));
        assertTrue(repository().supports("pt"));
        translator.supports = false;
        assertFalse(repository().supports("tlh"));
    }

    private static int count(List<String> texts, String text) {
        int total = 0;
        for (String each : texts) {
            if (each.equals(text)) total++;
        }
        return total;
    }

    /** Coleta o que o repositório publica. */
    private static final class Result implements HeroTranslationRepository.Callback {
        final List<HeroTranslation> updates = new ArrayList<>();
        Reason reason;
        HeroTranslation partial;

        @Override
        public void onUpdate(HeroTranslation translation) {
            updates.add(translation);
        }

        @Override
        public void onError(Reason reason, HeroTranslation partial) {
            this.reason = reason;
            this.partial = partial;
        }

        HeroTranslation last() {
            assertFalse("nenhuma atualização publicada", updates.isEmpty());
            return updates.get(updates.size() - 1);
        }
    }

    /** Tradutor de mentira: devolve o texto entre colchetes. */
    private static final class FakeTranslator implements TextTranslator {
        int opened;
        int closed;
        int translated;
        boolean supports = true;
        Reason failOnOpen;
        String failOnTranslate;
        java.util.function.Consumer<String> onTranslate;
        final List<String> texts = new ArrayList<>();

        @Override
        public boolean supports(String language) {
            return supports && List.of("pt", "es", "fr").contains(language.toLowerCase(Locale.ROOT));
        }

        @Override
        public Session open(String language, boolean allowMeteredDownload) throws TranslationException {
            if (failOnOpen != null && !(allowMeteredDownload && failOnOpen == Reason.NEEDS_DOWNLOAD)) {
                throw new TranslationException(failOnOpen);
            }
            opened++;
            return new Session() {
                @Override
                public String translate(String text) throws TranslationException {
                    if (text.equals(failOnTranslate)) throw new TranslationException(Reason.FAILED);
                    translated++;
                    texts.add(text);
                    if (onTranslate != null) onTranslate.accept(text);
                    return "[" + text + "]";
                }

                @Override
                public void close() {
                    closed++;
                }
            };
        }
    }
}
