package com.ovigia.app.ui.catalog;

import android.content.Context;

import com.ovigia.app.R;
import com.ovigia.app.model.ApiRef;
import com.ovigia.app.translation.ComicVineTexts;

import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.MonthDay;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Formatação dos dados da Comic Vine para a ficha, no idioma do app ({@link Locale#getDefault()}). */
final class DetailFormat {

    static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    static int size(List<?> list) {
        return list == null ? 0 : list.size();
    }

    static String number(long value) {
        return NumberFormat.getIntegerInstance(Locale.getDefault()).format(value);
    }

    /** "2008-06-06 11:27:39" → "6 de jun. de 2008"; devolve o texto original se não entender. */
    static String date(String apiDate) {
        if (isBlank(apiDate)) return null;
        try {
            Date parsed = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).parse(apiDate.trim());
            return parsed == null ? apiDate : DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault()).format(parsed);
        } catch (ParseException e) {
            return apiDate;
        }
    }

    static String date(long millis) {
        return DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault()).format(new Date(millis));
    }

    /**
     * Nascimento como a Comic Vine escreve ("Oct 14, 1962", "October 14") no
     * formato do idioma do app. {@code null} quando é texto livre ("Unknown"),
     * que aí vai para a tradução automática.
     */
    static String birth(String birth) {
        TemporalAccessor parsed = ComicVineTexts.birthDate(birth);
        try {
            if (parsed instanceof LocalDate) {
                return DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
                        .withLocale(Locale.getDefault()).format(parsed);
            }
            if (parsed instanceof MonthDay) {
                // Dia e mês na ordem do idioma ("14 de outubro", "October 14").
                String pattern = android.text.format.DateFormat
                        .getBestDateTimePattern(Locale.getDefault(), "MMMMd");
                return DateTimeFormatter.ofPattern(pattern, Locale.getDefault()).format(parsed);
            }
        } catch (RuntimeException e) {
            // Formato inesperado para o idioma: melhor mostrar o texto da API.
            return null;
        }
        return null;
    }

    /** Gênero da Comic Vine; {@code null} quando desconhecido. */
    static String gender(Context context, int gender) {
        if (gender == 1) return context.getString(R.string.hero_gender_male);
        if (gender == 2) return context.getString(R.string.hero_gender_female);
        return null;
    }

    /** Apelidos vêm um por linha. */
    static List<String> aliases(String aliases) {
        List<String> out = new ArrayList<>();
        if (aliases == null) return out;
        for (String line : aliases.split("\\r?\\n")) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty() && !out.contains(trimmed)) out.add(trimmed);
        }
        return out;
    }

    /** "Nome da edição · Edição #6". */
    static String issue(Context context, ApiRef issue) {
        if (issue == null) return null;
        String name = isBlank(issue.name) ? null : issue.name.trim();
        String number = isBlank(issue.issueNumber) ? null : context.getString(R.string.hero_issue_number, issue.issueNumber.trim());
        if (name == null) return number;
        return number == null ? name : name + " · " + number;
    }

    static String refName(Context context, ApiRef ref) {
        return ref == null || isBlank(ref.name) ? context.getString(R.string.hero_untitled) : ref.name.trim();
    }

    private DetailFormat() { }
}
