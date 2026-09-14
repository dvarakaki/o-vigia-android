package com.ovigia.app.game;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.ovigia.app.data.CharacterRepository;
import com.ovigia.app.engine.Answer;
import com.ovigia.app.engine.CharacterProfile;
import com.ovigia.app.engine.GameEngine;
import com.ovigia.app.learning.LearningStore;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Orquestra uma partida do Akinator: pede o elenco ao {@link CharacterRepository},
 * mantém o {@link GameEngine} e expõe o estado da tela via LiveData.
 *
 * É a MESMA instância para PerguntasActivity e RespostaActivity (compartilhada
 * através do ViewModelStore da Application — ver {@code OVigiaApplication} — já
 * que são duas Activities, não fragments de uma única tela). Isso substitui o
 * singleton manual que existia antes: agora sobrevive a rotação de tela e não
 * mistura lógica de jogo com código de Activity.
 */
public class GameViewModel extends ViewModel {

    /** Quantas alternativas oferecer quando o jogador rejeita o chute e não quer mais perguntas. */
    private static final int MAX_ALTERNATIVES = 5;

    private final CharacterRepository repository;
    private final LearningStore learningStore;
    private final Map<Integer, CharacterProfile> profilesById = new HashMap<>();
    /**
     * Respostas dadas na partida atual, na ordem em que foram feitas. Usado
     * pra alimentar o {@link LearningStore} quando o jogador revela o
     * personagem correto. So respostas com evidencia real entram — "Nao sei"
     * (NaN) fica de fora (ver {@link #answer(Answer)}).
     */
    private final List<LearningStore.AnswerRecord> currentGameAnswers = new ArrayList<>();
    /**
     * Pilha paralela ao historico do motor: true = ultima jogada teve evidencia
     * (foi pra {@link #currentGameAnswers}), false = foi um skip ("Nao sei").
     * Precisa disso pra que {@link #goBackOneQuestion()} saiba se deve tirar
     * uma entrada de {@code currentGameAnswers} ou nao — o motor lida com
     * skip vs. resposta real internamente, mas nao expoe isso pra fora.
     */
    private final Deque<Boolean> answerWasEvidence = new ArrayDeque<>();

    private GameEngine engine;
    private String currentQuestionKey;
    private boolean isLoading = false;
    /**
     * Ultimo id que foi apresentado como chute pelo motor. Guardado pra
     * distinguir "acerto do motor" de "acerto via alternativa" na hora de
     * gravar aprendizado — o segundo eh sinal MAIS forte de que o prior
     * estava errado.
     */
    private int lastGuessedId = -1;

    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<String> question = new MutableLiveData<>();
    private final MutableLiveData<Integer> questionNumber = new MutableLiveData<>();
    private final MutableLiveData<Boolean> canGoBack = new MutableLiveData<>(false);

    private final SingleLiveEvent<Integer> navigateToGuess = new SingleLiveEvent<>();
    private final SingleLiveEvent<Void> backToQuestions = new SingleLiveEvent<>();
    private final SingleLiveEvent<Boolean> gameOver = new SingleLiveEvent<>();
    /**
     * Outros candidatos pra oferecer quando o jogador rejeita um chute e não
     * quer mais perguntas. Estado normal (não é evento de navegação de
     * disparo único): a SelecionarPersonagemActivity lê o valor atual ao
     * abrir, já preenchido por {@link #stopGuessing()} antes de navegar.
     */
    private final MutableLiveData<List<CharacterProfile>> alternatives = new MutableLiveData<>();
    /** Jogador escolheu um personagem da lista de alternativas — não veio do motor, mas acertou. */
    private final SingleLiveEvent<Integer> alternateConfirmed = new SingleLiveEvent<>();

    public GameViewModel(CharacterRepository repository, LearningStore learningStore) {
        this.repository = repository;
        this.learningStore = learningStore;
    }

    public LiveData<Boolean> isLoading() { return loading; }
    public LiveData<String> error() { return error; }
    public LiveData<String> question() { return question; }
    public LiveData<Integer> questionNumber() { return questionNumber; }
    public LiveData<Boolean> canGoBack() { return canGoBack; }
    public LiveData<Integer> navigateToGuess() { return navigateToGuess; }
    public LiveData<Void> backToQuestions() { return backToQuestions; }
    /** true = jogador confirmou o chute; false = motor ficou sem candidatos. */
    public LiveData<Boolean> gameOver() { return gameOver; }
    public LiveData<List<CharacterProfile>> alternatives() { return alternatives; }
    public LiveData<Integer> alternateConfirmed() { return alternateConfirmed; }

    public CharacterProfile getProfile(int characterId) {
        return profilesById.get(characterId);
    }

    /** Chamado do onStart() da PerguntasActivity; idempotente. */
    public void ensureStarted() {
        if (engine != null) {
            advance();
            return;
        }
        if (isLoading) return;

        isLoading = true;
        loading.setValue(true);
        error.setValue(null);

        repository.loadCharacters(new CharacterRepository.Callback() {
            @Override
            public void onSuccess(List<CharacterProfile> profiles, Map<String, String> questionTextByKey) {
                isLoading = false;
                loading.setValue(false);
                profilesById.clear();
                for (CharacterProfile p : profiles) {
                    profilesById.put(p.id, p);
                }
                engine = new GameEngine(profiles, questionTextByKey,
                        id -> learningStore != null ? learningStore.popularityBoost(id) : 1.0);
                currentGameAnswers.clear();
                answerWasEvidence.clear();
                lastGuessedId = -1;
                advance();
            }

            @Override
            public void onError(String message) {
                isLoading = false;
                loading.setValue(false);
                error.setValue(message);
            }
        });
    }

