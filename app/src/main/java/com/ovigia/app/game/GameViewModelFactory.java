package com.ovigia.app.game;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.ovigia.app.data.CharacterRepository;
import com.ovigia.app.learning.LearningStore;

/** GameViewModel recebe o repositório pelo construtor, então precisa de uma factory. */
public class GameViewModelFactory implements ViewModelProvider.Factory {

    private final CharacterRepository repository;
    private final LearningStore learningStore;

    public GameViewModelFactory(CharacterRepository repository, LearningStore learningStore) {
        this.repository = repository;
        this.learningStore = learningStore;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        return (T) new GameViewModel(repository, learningStore);
    }
}
