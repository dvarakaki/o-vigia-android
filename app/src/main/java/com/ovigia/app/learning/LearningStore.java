package com.ovigia.app.learning;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Estado persistente de aprendizado entre partidas. Três coisas ao mesmo tempo,
 * no mesmo arquivo em disco pra manter I/O simples:
 *
 * 1. Contagem de acertos por personagem — usada como boost no prior inicial do
 *    {@code GameEngine}. Efeito imediato: personagens que ESTE jogador escolhe
 *    com frequência sobem no ranking desde a primeira pergunta.
 * 2. Correção de crenças por atributo — cada vez que o jogador revela o
 *    personagem correto E respondeu a uma pergunta, a resposta é agregada
 *    (média com força de prior) e passa a substituir/misturar com a crença
 *    original da Comic Vine. Corrige atributos mal marcados sem apagar o
 *    dado original quando ainda há pouco sinal.
 * 3. Histórico bruto de partidas — pra retreinamento offline futuro; não
 *    afeta comportamento agora, só acumula.
 *
 * Não é thread-safe por design: só a UI thread mexe aqui (via ViewModel).
 */
public final class LearningStore {

    private static final String TAG = "LearningStore";
    private static final String FILE_NAME = "learning_store.json";

    /**
     * Peso de cada acerto do jogador sobre o prior de popularidade. Com 0.5,
     * cada vez que o jogador confirmou o personagem X, o prior de X é
     * multiplicado por 1.5 — dois acertos dobram, três triplicam. Sobe rápido
     * o bastante pra sentir efeito depois de 1-2 partidas, mas não engole o
     * sinal de popularidade real da Comic Vine.
     */
    private static final double PICK_BOOST_WEIGHT = 0.5;

    /**
     * "Força" do prior original ao misturar com a crença aprendida. Fórmula:
     * {@code final = original * K/(K+n) + learnedMean * n/(K+n)}, onde n é o
     * número de respostas observadas pra esse par (personagem, atributo).
     * K=5 significa: com 1 resposta observada, o valor final ainda é 83%
     * baseado no original; com 5, meio a meio; a partir de ~15 respostas o
     * aprendido domina. Alto de propósito — um jogador pode discordar de
     * um atributo por engano, e não queremos apagar a Comic Vine numa
     * partida só.
     */
    private static final double BELIEF_PRIOR_STRENGTH = 5.0;

    /**
     * Máximo de partidas guardadas no log. Circular: quando estoura, descarta
     * as mais antigas. Existe pra o arquivo não crescer sem limite — o efeito
     * de aprendizado real vive nos agregados acima, não no log bruto.
     */
    private static final int MAX_GAME_LOG_ENTRIES = 500;

    private final File file;
    private final Gson gson = new Gson();
    private State state;

    public LearningStore(Context context) {
        this.file = new File(context.getApplicationContext().getFilesDir(), FILE_NAME);
        this.state = load();
    }

    /** Boost multiplicativo pro prior de popularidade. 1.0 = neutro. */
    public double popularityBoost(int characterId) {
        Integer picks = state.picksById.get(characterId);
        if (picks == null || picks <= 0) return 1.0;
        return 1.0 + PICK_BOOST_WEIGHT * picks;
    }

    /**
     * Crença aprendida em {@code [0,1]} pro par (personagem, atributo), ou
     * {@code null} se nunca foi observado. Quem chama decide como misturar
     * com a crença original — veja {@link #blend}.
     */
    public Double learnedBelief(int characterId, String key) {
        Map<String, double[]> perAttr = state.beliefsById.get(characterId);
        if (perAttr == null) return null;
        double[] sumCount = perAttr.get(key);
        if (sumCount == null || sumCount[1] <= 0) return null;
        return sumCount[0] / sumCount[1];
    }

