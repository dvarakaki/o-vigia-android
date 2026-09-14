package com.ovigia.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
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
import com.ovigia.app.databinding.ActivitySelecionarPersonagemBinding;
import com.ovigia.app.engine.CharacterProfile;
import com.ovigia.app.game.GameViewModel;

import java.util.List;

/**
 * Mostrada quando o jogador rejeita um chute e não quer responder mais
 * perguntas: lista os outros candidatos que o motor ainda considerava
 * prováveis, pra ele escolher um como o certo, ou dizer que nenhum aparece.
 */
public class SelecionarPersonagemActivity extends AppCompatActivity {

    /** Raio dos cantos arredondados da miniatura de cada card, em pixels. */
    private static final int THUMBNAIL_CORNER_RADIUS_PX = 24;

    private ActivitySelecionarPersonagemBinding binding;
    private GameViewModel viewModel;
    private Integer selectedCharacterId;
    private View selectedCard;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivitySelecionarPersonagemBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.main, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        OVigiaApplication app = (OVigiaApplication) getApplication();
        viewModel = new ViewModelProvider(app, app.gameViewModelFactory()).get(GameViewModel.class);

        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnNaoAparece.setOnClickListener(v -> viewModel.noneOfAlternatives());
        binding.btnValidar.setOnClickListener(v -> {
            if (selectedCharacterId != null) viewModel.confirmAlternateGuess(selectedCharacterId);
        });

        observeViewModel();
    }

    private void observeViewModel() {
        viewModel.alternatives().observe(this, this::renderAlternatives);

        viewModel.alternateConfirmed().observe(this, chosenId -> {
            if (chosenId == null) return;
            CharacterProfile chosen = viewModel.getProfile(chosenId);
            String name = chosen != null ? chosen.name : "";
            Toast.makeText(this, getString(R.string.alternate_correct_message, name),
                    Toast.LENGTH_LONG).show();
            goToMain();
        });

        viewModel.gameOver().observe(this, won -> {
            if (won == null) return;
            Toast.makeText(this, won ? R.string.victory_message : R.string.defeat_message,
                    Toast.LENGTH_LONG).show();
            goToMain();
        });
    }

    private void renderAlternatives(List<CharacterProfile> alternatives) {
        binding.containerAlternatives.removeAllViews();
        selectedCharacterId = null;
        selectedCard = null;
        setValidateEnabled(false);
        if (alternatives == null) return;

        for (CharacterProfile alternative : alternatives) {
            View card = LayoutInflater.from(this)
                    .inflate(R.layout.item_alternative_character, binding.containerAlternatives, false);
            ImageView image = card.findViewById(R.id.imageAlternative);
            TextView name = card.findViewById(R.id.tvAlternativeName);

            name.setText(alternative.name);
            // centerCrop + roundedCorners tem que vir junto num MultiTransformation:
            // usar .centerCrop() antes de .transform(RoundedCorners) faz o Glide
            // aplicar só a última e a miniatura vem sem crop.
            Glide.with(this)
                    .load(alternative.imageUrl)
                    .placeholder(R.drawable.ic_character_placeholder)
                    .error(R.drawable.ic_character_placeholder)
                    .transform(new MultiTransformation<>(
                            new CenterCrop(),
                            new RoundedCorners(THUMBNAIL_CORNER_RADIUS_PX)))
                    .into(image);

            card.setOnClickListener(v -> selectCard(card, alternative.id));
            binding.containerAlternatives.addView(card);
        }
    }

    private void selectCard(View card, int characterId) {
        if (selectedCard != null) {
            selectedCard.setBackgroundResource(R.drawable.bg_card_alternative);
        }
        card.setBackgroundResource(R.drawable.bg_card_alternative_selected);
        selectedCard = card;
        selectedCharacterId = characterId;
        setValidateEnabled(true);
    }

    private void setValidateEnabled(boolean enabled) {
        binding.btnValidar.setEnabled(enabled);
        binding.btnValidar.setAlpha(enabled ? 1f : 0.5f);
    }

    private void goToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}
