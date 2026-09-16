package com.ovigia.app.translation;

/** Falha ao traduzir um texto da Comic Vine. */
public final class TranslationException extends Exception {

    public enum Reason {
        /** O pacote de tradução ainda não está no aparelho e a rede é limitada: esperar o jogador decidir. */
        NEEDS_DOWNLOAD,
        /** Sem internet para baixar o pacote de tradução. */
        NO_CONNECTION,
        /** Qualquer outro problema (tradutor indisponível, tempo esgotado…). */
        FAILED
    }

    public final Reason reason;

    public TranslationException(Reason reason) {
        this(reason, null);
    }

    public TranslationException(Reason reason, Throwable cause) {
        super(reason.name(), cause);
        this.reason = reason;
    }
}
