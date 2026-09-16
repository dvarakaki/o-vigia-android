package com.ovigia.app.profile;

import com.ovigia.app.auth.AccountStore;

import java.io.File;

/** Estado da tela de edição do perfil. Imutável. */
public final class EditProfileUiState {

    public enum Status {
        LOADING,
        READY,
        /** Não há sessão: a tela deve sair. */
        SIGNED_OUT,
        /** A conta acabou de ser excluída. */
        DELETED
    }

    /** O que está sendo gravado agora; a tela desabilita só a seção correspondente. */
    public enum Busy { NONE, PROFILE, PASSWORD, IMAGE, DELETE }

    public final Status status;
    public final Busy busy;
    public final String name;
    public final String email;
    public final String bio;
    public final File avatarFile;
    public final File bannerFile;
    /** Erro da seção de dados (nome, bio, e-mail); {@code null} se não há. */
    public final AccountStore.Error profileError;
    public final AccountStore.Error passwordError;
    public final AccountStore.Error deleteError;

    EditProfileUiState(Status status, Busy busy, String name, String email, String bio, File avatarFile,
                       File bannerFile, AccountStore.Error profileError, AccountStore.Error passwordError,
                       AccountStore.Error deleteError) {
        this.status = status;
        this.busy = busy;
        this.name = name;
        this.email = email;
        this.bio = bio;
        this.avatarFile = avatarFile;
        this.bannerFile = bannerFile;
        this.profileError = profileError;
        this.passwordError = passwordError;
        this.deleteError = deleteError;
    }

    static EditProfileUiState of(Status status) {
        return new EditProfileUiState(status, Busy.NONE, null, null, null, null, null, null, null, null);
    }

    public boolean isBusy() {
        return busy != Busy.NONE;
    }

    /** Aviso pontual mostrado depois de uma operação. */
    public enum Message {
        PROFILE_SAVED,
        PASSWORD_CHANGED,
        IMAGE_UPDATED,
        IMAGE_REMOVED,
        IMAGE_FAILED
    }
}
