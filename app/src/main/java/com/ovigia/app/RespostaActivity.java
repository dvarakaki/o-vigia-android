package com.ovigia.app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.MultiTransformation;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.ovigia.app.databinding.ActivityRespostaBinding;
import com.ovigia.app.engine.CharacterProfile;
import com.ovigia.app.game.GameViewModel;

public class RespostaActivity extends AppCompatActivity {

    public static final String EXTRA_CHARACTER_ID = "extra_character_id";

    /** Raio dos cantos arredondados da foto do chute, em pixels. */
    private static final int GUESS_IMAGE_CORNER_RADIUS_PX = 32;

    private ActivityRespostaBinding binding;
    private GameViewModel viewModel;
    private int characterId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityRespostaBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.main, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        OVigiaApplication app = (OVigiaApplication) getApplication();
        viewModel = new ViewModelProvider(app, app.gameViewModelFactory()).get(GameViewModel.class);

        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnSim.setOnClickListener(v -> viewModel.confirmGuess());
        binding.btnNao.setOnClickListener(v -> {
            // Descarta o chute errado e passa a decisão pra próxima tela: o
            // jogador decide lá se quer continuar respondendo ou não. Substitui
            // esta Activity (não empilha) pra "Sim, continuar" voltar direto
            // pra tela de perguntas, sem passar de novo por aqui.
            viewModel.rejectGuess(characterId);
            startActivity(new Intent(this, ContinuarActivity.class));
            finish();
        });

        characterId = getIntent().getIntExtra(EXTRA_CHARACTER_ID, -1);
        if (!bindGuess(characterId)) {
            finish();
            return;
        }

        observeViewModel();
    }

    private void observeViewModel() {
        // Só resta o desfecho de vitória aqui: rejeitar o chute agora sempre
        // navega pra ContinuarActivity (ver o clique de btnNao acima), que é
        // quem trata o que vem a seguir (mais perguntas, novo chute ou
        // esgotar as alternativas).
        viewModel.gameOver().observe(this, won -> {
            if (won == null) return;
            Toast.makeText(this, won ? R.string.victory_message : R.string.defeat_message,
                    Toast.LENGTH_LONG).show();
            goToMain();
        });
    }

    private boolean bindGuess(int newCharacterId) {
        CharacterProfile guess = viewModel.getProfile(newCharacterId);
        if (guess == null) return false;

        characterId = newCharacterId;
        binding.tvGuessName.setText(guess.name);
        // centerCrop + roundedCorners tem que vir junto num MultiTransformation:
        // usar .centerCrop() antes de .transform(RoundedCorners) faz o Glide
        // aplicar só a última e a foto vem sem crop.
        Glide.with(this)
                .load(guess.imageUrl)
                .placeholder(R.drawable.ic_character_placeholder)
                .error(R.drawable.ic_character_placeholder)
                .transform(new MultiTransformation<>(
                        new CenterCrop(),
                        new RoundedCorners(GUESS_IMAGE_CORNER_RADIUS_PX)))
                .into(binding.imageGuess);
        return true;
    }

    private void goToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}
