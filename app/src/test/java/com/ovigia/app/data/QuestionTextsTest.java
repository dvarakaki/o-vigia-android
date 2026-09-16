package com.ovigia.app.data;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.Assert.assertTrue;

/**
 * Toda chave de pergunta precisa de texto em strings.xml (e em cada tradução)
 * E de uma linha em QuestionTexts — senão a pergunta some do jogo em silêncio.
 */
public class QuestionTextsTest {

    private static final String[] TRANSLATIONS = {"values-en", "values-es", "values-fr"};

    @Test
    public void everyQuestionKeyHasAStringResourceAndIsWired() throws IOException {
        String strings = read("src/main/res/values/strings.xml");
        String wiring = read("src/main/java/com/ovigia/app/data/QuestionTexts.java");

        for (String key : QuestionKeys.all()) {
            assertTrue("falta <string name=\"q_" + key + "\"> em strings.xml",
                    strings.contains("<string name=\"q_" + key + "\">"));
            assertTrue("falta R.string.q_" + key + " em QuestionTexts",
                    wiring.contains("R.string.q_" + key + ")"));
        }
    }

    @Test
    public void everyQuestionIsTranslated() throws IOException {
        for (String folder : TRANSLATIONS) {
            String strings = read("src/main/res/" + folder + "/strings.xml");
            for (String key : QuestionKeys.all()) {
                assertTrue("falta <string name=\"q_" + key + "\"> em " + folder + "/strings.xml",
                        strings.contains("<string name=\"q_" + key + "\">"));
            }
        }
    }

    private static String read(String path) throws IOException {
        return new String(Files.readAllBytes(new File(path).toPath()), StandardCharsets.UTF_8);
    }
}
