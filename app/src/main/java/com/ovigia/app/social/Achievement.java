package com.ovigia.app.social;

/**
 * Conquistas do jogador. Cada uma mede uma coisa ({@link Metric}) e desbloqueia
 * ao atingir {@link #target}. O nome do enum é o id gravado no servidor: não
 * renomear (ids desconhecidos vindos de outra versão são ignorados).
 */
public enum Achievement {
    FIRST_HERO(Metric.HEROES, null, 1),
    HEROES_10(Metric.HEROES, null, 10),
    HEROES_25(Metric.HEROES, null, 25),
    HEROES_50(Metric.HEROES, null, 50),
    AVENGERS_5(Metric.TEAM, "avengers", 5),
    XMEN_5(Metric.TEAM, "xmen", 5),
    GUARDIANS_3(Metric.TEAM, "guardians", 3),
    FANTASTIC_FOUR(Metric.TEAM, "f4", 4),
    VILLAINS_5(Metric.VILLAINS, null, 5),
    GAMES_10(Metric.GAMES, null, 10),
    GAMES_50(Metric.GAMES, null, 50),
    BEAT_WATCHER(Metric.PLAYER_WINS, null, 1),
    BEAT_WATCHER_10(Metric.PLAYER_WINS, null, 10);

    public enum Metric {
        /** Heróis desbloqueados no catálogo. */
        HEROES,
        /** Heróis desbloqueados de uma equipe do roster.json ({@link #team}). */
        TEAM,
        /** Heróis desbloqueados que são vilões (crença de vilania ≥ 0,5). */
        VILLAINS,
        /** Partidas jogadas. */
        GAMES,
        /** Partidas em que o Vigia não acertou de primeira. */
        PLAYER_WINS
    }

    public final Metric metric;
    /** Chave da equipe no roster.json quando {@link #metric} é {@link Metric#TEAM}. */
    public final String team;
    public final int target;

    Achievement(Metric metric, String team, int target) {
        this.metric = metric;
        this.team = team;
        this.target = target;
    }

    /** O enum com esse id, ou {@code null} se o id não existe nesta versão do app. */
    public static Achievement fromId(String id) {
        if (id == null) return null;
        for (Achievement a : values()) {
            if (a.name().equals(id)) return a;
        }
        return null;
    }
}
