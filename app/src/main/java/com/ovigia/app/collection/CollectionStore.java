package com.ovigia.app.collection;

import android.util.Log;

import com.google.gson.Gson;
import com.ovigia.app.util.AtomicFiles;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Coleção de personagens achados, por conta, guardada neste aparelho.
 *
 * Cada item guarda nome e foto no momento em que foi salvo, para a coleção
 * aparecer mesmo sem o elenco carregado (offline, sem cache).
 *
 * Operações bloqueantes (disco): chamar fora da main thread. Thread-safe.
 */
public final class CollectionStore {

    private static final String TAG = "CollectionStore";

    private final Supplier<File> fileSupplier;
    private final Gson gson = new Gson();
    private State state;

    public CollectionStore(Supplier<File> fileSupplier) {
        this.fileSupplier = fileSupplier;
    }

    public synchronized boolean contains(String accountId, int characterId) {
        ensureLoaded();
        return find(entriesOf(accountId, false), characterId) != null;
    }

    /**
     * Adiciona o personagem à coleção da conta. Devolve {@code false} se ele já
     * estava lá (a data original é mantida).
     */
    public synchronized boolean save(String accountId, int characterId, String name, String imageUrl) {
        ensureLoaded();
        List<Entry> entries = entriesOf(accountId, true);
        if (find(entries, characterId) != null) return false;
        entries.add(new Entry(characterId, name, imageUrl, System.currentTimeMillis(), false));
        persist();
        return true;
    }

    /**
     * Traz um herói desbloqueado em outro aparelho (vindo da conta online),
     * mantendo a data original. Devolve {@code false} se ele já estava na coleção.
     */
    public synchronized boolean importEntry(String accountId, int characterId, String name, String imageUrl,
                                            long savedAt) {
        ensureLoaded();
        List<Entry> entries = entriesOf(accountId, true);
        if (find(entries, characterId) != null) return false;
        entries.add(new Entry(characterId, name, imageUrl, savedAt > 0 ? savedAt : System.currentTimeMillis(), false));
        persist();
        return true;
    }

    /** Coleção da conta, do mais recente para o mais antigo. */
    public synchronized List<Entry> list(String accountId) {
        ensureLoaded();
        List<Entry> copy = new ArrayList<>(entriesOf(accountId, false));
        copy.sort((a, b) -> Long.compare(b.savedAt, a.savedAt));
        return Collections.unmodifiableList(copy);
    }

    /**
     * Marca heróis como já vistos no catálogo: a animação do cadeado sumindo toca
     * uma única vez por herói.
     */
    public synchronized void markSeenInCatalog(String accountId, Collection<Integer> characterIds) {
        ensureLoaded();
        List<Entry> entries = entriesOf(accountId, false);
        boolean changed = false;
        for (int i = 0; i < entries.size(); i++) {
            Entry e = entries.get(i);
            if (!e.seenInCatalog && characterIds.contains(e.characterId)) {
                entries.set(i, new Entry(e.characterId, e.name, e.imageUrl, e.savedAt, true));
                changed = true;
            }
        }
        if (changed) persist();
    }

    /** Apaga a coleção inteira da conta (usado ao excluir a conta). */
    public synchronized void deleteAccount(String accountId) {
        ensureLoaded();
        if (state.byAccount.remove(accountId) != null) persist();
    }

    private void ensureLoaded() {
        if (state == null) state = load();
    }

    private List<Entry> entriesOf(String accountId, boolean create) {
        List<Entry> entries = state.byAccount.get(accountId);
        if (entries == null) {
            entries = new ArrayList<>();
            if (create) state.byAccount.put(accountId, entries);
        }
        return entries;
    }

    private static Entry find(List<Entry> entries, int characterId) {
        for (Entry e : entries) {
            if (e.characterId == characterId) return e;
        }
        return null;
    }

    private State load() {
        File file = fileSupplier.get();
        if (!file.exists()) return new State();
        try {
            State loaded = gson.fromJson(AtomicFiles.readUtf8(file), State.class);
            if (loaded == null) return new State();
            if (loaded.byAccount == null) loaded.byAccount = new HashMap<>();
            for (List<Entry> entries : loaded.byAccount.values()) {
                if (entries != null) entries.removeIf(e -> e == null);
            }
            loaded.byAccount.values().removeIf(entries -> entries == null);
            return loaded;
        } catch (IOException | RuntimeException e) {
            Log.w(TAG, "Falha ao ler coleção; começando do zero", e);
            return new State();
        }
    }

    private void persist() {
        try {
            AtomicFiles.writeUtf8(fileSupplier.get(), gson.toJson(state, State.class));
        } catch (IOException e) {
            Log.w(TAG, "Falha ao salvar coleção", e);
        }
    }

    /** Herói desbloqueado. */
    public static final class Entry {
        public final int characterId;
        public final String name;
        public final String imageUrl;
        public final long savedAt;
        /** Já apareceu no catálogo depois de desbloqueado (a revelação animada já tocou). */
        public final boolean seenInCatalog;

        Entry(int characterId, String name, String imageUrl, long savedAt, boolean seenInCatalog) {
            this.characterId = characterId;
            this.name = name;
            this.imageUrl = imageUrl;
            this.savedAt = savedAt;
            this.seenInCatalog = seenInCatalog;
        }
    }

    /** Formato serializado: id da conta -> personagens. */
    private static final class State {
        Map<String, List<Entry>> byAccount = new HashMap<>();
    }
}
