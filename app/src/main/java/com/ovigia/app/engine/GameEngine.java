package com.ovigia.app.engine;

import java.util.AbstractMap;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.IntToDoubleFunction;

/**
 * Motor bayesiano do Akinator: mantém uma probabilidade posterior por
 * personagem e escolhe, a cada rodada, a pergunta que MINIMIZA a entropia
 * esperada da distribuição de probabilidade após a resposta — o critério
 * clássico de ganho de informação (Shannon) usado em sistemas de "20
 * perguntas". Não é uma aproximação: para cada pergunta candidata, simula
 * as duas respostas possíveis (sim/não), pondera pela chance de cada uma
 * ocorrer dado o estado atual, e escolhe a que deixa o jogo mais "decidido"
 * em média.
 */
public class GameEngine {

    private static final int MAX_QUESTIONS = 20;

    /**
     * Chuta na hora se um candidato sozinho já é muito provável. Alto de propósito:
     * jogador tem preferência forte por certeza sobre velocidade — chutes precipitados
     * são o que mais quebra a sensação de "algoritmo esperto".
     */
    private static final double GUESS_THRESHOLD = 0.85;

    /**
     * Chuta mais cedo quando o líder já disparou muito à frente do segundo
     * colocado, mesmo sem ter cruzado o threshold absoluto acima — útil
     * quando ainda sobram muitos candidatos "de cauda" com probabilidade
     * residual baixa que não deveriam segurar o jogo.
     *
     * Valores altos de propósito: com um elenco de ~150 personagens e vários
     * pares/trios quase idênticos em atributos (ver {@code Traits}), uma
     * folga fraca já acontece por ruído com poucas perguntas — é a causa mais
     * direta de "chute alucinado". Exigir 50% absoluto, 6x sobre o 2º e 3x
     * sobre o 3º garante que o líder está genuinamente destacado, não é só
     * "menos incerto que os outros".
     */
    private static final double MIN_CONFIDENT_PROBABILITY = 0.5;
    private static final double CONFIDENCE_RATIO = 6.0;

    /**
     * Nº mínimo de perguntas respondidas (desde o início do jogo OU desde o
     * último chute rejeitado) antes de aceitar QUALQUER chute — inclusive o
     * threshold absoluto acima. Sem isso, rejeitar um chute podia fazer o
     * motor chutar de novo na hora seguinte: ao remover o líder errado e
     * renormalizar, o segundo colocado às vezes já cruza 0.85 sozinho, mesmo
     * tendo tido pouquíssima evidência própria — é o padrão clássico de
     * "chuta errado, chuta errado nervosamente de novo".
     */
    private static final int MIN_QUESTIONS_BEFORE_GUESS = 8;

    /**
     * O atalho de confiança também exige folga sobre o TERCEIRO colocado, não só
     * o segundo — evita travar num "líder" que só está à frente por causa de um
     * empate triplo em atributos genéricos.
     */
    private static final double THIRD_PLACE_RATIO = 3.0;

    /**
     * Em vez de sempre escolher A pergunta de menor entropia esperada, sorteia
     * entre as melhores dentro dessa tolerância relativa — evita que a
     * primeira (e várias seguintes) pergunta seja sempre idêntica entre
     * partidas, já que o motor é 100% determinístico sem isso (prior fixo,
     * sem estado salvo entre partidas). Quando uma pergunta já se destaca
     * claramente das demais (fim de jogo, distribuição bem diferenciada), o
     * pool encolhe naturalmente para 1 e o motor volta a ser puramente guloso.
     */
    private static final double QUESTION_POOL_TOLERANCE = 0.08;
    private static final int MAX_QUESTION_POOL = 4;

    /**
     * Quando o líder já está claramente à frente mas ainda não é confiável o
     * bastante pra chutar, o motor troca de estratégia: em vez de escolher a
     * pergunta que corta a incerteza total (bom no começo, quando é preciso
     * eliminar candidatos em massa), escolhe a pergunta que melhor
     * DISCRIMINA o líder dos concorrentes restantes — a que o líder responderia
     * "sim" com força e os outros "não" (ou vice-versa). Isso é o chamado
     * "relative information gain": informação sobre a hipótese que importa,
     * não sobre a distribuição inteira.
     *
     * Trigger: líder com ≥30% de probabilidade E pelo menos 2x o 2º colocado.
     * Antes disso, ainda há incerteza demais pra mirar num personagem específico.
     */
    private static final double CONFIRM_MODE_MIN_LEAD_PROBABILITY = 0.30;
    private static final double CONFIRM_MODE_MIN_RATIO = 2.0;

