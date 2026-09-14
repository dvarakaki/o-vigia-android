package com.ovigia.app;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.ovigia.app.databinding.ActivityPerguntasBinding;
import com.ovigia.app.engine.Answer;
import com.ovigia.app.game.GameViewModel;

public class PerguntasActivity extends AppCompatActivity {

    private ActivityPerguntasBinding binding;
    private GameViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityPerguntasBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.main, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        OVigiaApplication app = (OVigiaApplication) getApplication();
        viewModel = new ViewModelProvider(app, app.gameViewModelFactory()).get(GameViewModel.class);

        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnPreviousQuestion.setOnClickListener(v -> viewModel.goBackOneQuestion());
        binding.btnSim.setOnClickListener(v -> viewModel.answer(Answer.SIM));
        binding.btnProvavelmenteSim.setOnClickListener(v -> viewModel.answer(Answer.PROVAVELMENTE_SIM));
        binding.btnNaoSei.setOnClickListener(v -> viewModel.answer(Answer.NAO_SEI));
        binding.btnProvavelmenteNao.setOnClickListener(v -> viewModel.answer(Answer.PROVAVELMENTE_NAO));
        binding.btnNao.setOnClickListener(v -> viewModel.answer(Answer.NAO));

        observeViewModel();
    }

    @Override
    protected void onStart() {
        super.onStart();
        viewModel.ensureStarted();
    }

    private void observeViewModel() {
        viewModel.isLoading().observe(this, loading -> {
            boolean isLoading = Boolean.TRUE.equals(loading);
            setButtonsEnabled(!isLoading);
            if (isLoading) {
                binding.tvQuestionNumber.setText("");
                binding.tvQuestionText.setOnClickListener(null);
                binding.tvQuestionText.setText(R.string.question_placeholder);
            }
        });

        viewModel.error().observe(this, message -> {
            if (message == null) return;
            setButtonsEnabled(false);
            binding.tvQuestionText.setText(message);
            binding.tvQuestionText.setOnClickListener(v -> viewModel.ensureStarted());
        });

        viewModel.questionNumber().observe(this, number -> {
            if (number == null) return;
            binding.tvQuestionNumber.setText(getString(R.string.question_number, number));
        });

        viewModel.canGoBack().observe(this, canGoBack -> {
            boolean enabled = Boolean.TRUE.equals(canGoBack);
            binding.btnPreviousQuestion.setEnabled(enabled);
            binding.btnPreviousQuestion.setAlpha(enabled ? 1f : 0.3f);
        });

        viewModel.question().observe(this, text -> {
            if (text == null) return;
            binding.tvQuestionText.setOnClickListener(null);
            binding.tvQuestionText.setText(text);
            setButtonsEnabled(true);
        });

        // O motor só desiste (gameOver=false) depois de um chute rejeitado,
        // e chutes só acontecem na RespostaActivity — é lá que esse evento é
        // tratado, nunca aqui.
        viewModel.navigateToGuess().observe(this, characterId -> {
            if (characterId == null) return;
            Intent intent = new Intent(this, RespostaActivity.class);
            intent.putExtra(RespostaActivity.EXTRA_CHARACTER_ID, characterId);
            startActivity(intent);
        });
    }

    private void setButtonsEnabled(boolean enabled) {
        binding.btnSim.setEnabled(enabled);
        binding.btnProvavelmenteSim.setEnabled(enabled);
        binding.btnNaoSei.setEnabled(enabled);
        binding.btnProvavelmenteNao.setEnabled(enabled);
        binding.btnNao.setEnabled(enabled);
    }
}