    /**
     * Mistura a crença original da Comic Vine com o que foi aprendido, usando
     * a força de prior {@link #BELIEF_PRIOR_STRENGTH}. Se nunca foi observado,
     * devolve o original sem tocar.
     */
    public double blend(int characterId, String key, double originalBelief) {
        Map<String, double[]> perAttr = state.beliefsById.get(characterId);
        if (perAttr == null) return originalBelief;
        double[] sumCount = perAttr.get(key);
        if (sumCount == null || sumCount[1] <= 0) return originalBelief;
        double n = sumCount[1];
        double learnedMean = sumCount[0] / n;
        double w = n / (BELIEF_PRIOR_STRENGTH + n);
        return originalBelief * (1 - w) + learnedMean * w;
    }

    /**
     * Registra que uma partida terminou com {@code correctId} como resposta
     * certa e {@code answers} como as respostas dadas (apenas as que tiveram
     * evidência — "Não sei" deve ficar de fora). Atualiza os três estados
     * (contagem, crenças, log) e persiste em disco.
     */
    public void recordGame(int correctId, List<AnswerRecord> answers) {
        Integer prev = state.picksById.get(correctId);
        state.picksById.put(correctId, (prev == null ? 0 : prev) + 1);

        Map<String, double[]> perAttr = state.beliefsById.get(correctId);
        if (perAttr == null) {
            perAttr = new LinkedHashMap<>();
            state.beliefsById.put(correctId, perAttr);
        }
        for (AnswerRecord a : answers) {
            if (a == null || a.key == null || Double.isNaN(a.value)) continue;
            double[] sumCount = perAttr.get(a.key);
            if (sumCount == null) {
                sumCount = new double[]{0.0, 0.0};
                perAttr.put(a.key, sumCount);
            }
            sumCount[0] += a.value;
            sumCount[1] += 1;
        }

        state.gameLog.add(new GameLogEntry(System.currentTimeMillis(), correctId, answers));
        while (state.gameLog.size() > MAX_GAME_LOG_ENTRIES) {
            state.gameLog.remove(0);
        }

        persist();
    }

    private State load() {
        if (!file.exists()) return new State();
        try (FileReader reader = new FileReader(file)) {
            State loaded = gson.fromJson(reader, State.class);
            if (loaded == null) return new State();
            if (loaded.picksById == null) loaded.picksById = new HashMap<>();
            if (loaded.beliefsById == null) loaded.beliefsById = new HashMap<>();
            if (loaded.gameLog == null) loaded.gameLog = new ArrayList<>();
            return loaded;
        } catch (IOException | RuntimeException e) {
            Log.w(TAG, "Falha ao ler estado de aprendizado; comecando do zero", e);
            return new State();
        }
    }

    private void persist() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
            gson.toJson(state, State.class, writer);
        } catch (IOException e) {
            Log.w(TAG, "Falha ao salvar estado de aprendizado", e);
        }
    }

    /** Reseta tudo — util pra debug e pra um botao "esquecer" no futuro. */
    public void reset() {
        state = new State();
        if (file.exists() && !file.delete()) {
            Log.w(TAG, "Falha ao apagar arquivo de aprendizado");
        }
    }

    /** Uma resposta dada numa partida — usada pra alimentar {@link #recordGame}. */
    public static final class AnswerRecord {
        public final String key;
        public final double value;

        public AnswerRecord(String key, double value) {
            this.key = key;
            this.value = value;
        }
    }

    /**
     * Snapshot completo de uma partida terminada. Guardado só pra log/análise
     * offline; não é lido de volta pela lógica em runtime.
     */
    private static final class GameLogEntry {
        final long timestamp;
        final int correctId;
        final List<AnswerRecord> answers;

        GameLogEntry(long timestamp, int correctId, List<AnswerRecord> answers) {
            this.timestamp = timestamp;
            this.correctId = correctId;
            this.answers = answers;
        }
    }

    /** Formato serializado. Public fields pro Gson conseguir ler/escrever direto. */
    private static final class State {
        Map<Integer, Integer> picksById = new HashMap<>();
        /**
         * id -> (attributeKey -> [sum, count]). Cada resposta soma seu valor
         * ao sum e incrementa count; a media {@code sum/count} eh a crenca
         * aprendida.
         */
        Map<Integer, Map<String, double[]>> beliefsById = new HashMap<>();
        List<GameLogEntry> gameLog = new ArrayList<>();
    }
}
