package com.ovigia.app.ui;

import android.animation.Animator;
import android.widget.ImageView;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;

import com.ovigia.app.R;
import com.ovigia.app.game.WatcherMood;

/**
 * A arte do Vigia numa tela: mostra o sprite do {@link WatcherMood} atual.
 * A primeira exibição entra direto (a tela já tem sua própria transição);
 * mudanças de humor depois disso trocam o sprite com {@link Motion#swapImage}.
 */
public final class WatcherArt {

    private final ImageView image;
    private final Motion motion;
    @Nullable private WatcherMood shown;
    @Nullable private Animator swap;

    public WatcherArt(ImageView image, Motion motion) {
        this.image = image;
        this.motion = motion;
    }

    public void show(WatcherMood mood) {
        if (mood == shown) return;
        boolean firstTime = shown == null;
        shown = mood;
        if (swap != null) swap.cancel();
        if (firstTime) {
            image.setImageResource(drawableFor(mood));
        } else {
            swap = motion.swapImage(image, drawableFor(mood), mood == WatcherMood.ANGRY);
        }
    }

    /** Sinal visível de que ele registrou um "Não sei" — o humor por design não muda. */
    public void shrug() {
        motion.shrug(image);
    }

    @DrawableRes
    public static int drawableFor(WatcherMood mood) {
        switch (mood) {
            case FOCUSED: return R.drawable.art_watcher_focused;
            case CONFIDENT: return R.drawable.art_watcher_confident;
            case SKEPTICAL: return R.drawable.art_watcher_skeptical;
            case ANGRY: return R.drawable.art_watcher_angry;
            case TRIUMPHANT: return R.drawable.art_watcher_triumphant;
            case THINKING:
            default: return R.drawable.art_watcher;
        }
    }
}
