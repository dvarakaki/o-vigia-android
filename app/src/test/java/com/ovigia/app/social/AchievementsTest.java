package com.ovigia.app.social;

import com.ovigia.app.data.roster.RosterCatalog;

import org.junit.Test;

import java.io.StringReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AchievementsTest {

    private static final RosterCatalog ROSTER = RosterCatalog.parse(new StringReader("{\"characters\":["
            + "{\"id\":1,\"teams\":[\"avengers\"],\"powers\":[\"voo\"],\"villain\":0.08},"
            + "{\"id\":2,\"teams\":[\"avengers\",\"xmen\"],\"powers\":[\"voo\"],\"villain\":0.2},"
            + "{\"id\":3,\"teams\":[\"f4\"],\"powers\":[\"voo\"],\"villain\":0.9},"
            + "{\"id\":4,\"teams\":[],\"powers\":[\"voo\"],\"villain\":0.5}"
            + "]}"));

    private static AchievementProgress find(List<AchievementProgress> list, Achievement a) {
        for (AchievementProgress p : list) {
            if (p.achievement == a) return p;
        }
        throw new AssertionError(a + " não está na lista");
    }

    @Test
    public void everyAchievementIsEvaluated_inEnumOrder() {
        List<AchievementProgress> progress = Achievements.evaluate(Collections.emptyList(), ROSTER, 0, 0);
        assertEquals(Achievement.values().length, progress.size());
        for (int i = 0; i < progress.size(); i++) {
            assertEquals(Achievement.values()[i], progress.get(i).achievement);
            assertFalse(progress.get(i).isUnlocked());
        }
    }

    @Test
    public void heroesTeamsAndVillains_comeFromTheRoster() {
        List<AchievementProgress> progress = Achievements.evaluate(Arrays.asList(1, 2, 3, 4, 999), ROSTER, 0, 0);

        assertTrue(find(progress, Achievement.FIRST_HERO).isUnlocked());
        assertEquals("id fora do roster conta como herói", 5, find(progress, Achievement.HEROES_10).current);
        assertEquals(2, find(progress, Achievement.AVENGERS_5).current);
        assertEquals(1, find(progress, Achievement.XMEN_5).current);
        assertEquals(1, find(progress, Achievement.FANTASTIC_FOUR).current);
        assertEquals("vilania 0,5 já conta", 2, find(progress, Achievement.VILLAINS_5).current);
    }

    @Test
    public void gamesAndWins_unlockAtTheTarget_andProgressIsCapped() {
        List<AchievementProgress> progress = Achievements.evaluate(Collections.emptyList(), null, 120, 1);

        AchievementProgress games50 = find(progress, Achievement.GAMES_50);
        assertTrue(games50.isUnlocked());
        assertEquals(50, games50.current);
        assertEquals(100, games50.percent());
        assertTrue(find(progress, Achievement.BEAT_WATCHER).isUnlocked());
        assertEquals(10, find(progress, Achievement.BEAT_WATCHER_10).percent());
        assertEquals("sem roster, equipes ficam zeradas", 0, find(progress, Achievement.AVENGERS_5).current);
        assertEquals(3, Achievements.unlockedCount(progress));
    }

    @Test
    public void publishedFormat_roundTrips_andIgnoresUnknownIds() {
        List<AchievementProgress> original = Achievements.evaluate(Arrays.asList(1, 2), ROSTER, 12, 3);
        Map<String, Integer> published = new HashMap<>(Achievements.toPublished(original));
        published.put("CONQUISTA_DO_FUTURO", 7);

        List<AchievementProgress> read = Achievements.fromPublished(published);
        assertEquals(original.size(), read.size());
        for (int i = 0; i < read.size(); i++) {
            assertEquals(original.get(i).achievement, read.get(i).achievement);
            assertEquals(original.get(i).current, read.get(i).current);
        }
        assertEquals(0, Achievements.fromPublished(null).get(0).current);
    }
}
