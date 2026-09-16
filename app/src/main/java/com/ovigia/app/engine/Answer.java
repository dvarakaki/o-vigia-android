package com.ovigia.app.engine;

/**
 * As respostas que o jogador pode dar a cada pergunta, com o modelo de ruído
 * usado pela atualização bayesiana do {@link GameEngine}.
 *
 * Cada resposta carrega duas probabilidades: a chance de um jogador dar essa
 * resposta quando o personagem TEM o traço ({@link #pIfTrue}) e quando NÃO tem
 * ({@link #pIfFalse}). Com a crença {@code b} = P(traço) de um personagem, a
 * verossimilhança é a mistura {@code b·pIfTrue + (1−b)·pIfFalse} — ou seja,
 * uma resposta "errada" nunca zera um candidato, só o enfraquece na medida da
 * taxa de erro humana. Isso substitui a heurística antiga
 * {@code 1 − |crença − resposta|}, que não era uma probabilidade de verdade.
 *
 * As taxas somam 1 entre as quatro respostas com evidência (a tabela é uma
 * distribuição) e são simétricas: "Provavelmente sim" para quem tem o traço é
 * tão comum quanto "Provavelmente não" para quem não tem. São um ponto de
 * partida razoável; o {@code gameLog} do {@code LearningStore} guarda as
 * partidas completas justamente para recalibrá-las com dados reais.
 *
 * {@link #NAO_SEI} não é evidência: o ViewModel roteia essa resposta para
 * {@link GameEngine#skipQuestion}, que não altera probabilidades.
 */
public enum Answer {
    SIM(1.0, 0.80, 0.03),
    PROVAVELMENTE_SIM(0.75, 0.12, 0.05),
    NAO_SEI(Double.NaN, Double.NaN, Double.NaN),
    PROVAVELMENTE_NAO(0.25, 0.05, 0.12),
    NAO(0.0, 0.03, 0.80);

    /** Valor numérico na escala [0,1], usado pelo aprendizado (média das respostas por atributo). */
    public final double value;
    private final double pIfTrue;
    private final double pIfFalse;

    /** Respostas que trazem evidência, na ordem usada para simular desfechos. */
    static final Answer[] WITH_EVIDENCE = { SIM, PROVAVELMENTE_SIM, PROVAVELMENTE_NAO, NAO };

    Answer(double value, double pIfTrue, double pIfFalse) {
        this.value = value;
        this.pIfTrue = pIfTrue;
        this.pIfFalse = pIfFalse;
    }

    public boolean isEvidence() {
        return this != NAO_SEI;
    }

    /** P(jogador dá esta resposta | personagem com crença {@code belief} no traço). */
    double likelihood(double belief) {
        return belief * pIfTrue + (1 - belief) * pIfFalse;
    }
}
