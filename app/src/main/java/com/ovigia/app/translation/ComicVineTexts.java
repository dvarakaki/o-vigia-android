package com.ovigia.app.translation;

import com.ovigia.app.model.CharacterDetail;

import java.time.LocalDate;
import java.time.MonthDay;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Textos soltos da ficha da Comic Vine: quais precisam de tradução automática e
 * o que dá para resolver sem tradutor nenhum (datas viram data, galerias e
 * vocabulário fechado têm texto próprio em {@code ComicVineGlossary}).
 */
public final class ComicVineTexts {

    /** Galerias com nome traduzido no próprio app. */
    private static final Set<String> KNOWN_GALLERIES = new HashSet<>(Collections.singletonList("all images"));

    /** Como a Comic Vine escreve nascimentos: "Oct 14, 1962", "October 14". */
    private static final DateTimeFormatter[] BIRTH_DATES = {
            DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH),
    };
    private static final DateTimeFormatter[] BIRTH_DAYS = {
            DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("MMMM d", Locale.ENGLISH),
    };

    private static final Pattern SPACES = Pattern.compile("[\\s\\u00A0]+");
    private static final Pattern LETTER = Pattern.compile("\\p{L}");

    /** Espaços em branco iguais aos do resto do pipeline: a chave do cache é o texto normalizado. */
    public static String normalize(String text) {
        return text == null ? "" : SPACES.matcher(text).replaceAll(" ").trim();
    }

    /** Galerias da imagem, que a API entrega separadas por vírgula. */
    public static List<String> galleries(String imageTags) {
        List<String> galleries = new ArrayList<>();
        if (imageTags == null) return galleries;
        for (String tag : imageTags.split(",")) {
            String name = normalize(tag);
            if (!name.isEmpty() && !galleries.contains(name)) galleries.add(name);
        }
        return galleries;
    }

    /** A galeria tem nome traduzido no app e não precisa de tradutor. */
    public static boolean isKnownGallery(String gallery) {
        return KNOWN_GALLERIES.contains(normalize(gallery).toLowerCase(Locale.ROOT));
    }

    /**
     * Nascimento como data, quando a Comic Vine escreveu uma ({@link LocalDate}
     * ou {@link MonthDay}, quando não há ano). {@code null} em texto livre
     * ("Unknown", "Antes de Asgard"…), que aí vai para o tradutor.
     */
    public static TemporalAccessor birthDate(String birth) {
        String text = normalize(birth);
        if (text.isEmpty()) return null;
        for (DateTimeFormatter format : BIRTH_DATES) {
            try {
                return LocalDate.parse(text, format);
            } catch (DateTimeParseException ignored) {
                // Próximo formato.
            }
        }
        for (DateTimeFormatter format : BIRTH_DAYS) {
            try {
                return MonthDay.parse(text, format);
            } catch (DateTimeParseException ignored) {
                // Próximo formato.
            }
        }
        return null;
    }

    /**
     * Textos curtos da ficha que só o tradutor resolve: o resumo, o nascimento
     * que não é data e as galerias sem nome próprio. Sem repetição e sem o que
     * não tem palavra nenhuma (números, pontuação).
     */
    public static List<String> shortTexts(CharacterDetail detail) {
        Set<String> texts = new LinkedHashSet<>();
        if (detail != null) {
            texts.add(normalize(detail.deck));
            if (birthDate(detail.birth) == null) texts.add(normalize(detail.birth));
            if (detail.image != null) {
                for (String gallery : galleries(detail.image.imageTags)) {
                    if (!isKnownGallery(gallery)) texts.add(gallery);
                }
            }
        }
        List<String> translatable = new ArrayList<>();
        for (String text : texts) {
            if (LETTER.matcher(text).find()) translatable.add(text);
        }
        return translatable;
    }

    /** Idiomas em que a Comic Vine escreve — o que não precisa de tradução. */
    public static boolean isSourceLanguage(String language) {
        return Arrays.asList("en", "eng").contains(normalize(language).toLowerCase(Locale.ROOT));
    }

    private ComicVineTexts() { }
}
