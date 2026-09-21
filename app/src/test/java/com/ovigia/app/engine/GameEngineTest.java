package com.ovigia.app.engine;

import org.junit.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Testes do motor bayesiano em isolamento — sem Android, sem rede: só a
 * lógica de perguntas/probabilidades, que é a parte que mais quebra
 * silenciosamente quando alguém mexe no algoritmo.
 */
public class GameEngineTest {

    private static CharacterProfile profile(int id, String name, Map<String, Double> attrs) {
        return new CharacterProfile(id, name, "http://img", null, attrs, 0, false);
    }

    private static Map<String, Double> attrs(Object... keyValuePairs) {
        Map<String, Double> m = new LinkedHashMap<>();
        for (int i = 0; i < keyValuePairs.length; i += 2) {
            m.put((String) keyValuePairs[i], (Double) keyValuePairs[i + 1]);
        }
        return m;
    }

    private static Map<String, String> questions(String... keys) {
        Map<String, String> q = new LinkedHashMap<>();
        for (String k : keys) q.put(k, "É " + k + "?");
        return q;
    }

    private static GameEngine engine(List<CharacterProfile> candidates, Map<String, String> questions) {
        return new GameEngine(candidates, questions, id -> 1.0, new Random(42));
    }

    private static double totalProbability(List<CharacterProfile> candidates) {
        double sum = 0;
        for (CharacterProfile c : candidates) sum += c.probability;
        return sum;
    }

    @Test
    public void answerLikelihoods_formADistributionForEachTruthValue() {
        double ifTrue = 0;
        double ifFalse = 0;
        for (Answer a : Answer.WITH_EVIDENCE) {
            ifTrue += a.likelihood(1.0);
            ifFalse += a.likelihood(0.0);
        }
        assertEquals(1.0, ifTrue, 1e-9);
        assertEquals(1.0, ifFalse, 1e-9);
    }

    @Test
    public void singleCandidate_guessesImmediately() {
        List<CharacterProfile> candidates = new ArrayList<>();
        candidates.add(profile(1, "Solo", attrs("forca", 0.9)));

        GameEngine engine = engine(candidates, questions("forca"));

        assertTrue(engine.shouldGuessNow());
        assertEquals(1, engine.topGuess().id);
    }

    @Test
    public void answeringSim_favorsCharacterWithMatchingBelief() {
        List<CharacterProfile> candidates = new ArrayList<>();
        candidates.add(profile(1, "Forte", attrs("forca", 0.95)));
        candidates.add(profile(2, "Fraco", attrs("forca", 0.05)));

        GameEngine engine = engine(candidates, questions("forca"));
        String key = engine.nextQuestionKey();
        assertEquals("forca", key);

        engine.answer(key, Answer.SIM);

        CharacterProfile top = engine.topGuess();
        assertEquals(1, top.id);
        assertTrue("líder deveria ficar bem à frente após um SIM decisivo", top.probability > 0.8);
        assertEquals(1.0, totalProbability(candidates), 1e-9);
    }

    @Test
    public void wrongAnswer_weakensButNeverEliminatesACandidate() {
        List<CharacterProfile> candidates = new ArrayList<>();
        candidates.add(profile(1, "Forte", attrs("forca", 0.92)));
        candidates.add(profile(2, "Fraco", attrs("forca", 0.08)));

        GameEngine engine = engine(candidates, questions("forca"));
        engine.answer("forca", Answer.NAO);

        assertEquals(2, engine.topGuess().id);
        assertTrue("erro humano não pode zerar o candidato", candidates.get(0).probability > 0.05);
    }

    @Test
    public void probablyAnswers_moveLessThanCertainOnes() {
        List<CharacterProfile> a = new ArrayList<>();
        a.add(profile(1, "Forte", attrs("forca", 0.92)));
        a.add(profile(2, "Fraco", attrs("forca", 0.08)));
        engine(a, questions("forca")).answer("forca", Answer.SIM);

        List<CharacterProfile> b = new ArrayList<>();
        b.add(profile(1, "Forte", attrs("forca", 0.92)));
        b.add(profile(2, "Fraco", attrs("forca", 0.08)));
        engine(b, questions("forca")).answer("forca", Answer.PROVAVELMENTE_SIM);

        assertTrue(a.get(0).probability > b.get(0).probability);
        assertTrue(b.get(0).probability > 0.5);
    }

