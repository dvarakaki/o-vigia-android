package com.ovigia.app.translation;

import com.ovigia.app.model.CharacterDetail;

/** Tradução da ficha de um herói. Abstraída para os ViewModels serem testados com um fake. */
public interface HeroTranslationRepository {

    /** Vale a pena traduzir para {@code language} (há tradutor e não é o idioma da API). */
    boolean supports(String language);

    /** Chamado sempre na main thread. */
    interface Callback {

        /** Uma parte ficou pronta — ou tudo, quando {@link HeroTranslation#complete}. */
        void onUpdate(HeroTranslation translation);

        /** @param partial o que já havia sido traduzido antes da falha (pode estar vazio) */
        void onError(TranslationException.Reason reason, HeroTranslation partial);
    }

    /** Trabalho em andamento; cancelar não perde o que já foi traduzido. */
    interface Job {
        void cancel();
    }

    /**
     * Traduz resumo, nascimento, galerias e biografia da ficha.
     *
     * @param allowMeteredDownload o jogador autorizou baixar o pacote de tradução em rede móvel
     */
    Job translate(CharacterDetail detail, String language, boolean allowMeteredDownload, Callback callback);
}