    // Nenhuma crença fica em 0 ou 1 puros: mantém o jogo tolerante a respostas
    // "erradas" do jogador em vez de zerar um candidato para sempre.
    private static final double MIN_LIKELIHOOD = 0.05;

    private static final double LOG2 = Math.log(2);

    private final List<CharacterProfile> candidates;
    private final Map<String, String> questionTextByKey;
    private final Set<String> askedKeys = new HashSet<>();
    private final Set<Integer> rejectedIds = new HashSet<>();
    private final Deque<Snapshot> history = new ArrayDeque<>();
    private final Random random = new Random();
    private int questionsAsked = 0;
    private int guessEligibleFrom = 0;
    /**
     * Chave a devolver na próxima chamada de {@link #nextQuestionKey()}, sem
     * sortear de novo — usado só após {@link #goBack()} pra preservar a MESMA
     * pergunta que o jogador tinha acabado de ver. Sem isso, o sorteio entre
     * as N melhores por entropia (ver {@link #QUESTION_POOL_TOLERANCE}) faz
     * o motor escolher outra pergunta do top ao voltar, mesmo com o estado
     * de probabilidades já restaurado — o enunciado troca do nada.
     */
    private String pendingQuestionKey = null;

    public GameEngine(List<CharacterProfile> profiles, Map<String, String> questionTextByKey) {
        this(profiles, questionTextByKey, id -> 1.0);
    }

    /**
     * Variante que aceita um boost adicional por personagem no prior inicial —
     * usado pelo sistema de aprendizado ({@code LearningStore}) pra dar peso
     * extra a personagens que o jogador ja escolheu no passado. O boost eh
     * multiplicativo sobre o peso da popularidade da Comic Vine, entao
     * {@code 1.0} = neutro.
     */
    public GameEngine(List<CharacterProfile> profiles, Map<String, String> questionTextByKey,
                      IntToDoubleFunction popularityBoost) {
        this.candidates = new ArrayList<>(profiles);
        this.questionTextByKey = new LinkedHashMap<>(questionTextByKey);
        applyPopularityPrior(popularityBoost);
    }

    /**
     * Multiplicador extra no prior de personagens da lista mainstream (ver
     * {@code Rosters.MAINSTREAM}). Resolve o problema em que personagens com
     * {@code count_of_issue_appearances} alto mas reconhecimento baixo (Luke Cage,
     * Songbird, Speedball) dominam a distribuição inicial — o jogador nunca
     * está pensando neles.
     */
    private static final double MAINSTREAM_PRIOR_BOOST = 4.0;

    /**
     * Prior inicial ponderado por popularidade real: aparições em quadrinhos
     * (Comic Vine) + multiplicador de reconhecimento mainstream curado à mão.
     * Sem o boost mainstream, personagens obscuros com muitas aparições
     * empatam com heróis-símbolo pelo simples fato de terem sido "publicados
     * muito" — o que corrompe o prior. Usa log(2 + aparições) — escala suave
     * que reduz a distância entre "muito" e "pouco" popular sem apagar o
     * sinal, e nunca gera peso zero mesmo para personagens sem esse dado.
     */
    private void applyPopularityPrior(IntToDoubleFunction popularityBoost) {
        if (candidates.isEmpty()) return;
        double totalWeight = 0;
        double[] weights = new double[candidates.size()];
        for (int i = 0; i < candidates.size(); i++) {
            CharacterProfile c = candidates.get(i);
            double base = Math.log(2 + Math.max(0, c.issueCount));
            double weight = c.isMainstream ? base * MAINSTREAM_PRIOR_BOOST : base;
            // Boost aprendido: personagens que este jogador ja confirmou no
            // passado sobem no prior. Multiplicador vem do LearningStore.
            double learned = popularityBoost.applyAsDouble(c.id);
            if (learned > 0) weight *= learned;
            weights[i] = weight;
            totalWeight += weights[i];
        }
        for (int i = 0; i < candidates.size(); i++) {
            candidates.get(i).probability = weights[i] / totalWeight;
        }
    }

