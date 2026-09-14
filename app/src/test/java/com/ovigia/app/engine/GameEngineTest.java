package com.ovigia.app.engine;

import org.junit.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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
        return new CharacterProfile(id, name, "http://img", attrs, 0, false);
    }

    private static Map<String, Double> attrs(Object... keyValuePairs) {
        Map<String, Double> m = new LinkedHashMap<>();
        for (int i = 0; i < keyValuePairs.length; i += 2) {
            m.put((String) keyValuePairs[i], (Double) keyValuePairs[i + 1]);
        }
        return m;
    }

    @Test
    public void singleCandidate_guessesImmediately() {
        List<CharacterProfile> candidates = new ArrayList<>();
        candidates.add(profile(1, "Solo", attrs("forca", 0.9)));
        Map<String, String> questions = new LinkedHashMap<>();
        questions.put("forca", "Tem força?");

        GameEngine engine = new GameEngine(candidates, questions);

        assertTrue(engine.shouldGuessNow());
        assertEquals(1, engine.topGuess().id);
    }

    @Test
    public void answeringSim_favorsCharacterWithMatchingBelief() {
        List<CharacterProfile> candidates = new ArrayList<>();
        candidates.add(profile(1, "Forte", attrs("forca", 0.95)));
        candidates.add(profile(2, "Fraco", attrs("forca", 0.05)));
        Map<String, String> questions = new LinkedHashMap<>();
        questions.put("forca", "Tem força?");

        GameEngine engine = new GameEngine(candidates, questions);
        String key = engine.nextQuestionKey();
        assertEquals("forca", key);

        engine.answer(key, Answer.SIM);

        CharacterProfile top = engine.topGuess();
        assertEquals(1, top.id);
        assertTrue("líder deveria ficar bem à frente após um SIM decisivo", top.probability > 0.8);
    }

    @Test
    public void answeringNao_favorsOppositeCharacter() {
        List<CharacterProfile> candidates = new ArrayList<>();
        candidates.add(profile(1, "Forte", attrs("forca", 0.95)));
        candidates.add(profile(2, "Fraco", attrs("forca", 0.05)));
        Map<String, String> questions = new LinkedHashMap<>();
        questions.put("forca", "Tem força?");

        GameEngine engine = new GameEngine(candidates, questions);
        engine.answer("forca", Answer.NAO);

        assertEquals(2, engine.topGuess().id);
    }

    @Test
    public void nextQuestionKey_neverRepeatsAskedAttribute() {
        List<CharacterProfile> candidates = new ArrayList<>();
        candidates.add(profile(1, "A", attrs("x", 0.9, "y", 0.1)));
        candidates.add(profile(2, "B", attrs("x", 0.1, "y", 0.9)));
        Map<String, String> questions = new LinkedHashMap<>();
        questions.put("x", "É x?");
        questions.put("y", "É y?");

        GameEngine engine = new GameEngine(candidates, questions);
        String first = engine.nextQuestionKey();
        engine.answer(first, Answer.PROVAVELMENTE_SIM);

        String second = engine.nextQuestionKey();
        assertNotNull(second);
        assertFalse("não deveria repetir a mesma pergunta", second.equals(first));
    }

    @Test
    public void skipQuestion_marksAsAskedButDoesNotChangeProbabilities() {
        List<CharacterProfile> candidates = new ArrayList<>();
        candidates.add(profile(1, "A", attrs("x", 0.9, "y", 0.1)));
        candidates.add(profile(2, "B", attrs("x", 0.1, "y", 0.9)));
        Map<String, String> questions = new LinkedHashMap<>();
        questions.put("x", "É x?");
        questions.put("y", "É y?");

        GameEngine engine = new GameEngine(candidates, questions);
        double probBefore = candidates.get(0).probability;

        engine.skipQuestion("x");

        assertEquals("Não sei não deve alterar probabilidade", probBefore, candidates.get(0).probability, 1e-9);
        assertEquals("Não sei não deve contar como pergunta feita", 0, engine.questionsAsked());
        assertFalse("pergunta pulada não deve ser oferecida de novo", "x".equals(engine.nextQuestionKey()));
    }

    @Test
    public void goBack_undoesSkipQuestionWithoutDecrementingCounter() {
        List<CharacterProfile> candidates = new ArrayList<>();
        candidates.add(profile(1, "A", attrs("x", 0.9, "y", 0.1)));
        candidates.add(profile(2, "B", attrs("x", 0.1, "y", 0.9)));
        Map<String, String> questions = new LinkedHashMap<>();
        questions.put("x", "É x?");
        questions.put("y", "É y?");

        GameEngine engine = new GameEngine(candidates, questions);
        engine.answer("x", Answer.SIM);
        assertEquals(1, engine.questionsAsked());

        engine.skipQuestion("y");
        assertEquals("skip não incrementa contador", 1, engine.questionsAsked());

        engine.goBack();
        assertEquals("goBack de skip não decrementa contador", 1, engine.questionsAsked());
        assertEquals("pergunta pulada volta a estar disponível", "y", engine.nextQuestionKey());
    }

    @Test
    public void rejectGuess_removesCandidateAndRedistributesProbability() {
        List<CharacterProfile> candidates = new ArrayList<>();
        candidates.add(profile(1, "A", attrs("x", 0.9)));
        candidates.add(profile(2, "B", attrs("x", 0.5)));
        Map<String, String> questions = new LinkedHashMap<>();
        questions.put("x", "É x?");

        GameEngine engine = new GameEngine(candidates, questions);
        // força A a ser o líder
        engine.answer("x", Answer.SIM);
        assertEquals(1, engine.topGuess().id);

        engine.rejectGuess(1);

        assertEquals("com A rejeitado, só sobra B", 2, engine.topGuess().id);
    }

    @Test
    public void rejectingEveryCandidate_leavesNoGuess() {
        List<CharacterProfile> candidates = new ArrayList<>();
        candidates.add(profile(1, "A", attrs("x", 0.9)));
        candidates.add(profile(2, "B", attrs("x", 0.1)));
        Map<String, String> questions = new LinkedHashMap<>();
        questions.put("x", "É x?");

        GameEngine engine = new GameEngine(candidates, questions);
        engine.rejectGuess(1);
        engine.rejectGuess(2);

        assertNull(engine.topGuess());
        assertTrue(engine.shouldGuessNow());
    }

    @Test
    public void freshGame_cannotGoBack() {
        List<CharacterProfile> candidates = new ArrayList<>();
        candidates.add(profile(1, "A", attrs("x", 0.9)));
        candidates.add(profile(2, "B", attrs("x", 0.1)));
        Map<String, String> questions = new LinkedHashMap<>();
        questions.put("x", "É x?");

        GameEngine engine = new GameEngine(candidates, questions);

        assertFalse(engine.canGoBack());
    }

    @Test
    public void goBack_undoesLastAnswerAndAsksTheSameQuestionAgain() {
        List<CharacterProfile> candidates = new ArrayList<>();
        candidates.add(profile(1, "A", attrs("x", 0.9, "y", 0.5)));
        candidates.add(profile(2, "B", attrs("x", 0.1, "y", 0.5)));
        Map<String, String> questions = new LinkedHashMap<>();
        questions.put("x", "É x?");
        questions.put("y", "É y?");

        GameEngine engine = new GameEngine(candidates, questions);
        String firstKey = engine.nextQuestionKey();
        engine.answer(firstKey, Answer.SIM);
        assertTrue(engine.canGoBack());

        engine.goBack();

        assertFalse("desfazer a única resposta deveria zerar o histórico", engine.canGoBack());
        assertEquals("probabilidade deveria voltar a ser uniforme", 0.5, candidates.get(0).probability, 1e-9);
        assertEquals("a pergunta desfeita deveria poder ser escolhida de novo",
                firstKey, engine.nextQuestionKey());
    }

    @Test
    public void goBack_onEmptyHistory_isANoOp() {
        List<CharacterProfile> candidates = new ArrayList<>();
        candidates.add(profile(1, "A", attrs("x", 0.9)));
        Map<String, String> questions = new LinkedHashMap<>();
        questions.put("x", "É x?");

        GameEngine engine = new GameEngine(candidates, questions);
        engine.goBack(); // não deve lançar exceção nem mudar nada

        assertEquals(0, engine.questionsAsked());
    }

    @Test
    public void fullGame_convergesOnTheRightCharacterEvenWithLookAlikes() {
        // Dois personagens quase idênticos, só divergem num atributo raro —
        // é exatamente o cenário (Wolverine vs. Fera) que motivou trocar a
        // heurística antiga por entropia real.
        List<CharacterProfile> candidates = new ArrayList<>();
        candidates.add(profile(1, "LookAlikeA",
                attrs("mutante", 0.9, "forca", 0.9, "sentidos", 0.9, "genio", 0.1)));
        candidates.add(profile(2, "LookAlikeB",
                attrs("mutante", 0.9, "forca", 0.9, "sentidos", 0.9, "genio", 0.9)));
        candidates.add(profile(3, "Diferente",
                attrs("mutante", 0.1, "forca", 0.1, "sentidos", 0.1, "genio", 0.1)));

        Map<String, String> questions = new LinkedHashMap<>();
        questions.put("mutante", "É mutante?");
        questions.put("forca", "Tem força?");
        questions.put("sentidos", "Tem sentidos aguçados?");
        questions.put("genio", "É um gênio?");

        GameEngine engine = new GameEngine(candidates, questions);

        // Simula o jogador pensando em LookAlikeB até o motor arriscar um chute.
        int guardrail = 0;
        while (!engine.shouldGuessNow() && guardrail < 10) {
            String key = engine.nextQuestionKey();
            double belief = candidates.get(1).attributes.getOrDefault(key, 0.1);
            Answer answer = belief >= 0.5 ? Answer.SIM : Answer.NAO;
            engine.answer(key, answer);
            guardrail++;
        }

        assertEquals("deveria diferenciar os dois parecidos pelo atributo raro (genio)",
                2, engine.topGuess().id);
    }
}
