package com.ovigia.app.model;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class NamedRef implements Serializable {
    @SerializedName("id") public int id;
    @SerializedName("name") public String name;
}
