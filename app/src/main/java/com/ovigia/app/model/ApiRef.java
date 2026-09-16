package com.ovigia.app.model;

import com.google.gson.annotations.SerializedName;

/**
 * Referência a outro recurso da Comic Vine (personagem, equipe, edição, filme,
 * poder, criador…), como vem nas listas do detalhe de um personagem.
 */
public class ApiRef {
    @SerializedName("id") public int id;
    @SerializedName("name") public String name;
    @SerializedName("api_detail_url") public String apiDetailUrl;
    @SerializedName("site_detail_url") public String siteDetailUrl;
    /** Só em referências a edições (ex.: primeira aparição). */
    @SerializedName("issue_number") public String issueNumber;
}
