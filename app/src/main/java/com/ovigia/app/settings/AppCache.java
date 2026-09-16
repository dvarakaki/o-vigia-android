package com.ovigia.app.settings;

/**
 * Arquivos baixados que podem ser apagados sem perder nada do jogador: voltam a
 * ser baixados quando alguma tela precisar. Bloqueante: chamar fora da main thread.
 */
public interface AppCache {

    long sizeBytes();

    void clear();
}
