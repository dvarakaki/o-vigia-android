package com.ovigia.app.catalog;

import com.ovigia.app.data.CharacterRepository.LoadError;
import com.ovigia.app.model.CharacterDetail;
import com.ovigia.app.translation.HeroTranslation;

import java.util.Collections;
import java.util.Set;

/** Estado da ficha do herói. Imutável. */
public final class HeroDetailUiState {

    public enum Status {
        /** Buscando a ficha; {@link #previewName}/{@link #previewImageUrl} já podem aparecer. */
        LOADING,
        READY,
        /** A ficha não pôde ser carregada; ver {@link #error}. */
        ERROR,
        /** O herói não está desbloqueado nesta conta. */
        LOCKED,
        SIGNED_OUT
    }

    /** Como está a tradução dos textos que a Comic Vine só tem em inglês. */
    public enum TranslationStatus {
        /** O app está em inglês, como a API: não há o que traduzir. */
        NONE,
        /** Em andamento; {@link #translation} já pode ter uma parte pronta. */
        TRANSLATING,
        DONE,
        /** Falta baixar o pacote do idioma e a rede é limitada: esperar o jogador decidir. */
        NEEDS_DOWNLOAD,
        /** Sem internet para baixar o pacote do idioma. */
        NO_CONNECTION,
        FAILED
    }

    public final Status status;
    public final CharacterDetail detail;
    /** A ficha veio de uma cópia salva porque a rede falhou. */
    public final boolean offlineCopy;
    public final LoadError error;
    /** Nome e imagem guardados no desbloqueio, para mostrar antes da ficha chegar. */
    public final String previewName;
    public final String previewImageUrl;
    public final long unlockedAt;
    /** Heróis desbloqueados da conta: aliados e inimigos desbloqueados abrem a própria ficha. */
    public final Set<Integer> unlockedIds;
    /** Textos traduzidos da ficha; {@code null} quando não há tradução (ainda). */
    public final HeroTranslation translation;
    public final TranslationStatus translationStatus;
    /** O jogador pediu para ver o texto original em inglês. */
    public final boolean showOriginal;

    private HeroDetailUiState(Status status, CharacterDetail detail, boolean offlineCopy, LoadError error,
                              String previewName, String previewImageUrl, long unlockedAt, Set<Integer> unlockedIds,
                              HeroTranslation translation, TranslationStatus translationStatus, boolean showOriginal) {
        this.status = status;
        this.detail = detail;
        this.offlineCopy = offlineCopy;
        this.error = error;
        this.previewName = previewName;
        this.previewImageUrl = previewImageUrl;
        this.unlockedAt = unlockedAt;
        this.unlockedIds = Collections.unmodifiableSet(unlockedIds);
        this.translation = translation;
        this.translationStatus = translationStatus;
        this.showOriginal = showOriginal;
    }

    HeroDetailUiState(Status status, CharacterDetail detail, boolean offlineCopy, LoadError error,
                      String previewName, String previewImageUrl, long unlockedAt, Set<Integer> unlockedIds) {
        this(status, detail, offlineCopy, error, previewName, previewImageUrl, unlockedAt, unlockedIds,
                null, TranslationStatus.NONE, false);
    }

    static HeroDetailUiState of(Status status) {
        return new HeroDetailUiState(status, null, false, null, null, null, 0, Collections.emptySet());
    }

    HeroDetailUiState withTranslation(TranslationStatus translationStatus, HeroTranslation translation) {
        return new HeroDetailUiState(status, detail, offlineCopy, error, previewName, previewImageUrl,
                unlockedAt, unlockedIds, translation, translationStatus, showOriginal);
    }

    HeroDetailUiState withShowOriginal(boolean showOriginal) {
        return new HeroDetailUiState(status, detail, offlineCopy, error, previewName, previewImageUrl,
                unlockedAt, unlockedIds, translation, translationStatus, showOriginal);
    }

    /** Há tradução (pronta ou em andamento) para mostrar ou avisar na tela. */
    public boolean hasTranslation() {
        return translation != null && !translation.isEmpty();
    }
}
