package com.ovigia.app.social;

/** Pedido de amizade pendente de {@link #from} para {@link #to}. Imutável. */
public final class FriendRequest {

    /** Cartões como estavam quando o pedido foi enviado. */
    public final UserCard from;
    public final UserCard to;
    public final long createdAt;

    public FriendRequest(UserCard from, UserCard to, long createdAt) {
        this.from = from;
        this.to = to;
        this.createdAt = createdAt;
    }
}
