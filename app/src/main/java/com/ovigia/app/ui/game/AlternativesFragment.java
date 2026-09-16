package com.ovigia.app.ui.game;

import android.os.Bundle;
import android.view.View;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.ovigia.app.R;

/** Candidatos que o motor ainda considerava prováveis, para o jogador apontar o certo. */
public class AlternativesFragment extends CharacterListFragment {

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.tvTitle.setText(R.string.select_alternative_title);
        binding.btnPrimary.setText(R.string.btn_validar);
        binding.btnSecondary.setText(R.string.btn_none_of_alternatives);
        binding.btnSecondary.setOnClickListener(v -> {
            if (isCurrentDestination(R.id.alternativesFragment)) nav().navigate(R.id.revealFragment);
        });

        // Voltar = continuar respondendo perguntas.
        OnBackPressedCallback back = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (isCurrentDestination(R.id.alternativesFragment)) viewModel.continueGuessing();
            }
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), back);
        binding.btnBack.setOnClickListener(v -> back.handleOnBackPressed());
    }

    @Override
    protected int destinationId() {
        return R.id.alternativesFragment;
    }

    @Override
    protected void onCharactersAvailable() {
        show(viewModel.alternatives());
    }

    @Override
    protected void onConfirm(int characterId) {
        viewModel.confirmAlternative(characterId);
    }
}
