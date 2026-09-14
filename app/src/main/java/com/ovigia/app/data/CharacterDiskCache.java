package com.ovigia.app.data;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.ovigia.app.model.Character;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Guarda o elenco (bruto, como a Comic Vine devolve) em um arquivo local,
 * pra não depender de rede — nem do limite de requisições da API — toda vez
 * que o app abre. Uma vez que o elenco foi buscado com sucesso uma vez, o
 * jogo funciona offline dali em diante até o cache expirar.
 */
final class CharacterDiskCache {

    private static final String TAG = "CharacterDiskCache";
    private static final String FILE_NAME = "characters_cache.json";
    private static final long TTL_MILLIS = TimeUnit.DAYS.toMillis(7);
    private static final Type LIST_TYPE = new TypeToken<List<Character>>() { }.getType();

    private final File file;
    private final Gson gson = new Gson();

    CharacterDiskCache(Context context) {
        this.file = new File(context.getApplicationContext().getFilesDir(), FILE_NAME);
    }

    /** Devolve o elenco salvo, mesmo se estiver "velho" — útil como último recurso sem rede. */
    List<Character> readStale() {
        if (!file.exists()) return null;
        try (FileReader reader = new FileReader(file)) {
            List<Character> characters = gson.fromJson(reader, LIST_TYPE);
            return (characters == null || characters.isEmpty()) ? null : characters;
        } catch (IOException | RuntimeException e) {
            Log.w(TAG, "Falha ao ler cache em disco", e);
            return null;
        }
    }

    /** Devolve o elenco salvo só se ainda estiver dentro da validade (ver {@link #TTL_MILLIS}). */
    List<Character> readFresh() {
        if (!file.exists() || isExpired()) return null;
        return readStale();
    }

    void write(List<Character> characters) {
        try (FileWriter writer = new FileWriter(file)) {
            gson.toJson(characters, LIST_TYPE, writer);
        } catch (IOException e) {
            Log.w(TAG, "Falha ao salvar cache em disco", e);
        }
    }

    private boolean isExpired() {
        return System.currentTimeMillis() - file.lastModified() > TTL_MILLIS;
    }
}
