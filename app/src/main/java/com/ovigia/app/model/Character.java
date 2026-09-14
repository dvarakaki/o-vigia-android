package com.ovigia.app.model;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

/**
 * Personagem retornado pela Comic Vine API — /characters.
 *
 * Note que "teams" e "powers" NÃO aparecem aqui: esses campos só vêm
 * preenchidos no endpoint de detalhe de um personagem por vez
 * (/character/{id}/), não no de lista que usamos para buscar o elenco
 * inteiro em 1 request. Ver {@link com.ovigia.app.api.Traits}.
 */
public class Character implements Serializable {

    @SerializedName("id") public int id;
    @SerializedName("name") public String name;
    @SerializedName("real_name") public String realName;
    @SerializedName("gender") public int gender; // 1=M, 2=F, 0=outro/desconhecido
    @SerializedName("origin") public NamedRef origin;
    @SerializedName("image") public ImageData image;
    /** Quantidade de quadrinhos em que o personagem apareceu — proxy de popularidade/reconhecimento. */
    @SerializedName("count_of_issue_appearances") public int issueCount;
}
