package com.ovigia.app.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Vocabulário de perguntas do jogo. As chaves são estáveis — o
 * {@code LearningStore} guarda o aprendizado indexado por elas, então renomear
 * uma chave descarta o que foi aprendido sobre ela.
 *
 * O texto de cada pergunta vive em {@code strings.xml} (recurso
 * {@code q_<chave>}), para permitir tradução; ver {@code QuestionTexts}.
 */
public final class QuestionKeys {

    public static final String GENDER_MALE = "gender_m";
    public static final String IS_VILLAIN = "is_villain";

    public static final String ORIGIN_PREFIX = "origin_";
    public static final String TEAM_PREFIX = "team_";
    public static final String POWER_PREFIX = "power_";

    /**
     * Valores de "origin" da Comic Vine que viram pergunta → sufixo da chave.
     * "Other" fica de fora de propósito: é um bucket genérico demais para uma
     * pergunta de sim/não útil.
     */
    public static final Map<String, String> ORIGINS;
    static {
        Map<String, String> origins = new LinkedHashMap<>();
        origins.put("Human", "human");
        origins.put("Mutant", "mutant");
        origins.put("Alien", "alien");
        origins.put("God/Eternal", "god_eternal");
        origins.put("Robot", "robot");
        origins.put("Radiation", "radiation");
        origins.put("Cyborg", "cyborg");
        origins.put("Animal", "animal");
        ORIGINS = Collections.unmodifiableMap(origins);
    }

    public static final List<String> TEAMS = Collections.unmodifiableList(Arrays.asList(
            "avengers", "xmen", "guardians", "f4", "inhumans", "eternals"));

    public static final List<String> POWERS = Collections.unmodifiableList(Arrays.asList(
            "forca", "voo", "cura", "genio", "invisibilidade", "telepatia", "telecinese",
            "magia", "armas", "teia", "elasticidade", "imortal", "tamanho", "teletransporte",
            "energia", "sentidos", "tecnologia", "invulneravel", "velocidade", "absorver",
            "realidade", "atravessar", "mimetismo", "metamorfose", "viagem_no_tempo", "ilusao",
            "jovem"));

    /** Todas as chaves de pergunta possíveis, na ordem de exibição dos recursos. */
    public static List<String> all() {
        List<String> keys = new ArrayList<>();
        keys.add(GENDER_MALE);
        for (String origin : ORIGINS.values()) keys.add(ORIGIN_PREFIX + origin);
        for (String team : TEAMS) keys.add(TEAM_PREFIX + team);
        for (String power : POWERS) keys.add(POWER_PREFIX + power);
        keys.add(IS_VILLAIN);
        return keys;
    }

    private QuestionKeys() { }
}
