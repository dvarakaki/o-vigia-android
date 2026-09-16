package com.ovigia.app.settings;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.LocaleList;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

/**
 * Idioma do app, independente do aparelho. No Android 13+ quem guarda a escolha
 * é o sistema (e ela aparece nas configurações do Android); antes disso, o
 * AppCompat guarda num arquivo próprio ({@code autoStoreLocales} no manifesto).
 */
public final class AppLocales {

    /** Idioma escolhido no app. Chamar depois de a Activity existir. */
    public static AppLanguage current() {
        return AppLanguage.fromLanguageTags(AppCompatDelegate.getApplicationLocales().toLanguageTags());
    }

    /** Troca o idioma; as Activities são recriadas já traduzidas. Main thread. */
    public static void apply(AppLanguage language) {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language.tag));
    }

    /**
     * Recursos no idioma do app para quem não tem uma Activity à mão (ex.: textos
     * das perguntas no repositório). Pode ser chamado fora da main thread.
     */
    public static Resources resources(Context app) {
        // No Android 13+ o sistema já traduz a Application. Consultar o AppCompat
        // aqui percorreria a lista de Activities dele sem sincronização.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return app.getResources();
        // Antes disso o AppCompat só traduz as Activities; a escolha fica num campo estático.
        LocaleListCompat locales = AppCompatDelegate.getApplicationLocales();
        if (locales.isEmpty()) return app.getResources();
        Configuration config = new Configuration(app.getResources().getConfiguration());
        config.setLocales(LocaleList.forLanguageTags(locales.toLanguageTags()));
        return app.createConfigurationContext(config).getResources();
    }

    private AppLocales() { }
}
