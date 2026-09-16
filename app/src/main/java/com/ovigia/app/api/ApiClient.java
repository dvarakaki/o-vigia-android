package com.ovigia.app.api;

import android.util.Log;

import com.ovigia.app.BuildConfig;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Configuração do cliente HTTP da Comic Vine.
 *
 * A URL base e a chave vêm do {@code local.properties} (ver README). Para
 * publicar o app sem embutir a chave no APK, aponte {@code COMIC_VINE_BASE_URL}
 * para um proxy seu que injete a chave no servidor e deixe
 * {@code COMIC_VINE_API_KEY} vazia.
 */
public final class ApiClient {

    private static final String TAG = "ComicVineHttp";
    public static final String DEFAULT_BASE_URL = "https://comicvine.gamespot.com/api/";
    public static final String FORMAT = "json";
    private static final String USER_AGENT = "OVigiaApp/" + BuildConfig.VERSION_NAME + " (Android)";

    /**
     * Só os campos usados pelo motor — payload pequeno, resposta rápida.
     * count_of_issue_appearances alimenta o prior de popularidade do motor.
     */
    public static final String GAME_FIELDS = "id,name,gender,origin,image,count_of_issue_appearances";

    /** Máximo de resultados por página que a própria Comic Vine permite. */
    public static final int PAGE_SIZE = 100;

    private ApiClient() { }

    /** Chave a enviar como {@code api_key}; {@code null} quando um proxy injeta a chave. */
    public static String apiKey() {
        String key = BuildConfig.COMIC_VINE_API_KEY;
        return key == null || key.isEmpty() ? null : key;
    }

    /** Há como chegar à API: ou temos chave, ou a URL aponta para um proxy. */
    public static boolean isConfigured() {
        return apiKey() != null || !DEFAULT_BASE_URL.equals(BuildConfig.COMIC_VINE_BASE_URL);
    }

    public static ComicVineService create() {
        // A chave vai na query string: o log (só em debug) mostra a URL sem ela.
        HttpLoggingInterceptor log = new HttpLoggingInterceptor(message ->
                Log.i(TAG, message.replaceAll("api_key=[^&\\s]+", "api_key=***")));
        log.setLevel(BuildConfig.DEBUG ? HttpLoggingInterceptor.Level.BASIC : HttpLoggingInterceptor.Level.NONE);

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(chain -> chain.proceed(
                        chain.request().newBuilder()
                                .header("User-Agent", USER_AGENT)
                                .header("Accept", "application/json")
                                .build()))
                .addInterceptor(log)
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();

        return new Retrofit.Builder()
                .baseUrl(BuildConfig.COMIC_VINE_BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(ComicVineService.class);
    }
}
