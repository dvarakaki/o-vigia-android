package com.ovigia.app.translation;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Todo termo do glossário precisa de texto em todos os idiomas E de uma linha em
 * {@code ComicVineGlossary} — senão o herói aparece com o poder em inglês, ou a
 * string fica no APK sem ninguém usar.
 */
public class ComicVineGlossaryTest {

    private static final String[] LANGUAGES = {"values", "values-en", "values-es", "values-fr"};
    private static final Pattern REFERENCE = Pattern.compile("R\\.string\\.(cv_[a-z0-9_]+)");
    private static final Pattern DECLARATION = Pattern.compile("<string name=\"(cv_[a-z0-9_]+)\">");

    @Test
    public void todoTermoDoGlossarioTemTextoEmTodosOsIdiomas() throws IOException {
        Set<String> referenced = names(REFERENCE,
                read("src/main/java/com/ovigia/app/translation/ComicVineGlossary.java"));
        assertFalse("nenhum termo no glossário?", referenced.isEmpty());

        for (String language : LANGUAGES) {
            Set<String> declared = names(DECLARATION, read("src/main/res/" + language + "/glossary.xml"));
            for (String name : referenced) {
                assertTrue("falta <string name=\"" + name + "\"> em " + language + "/glossary.xml",
                        declared.contains(name));
            }
        }
    }

    @Test
    public void naoSobraTextoSemUso() throws IOException {
        Set<String> referenced = names(REFERENCE,
                read("src/main/java/com/ovigia/app/translation/ComicVineGlossary.java"));
        Set<String> declared = names(DECLARATION, read("src/main/res/values/glossary.xml"));

        declared.removeAll(referenced);
        assertEquals("texto de glossário que ninguém usa", Set.of(), declared);
    }

    @Test
    public void asOrigensDoJogoTambemEstaoNoGlossario() throws IOException {
        Set<String> declared = names(DECLARATION, read("src/main/res/values/glossary.xml"));
        // As origens que viram pergunta são as mesmas que aparecem na ficha.
        for (String origin : new String[]{"human", "mutant", "alien", "god_eternal", "robot",
                "radiation", "cyborg", "animal"}) {
            assertTrue("falta cv_origin_" + origin, declared.contains("cv_origin_" + origin));
        }
    }

    private static Set<String> names(Pattern pattern, String content) {
        Set<String> names = new LinkedHashSet<>();
        Matcher matcher = pattern.matcher(content);
        while (matcher.find()) names.add(matcher.group(1));
        return names;
    }

    private static String read(String path) throws IOException {
        return new String(Files.readAllBytes(new File(path).toPath()), StandardCharsets.UTF_8);
    }
}
