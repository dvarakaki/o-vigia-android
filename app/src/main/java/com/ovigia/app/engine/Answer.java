package com.ovigia.app.engine;

/**
 * As respostas que o jogador pode dar a cada pergunta.
 *
 * A escala é contínua ([0,1]) do ponto de vista do motor bayesiano — os valores
 * intermediários 0.75/0.25 dão evidência parcial GENUÍNA: pra um personagem com
 * belief 0.92 no atributo, "Provavelmente sim" gera verossimilhança ~0.83; pra
 * quem tem belief 0.08, ~0.33 — separação real. Um valor central 0.5 ("Talvez")
 * daria ~0.58 dos dois lados e não separaria nada, então foi eliminado.
 *
 * {@link #NAO_SEI} é uma sentinela (NaN) — não passa pelo update bayesiano. O
 * ViewModel roteia essa resposta pra {@link GameEngine#skipQuestion}, que
 * marca a pergunta como já feita (não repetir) mas NÃO altera probabilidades
 * nem incrementa o contador de perguntas.
 */
public enum Answer {
    SIM(1.0),
    PROVAVELMENTE_SIM(0.75),
    NAO_SEI(Double.NaN),
    PROVAVELMENTE_NAO(0.25),
    NAO(0.0);

    public final double value;

    Answer(double value) {
        this.value = value;
    }
}
