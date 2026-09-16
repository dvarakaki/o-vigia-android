package com.ovigia.app.data;

import android.util.Log;

import com.ovigia.app.api.ApiClient;
import com.ovigia.app.api.ComicVineService;
import com.ovigia.app.data.roster.RosterCatalog;
import com.ovigia.app.engine.CharacterProfile;
import com.ovigia.app.engine.GameEngine;
import com.ovigia.app.learning.LearningStore;
import com.ovigia.app.model.Character;
import com.ovigia.app.model.ComicVineResponse;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

import retrofit2.Response;

/**
 * Implementação real de {@link CharacterRepository}: busca o elenco na Comic
 * Vine e converte para o formato do motor do jogo.
 *
 * Camadas de cache:
 * 1. Memória — perfis "puros" (sem aprendizado), enquanto o processo viver.
 * 2. Disco ({@link CharacterDiskCache}) — sobrevive ao app fechar; com ele o
 *    jogo funciona offline (e imune ao limite de requisições) por 7 dias, e um
 *    cache vencido ainda serve de último recurso se a rede falhar.
 *
 * Todo I/O (assets, disco, rede) roda no {@code ioExecutor}; o callback volta
 * pelo {@code mainExecutor}.
 *
 * Os textos das perguntas são lidos de novo a cada carga: se o jogador trocar o
 * idioma, a próxima partida já vem traduzida sem refazer os perfis.
 */
public final class ComicVineCharacterRepository implements CharacterRepository {

    private static final String TAG = "CharacterRepository";

    /** Status do corpo de resposta da Comic Vine (independente do HTTP). */
    private static final int CV_STATUS_OK = 1;
    private static final int CV_STATUS_INVALID_KEY = 100;
    private static final int CV_STATUS_RATE_LIMIT = 107;

    /** Carrega o catálogo curado (normalmente de {@code assets/roster.json}). */
    public interface RosterSource {
        RosterCatalog load() throws IOException;
    }

    private final ComicVineService service;
    private final String apiKey;
    private final boolean apiConfigured;
    private final RosterSource rosterSource;
    private final Supplier<Map<String, String>> questionTexts;
    private final LearningStore learningStore;
    private final Supplier<String> currentAccountId;
    private final CharacterDiskCache diskCache;
    private final Executor ioExecutor;
    private final Executor mainExecutor;

    private volatile List<CharacterProfile> cachedRawProfiles;
    /** Chaves das perguntas usadas pelo elenco, na ordem em que apareceram. */
    private volatile List<String> cachedQuestionKeys;

    /**
     * @param questionTexts textos no idioma atual; chamado no executor de I/O a cada carga.
     * @param currentAccountId id da conta ativa (ou {@code null} sem sessão), consultado a cada
     *                         carga para o aprendizado misturado ser sempre o da conta logada.
     */
    public ComicVineCharacterRepository(ComicVineService service, String apiKey, boolean apiConfigured,
                                        RosterSource rosterSource, Supplier<Map<String, String>> questionTexts,
                                        LearningStore learningStore, Supplier<String> currentAccountId,
                                        Supplier<File> cacheFile,
                                        Executor ioExecutor, Executor mainExecutor) {
        this.service = service;
        this.apiKey = apiKey;
        this.apiConfigured = apiConfigured;
        this.rosterSource = rosterSource;
        this.questionTexts = questionTexts;
        this.learningStore = learningStore;
        this.currentAccountId = currentAccountId;
        this.diskCache = new CharacterDiskCache(cacheFile);
        this.ioExecutor = ioExecutor;
        this.mainExecutor = mainExecutor;
    }

    @Override
    public void loadCharacters(Callback callback) {
        ioExecutor.execute(() -> {
            try {
                learningStore.ensureLoaded();
                Map<String, String> texts = questionTexts.get();
                if (cachedRawProfiles == null) {
                    buildRawProfiles(texts);
                }
                Map<String, String> questions = new LinkedHashMap<>();
                for (String key : cachedQuestionKeys) {
                    String text = texts.get(key);
                    if (text != null) questions.put(key, text);
                }
                String accountId = currentAccountId.get();
                List<CharacterProfile> profiles = applyLearning(cachedRawProfiles, questions, accountId);
                mainExecutor.execute(() -> callback.onSuccess(profiles, Collections.unmodifiableMap(questions)));
            } catch (LoadException e) {
                mainExecutor.execute(() -> callback.onError(e.error));
            }
        });
    }

