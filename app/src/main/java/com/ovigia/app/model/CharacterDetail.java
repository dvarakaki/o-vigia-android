package com.ovigia.app.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Personagem completo, como o endpoint de detalhe da Comic Vine devolve
 * ({@code /character/4005-{id}/} sem {@code field_list}): todos os campos que a
 * API oferece. Listas podem ser enormes (milhares de edições) e qualquer campo
 * pode vir nulo ou vazio.
 */
public class CharacterDetail {
    @SerializedName("id") public int id;
    @SerializedName("name") public String name;
    @SerializedName("real_name") public String realName;
    /** Apelidos separados por quebra de linha. */
    @SerializedName("aliases") public String aliases;
    /** Resumo de uma ou duas frases. */
    @SerializedName("deck") public String deck;
    /** Biografia completa em HTML. */
    @SerializedName("description") public String description;
    @SerializedName("birth") public String birth;
    /** 1 = masculino, 2 = feminino, 0 = outro/desconhecido. */
    @SerializedName("gender") public int gender;
    @SerializedName("origin") public ApiRef origin;
    @SerializedName("publisher") public ApiRef publisher;
    @SerializedName("image") public ImageData image;
    @SerializedName("count_of_issue_appearances") public int countOfIssueAppearances;
    @SerializedName("first_appeared_in_issue") public ApiRef firstAppearedInIssue;
    /** "aaaa-mm-dd hh:mm:ss", horário do servidor da Comic Vine. */
    @SerializedName("date_added") public String dateAdded;
    @SerializedName("date_last_updated") public String dateLastUpdated;
    @SerializedName("api_detail_url") public String apiDetailUrl;
    @SerializedName("site_detail_url") public String siteDetailUrl;

    @SerializedName("powers") public List<ApiRef> powers;
    @SerializedName("creators") public List<ApiRef> creators;
    @SerializedName("teams") public List<ApiRef> teams;
    @SerializedName("team_friends") public List<ApiRef> teamFriends;
    @SerializedName("team_enemies") public List<ApiRef> teamEnemies;
    @SerializedName("character_friends") public List<ApiRef> characterFriends;
    @SerializedName("character_enemies") public List<ApiRef> characterEnemies;
    @SerializedName("movies") public List<ApiRef> movies;
    @SerializedName("issue_credits") public List<ApiRef> issueCredits;
    @SerializedName("issues_died_in") public List<ApiRef> issuesDiedIn;
    @SerializedName("story_arc_credits") public List<ApiRef> storyArcCredits;
    @SerializedName("volume_credits") public List<ApiRef> volumeCredits;
}
