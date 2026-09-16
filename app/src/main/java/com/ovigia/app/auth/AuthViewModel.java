package com.ovigia.app.auth;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
 *
 * Entrar com um e-mail que este aparelho não conhece não é logo um erro: pode
 * ser uma conta que existe no servidor (app reinstalado, celular novo). Antes de
 * recusar, o {@link OnlineAccounts} tenta trazê-la de volta.
 */
public class AuthViewModel extends ViewModel {

    /** A parte online do login. Sempre chamada no executor social (rede). */
    public interface OnlineAccounts {

        /** Depois de entrar ou criar a conta, com a senha digitada: reabre a sessão online. */
        void onSignedIn(AccountStore.Account account, String password);

        /**
         * Recria neste aparelho a conta online desse e-mail, já com a sessão
         * aberta, ou devolve {@code null} se não der (sem rede, senha errada ou
         * e-mail sem conta online).
         */
        @Nullable
        AccountStore.Account recover(String email, String password);
    }

    /** Login só local: nada de conta online. */
    private static final OnlineAccounts OFFLINE_ONLY = new OnlineAccounts() {
        @Override
        public void onSignedIn(AccountStore.Account account, String password) { }

        @Nullable
        @Override
        public AccountStore.Account recover(String email, String password) {
            return null;
        }
    };

    private final AccountStore accountStore;
    private final Executor ioExecutor;
    private final Executor socialExecutor;
    private final Executor mainExecutor;
    private final OnlineAccounts online;

    private final MutableLiveData<AuthUiState> state = new MutableLiveData<>(AuthUiState.initial(AuthUiState.Mode.SIGN_IN));
    private final MutableLiveData<Event<AccountStore.Account>> signedIn = new MutableLiveData<>();

    public AuthViewModel(AccountStore accountStore, Executor ioExecutor, Executor mainExecutor) {
        this(accountStore, ioExecutor, ioExecutor, mainExecutor, OFFLINE_ONLY);
    }

    public AuthViewModel(AccountStore accountStore, Executor ioExecutor, Executor socialExecutor,
                         Executor mainExecutor, OnlineAccounts online) {
        this.accountStore = accountStore;
        this.ioExecutor = ioExecutor;
        this.socialExecutor = socialExecutor;
        this.mainExecutor = mainExecutor;
        this.online = online;
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
            if (result.isSuccess()) {
                socialExecutor.execute(() -> online.onSignedIn(result.account, password));
                finish(mode, result.account, null);
                return;
            }
            if (mode == AuthUiState.Mode.SIGN_IN && result.error == AccountStore.Error.WRONG_CREDENTIALS
                    && !accountStore.knowsEmail(email)) {
                // E-mail que este aparelho não conhece: a conta pode estar no servidor.
                // Com a conta aqui, só a senha está errada — não adianta procurar.
                socialExecutor.execute(() -> {
                    AccountStore.Account recovered = online.recover(email, password);
                    finish(mode, recovered, result.error);
                });
                return;
            }
            finish(mode, null, result.error);
        });
    }

    private void finish(AuthUiState.Mode mode, @Nullable AccountStore.Account account,
                        @Nullable AccountStore.Error error) {
        mainExecutor.execute(() -> {
            if (account != null) {
                state.setValue(AuthUiState.initial(mode));
                signedIn.setValue(new Event<>(account));
            } else {
                state.setValue(AuthUiState.failed(mode, error));
            }
        });
    }

    public static final class Factory implements ViewModelProvider.Factory {

        private final AccountStore accountStore;
        private final Executor ioExecutor;
        private final Executor socialExecutor;
        private final Executor mainExecutor;
        private final OnlineAccounts online;

        public Factory(AccountStore accountStore, Executor ioExecutor, Executor socialExecutor,
                       Executor mainExecutor, OnlineAccounts online) {
            this.accountStore = accountStore;
            this.ioExecutor = ioExecutor;
            this.socialExecutor = socialExecutor;
            this.mainExecutor = mainExecutor;
            this.online = online;
        }

        @NonNull
        @Override
        @SuppressWarnings("unchecked")
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            return (T) new AuthViewModel(accountStore, ioExecutor, socialExecutor, mainExecutor, online);
        }
    }
}
