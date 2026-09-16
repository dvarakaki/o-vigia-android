package com.ovigia.app.social;

import androidx.annotation.Nullable;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/** Estado da aba de amigos. Imutável: cada mudança gera uma cópia com {@code with…}. */
public final class FriendsUiState {

    public enum Status {
        LOADING,
        NOT_CONFIGURED,
        /** Sem conta local: a tela manda para o login. */
        SIGNED_OUT,
        /** Pedir a senha para conectar a conta online. */
        NEEDS_CONNECTION,
        /** Escolher o @usuario. */
        NEEDS_USERNAME,
        READY,
        /** Conectado, mas amigos e pedidos não carregaram ({@link #error}). */
        ERROR
    }

    public final Status status;
    /** Enviando um formulário (conectar, @usuario) ou recarregando. */
    public final boolean working;
    /** Erro do formulário atual ou da carga. */
    @Nullable public final SocialException.Error error;
    @Nullable public final UserCard me;
    /** Sugestão para o campo de @usuario. */
    public final String suggestedUsername;
    public final FriendsHub hub;
    public final Search search;
    /** Contas com uma ação em andamento (os botões da linha ficam desabilitados). */
    public final Set<String> busyUids;

    private FriendsUiState(Status status, boolean working, @Nullable SocialException.Error error,
                           @Nullable UserCard me, String suggestedUsername, FriendsHub hub, Search search,
                           Set<String> busyUids) {
        this.status = status;
        this.working = working;
        this.error = error;
        this.me = me;
        this.suggestedUsername = suggestedUsername;
        this.hub = hub;
        this.search = search;
        this.busyUids = Collections.unmodifiableSet(busyUids);
    }

    static FriendsUiState of(Status status) {
        return new FriendsUiState(status, false, null, null, "", FriendsHub.EMPTY, Search.IDLE,
                Collections.emptySet());
    }

    FriendsUiState withStatus(Status newStatus) {
        return new FriendsUiState(newStatus, working, error, me, suggestedUsername, hub, search, busyUids);
    }

    FriendsUiState withWorking(boolean newWorking) {
        return new FriendsUiState(status, newWorking, newWorking ? null : error, me, suggestedUsername, hub,
                search, busyUids);
    }

    FriendsUiState withError(@Nullable SocialException.Error newError) {
        return new FriendsUiState(status, false, newError, me, suggestedUsername, hub, search, busyUids);
    }

    FriendsUiState withMe(@Nullable UserCard newMe) {
        return new FriendsUiState(status, working, error, newMe, suggestedUsername, hub, search, busyUids);
    }

    FriendsUiState withSuggestion(String suggestion) {
        return new FriendsUiState(status, working, error, me, suggestion, hub, search, busyUids);
    }

    FriendsUiState withHub(FriendsHub newHub) {
        Search updated = search.result == null || me == null ? search
                : search.withRelationship(newHub.relationshipWith(me.uid, search.result.uid));
        return new FriendsUiState(status, working, error, me, suggestedUsername, newHub, updated, busyUids);
    }

    FriendsUiState withSearch(Search newSearch) {
        return new FriendsUiState(status, working, error, me, suggestedUsername, hub, newSearch, busyUids);
    }

    FriendsUiState withBusy(String uid, boolean busy) {
        Set<String> copy = new HashSet<>(busyUids);
        if (busy) copy.add(uid); else copy.remove(uid);
        return new FriendsUiState(status, working, error, me, suggestedUsername, hub, search, copy);
    }

    /** Busca por @usuario. Imutável. */
    public static final class Search {
        static final Search IDLE = new Search(false, null, null, null);

        public final boolean searching;
        @Nullable public final UserCard result;
        @Nullable public final FriendsHub.Relationship relationship;
        /** {@code NOT_FOUND}, {@code USERNAME_INVALID}, {@code OFFLINE}… */
        @Nullable public final SocialException.Error error;

        Search(boolean searching, @Nullable UserCard result, @Nullable FriendsHub.Relationship relationship,
               @Nullable SocialException.Error error) {
            this.searching = searching;
            this.result = result;
            this.relationship = relationship;
            this.error = error;
        }

        Search withRelationship(FriendsHub.Relationship newRelationship) {
            return new Search(searching, result, newRelationship, error);
        }
    }

    /** Aviso curto depois de uma ação. */
    public enum Message {
        REQUEST_SENT,
        BECAME_FRIENDS,
        REQUEST_DECLINED,
        REQUEST_CANCELED,
        USERNAME_SAVED,
        CONNECTED,
        ACTION_FAILED_OFFLINE,
        ACTION_FAILED
    }
}
