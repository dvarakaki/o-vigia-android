package com.ovigia.app.profile;

import android.net.Uri;

import com.ovigia.app.auth.AccountStore.ImageKind;

import java.io.File;
import java.io.IOException;

/**
 * Pasta das imagens do perfil (foto e banner). Abstraída para os ViewModels
 * serem testados na JVM sem decodificar imagens de verdade.
 *
 * Operações bloqueantes: chamar fora da main thread.
 */
public interface ProfileImages {

    /**
     * Copia a imagem escolhida pelo jogador para a pasta do app, já reduzida ao
     * tamanho de exibição. Devolve o nome do arquivo criado.
     */
    String importImage(Uri source, ImageKind kind, String accountId) throws IOException;

    /** Arquivo de um nome devolvido por {@link #importImage}, ou {@code null} para {@code null}. */
    File file(String fileName);

    /** Apaga o arquivo, se existir. Ignora {@code null}. */
    void delete(String fileName);

    /**
     * Versão reduzida da imagem para publicar online: JPEG com no máximo
     * {@code maxPx} no lado maior, codificado em Base64. {@code null} se não há
     * arquivo ou ele não pôde ser lido.
     */
    String encodeForSharing(String fileName, int maxPx);
}
