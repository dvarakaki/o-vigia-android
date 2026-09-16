package com.ovigia.app.profile;

import android.net.Uri;

import com.ovigia.app.auth.AccountStore.ImageKind;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** {@link ProfileImages} sem decodificar nada: só nomes de arquivo e registro do que foi apagado. */
public final class FakeProfileImages implements ProfileImages {

    private final File directory;
    private int counter = 0;
    boolean failNextImport = false;
    final List<String> deleted = new ArrayList<>();

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
