package com.ovigia.app.social;

import androidx.annotation.Nullable;

import com.ovigia.app.profile.PlayerRank;

import java.util.Collections;
import java.util.Set;

/** Estado do perfil de um amigo. Imutável. */
public final class FriendProfileUiState {

    public enum Status {
        LOADING,
        READY,
        /** Não carregou; ver {@link #error} ({@code PERMISSION_DENIED}: não são mais amigos). */
        ERROR,
        /** A amizade foi desfeita nesta tela: ela deve fechar. */
        REMOVED
    }

    public final Status status;
    @Nullable public final PublicProfile profile;
    /** Heróis que o jogador também desbloqueou (esses abrem a ficha). */
    public final Set<Integer> myUnlockedIds;
    @Nullable public final SocialException.Error error;
    /** Desfazendo a amizade. */
    public final boolean removing;

    FriendProfileUiState(Status status, @Nullable PublicProfile profile, Set<Integer> myUnlockedIds,
                         @Nullable SocialException.Error error, boolean removing) {
        this.status = status;
        this.profile = profile;
        this.myUnlockedIds = Collections.unmodifiableSet(myUnlockedIds);
        this.error = error;
        this.removing = removing;
    }

    static FriendProfileUiState loading() {
        return new FriendProfileUiState(Status.LOADING, null, Collections.emptySet(), null, false);
    }

    public PlayerRank rank() {
        return PlayerRank.forGames(profile == null ? 0 : profile.gamesPlayed);
    }
}
