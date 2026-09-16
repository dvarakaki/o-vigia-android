package com.ovigia.app.profile;

import android.net.Uri;

import com.ovigia.app.auth.AccountStore.ImageKind;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** {@link ProfileImages} sem decodificar nada: só nomes de arquivo e registro do que foi apagado. */
public final class FakeProfileImages implements ProfileImages {

    private final File directory;
    private int counter = 0;
    boolean failNextImport = false;
    final List<String> deleted = new ArrayList<>();
    /** Nome do arquivo -> Base64 gravado por {@link #saveShared}. */
    public final Map<String, String> saved = new HashMap<>();

    public FakeProfileImages(File directory) {
        this.directory = directory;
    }

    @Override
    public String importImage(Uri source, ImageKind kind, String accountId) throws IOException {
        if (failNextImport) {
            failNextImport = false;
            throw new IOException("imagem ilegível");
        }
        return kind.name().toLowerCase() + "_" + accountId + "_" + (++counter) + ".jpg";
    }

    /** Não decodifica nada: guarda o Base64 recebido e devolve um nome novo. */
    @Override
    public String saveShared(String base64, ImageKind kind, String accountId) throws IOException {
        if (failNextImport) {
            failNextImport = false;
            throw new IOException("imagem ilegível");
        }
        String name = kind.name().toLowerCase() + "_" + accountId + "_" + (++counter) + ".jpg";
        saved.put(name, base64);
        return name;
    }

    @Override
    public File file(String fileName) {
        return fileName == null ? null : new File(directory, fileName);
    }

    @Override
    public void delete(String fileName) {
        if (fileName != null) deleted.add(fileName);
    }

    /** Não reduz nada: devolve um texto que identifica arquivo e tamanho pedidos. */
    @Override
    public String encodeForSharing(String fileName, int maxPx) {
        return fileName == null ? null : "shared:" + fileName + "@" + maxPx;
    }
}
