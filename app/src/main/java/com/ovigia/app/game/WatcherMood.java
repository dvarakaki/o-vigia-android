package com.ovigia.app.game;

import androidx.annotation.Nullable;

import com.ovigia.app.learning.LearningStore.Outcome;

/**
 * Humor do Vigia durante a partida — escolhe o sprite mostrado nas telas.
 *
 * O humor "de repouso" acompanha a confiança do motor (a probabilidade do
 * personagem que lidera). Por cima disso vêm as reações: quando uma resposta
 * derruba um favorito em que o Vigia já apostava, ou quando o jogador rejeita
 * um chute, ele fica desconfiado ou irritado conforme o tamanho da expectativa
 * quebrada. A reação dura até a próxima resposta com evidência.
 */
public enum WatcherMood {
    /** Sem favorito: lendo a mente do jogador. */
    THINKING,
    /** Um favorito começa a se destacar. */
    FOCUSED,
    /** Expectativa alta: acha que já sabe quem é. */
    CONFIDENT,
    /** Decepção: um favorito promissor perdeu força, ou um chute morno foi rejeitado. */
    SKEPTICAL,
    /** Expectativa alta quebrada: o favorito despencou, ou um chute confiante foi rejeitado. */
    ANGRY,
    /** Acertou o personagem. */
    TRIUMPHANT;

    // Limiares calibrados simulando partidas no roster.json com um jogador ruidoso:
    // o Vigia passa ~metade da partida pensando, se irrita por resposta em ~1 de
    // cada 6 partidas e desconfia ~1 vez por partida. Em ~90% das reações o
    // favorito derrubado não era mesmo o personagem do jogador.

    /** Líder a partir desta probabilidade: o Vigia já tem um favorito. */
    static final double FOCUSED_AT = 0.20;

    /**
     * Líder a partir desta probabilidade: expectativa alta. Abaixo do limiar de
     * chute do motor de propósito — o Vigia "acha que sabe" algumas perguntas
     * antes de arriscar, e é aí que uma resposta pode quebrar a expectativa.
     */
    static final double CONFIDENT_AT = 0.40;

    /**
     * Quanto da probabilidade o favorito precisa manter para não haver reação:
     * abaixo de {@code SKEPTICAL_KEEPS}× o valor de antes o Vigia desconfia;
     * abaixo de {@code ANGRY_KEEPS}×, com expectativa alta, ele se irrita.
     */
    static final double SKEPTICAL_KEEPS = 0.5;
    static final double ANGRY_KEEPS = 0.4;

    /** Humor para a confiança atual, sem reação a nada. */
    static WatcherMood forConfidence(double leaderProbability) {
        if (leaderProbability >= CONFIDENT_AT) return CONFIDENT;
        if (leaderProbability >= FOCUSED_AT) return FOCUSED;
        return THINKING;
    }

    /**
     * Reação a uma resposta com evidência.
     *
     * @param expectation     probabilidade do líder ANTES da resposta
     * @param formerLeader    probabilidade desse mesmo personagem DEPOIS da resposta
     * @param leaderAfter     probabilidade do líder (talvez outro) depois da resposta
     */
    static WatcherMood afterAnswer(double expectation, double formerLeader, double leaderAfter) {
        if (expectation >= CONFIDENT_AT && formerLeader < expectation * ANGRY_KEEPS) return ANGRY;
        if (expectation >= FOCUSED_AT && formerLeader < expectation * SKEPTICAL_KEEPS) return SKEPTICAL;
        return forConfidence(leaderAfter);
    }

    /** Postura ao apresentar um chute: convicto, ou só arriscando porque as perguntas acabaram. */
    static WatcherMood forGuess(double guessProbability) {
        return guessProbability >= CONFIDENT_AT ? CONFIDENT : FOCUSED;
    }

    /** Reação ao chute rejeitado, pelo tanto que o Vigia confiava nele. */
    static WatcherMood afterRejectedGuess(double guessProbability) {
        return guessProbability >= CONFIDENT_AT ? ANGRY : SKEPTICAL;
    }

    /** Humor na tela de resultado. */
    public static WatcherMood forOutcome(@Nullable Outcome outcome) {
        if (outcome == null) return THINKING;
        switch (outcome) {
            case ENGINE_GUESSED: return TRIUMPHANT;
            // "Ufa! Era ele mesmo!": estava na lista dele.
            case PICKED_FROM_ALTERNATIVES: return CONFIDENT;
            // "Da próxima vez eu acerto."
            case REVEALED_AFTER_LOSS:
            case LOST_UNREVEALED:
            default: return SKEPTICAL;
        }
    }
}
