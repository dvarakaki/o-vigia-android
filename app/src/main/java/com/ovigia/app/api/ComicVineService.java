package com.ovigia.app.api;

import com.ovigia.app.model.Character;
import com.ovigia.app.model.ComicVineResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface ComicVineService {

    /**
     * A API usa filter=chave:valor; para IDs específicos: filter="id:1|2|3".
     * Nunca devolve mais que 100 resultados por página (limite da própria
     * Comic Vine) — para elencos maiores que isso é preciso paginar com
     * `offset`, ver {@link com.ovigia.app.data.ComicVineCharacterRepository}.
     */
    @GET("characters/")
    Call<ComicVineResponse<List<Character>>> listCharacters(
            @Query("api_key") String apiKey,
            @Query("format") String format,
            @Query("limit") int limit,
            @Query("offset") int offset,
            @Query("filter") String filter,
            @Query("field_list") String fieldList
    );
}
