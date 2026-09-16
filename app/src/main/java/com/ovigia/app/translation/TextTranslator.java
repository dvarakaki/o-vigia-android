package com.ovigia.app.translation;

/**
 * Tradutor de textos do inglês — idioma em que a Comic Vine devolve tudo — para
 * o idioma do app. Bloqueante: chamar fora da main thread.
 *
 * Abstraído para os testes rodarem na JVM, sem o tradutor do aparelho.
 */
public interface TextTranslator {

    /** Existe tradução do inglês para {@code language} (ISO 639-1, ex.: {@code "pt"}). */
    boolean supports(String language);

    /**
     * Abre uma sessão para {@code language}, baixando o pacote de tradução se
     * ainda não estiver no aparelho.
     *
     * @param allowMeteredDownload baixar mesmo em rede móvel (o pacote tem dezenas de MB)
     * @throws TranslationException {@link TranslationException.Reason#NEEDS_DOWNLOAD} quando
     *     falta o pacote e a rede é limitada sem autorização do jogador
     */
    Session open(String language, boolean allowMeteredDownload) throws TranslationException;

    /** Sessão de tradução; fechar ao terminar para liberar o modelo da memória. */
    interface Session extends AutoCloseable {

        String translate(String text) throws TranslationException;

        @Override
        void close();
    }
}
