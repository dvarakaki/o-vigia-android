package com.ovigia.app.data;

import com.ovigia.app.engine.CharacterProfile;

import java.util.List;
import java.util.Map;

/**
 * Fonte do elenco de personagens usado pelo motor do Akinator. Abstraída
 * como interface para que o {@code GameViewModel} não dependa de detalhes de
 * rede (Retrofit/Comic Vine) — só de "me dê os personagens e as perguntas".
 * Facilita trocar a fonte de dados no futuro e testar o ViewModel com um
 * fake.
 */
public interface CharacterRepository {

    interface Callback {
        void onSuccess(List<CharacterProfile> profiles, Map<String, String> questionTextByKey);
        void onError(String message);
    }

    void loadCharacters(Callback callback);
}
