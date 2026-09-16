package com.ovigia.app.ui.game;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.ovigia.app.R;
import com.ovigia.app.databinding.FragmentCharacterListBinding;
import com.ovigia.app.engine.CharacterProfile;

import java.util.List;

/** Base das telas que listam personagens para o jogador escolher um. */
abstract class CharacterListFragment extends GameFragment {

    private static final String STATE_SELECTED = "selected";

    protected FragmentCharacterListBinding binding;
    protected CharacterAdapter adapter;
    private boolean populated = false;

    CharacterListFragment() {
        super(R.layout.fragment_character_list);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        binding = FragmentCharacterListBinding.bind(view);
        super.onViewCreated(view, savedInstanceState);

        adapter = new CharacterAdapter(id -> binding.btnPrimary.setEnabled(true));
        if (savedInstanceState != null) {
            adapter.setSelectedId(savedInstanceState.getInt(STATE_SELECTED, -1));
        }
        binding.list.setAdapter(adapter);
        binding.btnPrimary.setOnClickListener(v -> {
            if (adapter.selectedId() >= 0 && isCurrentDestination(destinationId())) {
                onConfirm(adapter.selectedId());
            }
        });

        viewModel.state().observe(getViewLifecycleOwner(), state -> {
            binding.progress.setVisibility(state.isLoaded() ? View.GONE : View.VISIBLE);
            if (!populated && state.isLoaded()) {
                populated = true;
                onCharactersAvailable();
            }
        });
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (adapter != null) outState.putInt(STATE_SELECTED, adapter.selectedId());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        adapter = null;
        populated = false;
    }

    /** Mostra {@code characters}, mantendo a seleção só se ela ainda estiver na lista. */
    protected void show(List<CharacterProfile> characters) {
        boolean selectionVisible = false;
        for (CharacterProfile c : characters) {
            if (c.id == adapter.selectedId()) selectionVisible = true;
        }
        if (!selectionVisible) adapter.setSelectedId(-1);
        binding.btnPrimary.setEnabled(selectionVisible);
        binding.tvEmpty.setVisibility(characters.isEmpty() ? View.VISIBLE : View.GONE);
        adapter.submitList(characters);
    }

    protected abstract int destinationId();

    /** O elenco está carregado: preencher a lista. */
    protected abstract void onCharactersAvailable();

    protected abstract void onConfirm(int characterId);
}
