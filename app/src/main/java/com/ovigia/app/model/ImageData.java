package com.ovigia.app.model;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class ImageData implements Serializable {
    @SerializedName("medium_url") public String mediumUrl;
    @SerializedName("screen_url") public String screenUrl;
    @SerializedName("screen_large_url") public String screenLargeUrl;
    @SerializedName("small_url") public String smallUrl;
    @SerializedName("super_url") public String superUrl;
    @SerializedName("original_url") public String originalUrl;

    /** Melhor imagem para exibir o personagem em destaque (chute do Akinator). */
    public String bestForHero() {
        if (notBlank(superUrl)) return superUrl;
        if (notBlank(screenLargeUrl)) return screenLargeUrl;
        if (notBlank(originalUrl)) return originalUrl;
        if (notBlank(screenUrl)) return screenUrl;
        if (notBlank(mediumUrl)) return mediumUrl;
        return smallUrl;
    }

    private boolean notBlank(String s) { return s != null && !s.isEmpty(); }
}