    @Test
    public void entropyMode_prefersTheQuestionThatSplitsCandidates() {
        List<CharacterProfile> candidates = new ArrayList<>();
        // "inutil" é igual para todos; "divide" separa metade/metade.
        candidates.add(profile(1, "A", attrs("inutil", 0.9, "divide", 0.9)));
        candidates.add(profile(2, "B", attrs("inutil", 0.9, "divide", 0.9)));
        candidates.add(profile(3, "C", attrs("inutil", 0.9, "divide", 0.1)));
        candidates.add(profile(4, "D", attrs("inutil", 0.9, "divide", 0.1)));

        for (int seed = 0; seed < 20; seed++) {
            GameEngine engine = new GameEngine(new ArrayList<>(candidates), questions("inutil", "divide"),
                    id -> 1.0, new Random(seed));
            assertEquals("divide", engine.nextQuestionKey());
        }
    }

    @Test
    public void nextQuestionKey_neverRepeatsAskedAttribute() {
        List<CharacterProfile> candidates = new ArrayList<>();
        candidates.add(profile(1, "A", attrs("x", 0.9, "y", 0.1)));
        candidates.add(profile(2, "B", attrs("x", 0.1, "y", 0.9)));

        GameEngine engine = engine(candidates, questions("x", "y"));
        String first = engine.nextQuestionKey();
        engine.answer(first, Answer.PROVAVELMENTE_SIM);

        String second = engine.nextQuestionKey();
        assertNotNull(second);
        assertNotEquals("não deveria repetir a mesma pergunta", first, second);
    }

    @Test
    public void skipQuestion_marksAsAskedButDoesNotChangeProbabilities() {
        List<CharacterProfile> candidates = new ArrayList<>();
        candidates.add(profile(1, "A", attrs("x", 0.9, "y", 0.1)));
        candidates.add(profile(2, "B", attrs("x", 0.1, "y", 0.9)));

        GameEngine engine = engine(candidates, questions("x", "y"));
        double probBefore = candidates.get(0).probability;

        engine.skipQuestion("x");

        assertEquals("Não sei não deve alterar probabilidade", probBefore, candidates.get(0).probability, 1e-9);
        assertEquals("Não sei não deve contar como pergunta feita", 0, engine.questionsAsked());
        assertEquals("Não sei conta como pergunta apresentada", 1, engine.questionsShown());
        assertNotEquals("pergunta pulada não deve ser oferecida de novo", "x", engine.nextQuestionKey());
    }

    @Test
    public void answerNaoSei_isTreatedAsSkip() {
        List<CharacterProfile> candidates = new ArrayList<>();
        candidates.add(profile(1, "A", attrs("x", 0.9)));
        candidates.add(profile(2, "B", attrs("x", 0.1)));

        GameEngine engine = engine(candidates, questions("x"));
        engine.answer("x", Answer.NAO_SEI);

        assertEquals(0, engine.questionsAsked());
        assertEquals(0.5, candidates.get(0).probability, 1e-9);
    }

    @Test
    public void goBack_undoesSkipQuestionWithoutDecrementingCounter() {
        List<CharacterProfile> candidates = new ArrayList<>();
        candidates.add(profile(1, "A", attrs("x", 0.9, "y", 0.1)));
        candidates.add(profile(2, "B", attrs("x", 0.1, "y", 0.9)));

        GameEngine engine = engine(candidates, questions("x", "y"));
        engine.answer("x", Answer.SIM);
        assertEquals(1, engine.questionsAsked());

        engine.skipQuestion("y");
        assertEquals("skip não incrementa contador", 1, engine.questionsAsked());

        engine.goBack();
        assertEquals("goBack de skip não decrementa contador", 1, engine.questionsAsked());
        assertEquals("pergunta pulada volta a estar disponível", "y", engine.nextQuestionKey());
    }

    @Test
    public void goBackAfterRejectingAGuess_keepsDistributionNormalizedAndRejectedOut() {
        List<CharacterProfile> candidates = new ArrayList<>();
        candidates.add(profile(1, "A", attrs("x", 0.9)));
        candidates.add(profile(2, "B", attrs("x", 0.5)));
        candidates.add(profile(3, "C", attrs("x", 0.1)));

        GameEngine engine = engine(candidates, questions("x"));
        engine.answer("x", Answer.SIM);
        engine.rejectGuess(1);

        engine.goBack();

        assertEquals("rejeitado continua fora", 0.0, candidates.get(0).probability, 1e-12);
        assertEquals("distribuição precisa somar 1", 1.0, totalProbability(candidates), 1e-9);
        assertNotEquals(1, engine.topGuess().id);
    }

    @Test
    public void rejectGuess_removesCandidateAndRedistributesProbability() {
        List<CharacterProfile> candidates = new ArrayList<>();
        candidates.add(profile(1, "A", attrs("x", 0.9)));
        candidates.add(profile(2, "B", attrs("x", 0.5)));

        GameEngine engine = engine(candidates, questions("x"));
        engine.answer("x", Answer.SIM);
        assertEquals(1, engine.topGuess().id);

        engine.rejectGuess(1);

        assertEquals("com A rejeitado, só sobra B", 2, engine.topGuess().id);
        assertEquals(1.0, candidates.get(1).probability, 1e-9);
    }

