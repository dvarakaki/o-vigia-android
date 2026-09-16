package com.ovigia.app.ui.home;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.SavedStateHandle;
import androidx.navigation.NavBackStackEntry;
import androidx.navigation.NavDestination;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.snackbar.Snackbar;
import com.ovigia.app.OVigiaApplication;
import com.ovigia.app.R;
import com.ovigia.app.databinding.FragmentHomeBinding;
import com.ovigia.app.ui.FadeNavOptions;
import com.ovigia.app.ui.Motion;
import com.ovigia.app.ui.SystemBarInsets;

/** Tela inicial: começar partida, catálogo de heróis, perfil e configurações. */
public class HomeFragment extends Fragment {

    /** Mensagem de resultado deixada pela partida que acabou de terminar. */
    public static final String KEY_RESULT_MESSAGE = "result_message";

    private FragmentHomeBinding binding;
    private final Motion motion = new Motion();
    /** A entrada só toca ao abrir o app (voltar de outra tela não repete). */
    private boolean entered = false;

    public HomeFragment() {
        super(R.layout.fragment_home);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding = FragmentHomeBinding.bind(view);
        SystemBarInsets.padTop(view);
        if (!entered && savedInstanceState == null) {
            motion.popIn(binding.imageArt, 60);
            motion.staggerIn(160, binding.title.getRoot(), binding.btnMeDesafie);
        }
        entered = true;

        binding.btnMeDesafie.setOnClickListener(v -> {
            if (isCurrent()) NavHostFragment.findNavController(this).navigate(R.id.action_home_to_game);
        });
        // Perfil e catálogo são da conta: sem login, o login abre antes e segue para eles.
        binding.btnProfile.setOnClickListener(v ->
                FadeNavOptions.navigateSignedIn(this, R.id.homeFragment, R.id.profileFragment, null));
        binding.btnCatalog.setOnClickListener(v ->
                FadeNavOptions.navigateSignedIn(this, R.id.homeFragment, R.id.catalogFragment, null));
        binding.btnFriends.setOnClickListener(v ->
                FadeNavOptions.navigateSignedIn(this, R.id.homeFragment, R.id.friendsFragment, null));
        binding.btnSettings.setOnClickListener(v -> {
            if (isCurrent()) {
                NavHostFragment.findNavController(this).navigate(R.id.settingsFragment, null,
                        FadeNavOptions.builder().build());
            }
        });

        NavBackStackEntry entry = NavHostFragment.findNavController(this).getCurrentBackStackEntry();
        if (entry != null) {
            SavedStateHandle handle = entry.getSavedStateHandle();
            handle.<String>getLiveData(KEY_RESULT_MESSAGE).observe(getViewLifecycleOwner(), message -> {
                if (message == null) return;
                handle.set(KEY_RESULT_MESSAGE, null);
                // Partida perdida sem revelar: as estatísticas mudaram para os amigos também.
                ((OVigiaApplication) requireActivity().getApplication()).container().socialRepository.publishQuietly();
                Snackbar.make(view, message, Snackbar.LENGTH_LONG)
                        .setAnchorView(binding.btnMeDesafie)
                        .show();
            });
        }
    }

    @Override
    public void onDestroyView() {
        motion.cancelAll();
        super.onDestroyView();
        binding = null;
    }

    /** Se a Home ainda é o destino atual — toque duplo não abre a tela duas vezes. */
    private boolean isCurrent() {
        NavDestination current = NavHostFragment.findNavController(this).getCurrentDestination();
        return isAdded() && current != null && current.getId() == R.id.homeFragment;
    }
}
