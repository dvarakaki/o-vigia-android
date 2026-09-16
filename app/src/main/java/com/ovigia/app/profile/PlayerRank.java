package com.ovigia.app.profile;

/** Título do jogador no perfil, pela quantidade de partidas jogadas. */
public enum PlayerRank {
    NEWCOMER(0),
    CURIOUS(1),
    CHALLENGER(5),
    VETERAN(20),
    LEGEND(50);

    /** Partidas necessárias para alcançar este título. */
    public final int minGames;

    PlayerRank(int minGames) {
        this.minGames = minGames;
    }

    public static PlayerRank forGames(int gamesPlayed) {
        PlayerRank rank = NEWCOMER;
        for (PlayerRank r : values()) {
            if (gamesPlayed >= r.minGames) rank = r;
        }
        return rank;
    }

    /** Próximo título, ou {@code null} se este já é o último. */
    public PlayerRank next() {
        PlayerRank[] all = values();
        return ordinal() + 1 < all.length ? all[ordinal() + 1] : null;
    }
}
