package com.ovigia.app.social;

/** Quanto falta para uma {@link Achievement}. Imutável. */
public final class AchievementProgress {

    public final Achievement achievement;
    /** Valor atual, limitado ao alvo (uma conquista não passa de 100%). */
    public final int current;

    public AchievementProgress(Achievement achievement, int current) {
        this.achievement = achievement;
        this.current = Math.max(0, Math.min(current, achievement.target));
    }

    public boolean isUnlocked() {
        return current >= achievement.target;
    }

    /** Progresso em [0,100]. */
    public int percent() {
        return 100 * current / achievement.target;
    }
}
