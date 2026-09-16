package com.ovigia.app.social;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.ovigia.app.social.FriendProfileUiState.Status;
import com.ovigia.app.util.Event;

import java.util.concurrent.Executor;

/** Perfil completo de um amigo: carrega do servidor e permite desfazer a amizade. */
public class FriendProfileViewModel extends ViewModel {

    private final String friendUid;
    private final SocialRepository repository;
    private final Executor socialExecutor;
    private final Executor mainExecutor;

    private final MutableLiveData<FriendProfileUiState> state = new MutableLiveData<>(FriendProfileUiState.loading());
    /** Falha ao desfazer a amizade (o perfil continua na tela). */
    private final MutableLiveData<Event<SocialException.Error>> removeFailures = new MutableLiveData<>();
    private boolean started = false;

    public FriendProfileViewModel(String friendUid, SocialRepository repository, Executor socialExecutor,
                                  Executor mainExecutor) {
        this.friendUid = friendUid;
        this.repository = repository;
        this.socialExecutor = socialExecutor;
        this.mainExecutor = mainExecutor;
    }

    public LiveData<FriendProfileUiState> state() { return state; }

    public LiveData<Event<SocialException.Error>> removeFailures() { return removeFailures; }

    public void start() {
        if (started) return;
        started = true;
        load();
    }

    public void retry() {
        FriendProfileUiState current = state.getValue();
        if (current.status != Status.ERROR) return;
        state.setValue(FriendProfileUiState.loading());
        load();
    }

    private void load() {
        socialExecutor.execute(() -> {
            try {
                SocialRepository.FriendProfile loaded = repository.friendProfile(friendUid);
                mainExecutor.execute(() -> state.setValue(new FriendProfileUiState(Status.READY, loaded.profile,
                        loaded.myUnlockedIds, null, false)));
            } catch (SocialException e) {
                mainExecutor.execute(() -> state.setValue(new FriendProfileUiState(Status.ERROR, null,
                        state.getValue().myUnlockedIds, e.error, false)));
            }
        });
    }

    public void removeFriend() {
        FriendProfileUiState current = state.getValue();
        if (current.status != Status.READY || current.removing) return;
        state.setValue(new FriendProfileUiState(Status.READY, current.profile, current.myUnlockedIds, null, true));
        socialExecutor.execute(() -> {
            try {
                repository.removeFriend(friendUid);
                mainExecutor.execute(() -> state.setValue(new FriendProfileUiState(Status.REMOVED, current.profile,
                        current.myUnlockedIds, null, false)));
            } catch (SocialException e) {
                mainExecutor.execute(() -> {
                    state.setValue(new FriendProfileUiState(Status.READY, current.profile, current.myUnlockedIds,
                            null, false));
                    removeFailures.setValue(new Event<>(e.error));
                });
            }
        });
    }

    public static final class Factory implements ViewModelProvider.Factory {

        private final String friendUid;
        private final SocialRepository repository;
        private final Executor socialExecutor;
        private final Executor mainExecutor;

        public Factory(String friendUid, SocialRepository repository, Executor socialExecutor, Executor mainExecutor) {
            this.friendUid = friendUid;
            this.repository = repository;
            this.socialExecutor = socialExecutor;
            this.mainExecutor = mainExecutor;
        }

        @NonNull
        @Override
        @SuppressWarnings("unchecked")
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            return (T) new FriendProfileViewModel(friendUid, repository, socialExecutor, mainExecutor);
        }
    }
}
