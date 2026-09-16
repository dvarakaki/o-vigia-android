package com.ovigia.app.ui.friends;

import android.view.View;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.ovigia.app.R;

import java.util.Base64;

/**
 * Foto e banner publicados por outros jogadores (JPEG em Base64 dentro dos
 * documentos online), com os mesmos padrões do próprio perfil quando não há imagem.
 */
public final class SharedImages {

    /** @param iconPaddingPx respiro do ícone padrão quando não há foto */
    public static void bindAvatar(Fragment fragment, ImageView view, @Nullable String base64, int iconPaddingPx) {
        byte[] bytes = decode(base64);
        if (bytes == null) {
            Glide.with(fragment).clear(view);
            view.setPadding(iconPaddingPx, iconPaddingPx, iconPaddingPx, iconPaddingPx);
            view.setImageResource(R.drawable.ic_person);
            view.setImageTintList(ContextCompat.getColorStateList(fragment.requireContext(), R.color.vigia_gold));
            return;
        }
        view.setPadding(0, 0, 0, 0);
        view.setImageTintList(null);
        Glide.with(fragment).load(bytes).transform(new CircleCrop()).into(view);
    }

    /** Sem banner: a galáxia do app com a tinta azul-violeta, como no próprio perfil. */
    public static void bindBanner(Fragment fragment, ImageView view, View tint, @Nullable String base64) {
        byte[] bytes = decode(base64);
        if (bytes == null) {
            Glide.with(fragment).clear(view);
            view.setImageResource(R.drawable.bg_galaxy);
            tint.setVisibility(View.VISIBLE);
            return;
        }
        tint.setVisibility(View.GONE);
        Glide.with(fragment).load(bytes).centerCrop().placeholder(R.drawable.bg_galaxy).into(view);
    }

    @Nullable
    private static byte[] decode(@Nullable String base64) {
        if (base64 == null || base64.isEmpty()) return null;
        try {
            return Base64.getDecoder().decode(base64);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private SharedImages() { }
}
