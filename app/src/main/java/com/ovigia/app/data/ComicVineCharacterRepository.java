package com.ovigia.app.data;

import android.content.Context;

import com.ovigia.app.api.ApiClient;
import com.ovigia.app.api.Rosters;
import com.ovigia.app.engine.CharacterMapper;
import com.ovigia.app.engine.CharacterProfile;
import com.ovigia.app.learning.LearningStore;
import com.ovigia.app.model.Character;
import com.ovigia.app.model.ComicVineResponse;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Response;

/**
 * Implementação real de {@link CharacterRepository}: busca o elenco na
 * Comic Vine e converte para o formato do motor do jogo.
 *
 * Duas camadas de cache, dos dois lados do processo:
 * 1. Memória — dura enquanto o processo do app viver (ver {@code OVigiaApplication}).
 * 2. Disco ({@link CharacterDiskCache}) — sobrevive o app fechar. Uma vez
 *    buscado com sucesso, o jogo funciona offline (e imune ao limite de
 *    requisições da Comic Vine) até o cache expirar ou o app ser desinstalado.
 *
 * O elenco é maior que os 100 resultados por página que a Comic Vine
 * permite, então busca em páginas sucessivas (offset 0, 100, 200...) até
 * reunir tudo.
 */
public final class ComicVineCharacterRepository implements CharacterRepository {

    private final CharacterDiskCache diskCache;
    private final LearningStore learningStore;

    private Map<String, String> cachedQuestionText;
    /**
     * Cache de perfis "puros" (sem overrides do LearningStore). Cada chamada
     * a loadCharacters reaplica os overrides atuais em cima destes, entao
     * uma partida ganha o aprendizado das partidas anteriores mesmo que o
     * app nao tenha sido reaberto.
     */
    private List<CharacterProfile> cachedRawProfiles;

    public ComicVineCharacterRepository(Context context, LearningStore learningStore) {
        this.diskCache = new CharacterDiskCache(context);
        this.learningStore = learningStore;
    }

    @Override
    public void loadCharacters(Callback callback) {
        if (cachedRawProfiles != null) {
            callback.onSuccess(applyLearning(cachedRawProfiles), cachedQuestionText);
            return;
        }

        List<Character> fresh = diskCache.readFresh();
        if (fresh != null) {
            buildProfilesAndRespond(fresh, callback);
            return;
        }

        fetchPage(0, new ArrayList<>(), callback);
    }

    private void fetchPage(int offset, List<Character> accumulated, Callback callback) {
        ApiClient.get().listCharacters(
                ApiClient.API_KEY,
                ApiClient.FORMAT,
                ApiClient.PAGE_SIZE,
                offset,
                "id:" + Rosters.ICONS,
                ApiClient.GAME_FIELDS
        ).enqueue(new retrofit2.Callback<ComicVineResponse<List<Character>>>() {
            @Override
            public void onResponse(Call<ComicVineResponse<List<Character>>> call,
                                    Response<ComicVineResponse<List<Character>>> response) {
                ComicVineResponse<List<Character>> body = response.body();
                if (!response.isSuccessful() || body == null || body.results == null) {
                    onNetworkFailure(describeHttpError(response.code()), callback);
                    return;
                }

                accumulated.addAll(body.results);

                boolean morePages = !body.results.isEmpty() && accumulated.size() < body.numberOfTotalResults;
                if (morePages) {
                    fetchPage(offset + ApiClient.PAGE_SIZE, accumulated, callback);
                    return;
                }

                diskCache.write(accumulated);
                buildProfilesAndRespond(accumulated, callback);
            }

            @Override
            public void onFailure(Call<ComicVineResponse<List<Character>>> call, Throwable t) {
                onNetworkFailure("Sem conexão com a internet.", callback);
            }
        });
    }

    /** Rede falhou (offline, timeout, rate limit...): tenta um cache velho antes de desistir. */
    private void onNetworkFailure(String message, Callback callback) {
        List<Character> stale = diskCache.readStale();
        if (stale != null) {
            buildProfilesAndRespond(stale, callback);
        } else {
            callback.onError(message);
        }
    }

    private static String describeHttpError(int httpCode) {
        if (httpCode == 420 || httpCode == 429) {
            return "A Comic Vine está recebendo requisições demais no momento. Tente de novo em alguns minutos.";
        }
        return "Não foi possível carregar os personagens.";
    }

    private void buildProfilesAndRespond(List<Character> allCharacters, Callback callback) {
        Map<String, String> questionTextByKey = new LinkedHashMap<>();
        List<CharacterProfile> profiles = new ArrayList<>();
        for (Character c : allCharacters) {
            if (c.image == null || c.image.bestForHero() == null) continue;
            profiles.add(CharacterMapper.toProfile(c, questionTextByKey));
        }

        if (profiles.isEmpty()) {
            callback.onError("Nenhum personagem válido foi retornado pela API.");
            return;
        }

        cachedRawProfiles = profiles;
        cachedQuestionText = questionTextByKey;
        callback.onSuccess(applyLearning(profiles), questionTextByKey);
    }

    /**
     * Devolve uma copia dos perfis com as crencas ajustadas pelo que o
     * {@link LearningStore} aprendeu ate agora. O prior de popularidade
     * (contagem de acertos) NAO eh aplicado aqui — vai direto no
     * {@code GameEngine} via boost multiplicativo, junto com a popularidade
     * da Comic Vine.
     */
    private List<CharacterProfile> applyLearning(List<CharacterProfile> raw) {
        if (learningStore == null) return raw;
        List<CharacterProfile> adjusted = new ArrayList<>(raw.size());
        for (CharacterProfile p : raw) {
            Map<String, Double> newAttrs = new LinkedHashMap<>(p.attributes);
            for (Map.Entry<String, Double> entry : p.attributes.entrySet()) {
                double blended = learningStore.blend(p.id, entry.getKey(), entry.getValue());
                if (blended != entry.getValue()) {
                    newAttrs.put(entry.getKey(), blended);
                }
            }
            adjusted.add(new CharacterProfile(p.id, p.name, p.imageUrl, newAttrs,
                    p.issueCount, p.isMainstream));
        }
        return adjusted;
    }
}
