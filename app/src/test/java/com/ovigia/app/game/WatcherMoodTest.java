package com.ovigia.app.game;

import com.ovigia.app.learning.LearningStore.Outcome;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/** Regras de reação do Vigia, sem motor nem Android. */
public class WatcherMoodTest {

    @Test
    public void restingMood_followsTheLeadersProbability() {
        assertEquals(WatcherMood.THINKING, WatcherMood.forConfidence(0.05));
        assertEquals(WatcherMood.FOCUSED, WatcherMood.forConfidence(WatcherMood.FOCUSED_AT));
        assertEquals(WatcherMood.CONFIDENT, WatcherMood.forConfidence(WatcherMood.CONFIDENT_AT));
        assertEquals(WatcherMood.CONFIDENT, WatcherMood.forConfidence(0.95));
    }

    @Test
    public void highExpectationCrushed_isAngry() {
        assertEquals(WatcherMood.ANGRY, WatcherMood.afterAnswer(0.7, 0.1, 0.5));
    }

    @Test
    public void angerWinsEvenIfAnotherFavoriteTakesOver() {
        assertEquals("a aposta dele quebrou, mesmo com outro líder forte",
                WatcherMood.ANGRY, WatcherMood.afterAnswer(0.6, 0.05, 0.9));
    }

    @Test
    public void highExpectationDented_isOnlySkeptical() {
        // Manteve 45%: abaixo do limiar de desconfiança, acima do de raiva.
        assertEquals(WatcherMood.SKEPTICAL, WatcherMood.afterAnswer(0.6, 0.27, 0.3));
    }

    @Test
    public void promisingFavoriteLost_isSkepticalNotAngry() {
        assertEquals(WatcherMood.SKEPTICAL, WatcherMood.afterAnswer(0.3, 0.02, 0.2));
    }

    @Test
    public void smallDrop_doesNotProvokeAReaction() {
        assertEquals(WatcherMood.CONFIDENT, WatcherMood.afterAnswer(0.6, 0.5, 0.5));
    }

    @Test
    public void withoutAFavorite_thereIsNothingToBreak() {
        assertEquals(WatcherMood.THINKING, WatcherMood.afterAnswer(0.1, 0.0, 0.1));
    }

    @Test
    public void risingConfidence_showsWithoutReaction() {
        assertEquals(WatcherMood.CONFIDENT, WatcherMood.afterAnswer(0.3, 0.8, 0.8));
    }

    @Test
    public void guessPosture_dependsOnConviction() {
        assertEquals(WatcherMood.CONFIDENT, WatcherMood.forGuess(0.9));
        assertEquals(WatcherMood.FOCUSED, WatcherMood.forGuess(0.15));
    }

    @Test
    public void rejectedGuess_angersOnlyWhenHeWasSure() {
        assertEquals(WatcherMood.ANGRY, WatcherMood.afterRejectedGuess(0.9));
        assertEquals(WatcherMood.SKEPTICAL, WatcherMood.afterRejectedGuess(0.15));
    }

    @Test
    public void outcome_mapsToTheResultPose() {
        assertEquals(WatcherMood.TRIUMPHANT, WatcherMood.forOutcome(Outcome.ENGINE_GUESSED));
        assertEquals(WatcherMood.CONFIDENT, WatcherMood.forOutcome(Outcome.PICKED_FROM_ALTERNATIVES));
        assertEquals(WatcherMood.SKEPTICAL, WatcherMood.forOutcome(Outcome.REVEALED_AFTER_LOSS));
        assertEquals(WatcherMood.THINKING, WatcherMood.forOutcome(null));
    }
}
