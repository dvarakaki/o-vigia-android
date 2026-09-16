package com.ovigia.app.auth;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.ovigia.app.util.Event;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.concurrent.Executor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class AuthViewModelTest {

    @Rule
    public InstantTaskExecutorRule instantLiveData = new InstantTaskExecutorRule();

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private final Executor direct = Runnable::run;
    private AccountStore store;

    @Before
    public void setUp() {
        store = new AccountStore(() -> new File(tmp.getRoot(), "accounts.json"), 1_000);
    }

    @Test
    public void startsInSignIn_andSignUpCreatesAccountOnce() {
        AuthViewModel vm = new AuthViewModel(store, direct, direct);
        assertEquals(AuthUiState.Mode.SIGN_IN, vm.state().getValue().mode);

        vm.setMode(AuthUiState.Mode.SIGN_UP);
        vm.submit("Ana", "ana@b.com", "segredo1");

        Event<AccountStore.Account> event = vm.signedIn().getValue();
        assertNotNull(event);
        assertEquals("Ana", event.consume().name);
        assertNull("evento de sucesso é consumido uma vez", event.consume());
        assertFalse(vm.state().getValue().loading);
        assertNotNull(store.currentAccount());
    }

    @Test
    public void wrongPassword_reportsErrorAndStaysSignedOut() {
        store.signUp("Ana", "ana@b.com", "segredo1");
        store.signOut();

        AuthViewModel vm = new AuthViewModel(store, direct, direct);
        vm.submit("", "ana@b.com", "errada1");

        assertEquals(AccountStore.Error.WRONG_CREDENTIALS, vm.state().getValue().error);
        assertNull(vm.signedIn().getValue());
        assertNull(store.currentAccount());
    }

    @Test
    public void switchingMode_clearsPreviousError() {
        AuthViewModel vm = new AuthViewModel(store, direct, direct);
        vm.submit("", "sem-arroba", "x");
        assertEquals(AccountStore.Error.INVALID_EMAIL, vm.state().getValue().error);

        vm.setMode(AuthUiState.Mode.SIGN_UP);

        assertNull(vm.state().getValue().error);
        assertEquals(AuthUiState.Mode.SIGN_UP, vm.state().getValue().mode);
    }
}
