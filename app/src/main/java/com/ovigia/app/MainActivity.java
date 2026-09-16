package com.ovigia.app;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.splashscreen.SplashScreen;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.ovigia.app.databinding.ActivityMainBinding;

/**
 * Activity única: hospeda o NavHostFragment com todas as telas. O fundo espacial
 * vai de ponta a ponta; aqui o conteúdo recua das laterais, da barra de navegação
 * e do teclado. O topo (barra de status) fica com cada tela — ver
 * {@link com.ovigia.app.ui.SystemBarInsets} — para permitir cabeçalhos imersivos.
 */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.navHost, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout()
                    | WindowInsetsCompat.Type.ime());
            v.setPadding(bars.left, 0, bars.right, bars.bottom);
            // Não consome: as telas ainda precisam do topo.
            return insets;
        });
    }
}
