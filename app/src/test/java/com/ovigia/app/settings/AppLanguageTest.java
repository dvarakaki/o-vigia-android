package com.ovigia.app.settings;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AppLanguageTest {

    @Test
    public void fromLanguageTags_matchesByLanguageIgnoringRegion() {
        assertEquals(AppLanguage.PORTUGUESE_BRAZIL, AppLanguage.fromLanguageTags("pt-BR"));
        assertEquals(AppLanguage.PORTUGUESE_BRAZIL, AppLanguage.fromLanguageTags("pt-PT"));
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromLanguageTags("en-US"));
        assertEquals(AppLanguage.SPANISH, AppLanguage.fromLanguageTags("es-419"));
        assertEquals(AppLanguage.FRENCH, AppLanguage.fromLanguageTags("fr-CA"));
    }

    @Test
    public void fromLanguageTags_usesOnlyTheFirstLanguage() {
        assertEquals(AppLanguage.SPANISH, AppLanguage.fromLanguageTags("es,en-US"));
    }

    @Test
    public void fromLanguageTags_emptyOrUntranslated_isSystem() {
        assertEquals(AppLanguage.SYSTEM, AppLanguage.fromLanguageTags(""));
        assertEquals(AppLanguage.SYSTEM, AppLanguage.fromLanguageTags(null));
        assertEquals(AppLanguage.SYSTEM, AppLanguage.fromLanguageTags("de-DE"));
    }

    @Test
    public void everyTagRoundTrips() {
        for (AppLanguage language : AppLanguage.values()) {
            assertEquals(language, AppLanguage.fromLanguageTags(language.tag));
        }
    }

    /** O seletor do Android 13+ e as traduções precisam oferecer os mesmos idiomas do app. */
    @Test
    public void everyLanguageIsDeclaredAndTranslated() throws IOException {
        String localeConfig = new String(Files.readAllBytes(
                new File("src/main/res/xml/locales_config.xml").toPath()), StandardCharsets.UTF_8);
        for (AppLanguage language : AppLanguage.values()) {
            if (language == AppLanguage.SYSTEM) continue;
            assertTrue("falta " + language.tag + " em locales_config.xml",
                    localeConfig.contains("android:name=\"" + language.tag + "\""));
            if (language == AppLanguage.PORTUGUESE_BRAZIL) continue; // idioma base: values/
            String folder = "src/main/res/values-" + Locale.forLanguageTag(language.tag).getLanguage();
            assertTrue("falta " + folder + "/strings.xml", new File(folder, "strings.xml").exists());
        }
    }

    /**
     * Os textos da Comic Vine são traduzidos para o idioma dos próprios strings.xml:
     * se {@code content_language} não bater com a pasta, a ficha sairia num idioma
     * e a tela em outro.
     */
    @Test
    public void cadaIdiomaDizParaOndeTraduzirOsTextosDaApi() throws IOException {
        for (AppLanguage language : AppLanguage.values()) {
            if (language == AppLanguage.SYSTEM) continue;
            String code = Locale.forLanguageTag(language.tag).getLanguage();
            String folder = language == AppLanguage.PORTUGUESE_BRAZIL ? "values" : "values-" + code;
            String strings = new String(Files.readAllBytes(
                    new File("src/main/res/" + folder + "/strings.xml").toPath()), StandardCharsets.UTF_8);
            assertTrue("falta <string name=\"content_language\">" + code + "</string> em " + folder,
                    strings.contains("<string name=\"content_language\">" + code + "</string>"));
        }
    }
}
