package com.ovigia.app.ui.auth;

import androidx.annotation.Nullable;

import com.ovigia.app.auth.AccountStore;
import com.ovigia.app.auth.AuthViewModel;
import com.ovigia.app.social.SocialRepository;

/**
 * O login visto pela conta online: quem já tinha conectado volta a ficar online
 * sem digitar a senha de novo, e um e-mail que este aparelho não conhece é
 * procurado no servidor antes de virar erro.
 */
final class OnlineAuth implements AuthViewModel.OnlineAccounts {

    private final SocialRepository social;

    OnlineAuth(SocialRepository social) {
        this.social = social;
    }

    @Override
    public void onSignedIn(AccountStore.Account account, String password) {
        social.resumeAfterSignIn(account, password);
    }

    @Nullable
    @Override
    public AccountStore.Account recover(String email, String password) {
        return social.recover(email, password);
    }
}
