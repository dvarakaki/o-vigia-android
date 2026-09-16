package com.ovigia.app.game;

import com.ovigia.app.data.CharacterRepository.LoadError;

/** Estado da partida exposto às telas. Imutável. */
public final class GameUiState {

    public enum Phase {
        /** Carregando o elenco. */
        LOADING,
        /** Falhou ao carregar; ver {@link #error}. */
        ERROR,
        /** Há uma pergunta na tela esperando resposta. */
        ASKING,
        /** O motor chutou ou ofereceu alternativas: a decisão está em outra tela. */
        DECIDING,
        /** Partida encerrada. */
        FINISHED
    }

    public final Phase phase;
    public final LoadError error;
    public final String questionText;
    public final int questionNumber;
    public final boolean canGoBack;
    /** Como o Vigia está reagindo à partida — escolhe o sprite das telas. */
    public final WatcherMood mood;

    private GameUiState(Phase phase, LoadError error, String questionText, int questionNumber, boolean canGoBack,
                        WatcherMood mood) {
        this.phase = phase;
        this.error = error;
        this.questionText = questionText;
        this.questionNumber = questionNumber;
        this.canGoBack = canGoBack;
        this.mood = mood;
    }

    static GameUiState loading() {
        return new GameUiState(Phase.LOADING, null, null, 0, false, WatcherMood.THINKING);
    }

    static GameUiState error(LoadError error) {
        return new GameUiState(Phase.ERROR, error, null, 0, false, WatcherMood.THINKING);
    }

    static GameUiState asking(String questionText, int questionNumber, boolean canGoBack, WatcherMood mood) {
        return new GameUiState(Phase.ASKING, null, questionText, questionNumber, canGoBack, mood);
    }

    static GameUiState deciding(WatcherMood mood) {
        return new GameUiState(Phase.DECIDING, null, null, 0, false, mood);
    }

    static GameUiState finished(WatcherMood mood) {
        return new GameUiState(Phase.FINISHED, null, null, 0, false, mood);
    }

    /** O elenco está carregado (perfis e alternativas podem ser consultados). */
    public boolean isLoaded() {
        return phase == Phase.ASKING || phase == Phase.DECIDING || phase == Phase.FINISHED;
    }
}
