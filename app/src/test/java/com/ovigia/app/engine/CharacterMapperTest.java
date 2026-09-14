package com.ovigia.app.engine;

import com.ovigia.app.model.Character;
import com.ovigia.app.model.ImageData;
import com.ovigia.app.model.NamedRef;

import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Testa a tradução Comic Vine -> atributos do jogo. O que mais importa aqui
 * é não deixar passar batido uma pergunta malformada (ver o incidente do
 * "Seu personagem tem origem \"Other\"?" que motivou excluir esse valor).
 */
public class CharacterMapperTest {

    private static Character character(int id, int gender, String originName) {
        Character c = new Character();
        c.id = id;
        c.name = "Teste";
        c.gender = gender;
        if (originName != null) {
            c.origin = new NamedRef();
            c.origin.name = originName;
        }
        c.image = new ImageData();
        c.image.originalUrl = "http://img/teste.png";
        return c;
    }

    @Test
    public void maleCharacter_getsHighBeliefForGenderMasculine() {
        Map<String, String> questions = new LinkedHashMap<>();
        CharacterProfile profile = CharacterMapper.toProfile(character(1, 1, "Human"), questions);

        assertTrue(profile.attributes.get("gender_m") > 0.5);
        assertEquals("Seu personagem é do gênero masculino?", questions.get("gender_m"));
    }

    @Test
    public void femaleCharacter_getsLowBeliefForGenderMasculine() {
        Map<String, String> questions = new LinkedHashMap<>();
        CharacterProfile profile = CharacterMapper.toProfile(character(1, 2, "Human"), questions);

        assertTrue(profile.attributes.get("gender_m") < 0.5);
    }

    @Test
    public void knownOrigin_generatesQuestion() {
        Map<String, String> questions = new LinkedHashMap<>();
        CharacterProfile profile = CharacterMapper.toProfile(character(1, 1, "Mutant"), questions);

        assertNotNull(profile.attributes.get("origin_mutant"));
        assertEquals("Seu personagem é um mutante?", questions.get("origin_mutant"));
    }

    /**
     * "Other" é um bucket genérico demais da Comic Vine pra virar pergunta —
     * ver o comentário em CharacterMapper. Nenhum atributo "origin_other"
     * deve ser gerado.
     */
    @Test
    public void unmappedOrigin_doesNotGenerateAQuestion() {
        Map<String, String> questions = new LinkedHashMap<>();
        CharacterProfile profile = CharacterMapper.toProfile(character(1, 1, "Other"), questions);

        assertNull(profile.attributes.get("origin_other"));
        assertTrue(questions.keySet().stream().noneMatch(k -> k.startsWith("origin_")));
    }

    @Test
    public void everyProfile_alwaysHasVillainAndTeamAttributes() {
        Map<String, String> questions = new LinkedHashMap<>();
        CharacterProfile profile = CharacterMapper.toProfile(character(999999, 1, "Human"), questions);

        assertNotNull("personagem fora do Rosters ainda precisa responder 'não vilão'",
                profile.attributes.get("is_villain"));
        assertNotNull(profile.attributes.get("team_avengers"));
        assertNotNull(profile.attributes.get("team_xmen"));
        assertNotNull(profile.attributes.get("team_guardians"));
        assertNotNull(profile.attributes.get("team_f4"));
        assertNotNull(profile.attributes.get("team_inhumans"));
        assertNotNull(profile.attributes.get("team_eternals"));
    }
}
