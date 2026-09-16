package com.ovigia.app.settings;

import java.util.Locale;

/**
 * Idiomas em que o app foi traduzido. O português é o idioma base
 * ({@code values/}); os demais têm {@code values-<idioma>/}. Cada um também
 * precisa estar em {@code xml/locales_config.xml} — {@code AppLanguageTest} confere.
 */
public enum AppLanguage {
    /** Segue o idioma do aparelho. */
    SYSTEM(""),
    PORTUGUESE_BRAZIL("pt-BR"),
    ENGLISH("en"),
    SPANISH("es"),
    FRENCH("fr");

    /** Tag BCP 47 guardada pelo sistema; vazia em {@link #SYSTEM}. */
    public final String tag;

    AppLanguage(String tag) {
        this.tag = tag;
    }

    /**
     * Idioma correspondente às tags escolhidas no app (ex.: {@code "en-US,pt-BR"}).
     * Só o primeiro idioma conta e a região é ignorada: {@code "pt-PT"} também é
     * português. Tags vazias ou sem tradução voltam {@link #SYSTEM}.
     */
    public static AppLanguage fromLanguageTags(String tags) {
        if (tags == null || tags.trim().isEmpty()) return SYSTEM;
        String first = tags.split(",")[0].trim();
        String language = Locale.forLanguageTag(first).getLanguage();
        for (AppLanguage candidate : values()) {
            if (candidate != SYSTEM && Locale.forLanguageTag(candidate.tag).getLanguage().equals(language)) {
                return candidate;
            }
        }
        return SYSTEM;
    }
}
