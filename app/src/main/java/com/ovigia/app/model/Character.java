package com.ovigia.app.model;

import com.google.gson.annotations.SerializedName;

/**
 * Personagem retornado pela Comic Vine API — /characters.
 *
 * Note que "teams" e "powers" NÃO aparecem aqui: esses campos só vêm
 * preenchidos no endpoint de detalhe de um personagem por vez
 * (/character/{id}/), não no de lista que usamos para buscar o elenco
 * inteiro em 1 request. Ver {@code assets/roster.json}.
 */
public class Character {

    @SerializedName("id") public int id;
    @SerializedName("name") public String name;
    @SerializedName("gender") public int gender; // 1=M, 2=F, 0=outro/desconhecido
    @SerializedName("origin") public NamedRef origin;
    @SerializedName("image") public ImageData image;
    /** Quantidade de quadrinhos em que o personagem apareceu — proxy de popularidade/reconhecimento. */
    @SerializedName("count_of_issue_appearances") public int issueCount;
}
