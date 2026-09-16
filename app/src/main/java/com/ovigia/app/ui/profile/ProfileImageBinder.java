package com.ovigia.app.ui.profile;

import android.view.View;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.ovigia.app.R;

import java.io.File;

/** Carrega foto e banner do perfil (ou os padrões) — igual no perfil e na edição. */
final class ProfileImageBinder {

    static void bindAvatar(Fragment fragment, ImageView view, @Nullable File file) {
        if (file == null) {
            Glide.with(fragment).clear(view);
            int padding = fragment.getResources().getDimensionPixelSize(R.dimen.avatar_icon_padding);
            view.setPadding(padding, padding, padding, padding);
            view.setImageResource(R.drawable.ic_person);
            view.setImageTintList(ContextCompat.getColorStateList(fragment.requireContext(), R.color.vigia_gold));
            return;
        }
        view.setPadding(0, 0, 0, 0);
        view.setImageTintList(null);
        // O nome do arquivo muda a cada troca, então o cache do Glide nunca mostra a foto antiga.
        Glide.with(fragment).load(file).transform(new CircleCrop())
                .transition(DrawableTransitionOptions.withCrossFade(200))
                .into(view);
    }

    /**
     * Sem imagem própria: a galáxia do app com a tinta azul-violeta ({@code tint}).
     * Com imagem: a foto do jogador, sem tinta (só o degradê que funde com a tela).
     */
    static void bindBanner(Fragment fragment, ImageView view, View tint, @Nullable File file) {
        if (file == null) {
            Glide.with(fragment).clear(view);
            view.setImageResource(R.drawable.bg_galaxy);
            tint.setVisibility(View.VISIBLE);
            return;
        }
        tint.setVisibility(View.GONE);
        Glide.with(fragment).load(file).centerCrop()
                .placeholder(R.drawable.bg_galaxy)
                .error(R.drawable.bg_galaxy)
                .transition(DrawableTransitionOptions.withCrossFade(200))
                .into(view);
    }

    private ProfileImageBinder() { }
}
