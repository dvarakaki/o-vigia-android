package com.ovigia.app.translation;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SentenceChunksTest {

    @Test
    public void textoCurtoVaiInteiro() {
        assertEquals(List.of("He was born in Canada."), SentenceChunks.split("He was born in Canada.", 600));
        assertEquals(List.of(), SentenceChunks.split("   ", 600));
    }

    @Test
    public void textoLongoEQuebradoEmFrasesInteiras() {
        String paragraph = "He was born in Canada. His mother was ill. Rose came to the estate. "
                + "Together they befriended Dog.";

        List<String> chunks = SentenceChunks.split(paragraph, 40);

        assertTrue(chunks.size() + " pedaços", chunks.size() > 1);
        for (String chunk : chunks) {
            assertTrue(chunk, chunk.endsWith(".") && !chunk.startsWith(" "));
        }
        assertEquals(paragraph, String.join(" ", chunks));
    }

    @Test
    public void fraseMaiorQueOLimiteNaoEPartidaNoMeio() {
        String sentence = "A very long sentence without any period inside of it at all";

        assertEquals(List.of(sentence), SentenceChunks.split(sentence, 10));
    }
}
