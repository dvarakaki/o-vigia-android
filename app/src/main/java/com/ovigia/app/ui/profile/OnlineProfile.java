package com.ovigia.app.ui.profile;

import com.ovigia.app.auth.AccountStore;
import com.ovigia.app.profile.EditProfileViewModel;
import com.ovigia.app.social.SocialException;
import com.ovigia.app.social.SocialRepository;

import java.util.concurrent.Executor;

/** A edição do perfil vista pela conta online: mudanças publicadas, senha e exclusão acompanhando a local. */
final class OnlineProfile implements EditProfileViewModel.OnlineAccount {

    private final SocialRepository social;
    private final Executor socialExecutor;

    OnlineProfile(SocialRepository social, Executor socialExecutor) {
        this.social = social;
        this.socialExecutor = socialExecutor;
    }

    @Override
    public void profileChanged() {
        social.publishQuietly();
    }

    @Override
    public void passwordChanged(AccountStore.Account account, String currentPassword, String newPassword) {
        socialExecutor.execute(() -> social.onPasswordChanged(account, currentPassword, newPassword));
    }

    @Override
    public void deleteOnline(AccountStore.Account account, String password) throws SocialException {
        social.deleteOnlineAccount(account, password);
    }
}