    /**
     * Escolhe, entre os atributos ainda não perguntados, o que minimiza a
     * entropia esperada da distribuição de probabilidade após a resposta.
     *
     * Para cada atributo: estima P(sim) como a crença média ponderada pela
     * probabilidade atual dos candidatos, simula a atualização bayesiana
     * para os dois desfechos possíveis (sim e não) e calcula a entropia de
     * Shannon resultante em cada um. A entropia esperada é a média dessas
     * duas, ponderada por P(sim)/P(não). Quanto menor, mais "decidido" o
     * jogo fica — é o mesmo princípio de ganho de informação de árvores de
     * decisão (ID3/C4.5), aplicado aqui a um espaço de hipóteses bayesiano
     * em vez de uma árvore fixa.
     */
    public String nextQuestionKey() {
        if (pendingQuestionKey != null) {
            String key = pendingQuestionKey;
            pendingQuestionKey = null;
            return key;
        }
        CharacterProfile[] top = topThree();
        if (isInConfirmMode(top)) {
            String key = pickConfirmationQuestion(top[0]);
            if (key != null) return key;
            // Se por algum motivo o modo confirmação não achou pergunta útil
            // (ex.: líder é indistinguível dos outros em todos os atributos
            // ainda não perguntados), cai pro modo entropia normal.
        }
        return pickEntropyQuestion();
    }

    /**
     * Modo "cortar a dúvida": escolhe a pergunta que minimiza a entropia
     * esperada da distribuição inteira (Shannon). Bom quando a incerteza
     * ainda está espalhada — corta a massa de candidatos no meio.
     */
    private String pickEntropyQuestion() {
        List<Map.Entry<String, Double>> scored = new ArrayList<>();

        for (String key : questionTextByKey.keySet()) {
            if (askedKeys.contains(key)) continue;

            double pYes = 0;
            for (CharacterProfile c : candidates) {
                if (rejectedIds.contains(c.id)) continue;
                pYes += c.probability * beliefOf(c, key);
            }
            double pNo = 1 - pYes;

            double expectedEntropy =
                    pYes * entropyIfAnswered(key, Answer.SIM.value)
                            + pNo * entropyIfAnswered(key, Answer.NAO.value);

            scored.add(new AbstractMap.SimpleEntry<>(key, expectedEntropy));
        }
        return pickFromPool(scored);
    }

    /**
     * Modo "confirmar o líder": escolhe a pergunta que MELHOR DISCRIMINA o
     * líder atual dos concorrentes restantes. Métrica é |belief_líder - avg(belief_outros)|,
     * ponderado pela massa dos outros — ignora candidatos de cauda que já
     * não segurariam o jogo mesmo sob resposta contrária. Uma pergunta que
     * o líder responderia forte "sim" e os concorrentes "não" (ou o inverso)
     * fecha ou abre o jogo em uma jogada, em vez de arranhar a distribuição
     * inteira.
     */
    private String pickConfirmationQuestion(CharacterProfile leader) {
        if (leader == null) return null;
        List<Map.Entry<String, Double>> scored = new ArrayList<>();

        double othersMass = 0;
        for (CharacterProfile c : candidates) {
            if (rejectedIds.contains(c.id) || c == leader) continue;
            othersMass += c.probability;
        }
        if (othersMass <= 0) return null;

        for (String key : questionTextByKey.keySet()) {
            if (askedKeys.contains(key)) continue;

            double leaderBelief = beliefOf(leader, key);
            double othersBelief = 0;
            for (CharacterProfile c : candidates) {
                if (rejectedIds.contains(c.id) || c == leader) continue;
                othersBelief += (c.probability / othersMass) * beliefOf(c, key);
            }
            // Negativo pra que "quanto mais discriminativo" ordene igual à
            // entropia (menor = melhor) e caia no mesmo pickFromPool.
            double score = -Math.abs(leaderBelief - othersBelief);
            scored.add(new AbstractMap.SimpleEntry<>(key, score));
        }
        return pickFromPool(scored);
    }

    /** Escolhe do pool das top-N perguntas ordenadas por score (menor = melhor). */
    private String pickFromPool(List<Map.Entry<String, Double>> scored) {
        if (scored.isEmpty()) return null;
        scored.sort(Map.Entry.comparingByValue());
        double best = scored.get(0).getValue();
        double cutoff = best + Math.max(QUESTION_POOL_TOLERANCE, Math.abs(best) * QUESTION_POOL_TOLERANCE);

        List<String> pool = new ArrayList<>();
        for (Map.Entry<String, Double> entry : scored) {
            if (pool.size() >= MAX_QUESTION_POOL || entry.getValue() > cutoff) break;
            pool.add(entry.getKey());
        }
        return pool.get(random.nextInt(pool.size()));
    }

    private boolean isInConfirmMode(CharacterProfile[] top) {
        CharacterProfile first = top[0];
        CharacterProfile second = top[1];
        if (first == null || second == null) return false;
        return first.probability >= CONFIRM_MODE_MIN_LEAD_PROBABILITY
                && first.probability >= second.probability * CONFIRM_MODE_MIN_RATIO;
    }

