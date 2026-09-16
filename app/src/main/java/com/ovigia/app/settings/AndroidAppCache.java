package com.ovigia.app.settings;

import android.content.Context;
import android.util.Log;

import com.bumptech.glide.Glide;

import java.io.File;
import java.util.function.Supplier;

/**
 * Cache limpável: imagens do Glide, fichas de heróis salvas e as traduções
 * delas. O cache do elenco fica de fora de propósito — sem ele a partida não
 * abre offline; o pacote do tradutor também, porque o sistema o guarda fora do
 * app e baixá-lo de novo custa dezenas de MB.
 */
public final class AndroidAppCache implements AppCache {

    private static final String TAG = "AppCache";

    private final Context app;
    private final Supplier<File> heroDetailsDirectory;
    private final Supplier<File> heroTranslationsDirectory;

    public AndroidAppCache(Context context, Supplier<File> heroDetailsDirectory,
                           Supplier<File> heroTranslationsDirectory) {
        this.app = context.getApplicationContext();
        this.heroDetailsDirectory = heroDetailsDirectory;
        this.heroTranslationsDirectory = heroTranslationsDirectory;
    }

    @Override
    public long sizeBytes() {
        return size(Glide.getPhotoCacheDir(app)) + size(heroDetailsDirectory.get())
                + size(heroTranslationsDirectory.get());
    }

    @Override
    public void clear() {
        Glide.get(app).clearDiskCache();
        deleteFiles(heroDetailsDirectory.get());
        deleteFiles(heroTranslationsDirectory.get());
    }

    private static void deleteFiles(File directory) {
        File[] files = directory.listFiles();
        if (files == null) return;
        for (File file : files) {
            if (file.isFile() && !file.delete()) Log.w(TAG, "Falha ao apagar " + file.getName());
        }
    }

    private static long size(File file) {
        if (file == null || !file.exists()) return 0;
        if (file.isFile()) return file.length();
        long total = 0;
        File[] children = file.listFiles();
        if (children != null) {
            for (File child : children) total += size(child);
        }
        return total;
    }
}
