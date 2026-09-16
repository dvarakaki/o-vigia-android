package com.ovigia.app.data;

import com.ovigia.app.data.roster.RosterCatalog;
import com.ovigia.app.engine.CharacterProfile;
import com.ovigia.app.model.Character;
import com.ovigia.app.model.ImageData;
import com.ovigia.app.model.NamedRef;

import org.junit.Before;
import org.junit.Test;

import java.io.StringReader;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** Tradução Comic Vine + curadoria → atributos do jogo. */
public class CharacterMapperTest {

    private static final String ROSTER = "{\"version\":1,\"characters\":["
            + "{\"id\":1,\"name\":\"Herói\",\"teams\":[\"xmen\"],\"powers\":[\"voo\",\"forca\"],\"villain\":0.08,\"mainstream\":true},"
            + "{\"id\":2,\"name\":\"Anti\",\"teams\":[],\"powers\":[\"armas\"],\"villain\":0.3,\"mainstream\":false}"
            + "]}";

    private CharacterMapper mapper;

    @Before
    public void setUp() {
        Map<String, String> texts = new LinkedHashMap<>();
        for (String key : QuestionKeys.all()) texts.put(key, "texto de " + key);
        mapper = new CharacterMapper(RosterCatalog.parse(new StringReader(ROSTER)), texts);
    }

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
    public void gender_mapsToHighOrLowBelief() {
        Map<String, String> questions = new LinkedHashMap<>();
        assertTrue(mapper.toProfile(character(1, 1, "Human"), questions).attributes.get("gender_m") > 0.5);
        assertTrue(mapper.toProfile(character(1, 2, "Human"), questions).attributes.get("gender_m") < 0.5);
        assertEquals("texto de gender_m", questions.get("gender_m"));
    }

    @Test
    public void knownOrigin_isYesForItAndNoForEveryOtherOrigin() {
        Map<String, String> questions = new LinkedHashMap<>();
        CharacterProfile profile = mapper.toProfile(character(1, 1, "Mutant"), questions);

        assertEquals(CharacterMapper.YES, profile.attributes.get("origin_mutant"), 1e-9);
        assertEquals(CharacterMapper.NO, profile.attributes.get("origin_alien"), 1e-9);
        assertEquals(QuestionKeys.ORIGINS.size(),
                questions.keySet().stream().filter(k -> k.startsWith("origin_")).count());
    }

    @Test
    public void unmappedOrigin_doesNotGenerateAQuestion() {
        Map<String, String> questions = new LinkedHashMap<>();
        CharacterProfile profile = mapper.toProfile(character(1, 1, "Other"), questions);

        assertNull(profile.attributes.get("origin_other"));
        assertTrue(questions.keySet().stream().noneMatch(k -> k.startsWith("origin_")));
    }

    @Test
    public void curatedData_setsTeamsPowersVillainAndMainstream() {
        Map<String, String> questions = new LinkedHashMap<>();
        CharacterProfile hero = mapper.toProfile(character(1, 1, null), questions);

        assertEquals(CharacterMapper.YES, hero.attributes.get("team_xmen"), 1e-9);
        assertEquals(CharacterMapper.NO, hero.attributes.get("team_avengers"), 1e-9);
        assertEquals(CharacterMapper.YES, hero.attributes.get("power_voo"), 1e-9);
        assertNull("poder não listado fica ausente (crença padrão do motor)", hero.attributes.get("power_magia"));
        assertTrue(hero.isMainstream);

        CharacterProfile antiHero = mapper.toProfile(character(2, 1, null), questions);
        assertEquals("crença graduada de vilania vem da curadoria", 0.3, antiHero.attributes.get("is_villain"), 1e-9);
        assertFalse(antiHero.isMainstream);
    }

    @Test
    public void characterOutsideRoster_stillAnswersTeamsAndVillain() {
        Map<String, String> questions = new LinkedHashMap<>();
        CharacterProfile profile = mapper.toProfile(character(999999, 1, "Human"), questions);

        assertNotNull(profile.attributes.get("is_villain"));
        for (String team : QuestionKeys.TEAMS) {
            assertNotNull(profile.attributes.get("team_" + team));
        }
    }

    @Test
    public void keysWithoutText_areSkipped() {
        CharacterMapper noTexts = new CharacterMapper(RosterCatalog.parse(new StringReader(ROSTER)),
                new LinkedHashMap<>());
        Map<String, String> questions = new LinkedHashMap<>();
        CharacterProfile profile = noTexts.toProfile(character(1, 1, "Mutant"), questions);

        assertTrue(profile.attributes.isEmpty());
        assertTrue(questions.isEmpty());
    }
}