    @Test
    public void rejectingEveryCandidate_leavesNoGuess() {
        List<CharacterProfile> candidates = new ArrayList<>();
        candidates.add(profile(1, "A", attrs("x", 0.9)));
        candidates.add(profile(2, "B", attrs("x", 0.1)));

        GameEngine engine = engine(candidates, questions("x"));
        engine.rejectGuess(1);
        engine.rejectGuess(2);

        assertNull(engine.topGuess());
        assertTrue(engine.shouldGuessNow());
        assertTrue(engine.remainingCandidates(5).isEmpty());
    }

    @Test
    public void freshGame_cannotGoBack() {
        List<CharacterProfile> candidates = new ArrayList<>();
        candidates.add(profile(1, "A", attrs("x", 0.9)));
        candidates.add(profile(2, "B", attrs("x", 0.1)));

        assertFalse(engine(candidates, questions("x")).canGoBack());
    }

    @Test
    public void goBack_undoesLastAnswerAndAsksTheSameQuestionAgain() {
        List<CharacterProfile> candidates = new ArrayList<>();
        candidates.add(profile(1, "A", attrs("x", 0.9, "y", 0.5)));
        candidates.add(profile(2, "B", attrs("x", 0.1, "y", 0.5)));

        GameEngine engine = engine(candidates, questions("x", "y"));
        String firstKey = engine.nextQuestionKey();
        engine.answer(firstKey, Answer.SIM);
        assertTrue(engine.canGoBack());

        engine.goBack();

        assertFalse("desfazer a única resposta deveria zerar o histórico", engine.canGoBack());
        assertEquals("probabilidade deveria voltar a ser uniforme", 0.5, candidates.get(0).probability, 1e-9);
        assertEquals("a pergunta desfeita deveria ser mostrada de novo", firstKey, engine.nextQuestionKey());
    }

    @Test
    public void setPendingQuestion_restoresAQuestionButIgnoresAskedOnes() {
        List<CharacterProfile> candidates = new ArrayList<>();
        candidates.add(profile(1, "A", attrs("x", 0.9, "y", 0.1, "z", 0.5)));
        candidates.add(profile(2, "B", attrs("x", 0.1, "y", 0.9, "z", 0.5)));

        GameEngine engine = engine(candidates, questions("x", "y", "z"));
        engine.setPendingQuestion("z");
        assertEquals("z", engine.nextQuestionKey());

        engine.answer("x", Answer.SIM);
        engine.setPendingQuestion("x");
        assertNotEquals("x", engine.nextQuestionKey());
    }

    @Test
    public void goBack_onEmptyHistory_isANoOp() {
        List<CharacterProfile> candidates = new ArrayList<>();
        candidates.add(profile(1, "A", attrs("x", 0.9)));

        GameEngine engine = engine(candidates, questions("x"));
        engine.goBack();

        assertEquals(0, engine.questionsAsked());
    }

    @Test
    public void popularityBoost_raisesThePriorOfLearnedFavorites() {
        List<CharacterProfile> candidates = new ArrayList<>();
        candidates.add(profile(1, "A", attrs("x", 0.9)));
        candidates.add(profile(2, "B", attrs("x", 0.9)));

        new GameEngine(candidates, questions("x"), id -> id == 2 ? 2.0 : 1.0, new Random(1));

        assertEquals(2.0 / 3.0, candidates.get(1).probability, 1e-9);
    }

    @Test
    public void fullGame_convergesOnTheRightCharacterEvenWithLookAlikes() {
        // Dois personagens quase idênticos, só divergem num atributo raro.
        List<CharacterProfile> candidates = new ArrayList<>();
        candidates.add(profile(1, "LookAlikeA",
                attrs("mutante", 0.9, "forca", 0.9, "sentidos", 0.9, "genio", 0.1)));
        candidates.add(profile(2, "LookAlikeB",
                attrs("mutante", 0.9, "forca", 0.9, "sentidos", 0.9, "genio", 0.9)));
        candidates.add(profile(3, "Diferente",
                attrs("mutante", 0.1, "forca", 0.1, "sentidos", 0.1, "genio", 0.1)));

        GameEngine engine = engine(candidates, questions("mutante", "forca", "sentidos", "genio"));

        int guardrail = 0;
        while (!engine.shouldGuessNow() && guardrail < 10) {
            String key = engine.nextQuestionKey();
            double belief = candidates.get(1).attributes.getOrDefault(key, GameEngine.MISSING_BELIEF);
            engine.answer(key, belief >= 0.5 ? Answer.SIM : Answer.NAO);
            guardrail++;
        }

        assertEquals("deveria diferenciar os dois parecidos pelo atributo raro (genio)",
                2, engine.topGuess().id);
    }
}
