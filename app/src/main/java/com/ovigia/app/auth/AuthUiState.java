package com.ovigia.app.auth;

/** Estado da tela de login/cadastro. Imutável. */
public final class AuthUiState {

    public enum Mode { SIGN_IN, SIGN_UP }

    public final Mode mode;
    public final boolean loading;
    /** Último erro de validação ou de credenciais; {@code null} se não há. */
    public final AccountStore.Error error;

    private AuthUiState(Mode mode, boolean loading, AccountStore.Error error) {
        this.mode = mode;
        this.loading = loading;
        this.error = error;
    }

    static AuthUiState initial(Mode mode) {
        return new AuthUiState(mode, false, null);
    }

    static AuthUiState loading(Mode mode) {
        return new AuthUiState(mode, true, null);
    }

    static AuthUiState failed(Mode mode, AccountStore.Error error) {
        return new AuthUiState(mode, false, error);
    }
}
