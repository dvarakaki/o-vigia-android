package com.ovigia.app.data;

import android.util.Log;

import com.google.gson.Gson;
import com.ovigia.app.api.ApiClient;
import com.ovigia.app.api.ComicVineService;
import com.ovigia.app.data.CharacterRepository.LoadError;
import com.ovigia.app.model.CharacterDetail;
import com.ovigia.app.model.ComicVineResponse;
import com.ovigia.app.util.AtomicFiles;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import retrofit2.Response;

/**
 * Busca a ficha completa na Comic Vine e guarda cada herói em
 * {@code files/hero_details/{id}.json}: a ficha abre na hora nas próximas vezes,
 * funciona offline e não gasta o limite de requisições da API. Um cache vencido
 * ainda serve de último recurso se a rede falhar.
 */
public final class ComicVineHeroDetailRepository implements HeroDetailRepository {

    private static final String TAG = "HeroDetailRepository";
    private static final long TTL_MILLIS = TimeUnit.DAYS.toMillis(30);

    private static final int CV_STATUS_OK = 1;
    private static final int CV_STATUS_INVALID_KEY = 100;
    private static final int CV_STATUS_NOT_FOUND = 101;
    private static final int CV_STATUS_RATE_LIMIT = 107;

    /** Chamada à API — separada para testar o tratamento de respostas sem rede. */
    public interface Remote {
        Response<ComicVineResponse<CharacterDetail>> fetch(int characterId) throws IOException;
    }

    private final Remote remote;
    private final boolean apiConfigured;
    private final Supplier<File> directory;
    private final Executor ioExecutor;
    private final Executor mainExecutor;
    private final Gson gson = new Gson();

    public ComicVineHeroDetailRepository(Remote remote, boolean apiConfigured, Supplier<File> directory,
                                         Executor ioExecutor, Executor mainExecutor) {
        this.remote = remote;
        this.apiConfigured = apiConfigured;
        this.directory = directory;
        this.ioExecutor = ioExecutor;
        this.mainExecutor = mainExecutor;
    }

    /** Remote real, sobre o serviço Retrofit. */
    public static Remote remote(ComicVineService service, String apiKey) {
        return id -> service.characterDetail(id, apiKey, ApiClient.FORMAT).execute();
    }

    @Override
    public void load(int characterId, boolean forceRefresh, Callback callback) {
        ioExecutor.execute(() -> {
            File file = new File(directory.get(), characterId + ".json");
            if (!forceRefresh && file.exists() && !isExpired(file)) {
                CharacterDetail cached = read(file);
                if (cached != null) {
                    mainExecutor.execute(() -> callback.onSuccess(cached, false));
                    return;
                }
            }
            try {
                CharacterDetail fresh = fetch(characterId);
                write(file, fresh);
                mainExecutor.execute(() -> callback.onSuccess(fresh, false));
            } catch (LoadFailure failure) {
                CharacterDetail stale = file.exists() ? read(file) : null;
                if (stale != null) {
                    Log.i(TAG, "Rede indisponível (" + failure.error + "); usando ficha salva");
                    mainExecutor.execute(() -> callback.onSuccess(stale, true));
                } else {
                    mainExecutor.execute(() -> callback.onError(failure.error));
                }
            }
        });
    }

    private CharacterDetail fetch(int characterId) throws LoadFailure {
        if (!apiConfigured) throw new LoadFailure(LoadError.NOT_CONFIGURED);
        Response<ComicVineResponse<CharacterDetail>> response;
        try {
            response = remote.fetch(characterId);
        } catch (IOException e) {
            throw new LoadFailure(LoadError.NO_CONNECTION);
        } catch (RuntimeException e) {
            // JSON inesperado, por exemplo.
            Log.w(TAG, "Resposta ilegível", e);
            throw new LoadFailure(LoadError.SERVER_ERROR);
        }
        int http = response.code();
        if (http == 420 || http == 429) throw new LoadFailure(LoadError.RATE_LIMITED);
        if (http == 401 || http == 403) throw new LoadFailure(LoadError.NOT_CONFIGURED);
        ComicVineResponse<CharacterDetail> body = response.body();
        if (!response.isSuccessful() || body == null) throw new LoadFailure(LoadError.SERVER_ERROR);
        if (body.statusCode == CV_STATUS_INVALID_KEY) throw new LoadFailure(LoadError.NOT_CONFIGURED);
        if (body.statusCode == CV_STATUS_RATE_LIMIT) throw new LoadFailure(LoadError.RATE_LIMITED);
        if (body.statusCode == CV_STATUS_NOT_FOUND) throw new LoadFailure(LoadError.EMPTY_ROSTER);
        if (body.statusCode != CV_STATUS_OK || body.results == null) throw new LoadFailure(LoadError.SERVER_ERROR);
        return body.results;
    }

    private CharacterDetail read(File file) {
        try {
            return gson.fromJson(AtomicFiles.readUtf8(file), CharacterDetail.class);
        } catch (IOException | RuntimeException e) {
            Log.w(TAG, "Ficha salva ilegível", e);
            return null;
        }
    }

    private void write(File file, CharacterDetail detail) {
        try {
            AtomicFiles.writeUtf8(file, gson.toJson(detail, CharacterDetail.class));
        } catch (IOException e) {
            Log.w(TAG, "Falha ao salvar ficha", e);
        }
    }

    private static boolean isExpired(File file) {
        return System.currentTimeMillis() - file.lastModified() > TTL_MILLIS;
    }

    private static final class LoadFailure extends Exception {
        final LoadError error;

        LoadFailure(LoadError error) {
            super(error.name(), null, false, false);
            this.error = error;
        }
    }
}
