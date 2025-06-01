package com.example.regimaps;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import com.example.regimaps.databinding.ActivityMainMenuBinding;

/**
 * Activitat principal del menú de l'aplicació.
 * Mostra tres opcions: veure punts guardats, veure polígons i accedir al mapa.
 */
public class MainMenuActivity extends AppCompatActivity {

    // View binding per accedir als elements de la interfície de manera segura.
    private ActivityMainMenuBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Infla el layout utilitzant View Binding.
        binding = ActivityMainMenuBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        // Botó per veure els punts guardats
        binding.btnViewSavedPoints.setOnClickListener(v -> {
            Log.d("MainMenuActivity", "Obrint llista de punts guardats");
            Intent intent = new Intent(MainMenuActivity.this, SavedPointsListActivity.class);
            startActivity(intent);
        });

        // Botó per veure els polígons guardats.
        binding.btnViewPolygons.setOnClickListener(v -> {
            Log.d("MainMenuActivity", "Obrint llista de polígons");
            Intent intent = new Intent(MainMenuActivity.this, PolygonsListActivity.class);
            startActivity(intent);
        });

        // Botó per anar al mapa principal.
        binding.btnGoToMap.setOnClickListener(v -> {
            Log.d("MainMenuActivity", "Obrint mapa");
            Intent intent = new Intent(MainMenuActivity.this, MapsActivity.class);
            startActivity(intent);
        });
    }
}