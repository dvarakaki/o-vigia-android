package com.ovigia.app.social;

import androidx.annotation.Nullable;

/**
 * Cartão público de um jogador: o que qualquer conta online vê numa busca ou
 * num pedido de amizade. Imutável.
 */
public final class UserCard {

    public final String uid;
    public final String username;
    public final String name;
    /** Miniatura da foto em JPEG codificado em Base64; {@code null} usa o ícone padrão. */
    @Nullable public final String avatar;

    public UserCard(String uid, String username, String name, @Nullable String avatar) {
        this.uid = uid;
        this.username = username;
        this.name = name;
        this.avatar = avatar;
    }
}
