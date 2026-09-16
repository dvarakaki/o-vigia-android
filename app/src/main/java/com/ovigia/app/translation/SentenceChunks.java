package com.ovigia.app.translation;

import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Corta um texto longo em pedaços de frases inteiras. O tradutor do aparelho foi
 * treinado em frases: pedaços curtos saem melhor, gastam menos memória e deixam
 * o cancelamento acontecer no meio de um parágrafo grande.
 */
final class SentenceChunks {

    /** Frases inteiras agrupadas em pedaços de até {@code maxChars} (uma frase maior que isso fica sozinha). */
    static List<String> split(String text, int maxChars) {
        String trimmed = text == null ? "" : text.trim();
        if (trimmed.isEmpty()) return Collections.emptyList();
        if (trimmed.length() <= maxChars) return Collections.singletonList(trimmed);

        List<String> chunks = new ArrayList<>();
        // O texto vem em inglês: as regras de fim de frase são as do idioma de origem.
        BreakIterator sentences = BreakIterator.getSentenceInstance(Locale.ENGLISH);
        sentences.setText(trimmed);
        StringBuilder chunk = new StringBuilder();
        int start = sentences.first();
        for (int end = sentences.next(); end != BreakIterator.DONE; start = end, end = sentences.next()) {
            String sentence = trimmed.substring(start, end);
            if (chunk.length() > 0 && chunk.length() + sentence.length() > maxChars) {
                add(chunks, chunk);
            }
            chunk.append(sentence);
        }
        add(chunks, chunk);
        return chunks;
    }

    private static void add(List<String> chunks, StringBuilder chunk) {
        String text = chunk.toString().trim();
        if (!text.isEmpty()) chunks.add(text);
        chunk.setLength(0);
    }

    private SentenceChunks() { }
}