    private synchronized void buildRawProfiles(Map<String, String> texts) throws LoadException {
        if (cachedRawProfiles != null) return;

        RosterCatalog roster;
        try {
            roster = rosterSource.load();
        } catch (IOException | RuntimeException e) {
            Log.e(TAG, "roster.json ilegível", e);
            throw new LoadException(LoadError.EMPTY_ROSTER);
        }

        List<Character> characters = diskCache.readFresh();
        if (characters == null) {
            try {
                if (!apiConfigured) throw new LoadException(LoadError.NOT_CONFIGURED);
                characters = fetchAll(roster);
                diskCache.write(characters);
            } catch (LoadException e) {
                characters = diskCache.readStale();
                if (characters == null) throw e;
                Log.i(TAG, "Rede indisponível (" + e.error + "); usando cache vencido");
            }
        }

        CharacterMapper mapper = new CharacterMapper(roster, texts);
        Map<String, String> questionTextByKey = new LinkedHashMap<>();
        List<CharacterProfile> profiles = new ArrayList<>();
        for (Character c : characters) {
            if (roster.get(c.id) == null) continue;
            if (c.image == null || c.image.bestForHero() == null) continue;
            profiles.add(mapper.toProfile(c, questionTextByKey));
        }
        if (profiles.isEmpty()) throw new LoadException(LoadError.EMPTY_ROSTER);

        cachedQuestionKeys = Collections.unmodifiableList(new ArrayList<>(questionTextByKey.keySet()));
        cachedRawProfiles = Collections.unmodifiableList(profiles);
    }

    /**
     * Busca todo o elenco, paginando: a Comic Vine devolve no máximo 100
     * resultados por página. Chamada síncrona — só no executor de I/O.
     */
    private List<Character> fetchAll(RosterCatalog roster) throws LoadException {
        Map<Integer, Character> byId = new LinkedHashMap<>();
        String filter = "id:" + roster.idFilter();
        int offset = 0;
        while (true) {
            Response<ComicVineResponse<List<Character>>> response;
            try {
                response = service.listCharacters(apiKey, ApiClient.FORMAT, ApiClient.PAGE_SIZE,
                        offset, filter, ApiClient.GAME_FIELDS).execute();
            } catch (IOException e) {
                throw new LoadException(LoadError.NO_CONNECTION);
            }

            int http = response.code();
            if (http == 420 || http == 429) throw new LoadException(LoadError.RATE_LIMITED);
            if (http == 401 || http == 403) throw new LoadException(LoadError.NOT_CONFIGURED);
            ComicVineResponse<List<Character>> body = response.body();
            if (!response.isSuccessful() || body == null) throw new LoadException(LoadError.SERVER_ERROR);
            if (body.statusCode == CV_STATUS_INVALID_KEY) throw new LoadException(LoadError.NOT_CONFIGURED);
            if (body.statusCode == CV_STATUS_RATE_LIMIT) throw new LoadException(LoadError.RATE_LIMITED);
            if (body.statusCode != CV_STATUS_OK || body.results == null) {
                throw new LoadException(LoadError.SERVER_ERROR);
            }

            for (Character c : body.results) byId.put(c.id, c);
            offset += ApiClient.PAGE_SIZE;
            if (body.results.isEmpty() || offset >= body.numberOfTotalResults) break;
        }
        if (byId.isEmpty()) throw new LoadException(LoadError.EMPTY_ROSTER);
        return new ArrayList<>(byId.values());
    }

    /**
     * Cópia dos perfis com as crenças ajustadas pelo {@link LearningStore} da conta
     * ativa. Percorre TODAS as perguntas da partida, não só os atributos que o perfil
     * já tem: se a curadoria esqueceu um poder e o jogador responde "sim" de forma
     * consistente, o aprendizado corrige a partir da crença padrão. Sem conta,
     * devolve os perfis brutos (o motor volta a usar só o prior curado).
     */
    private List<CharacterProfile> applyLearning(List<CharacterProfile> raw, Map<String, String> questions,
                                                 String accountId) {
        if (accountId == null) return raw;
        List<CharacterProfile> adjusted = new ArrayList<>(raw.size());
        for (CharacterProfile p : raw) {
            Map<String, Double> attrs = new LinkedHashMap<>(p.attributes);
            for (String key : questions.keySet()) {
                Double original = p.attributes.get(key);
                Double blended = learningStore.blendedBelief(accountId, p.id, key,
                        original != null ? original : GameEngine.MISSING_BELIEF);
                if (blended != null) attrs.put(key, blended);
            }
            adjusted.add(p.withAttributes(attrs));
        }
        return adjusted;
    }

    private static final class LoadException extends Exception {
        final LoadError error;

        LoadException(LoadError error) {
            super(error.name(), null, false, false);
            this.error = error;
        }
    }
}
