package com.ovigia.app.api;

import android.util.Log;

import com.ovigia.app.BuildConfig;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public final class ApiClient {

    private static final String TAG = "ApiClient";
    private static final String BASE_URL = "https://comicvine.gamespot.com/api/";
    public static final String FORMAT = "json";
    private static final String USER_AGENT = "OVigiaApp/1.0 (Android)";
    public static final String API_KEY = BuildConfig.COMIC_VINE_API_KEY;

    /**
     * Só os campos usados pelo motor do Akinator — payload pequeno, resposta rápida.
     * count_of_issue_appearances alimenta o prior de popularidade do {@link com.ovigia.app.engine.GameEngine}:
     * sem ele, um personagem obscuro (poucas aparições) começa tão provável quanto o Homem-Aranha,
     * o que faz o motor "alucinar" chutes em nomes que o jogador dificilmente escolheria.
     */
    public static final String GAME_FIELDS = "id,name,real_name,gender,origin,image,count_of_issue_appearances";

    /** Máximo de resultados por página que a própria Comic Vine permite. */
    public static final int PAGE_SIZE = 100;

    private static volatile ComicVineService service;

    private ApiClient() { }

    public static ComicVineService get() {
        if (service == null) {
            synchronized (ApiClient.class) {
                if (service == null) service = build();
            }
        }
        return service;
    }

    private static ComicVineService build() {
        HttpLoggingInterceptor log = new HttpLoggingInterceptor();
        log.setLevel(BuildConfig.DEBUG
                ? HttpLoggingInterceptor.Level.BASIC
                : HttpLoggingInterceptor.Level.NONE);

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(chain -> chain.proceed(
                        chain.request().newBuilder()
                                .header("User-Agent", USER_AGENT)
                                .header("Accept", "application/json")
                                .build()))
                .addInterceptor(log)
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .build();

        if (API_KEY == null || API_KEY.isEmpty()) {
            Log.w(TAG, "COMIC_VINE_API_KEY vazia — configure local.properties.");
        }

        return new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(ComicVineService.class);
    }
}
