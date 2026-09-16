package com.ovigia.app.data;

import com.ovigia.app.model.CharacterDetail;

/** Ficha completa de um herói. Abstraída para os ViewModels serem testados com um fake. */
public interface HeroDetailRepository {

    /** Chamado sempre na main thread. */
    interface Callback {
        /** @param offlineCopy veio de um cache vencido porque a rede falhou */
        void onSuccess(CharacterDetail detail, boolean offlineCopy);

        void onError(CharacterRepository.LoadError error);
    }

    /** Carrega do cache em disco ou da API; {@code forceRefresh} ignora o cache ainda válido. */
    void load(int characterId, boolean forceRefresh, Callback callback);
}
