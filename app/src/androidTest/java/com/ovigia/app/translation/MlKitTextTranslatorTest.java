package com.ovigia.app.translation;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Locale;

/**
 * O tradutor do aparelho, de ponta a ponta. Precisa de aparelho (ou emulador)
 * com internet na primeira vez: o pacote do idioma tem dezenas de MB e fica
 * guardado depois disso.
 *
 * <pre>./gradlew connectedDebugAndroidTest</pre>
 */
@RunWith(AndroidJUnit4.class)
public class MlKitTextTranslatorTest {

    private final Context context = ApplicationProvider.getApplicationContext();

    @Test
    public void traduzDoInglesParaOsIdiomasDoApp() throws Exception {
        MlKitTextTranslator translator = new MlKitTextTranslator(context);
        String english = "The man who would be known as Wolverine was born in Canada.";

        for (String language : new String[]{"pt", "es", "fr"}) {
            assertTrue(language + " deveria ter tradutor", translator.supports(language));
            try (TextTranslator.Session session = translator.open(language, true)) {
                String translated = session.translate(english);
                assertNotEquals(language, english, translated);
                assertTrue(language + ": " + translated,
                        translated.toLowerCase(Locale.ROOT).contains("wolverine"));
            }
        }
    }

    @Test
    public void idiomaSemTradutorEReconhecido() {
        assertFalse(new MlKitTextTranslator(context).supports("tlh"));
    }
}