    public String questionTextFor(String key) {
        return questionTextByKey.get(key);
    }

    /** Atualiza a crença em cada candidato dado que o jogador respondeu `answer` para `key`. */
    public void answer(String key, Answer answer) {
        double[] before = new double[candidates.size()];
        for (int i = 0; i < candidates.size(); i++) {
            before[i] = candidates.get(i).probability;
        }
        history.push(new Snapshot(key, before, false));

        askedKeys.add(key);
        questionsAsked++;

        double[] posterior = posteriorIfAnswered(key, answer.value);
        for (int i = 0; i < candidates.size(); i++) {
            candidates.get(i).probability = posterior[i];
        }
    }

    /**
     * Jogador respondeu "Não sei" — trata como se a pergunta nunca tivesse
     * sido feita, com uma exceção: marca a chave em {@link #askedKeys} pra
     * evitar que o motor ofereça a MESMA pergunta de novo em seguida. Se o
     * jogador não sabe, não vai passar a saber respondendo de novo.
     *
     * NÃO atualiza probabilidades e NÃO incrementa {@link #questionsAsked}
     * — o critério de "informação coletada" e a barra de {@link #MIN_QUESTIONS_BEFORE_GUESS}
     * consideram só respostas que trouxeram evidência real.
     *
     * Empilha um {@link Snapshot} marcado como skip pra que {@link #goBack}
     * consiga desfazer (removendo a chave de askedKeys sem alterar o contador).
     */
    public void skipQuestion(String key) {
        double[] before = new double[candidates.size()];
        for (int i = 0; i < candidates.size(); i++) {
            before[i] = candidates.get(i).probability;
        }
        history.push(new Snapshot(key, before, true));
        askedKeys.add(key);
    }

    /** Se dá pra desfazer a última resposta e voltar pra pergunta anterior. */
    public boolean canGoBack() {
        return !history.isEmpty();
    }

    /**
     * Desfaz a última resposta: restaura as probabilidades de antes dela e
     * libera o atributo pra ser perguntado de novo. Não mexe em chutes
     * rejeitados ({@link #rejectGuess}) — o botão de voltar vive na tela de
     * perguntas, não na de resposta.
     */
    public void goBack() {
        if (history.isEmpty()) return;
        Snapshot snapshot = history.pop();
        askedKeys.remove(snapshot.key);
        // "Não sei" não conta como pergunta feita, então não pode decrementar
        // o contador na volta — se contasse, o desfazer ficaria negativo e
        // MIN_QUESTIONS_BEFORE_GUESS aceitaria chutes cedo demais.
        if (!snapshot.wasSkip) {
            questionsAsked--;
        }
        for (int i = 0; i < candidates.size(); i++) {
            candidates.get(i).probability = snapshot.probabilitiesBefore[i];
        }
        // Força a próxima nextQuestionKey() a devolver EXATAMENTE a pergunta
        // que estava sendo mostrada, em vez de sortear entre as top-N por
        // entropia — o estado voltou, o enunciado tem que voltar também.
        pendingQuestionKey = snapshot.key;
    }

    /** Estado necessário pra desfazer uma resposta: a pergunta e as probabilidades de antes dela. */
    private static final class Snapshot {
        final String key;
        final double[] probabilitiesBefore;
        /** True se veio de {@link #skipQuestion} — {@link #goBack} não decrementa o contador nesse caso. */
        final boolean wasSkip;

        Snapshot(String key, double[] probabilitiesBefore, boolean wasSkip) {
            this.key = key;
            this.probabilitiesBefore = probabilitiesBefore;
            this.wasSkip = wasSkip;
        }
    }

    /**
     * Simula a atualização bayesiana de `answer(key, valor)` sem alterar o
     * estado do jogo, devolvendo a distribuição de probabilidade resultante
     * (já normalizada). Usado tanto por {@link #answer} (pra valer) quanto
     * por {@link #nextQuestionKey} (pra avaliar cada pergunta candidata).
     */
    private double[] posteriorIfAnswered(String key, double answerValue) {
        double totalMass = 0;
        double[] updated = new double[candidates.size()];

        for (int i = 0; i < candidates.size(); i++) {
            CharacterProfile c = candidates.get(i);
            if (rejectedIds.contains(c.id)) {
                updated[i] = 0;
                continue;
            }
            double belief = beliefOf(c, key);
            double likelihood = Math.max(MIN_LIKELIHOOD, 1.0 - Math.abs(belief - answerValue));
            updated[i] = c.probability * likelihood;
            totalMass += updated[i];
        }

        if (totalMass <= 0) totalMass = 1; // segurança: evita divisão por zero em casos degenerados
        for (int i = 0; i < updated.length; i++) {
            updated[i] = updated[i] / totalMass;
        }
        return updated;
    }

