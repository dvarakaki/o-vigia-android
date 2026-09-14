package com.ovigia.app.engine;

import java.util.Map;

/**
 * Um personagem candidato dentro de uma partida: identidade + crença [0,1]
 * em cada atributo (chave da pergunta) + probabilidade posterior atual.
 */
public class CharacterProfile {
    public final int id;
    public final String name;
    public final String imageUrl;
    public final Map<String, Double> attributes;

    /**
     * Aparições em quadrinhos (Comic Vine) — usado só para calcular o prior inicial
     * ({@link GameEngine}); não participa da atualização bayesiana em si.
     */
    public final int issueCount;

    /**
     * Se está na lista curada de personagens de reconhecimento mainstream (nível MCU +
     * X-Men clássicos + vilões-símbolo). Ganha boost extra no prior do
     * {@link GameEngine} pra evitar que personagens obscuros com {@code issueCount}
     * alto (Luke Cage, Songbird, Speedball…) dominem sem que ninguém tenha pensado neles.
     */
    public final boolean isMainstream;

    /** Probabilidade de ser o personagem escolhido pelo jogador; atualizada a cada resposta. */
    public double probability;

    public CharacterProfile(int id, String name, String imageUrl, Map<String, Double> attributes,
                            int issueCount, boolean isMainstream) {
        this.id = id;
        this.name = name;
        this.imageUrl = imageUrl;
        this.attributes = attributes;
        this.issueCount = issueCount;
        this.isMainstream = isMainstream;
    }
}
