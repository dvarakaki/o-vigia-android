package com.ovigia.app.catalog;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Estado do catálogo de heróis. Imutável. */
public final class CatalogUiState {

    public enum Status {
        LOADING,
        READY,
        /** Não há sessão: a tela deve mandar para o login. */
        SIGNED_OUT
    }

    public enum Filter { UNLOCKED, ALL }

    public final Status status;
    /** Itens visíveis com o filtro e a busca atuais. */
    public final List<Item> items;
    public final int unlockedCount;
    /** Tamanho do elenco jogável; 0 se o elenco não pôde ser carregado. */
    public final int totalCount;
    public final Filter filter;
    public final String query;

    CatalogUiState(Status status, List<Item> items, int unlockedCount, int totalCount, Filter filter, String query) {
        this.status = status;
        this.items = Collections.unmodifiableList(items);
        this.unlockedCount = unlockedCount;
        this.totalCount = totalCount;
        this.filter = filter;
        this.query = query;
    }

    static CatalogUiState of(Status status) {
        return new CatalogUiState(status, Collections.emptyList(), 0, 0, Filter.UNLOCKED, "");
    }

    /** O elenco completo está disponível (dá para mostrar os bloqueados e o total). */
    public boolean hasRoster() {
        return totalCount > 0;
    }

    /** Progresso em [0,100]. */
    public int progressPercent() {
        return totalCount == 0 ? 0 : Math.round(100f * unlockedCount / totalCount);
    }

    /** Um herói do catálogo. Bloqueados não revelam nome nem imagem. */
    public static final class Item {
        public final int characterId;
        public final boolean unlocked;
        /** {@code null} se bloqueado. */
        public final String name;
        /** {@code null} se bloqueado. */
        public final String imageUrl;
        /** 0 se bloqueado. */
        public final long unlockedAt;
        /** Desbloqueado desde a última visita ao catálogo: a carta revela com o cadeado sumindo. */
        public final boolean revealNow;

        Item(int characterId, boolean unlocked, String name, String imageUrl, long unlockedAt, boolean revealNow) {
            this.characterId = characterId;
            this.unlocked = unlocked;
            this.name = unlocked ? name : null;
            this.imageUrl = unlocked ? imageUrl : null;
            this.unlockedAt = unlocked ? unlockedAt : 0;
            this.revealNow = unlocked && revealNow;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Item)) return false;
            Item other = (Item) o;
            return characterId == other.characterId && unlocked == other.unlocked
                    && Objects.equals(name, other.name)
                    && Objects.equals(imageUrl, other.imageUrl);
        }

        @Override
        public int hashCode() {
            return Objects.hash(characterId, unlocked, name, imageUrl);
        }
    }
}