    /** Entropia de Shannon (em bits) da distribuição resultante de responder `answerValue` a `key`. */
    private double entropyIfAnswered(String key, double answerValue) {
        double[] posterior = posteriorIfAnswered(key, answerValue);
        double entropy = 0;
        for (double p : posterior) {
            if (p <= 0) continue;
            entropy -= p * (Math.log(p) / LOG2);
        }
        return entropy;
    }

    public CharacterProfile topGuess() {
        return topThree()[0];
    }

    public boolean shouldGuessNow() {
        CharacterProfile[] topThree = topThree();
        CharacterProfile top = topThree[0];
        if (top == null) return true;

        // Saídas estruturais: não há mais nada a ganhar perguntando, então chuta
        // mesmo sem ter atingido a barra de confiança normal.
        if (activeCandidateCount() <= 1) return true;
        if (askedKeys.size() >= questionTextByKey.size()) return true; // sem mais perguntas
        if (questionsAsked >= MAX_QUESTIONS) return true;

        boolean hasEnoughEvidence = (questionsAsked - guessEligibleFrom) >= MIN_QUESTIONS_BEFORE_GUESS;
        if (!hasEnoughEvidence) return false;

        if (top.probability >= GUESS_THRESHOLD) return true;

        CharacterProfile runnerUp = topThree[1];
        CharacterProfile third = topThree[2];
        if (runnerUp != null
                && top.probability >= MIN_CONFIDENT_PROBABILITY
                && top.probability >= runnerUp.probability * CONFIDENCE_RATIO
                && (third == null || top.probability >= third.probability * THIRD_PLACE_RATIO)) {
            return true; // líder disparado na frente do segundo E do terceiro colocado
        }

        return false;
    }

    /** [0] = mais provável, [1] = segundo, [2] = terceiro colocado (qualquer um pode ser null). */
    private CharacterProfile[] topThree() {
        CharacterProfile first = null;
        CharacterProfile second = null;
        CharacterProfile third = null;
        for (CharacterProfile c : candidates) {
            if (rejectedIds.contains(c.id)) continue;
            if (first == null || c.probability > first.probability) {
                third = second;
                second = first;
                first = c;
            } else if (second == null || c.probability > second.probability) {
                third = second;
                second = c;
            } else if (third == null || c.probability > third.probability) {
                third = c;
            }
        }
        return new CharacterProfile[] { first, second, third };
    }

    /** Descarta o chute atual (resposta "Não" na tela de resposta) e redistribui as probabilidades. */
    public void rejectGuess(int characterId) {
        rejectedIds.add(characterId);
        double mass = 0;
        for (CharacterProfile c : candidates) {
            if (!rejectedIds.contains(c.id)) mass += c.probability;
        }
        if (mass <= 0) mass = 1;
        for (CharacterProfile c : candidates) {
            c.probability = rejectedIds.contains(c.id) ? 0 : c.probability / mass;
        }
        // Reinicia a exigência de evidência: o segundo colocado não herda a
        // confiança que era do líder errado, tem que reconquistá-la com novas perguntas.
        guessEligibleFrom = questionsAsked;
    }

    public int questionsAsked() {
        return questionsAsked;
    }

    /**
     * Até {@code limit} candidatos ainda ativos, do mais pro menos provável — usado
     * para oferecer alternativas quando o jogador rejeita um chute e não quer
     * responder mais perguntas.
     */
    public List<CharacterProfile> remainingCandidates(int limit) {
        List<CharacterProfile> active = new ArrayList<>();
        for (CharacterProfile c : candidates) {
            if (!rejectedIds.contains(c.id)) active.add(c);
        }
        active.sort((a, b) -> Double.compare(b.probability, a.probability));
        return new ArrayList<>(active.subList(0, Math.min(limit, active.size())));
    }

    private int activeCandidateCount() {
        int n = 0;
        for (CharacterProfile c : candidates) {
            if (!rejectedIds.contains(c.id)) n++;
        }
        return n;
    }

    private double beliefOf(CharacterProfile c, String key) {
        Double belief = c.attributes.get(key);
        // Atributo ausente (ex.: personagem sem powers listados) é tratado como
        // um "não" fraco, não um "não sei" neutro — combina melhor com o dataset da Comic Vine.
        return belief == null ? 0.1 : belief;
    }
}
