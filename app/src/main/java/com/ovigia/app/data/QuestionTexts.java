package com.ovigia.app.data;

import android.content.res.Resources;

import com.ovigia.app.R;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Liga cada chave de {@link QuestionKeys} ao seu texto em {@code strings.xml}.
 *
 * O mapa é explícito (em vez de {@code Resources.getIdentifier}) para que o R8
 * enxergue as referências e não remova as strings ao encolher recursos.
 * {@code QuestionTextsTest} garante que toda chave tem recurso.
 */
public final class QuestionTexts {

    public static Map<String, String> load(Resources res) {
        Map<String, String> texts = new LinkedHashMap<>();
        texts.put(QuestionKeys.GENDER_MALE, res.getString(R.string.q_gender_m));

        texts.put("origin_human", res.getString(R.string.q_origin_human));
        texts.put("origin_mutant", res.getString(R.string.q_origin_mutant));
        texts.put("origin_alien", res.getString(R.string.q_origin_alien));
        texts.put("origin_god_eternal", res.getString(R.string.q_origin_god_eternal));
        texts.put("origin_robot", res.getString(R.string.q_origin_robot));
        texts.put("origin_radiation", res.getString(R.string.q_origin_radiation));
        texts.put("origin_cyborg", res.getString(R.string.q_origin_cyborg));
        texts.put("origin_animal", res.getString(R.string.q_origin_animal));

        texts.put("team_avengers", res.getString(R.string.q_team_avengers));
        texts.put("team_xmen", res.getString(R.string.q_team_xmen));
        texts.put("team_guardians", res.getString(R.string.q_team_guardians));
        texts.put("team_f4", res.getString(R.string.q_team_f4));
        texts.put("team_inhumans", res.getString(R.string.q_team_inhumans));
        texts.put("team_eternals", res.getString(R.string.q_team_eternals));

        texts.put("power_forca", res.getString(R.string.q_power_forca));
        texts.put("power_voo", res.getString(R.string.q_power_voo));
        texts.put("power_cura", res.getString(R.string.q_power_cura));
        texts.put("power_genio", res.getString(R.string.q_power_genio));
        texts.put("power_invisibilidade", res.getString(R.string.q_power_invisibilidade));
        texts.put("power_telepatia", res.getString(R.string.q_power_telepatia));
        texts.put("power_telecinese", res.getString(R.string.q_power_telecinese));
        texts.put("power_magia", res.getString(R.string.q_power_magia));
        texts.put("power_armas", res.getString(R.string.q_power_armas));
        texts.put("power_teia", res.getString(R.string.q_power_teia));
        texts.put("power_elasticidade", res.getString(R.string.q_power_elasticidade));
        texts.put("power_imortal", res.getString(R.string.q_power_imortal));
        texts.put("power_tamanho", res.getString(R.string.q_power_tamanho));
        texts.put("power_teletransporte", res.getString(R.string.q_power_teletransporte));
        texts.put("power_energia", res.getString(R.string.q_power_energia));
        texts.put("power_sentidos", res.getString(R.string.q_power_sentidos));
        texts.put("power_tecnologia", res.getString(R.string.q_power_tecnologia));
        texts.put("power_invulneravel", res.getString(R.string.q_power_invulneravel));
        texts.put("power_velocidade", res.getString(R.string.q_power_velocidade));
        texts.put("power_absorver", res.getString(R.string.q_power_absorver));
        texts.put("power_realidade", res.getString(R.string.q_power_realidade));
        texts.put("power_atravessar", res.getString(R.string.q_power_atravessar));
        texts.put("power_mimetismo", res.getString(R.string.q_power_mimetismo));
        texts.put("power_metamorfose", res.getString(R.string.q_power_metamorfose));
        texts.put("power_viagem_no_tempo", res.getString(R.string.q_power_viagem_no_tempo));
        texts.put("power_ilusao", res.getString(R.string.q_power_ilusao));
        texts.put("power_jovem", res.getString(R.string.q_power_jovem));

        texts.put(QuestionKeys.IS_VILLAIN, res.getString(R.string.q_is_villain));
        return texts;
    }

    private QuestionTexts() { }
}
