package com.ovigia.app.model;

import com.google.gson.annotations.SerializedName;

public class ComicVineResponse<T> {
    @SerializedName("status_code") public int statusCode;
    @SerializedName("number_of_total_results") public int numberOfTotalResults;
    @SerializedName("results") public T results;
}
