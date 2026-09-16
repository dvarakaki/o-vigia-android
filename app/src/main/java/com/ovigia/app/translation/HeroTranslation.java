package com.ovigia.app.translation;

import java.util.Collections;
import java.util.Map;

/**
 * A ficha de um herói traduzida — inteira ou em partes, porque a biografia pode
 * levar minutos e vai ficando pronta aos poucos. Imutável.
 */
public final class HeroTranslation {

    /** Idioma de destino (ISO 639-1, ex.: {@code "pt"}). */
    public final String language;
    /** Biografia HTML com os trechos já traduzidos; {@code null} enquanto nenhum estiver pronto. */
    public final String description;
    /** Quanto da biografia já foi traduzido, de 0 a 1. */
    public final float biographyProgress;
    /** Não falta mais nada para traduzir nesta ficha. */
    public final boolean complete;

    private final Map<String, String> texts;

    HeroTranslation(String language, Map<String, String> texts, String description,
                    float biographyProgress, boolean complete) {
        this.language = language;
        this.texts = Collections.unmodifiableMap(texts);
        this.description = description;
        this.biographyProgress = biographyProgress;
        this.complete = complete;
    }

    /** Tradução de um texto curto da ficha (resumo, nascimento, galeria); {@code null} se não houver. */
    public String text(String original) {
        return texts.get(ComicVineTexts.normalize(original));
    }

    /** Ainda não há nada traduzido para mostrar. */
    public boolean isEmpty() {
        return texts.isEmpty() && description == null;
    }
}
