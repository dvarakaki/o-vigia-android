package com.ovigia.app.auth;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.ovigia.app.util.Event;

import java.util.concurrent.Executor;

/**
 * Tela de login/cadastro: alterna o modo, valida no {@link AccountStore} (fora
 * da main thread — o hash da senha é lento de propósito) e avisa o sucesso uma
 * única vez.
 */
public class AuthViewModel extends ViewModel {

    /** Chamado no I/O depois de entrar ou criar a conta, com a senha digitada (ex.: reabrir a sessão online). */
    public interface SignInHook {
        void onSignedIn(AccountStore.Account account, String password);
    }

    private final AccountStore accountStore;
    private final Executor ioExecutor;
    private final Executor mainExecutor;
    private final SignInHook signInHook;

    private final MutableLiveData<AuthUiState> state = new MutableLiveData<>(AuthUiState.initial(AuthUiState.Mode.SIGN_IN));
    private final MutableLiveData<Event<AccountStore.Account>> signedIn = new MutableLiveData<>();

    public AuthViewModel(AccountStore accountStore, Executor ioExecutor, Executor mainExecutor) {
        this(accountStore, ioExecutor, mainExecutor, (account, password) -> { });
    }

    public AuthViewModel(AccountStore accountStore, Executor ioExecutor, Executor mainExecutor, SignInHook signInHook) {
        this.accountStore = accountStore;
        this.ioExecutor = ioExecutor;
        this.mainExecutor = mainExecutor;
        this.signInHook = signInHook;
    }

    public LiveData<AuthUiState> state() { return state; }

    /** Disparado uma vez quando o login ou o cadastro dá certo. */
    public LiveData<Event<AccountStore.Account>> signedIn() { return signedIn; }

    public void setMode(AuthUiState.Mode mode) {
        AuthUiState current = state.getValue();
        if (current.loading || current.mode == mode) return;
        state.setValue(AuthUiState.initial(mode));
    }

    public void submit(String name, String email, String password) {
        AuthUiState current = state.getValue();
        if (current.loading) return;
        AuthUiState.Mode mode = current.mode;
        state.setValue(AuthUiState.loading(mode));
        ioExecutor.execute(() -> {
            AccountStore.Result result = mode == AuthUiState.Mode.SIGN_UP
                    ? accountStore.signUp(name, email, password)
                    : accountStore.signIn(email, password);
            if (result.isSuccess()) signInHook.onSignedIn(result.account, password);
            mainExecutor.execute(() -> {
                if (result.isSuccess()) {
                    state.setValue(AuthUiState.initial(mode));
                    signedIn.setValue(new Event<>(result.account));
                } else {
                    state.setValue(AuthUiState.failed(mode, result.error));
                }
            });
        });
    }

    public static final class Factory implements ViewModelProvider.Factory {

        private final AccountStore accountStore;
        private final Executor ioExecutor;
        private final Executor mainExecutor;
        private final SignInHook signInHook;

        public Factory(AccountStore accountStore, Executor ioExecutor, Executor mainExecutor, SignInHook signInHook) {
            this.accountStore = accountStore;
            this.ioExecutor = ioExecutor;
            this.mainExecutor = mainExecutor;
            this.signInHook = signInHook;
        }

        @NonNull
        @Override
        @SuppressWarnings("unchecked")
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            return (T) new AuthViewModel(accountStore, ioExecutor, mainExecutor, signInHook);
        }
    }
}
