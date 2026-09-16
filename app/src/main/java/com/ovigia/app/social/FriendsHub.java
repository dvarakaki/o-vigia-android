package com.ovigia.app.social;

import androidx.annotation.Nullable;

import java.util.Collections;
import java.util.List;

/** Amigos e pedidos pendentes da conta conectada. Imutável. */
public final class FriendsHub {

    public static final FriendsHub EMPTY = new FriendsHub(Collections.emptyList(), Collections.emptyList(),
            Collections.emptyList());

    public final List<UserCard> friends;
    /** Pedidos que outros jogadores mandaram para esta conta. */
    public final List<FriendRequest> incoming;
    /** Pedidos que esta conta mandou e ainda não foram respondidos. */
    public final List<FriendRequest> outgoing;

    public FriendsHub(List<UserCard> friends, List<FriendRequest> incoming, List<FriendRequest> outgoing) {
        this.friends = Collections.unmodifiableList(friends);
        this.incoming = Collections.unmodifiableList(incoming);
        this.outgoing = Collections.unmodifiableList(outgoing);
    }

    /** Como esta conta está ligada a {@code uid}. */
    public Relationship relationshipWith(String myUid, String uid) {
        if (uid.equals(myUid)) return Relationship.SELF;
        for (UserCard f : friends) {
            if (f.uid.equals(uid)) return Relationship.FRIENDS;
        }
        if (incomingFrom(uid) != null) return Relationship.REQUEST_RECEIVED;
        for (FriendRequest r : outgoing) {
            if (r.to.uid.equals(uid)) return Relationship.REQUEST_SENT;
        }
        return Relationship.NONE;
    }

    @Nullable
    public FriendRequest incomingFrom(String uid) {
        for (FriendRequest r : incoming) {
            if (r.from.uid.equals(uid)) return r;
        }
        return null;
    }

    public enum Relationship {
        SELF,
        NONE,
        REQUEST_SENT,
        REQUEST_RECEIVED,
        FRIENDS
    }
}
