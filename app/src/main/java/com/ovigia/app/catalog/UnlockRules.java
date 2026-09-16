package com.ovigia.app.catalog;

import com.ovigia.app.learning.LearningStore.Outcome;

/** Quando um herói entra no catálogo do jogador. */
public final class UnlockRules {

    /**
     * O herói é desbloqueado quando o Vigia acerta: de primeira ou entre as
     * alternativas que ofereceu. Se o Vigia perdeu e o jogador só revelou em
     * quem pensou, o herói continua bloqueado.
     */
    public static boolean unlocks(Outcome outcome) {
        return outcome == Outcome.ENGINE_GUESSED || outcome == Outcome.PICKED_FROM_ALTERNATIVES;
    }

    private UnlockRules() { }
}
