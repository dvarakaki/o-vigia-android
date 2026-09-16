package com.ovigia.app.ui.auth;

import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavBackStackEntry;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.ovigia.app.AppContainer;
import com.ovigia.app.OVigiaApplication;
import com.ovigia.app.R;
import com.ovigia.app.auth.AccountStore;
import com.ovigia.app.auth.AuthUiState;
import com.ovigia.app.auth.AuthViewModel;
import com.ovigia.app.databinding.FragmentAuthBinding;
import com.ovigia.app.ui.FadeNavOptions;
import com.ovigia.app.ui.SystemBarInsets;

/**
 * Login e cadastro. Dois usos:
 * <ul>
 *   <li>{@link #ARG_NEXT_DESTINATION} com um destino (perfil, catálogo): ao entrar,
 *       abre esse destino no lugar desta tela.</li>
 *   <li>Sem destino (ex.: desbloquear herói no resultado): ao entrar, volta para quem
 *       abriu avisando pelo {@link #KEY_SIGNED_IN} no SavedStateHandle dela.</li>
 * </ul>
 */
public class AuthFragment extends Fragment {

    /** Id do destino a abrir depois do login; 0 = voltar para quem abriu. */
    public static final String ARG_NEXT_DESTINATION = "nextDestination";
    public static final String ARG_REASON = "reason";
    /** Resultado deixado na tela anterior quando o login dá certo. */
    public static final String KEY_SIGNED_IN = "signed_in";

    private FragmentAuthBinding binding;
    private AuthViewModel viewModel;

    public AuthFragment() {
        super(R.layout.fragment_auth);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding = FragmentAuthBinding.bind(view);
        SystemBarInsets.padTop(view);

        AppContainer container = ((OVigiaApplication) requireActivity().getApplication()).container();
        // Quem já tinha conectado aos amigos volta a ficar online sem digitar a senha de novo.
        viewModel = new ViewModelProvider(this, new AuthViewModel.Factory(
                container.accountStore, container.ioExecutor, container.mainExecutor,
                (account, password) -> container.socialExecutor.execute(
                        () -> container.socialRepository.resumeAfterSignIn(account, password))))
                .get(AuthViewModel.class);

        binding.btnBack.setOnClickListener(v -> nav().popBackStack());

        String reason = requireArguments().getString(ARG_REASON);
        binding.tvReason.setText(reason);
        binding.tvReason.setVisibility(reason != null ? View.VISIBLE : View.GONE);

        binding.modeToggle.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            clearErrors();
            viewModel.setMode(checkedId == R.id.btnModeSignUp ? AuthUiState.Mode.SIGN_UP : AuthUiState.Mode.SIGN_IN);
        });
        binding.btnSubmit.setOnClickListener(v -> submit());
        binding.etPassword.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId != EditorInfo.IME_ACTION_DONE) return false;
            submit();
            return true;
        });

        viewModel.state().observe(getViewLifecycleOwner(), this::render);
        viewModel.signedIn().observe(getViewLifecycleOwner(), event -> {
            AccountStore.Account account = event.consume();
            if (account != null) onSignedIn();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private NavController nav() {
        return NavHostFragment.findNavController(this);
    }

    private void submit() {
        clearErrors();
        viewModel.submit(text(binding.etName), text(binding.etEmail), text(binding.etPassword));
    }

    private static String text(EditText field) {
        return field.getText() == null ? "" : field.getText().toString();
    }

    private void render(AuthUiState state) {
        boolean signUp = state.mode == AuthUiState.Mode.SIGN_UP;
        binding.modeToggle.check(signUp ? R.id.btnModeSignUp : R.id.btnModeSignIn);
        binding.tvTitle.setText(signUp ? R.string.auth_title_sign_up : R.string.auth_title_sign_in);
        binding.nameLayout.setVisibility(signUp ? View.VISIBLE : View.GONE);
        binding.passwordLayout.setHelperText(signUp ? getString(R.string.auth_password_helper) : null);

        binding.btnSubmit.setText(state.loading ? null
                : getString(signUp ? R.string.auth_submit_sign_up : R.string.auth_submit_sign_in));
        binding.btnSubmit.setEnabled(!state.loading);
        binding.progress.setVisibility(state.loading ? View.VISIBLE : View.GONE);
        for (int i = 0; i < binding.modeToggle.getChildCount(); i++) {
            binding.modeToggle.getChildAt(i).setEnabled(!state.loading);
        }

        if (state.error != null) showError(state.error);
    }

    private void showError(AccountStore.Error error) {
        switch (error) {
            case NAME_REQUIRED:
                binding.nameLayout.setError(getString(R.string.auth_error_name_required));
                break;
            case INVALID_EMAIL:
                binding.emailLayout.setError(getString(R.string.auth_error_invalid_email));
                break;
            case EMAIL_IN_USE:
                binding.emailLayout.setError(getString(R.string.auth_error_email_in_use));
                break;
            case WEAK_PASSWORD:
                binding.passwordLayout.setError(getString(R.string.auth_error_weak_password));
                break;
            case WRONG_CREDENTIALS:
            default:
                binding.passwordLayout.setError(getString(R.string.auth_error_wrong_credentials));
                break;
        }
    }

    private void clearErrors() {
        binding.nameLayout.setError(null);
        binding.emailLayout.setError(null);
        binding.passwordLayout.setError(null);
    }

    private void onSignedIn() {
        WindowCompat.getInsetsController(requireActivity().getWindow(), requireView())
                .hide(WindowInsetsCompat.Type.ime());
        NavController nav = nav();
        int next = requireArguments().getInt(ARG_NEXT_DESTINATION, 0);
        if (next != 0) {
            nav.navigate(next, null, FadeNavOptions.popUpTo(R.id.authFragment, true));
            return;
        }
        NavBackStackEntry previous = nav.getPreviousBackStackEntry();
        if (previous != null) previous.getSavedStateHandle().set(KEY_SIGNED_IN, true);
        nav.popBackStack();
    }
}
