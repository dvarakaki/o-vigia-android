package com.ovigia.app;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStore;
import androidx.lifecycle.ViewModelStoreOwner;

import com.ovigia.app.data.CharacterRepository;
import com.ovigia.app.data.ComicVineCharacterRepository;
import com.ovigia.app.game.GameViewModelFactory;
import com.ovigia.app.learning.LearningStore;

/**
 * Dona do {@link ViewModelStore} usado pelo {@code GameViewModel}.
 *
 * O jogo acontece em duas Activities (Perguntas e Resposta), não em
 * fragments de uma única tela — então um ViewModel "por Activity" não
 * bastaria para compartilhar o estado da partida entre as duas. Ancorar o
 * ViewModelStore na Application (em vez de reintroduzir um singleton manual)
 * mantém o padrão oficial de ViewModel/LiveData: sobrevive rotação de tela,
 * observadores somem sozinhos com o ciclo de vida da Activity.
 */
public class OVigiaApplication extends Application implements ViewModelStoreOwner {

    private final ViewModelStore viewModelStore = new ViewModelStore();
    private CharacterRepository characterRepository;
    private LearningStore learningStore;

    @Override
    public void onCreate() {
        super.onCreate();
        learningStore = new LearningStore(this);
        characterRepository = new ComicVineCharacterRepository(this, learningStore);
    }

    @NonNull
    @Override
    public ViewModelStore getViewModelStore() {
        return viewModelStore;
    }

    public ViewModelProvider.Factory gameViewModelFactory() {
        return new GameViewModelFactory(characterRepository, learningStore);
    }
}