    public void answer(Answer answer) {
        if (engine == null || currentQuestionKey == null) return;
        if (answer == Answer.NAO_SEI) {
            // "Não sei" não é evidência — o motor só marca a pergunta como já
            // feita (pra não repetir) e passa pra próxima. Ver GameEngine.skipQuestion.
            engine.skipQuestion(currentQuestionKey);
            answerWasEvidence.push(false);
        } else {
            engine.answer(currentQuestionKey, answer);
            currentGameAnswers.add(
                    new LearningStore.AnswerRecord(currentQuestionKey, answer.value));
            answerWasEvidence.push(true);
        }
        advance();
    }

    /**
     * Desfaz a última resposta e volta pra pergunta anterior. Devolve false
     * (sem fazer nada) quando já está na primeira pergunta — quem chama
     * decide o que fazer nesse caso (normalmente sair da tela).
     */
    public boolean goBackOneQuestion() {
        if (engine == null || !engine.canGoBack()) return false;
        // Se a ultima jogada gerou evidencia, tira ela tambem do log de
        // aprendizado — senao registrariamos algo que o jogador voltou
        // atras de dizer. Skips ("Nao sei") nao entram no log e portanto
        // nao precisam sair.
        if (!answerWasEvidence.isEmpty() && Boolean.TRUE.equals(answerWasEvidence.pop())
                && !currentGameAnswers.isEmpty()) {
            currentGameAnswers.remove(currentGameAnswers.size() - 1);
        }
        engine.goBack();
        advance();
        return true;
    }

    /** Jogador confirmou que o chute da RespostaActivity está certo. */
    public void confirmGuess() {
        recordLearning(lastGuessedId);
        gameOver.setValue(true);
    }

    /**
     * Jogador rejeitou o chute: descarta o candidato errado. Não decide sozinho o
     * que vem a seguir — quem chama (RespostaActivity) navega pra tela que
     * pergunta se o jogador quer continuar respondendo ({@link #continueGuessing()})
     * ou parar e escolher entre as alternativas restantes ({@link #stopGuessing()}).
     */
    public void rejectGuess(int characterId) {
        if (engine == null) return;
        engine.rejectGuess(characterId);
    }

    /**
     * Jogador quis continuar: SEMPRE volta pra perguntar, nunca chuta direto.
     * Não pode chamar {@link #advance()} aqui — o rejectGuess acabou de
     * renormalizar as probabilidades, e o segundo colocado pode ter herdado
     * a massa do líder errado o suficiente pra shouldGuessNow() já querer
     * chutar de novo. O jogador PEDIU pra continuar respondendo; ignorar
     * isso é bug de UX. Só se realmente não sobrou pergunta nenhuma pra
     * fazer é que caímos pro fluxo de alternativas.
     */
    public void continueGuessing() {
        if (engine == null) return;
        String key = engine.nextQuestionKey();
        if (key == null) {
            stopGuessing();
            return;
        }
        currentQuestionKey = key;
        questionNumber.setValue(engine.questionsAsked() + 1);
        question.setValue(engine.questionTextFor(key));
        canGoBack.setValue(engine.canGoBack());
        // A PerguntasActivity (por baixo na pilha) já vai reagir sozinha ao
        // novo valor de `question`; só avisamos a ContinuarActivity pra fechar
        // e voltar pra ela.
        backToQuestions.setValue(null);
    }

    /** Jogador não quer mais perguntas: oferece os candidatos restantes mais prováveis. */
    public void stopGuessing() {
        if (engine == null) return;
        alternatives.setValue(engine.remainingCandidates(MAX_ALTERNATIVES));
    }

    /** Jogador escolheu um personagem certo entre as alternativas oferecidas. */
    public void confirmAlternateGuess(int characterId) {
        recordLearning(characterId);
        alternateConfirmed.setValue(characterId);
    }

    /**
     * Persiste no {@link LearningStore} tudo que a partida atual ensinou:
     * incrementa o contador de acertos do personagem correto e agrega as
     * respostas do jogador nas crencas por atributo. So chama uma vez por
     * partida — chamadas duplicadas cairiam fora porque {@code lastGuessedId}
     * ja foi consumido e {@code currentGameAnswers} ja foi limpo em resetGame.
     */
    private void recordLearning(int correctId) {
        if (learningStore == null || correctId < 0) return;
        learningStore.recordGame(correctId, new ArrayList<>(currentGameAnswers));
    }

    /** Nenhuma das alternativas oferecidas era o personagem certo. */
    public void noneOfAlternatives() {
        gameOver.setValue(false);
    }

    public void resetGame() {
        engine = null;
        currentQuestionKey = null;
        isLoading = false;
        currentGameAnswers.clear();
        answerWasEvidence.clear();
        lastGuessedId = -1;
        question.setValue(null);
        questionNumber.setValue(null);
        error.setValue(null);
        canGoBack.setValue(false);
    }

    private enum AdvanceResult { ASKING, GUESSING, GAME_OVER }

    private AdvanceResult advance() {
        if (engine.shouldGuessNow()) {
            CharacterProfile guess = engine.topGuess();
            if (guess == null) {
                gameOver.setValue(false);
                return AdvanceResult.GAME_OVER;
            }
            lastGuessedId = guess.id;
            navigateToGuess.setValue(guess.id);
            return AdvanceResult.GUESSING;
        }
        currentQuestionKey = engine.nextQuestionKey();
        questionNumber.setValue(engine.questionsAsked() + 1);
        question.setValue(engine.questionTextFor(currentQuestionKey));
        canGoBack.setValue(engine.canGoBack());
        return AdvanceResult.ASKING;
    }
}
