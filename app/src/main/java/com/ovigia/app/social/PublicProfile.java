package com.ovigia.app.social;

import androidx.annotation.Nullable;

import java.util.Collections;
import java.util.List;

/**
 * Perfil completo que só amigos veem: cartão, bio, banner, números, heróis
 * desbloqueados e conquistas. Imutável.
 */
public final class PublicProfile {

    public final UserCard card;
    @Nullable public final String bio;
    /** Banner reduzido em JPEG codificado em Base64; {@code null} usa a galáxia padrão. */
    @Nullable public final String banner;
    public final int gamesPlayed;
    public final int engineWins;
    public final int distinctCharacters;
    /** Do mais recente para o mais antigo. */
    public final List<Hero> heroes;
    /** Na ordem do enum {@link Achievement}. */
    public final List<AchievementProgress> achievements;
    /** Quando o dono publicou por último (millis), ou 0 se não se sabe. */
    public final long updatedAt;

    public PublicProfile(UserCard card, @Nullable String bio, @Nullable String banner, int gamesPlayed,
                         int engineWins, int distinctCharacters, List<Hero> heroes,
                         List<AchievementProgress> achievements, long updatedAt) {
        this.card = card;
        this.bio = bio;
        this.banner = banner;
        this.gamesPlayed = gamesPlayed;
        this.engineWins = engineWins;
        this.distinctCharacters = distinctCharacters;
        this.heroes = Collections.unmodifiableList(heroes);
        this.achievements = Collections.unmodifiableList(achievements);
        this.updatedAt = updatedAt;
    }

    public int playerWins() {
        return Math.max(0, gamesPlayed - engineWins);
    }

    /** Herói desbloqueado, com nome e imagem do momento do desbloqueio. */
    public static final class Hero {
        public final int characterId;
        public final String name;
        @Nullable public final String imageUrl;
        public final long unlockedAt;

        public Hero(int characterId, String name, @Nullable String imageUrl, long unlockedAt) {
            this.characterId = characterId;
            this.name = name;
            this.imageUrl = imageUrl;
            this.unlockedAt = unlockedAt;
        }
    }
}
