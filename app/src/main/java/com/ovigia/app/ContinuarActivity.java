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

import com.ovigia.app.databinding.ActivityContinuarBinding;
import com.ovigia.app.game.GameViewModel;

/**
 * Tela mostrada depois que o jogador rejeita um chute: pergunta se ele quer
 * continuar respondendo perguntas ou parar e escolher entre as outras
 * alternativas que o motor ainda considerava prováveis.
 */
public class ContinuarActivity extends AppCompatActivity {

    private ActivityContinuarBinding binding;
    private GameViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityContinuarBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.main, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        OVigiaApplication app = (OVigiaApplication) getApplication();
        viewModel = new ViewModelProvider(app, app.gameViewModelFactory()).get(GameViewModel.class);

        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnSim.setOnClickListener(v -> viewModel.continueGuessing());
        binding.btnNao.setOnClickListener(v -> {
            // Preenche a lista de alternativas antes de navegar — a próxima tela
            // só lê o valor atual, não é um evento de disparo único.
            viewModel.stopGuessing();
            startActivity(new Intent(this, SelecionarPersonagemActivity.class));
            finish();
        });

        observeViewModel();
    }

    private void observeViewModel() {
        // "Sim, continuar": o motor decidiu que precisa perguntar mais coisa —
        // a PerguntasActivity (por baixo na pilha) já reage sozinha ao novo
        // valor de `question`; só fechamos esta tela.
        viewModel.backToQuestions().observe(this, ignored -> finish());

        // O motor já tinha certeza o bastante de outro candidato pra chutar de
        // novo, sem passar por mais perguntas.
        viewModel.navigateToGuess().observe(this, characterId -> {
            if (characterId == null) return;
            Intent intent = new Intent(this, RespostaActivity.class);
            intent.putExtra(RespostaActivity.EXTRA_CHARACTER_ID, characterId);
            startActivity(intent);
            finish();
        });

        // O motor ficou sem candidatos pra oferecer.
        viewModel.gameOver().observe(this, won -> {
            if (won == null) return;
            Toast.makeText(this, won ? R.string.victory_message : R.string.defeat_message,
                    Toast.LENGTH_LONG).show();
            goToMain();
        });
    }

    private void goToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}
