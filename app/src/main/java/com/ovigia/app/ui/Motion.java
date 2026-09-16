package com.ovigia.app.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntFunction;

/**
 * Animações do app, curtas e discretas. Todas passam pelo sistema de animação
 * do Android, então respeitam "Remover animações" e a escala de animação das
 * opções do desenvolvedor.
 *
 * Cada tela guarda um {@link Motion} e chama {@link #cancelAll()} ao destruir a
 * view, para nenhuma animação continuar mexendo em views mortas.
 */
public final class Motion {

    public static final long ENTER_MS = 360;
    private static final long STAGGER_MS = 55;
    /** A cascata para de crescer depois de alguns itens: telas longas não ficam esperando. */
    private static final int MAX_STAGGER_STEPS = 8;

    private final List<Animator> running = new ArrayList<>();

    /** Cancela tudo que ainda está rodando (chamar em {@code onDestroyView}). */
    public void cancelAll() {
        for (Animator a : new ArrayList<>(running)) a.cancel();
        running.clear();
    }

    private <T extends Animator> T track(T animator) {
        running.add(animator);
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                running.remove(animation);
            }
        });
        return animator;
    }

    /** Entrada em cascata: cada view surge subindo um pouco, uma logo após a outra. */
    public void staggerIn(long startDelay, View... views) {
        int step = 0;
        for (View v : views) {
            if (v == null || v.getVisibility() != View.VISIBLE) continue;
            fadeUp(v, startDelay + Math.min(step, MAX_STAGGER_STEPS) * STAGGER_MS);
            step++;
        }
    }

    public void fadeUp(View view, long delay) {
        float distance = view.getResources().getDisplayMetrics().density * 16;
        view.setAlpha(0f);
        view.setTranslationY(distance);
        ObjectAnimator alpha = ObjectAnimator.ofFloat(view, View.ALPHA, 0f, 1f);
        ObjectAnimator move = ObjectAnimator.ofFloat(view, View.TRANSLATION_Y, distance, 0f);
        AnimatorSet set = new AnimatorSet();
        set.playTogether(alpha, move);
        set.setDuration(ENTER_MS);
        set.setStartDelay(delay);
        set.setInterpolator(new FastOutSlowInInterpolator());
        set.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationCancel(Animator animation) {
                view.setAlpha(1f);
                view.setTranslationY(0f);
            }
        });
        track(set).start();
    }

    /** Troca rápida de conteúdo (ex.: nova pergunta): o texto novo sobe e aparece em ~200 ms. */
    public void refresh(View... views) {
        float distance = views.length == 0 ? 0 : views[0].getResources().getDisplayMetrics().density * 10;
        long delay = 0;
        for (View view : views) {
            view.setAlpha(0f);
            view.setTranslationY(distance);
            AnimatorSet set = new AnimatorSet();
            set.playTogether(ObjectAnimator.ofFloat(view, View.ALPHA, 0f, 1f),
                    ObjectAnimator.ofFloat(view, View.TRANSLATION_Y, distance, 0f));
            set.setDuration(220);
            set.setStartDelay(delay);
            set.setInterpolator(new FastOutSlowInInterpolator());
            set.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationCancel(Animator animation) {
                    view.setAlpha(1f);
                    view.setTranslationY(0f);
                }
            });
            track(set).start();
            delay += 40;
        }
    }

    /** Pôster e afins: cresce levemente enquanto aparece. */
    public void popIn(View view, long delay) {
        view.setAlpha(0f);
        view.setScaleX(0.9f);
        view.setScaleY(0.9f);
        AnimatorSet set = new AnimatorSet();
        set.playTogether(
                ObjectAnimator.ofFloat(view, View.ALPHA, 0f, 1f),
                ObjectAnimator.ofFloat(view, View.SCALE_X, 0.9f, 1f),
                ObjectAnimator.ofFloat(view, View.SCALE_Y, 0.9f, 1f));
        set.setDuration(ENTER_MS + 80);
        set.setStartDelay(delay);
        set.setInterpolator(new DecelerateInterpolator(1.6f));
        set.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationCancel(Animator animation) {
                view.setAlpha(1f);
                view.setScaleX(1f);
                view.setScaleY(1f);
            }
        });
        track(set).start();
    }

    /**
     * Troca de expressão de um personagem: a imagem atual encolhe um pouco e
     * some, a nova volta com um leve impulso. Com {@code shake}, ela ainda treme
     * de lado a lado ao chegar (irritação).
     */
    public Animator swapImage(ImageView image, @DrawableRes int resId, boolean shake) {
        float shakeDistance = image.getResources().getDisplayMetrics().density * 8;

        AnimatorSet out = new AnimatorSet();
        out.playTogether(
                ObjectAnimator.ofFloat(image, View.ALPHA, image.getAlpha(), 0f),
                ObjectAnimator.ofFloat(image, View.SCALE_X, image.getScaleX(), 0.96f),
                ObjectAnimator.ofFloat(image, View.SCALE_Y, image.getScaleY(), 0.96f));
        out.setDuration(110);
        out.setInterpolator(new FastOutSlowInInterpolator());
        out.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                image.setImageResource(resId);
            }
        });

        AnimatorSet in = new AnimatorSet();
        in.playTogether(
                ObjectAnimator.ofFloat(image, View.ALPHA, 0f, 1f),
                ObjectAnimator.ofFloat(image, View.SCALE_X, 0.96f, 1f),
                ObjectAnimator.ofFloat(image, View.SCALE_Y, 0.96f, 1f));
        in.setDuration(240);
        in.setInterpolator(new OvershootInterpolator(1.4f));

        AnimatorSet all = new AnimatorSet();
        if (shake) {
            ObjectAnimator tremor = ObjectAnimator.ofFloat(image, View.TRANSLATION_X,
                    0f, -shakeDistance, shakeDistance, -shakeDistance * 0.7f, shakeDistance * 0.4f, 0f);
            tremor.setDuration(340);
            all.playSequentially(out, in, tremor);
        } else {
            all.playSequentially(out, in);
        }
        all.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationCancel(Animator animation) {
                image.setImageResource(resId);
                image.setAlpha(1f);
                image.setScaleX(1f);
                image.setScaleY(1f);
                image.setTranslationX(0f);
            }
        });
        track(all).start();
        return all;
    }

    /** Número contando de 0 até {@code target}. */
    public void countUp(TextView view, int target, IntFunction<String> format, long delay) {
        if (target <= 0) {
            view.setText(format.apply(target));
            return;
        }
        ValueAnimator animator = ValueAnimator.ofInt(0, target);
        animator.setDuration(Math.min(1100, 500 + target * 12L));
        animator.setStartDelay(delay);
        animator.setInterpolator(new FastOutSlowInInterpolator());
        animator.addUpdateListener(a -> view.setText(format.apply((int) a.getAnimatedValue())));
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationCancel(Animator animation) {
                view.setText(format.apply(target));
            }
        });
        view.setText(format.apply(0));
        track(animator).start();
    }

    private static Animator timed(Animator animator, long durationMs) {
        animator.setDuration(durationMs);
        animator.setInterpolator(new FastOutSlowInInterpolator());
        return animator;
    }

    /** Deixa a imagem em tons de cinza ({@code 0}) ou colorida ({@code 1}). */
    public static void setSaturation(ImageView image, float saturation) {
        if (saturation >= 1f) {
            image.clearColorFilter();
            return;
        }
        ColorMatrix matrix = new ColorMatrix();
        matrix.setSaturation(saturation);
        image.setColorFilter(new ColorMatrixColorFilter(matrix));
    }

    /**
     * Desbloqueio: o cadeado balança, abre, cresce e some; a imagem ganha cor e um
     * brilho dourado pulsa por trás.
     *
     * @param overlay  camada escura com o cadeado por cima da imagem
     * @param lockIcon o ícone do cadeado dentro da camada
     * @param image    imagem do herói (começa em tons de cinza)
     * @param glow     brilho atrás da imagem (opcional)
     * @param onEnd    chamado quando o cadeado some (ex.: mostrar o selo "desbloqueado")
     */
    public Animator unlock(View overlay, ImageView lockIcon, @DrawableRes int openIcon, ImageView image,
                           @Nullable View glow, long delay, @Nullable Runnable onEnd) {
        overlay.setVisibility(View.VISIBLE);
        overlay.setAlpha(1f);
        lockIcon.setAlpha(1f);
        lockIcon.setScaleX(1f);
        lockIcon.setScaleY(1f);
        lockIcon.setRotation(0f);
        setSaturation(image, 0f);

        // 1) balança, como quem gira a chave
        ObjectAnimator shake = ObjectAnimator.ofFloat(lockIcon, View.ROTATION, 0f, -14f, 12f, -9f, 6f, 0f);
        shake.setDuration(460);

        // 2) abre com um pequeno salto
        ObjectAnimator popX = ObjectAnimator.ofFloat(lockIcon, View.SCALE_X, 1f, 1.22f);
        ObjectAnimator popY = ObjectAnimator.ofFloat(lockIcon, View.SCALE_Y, 1f, 1.22f);
        AnimatorSet open = new AnimatorSet();
        open.playTogether(popX, popY);
        open.setDuration(180);
        open.setInterpolator(new OvershootInterpolator(2f));
        open.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                lockIcon.setImageResource(openIcon);
            }
        });

        // 3) o cadeado cresce e some, a camada escura sai e a cor volta (o brilho dura um pouco mais)
        ValueAnimator color = ValueAnimator.ofFloat(0f, 1f);
        color.addUpdateListener(a -> setSaturation(image, (float) a.getAnimatedValue()));
        List<Animator> parts = new ArrayList<>();
        parts.add(timed(ObjectAnimator.ofFloat(lockIcon, View.SCALE_X, 1.22f, 1.9f), 520));
        parts.add(timed(ObjectAnimator.ofFloat(lockIcon, View.SCALE_Y, 1.22f, 1.9f), 520));
        parts.add(timed(ObjectAnimator.ofFloat(lockIcon, View.ALPHA, 1f, 0f), 520));
        parts.add(timed(ObjectAnimator.ofFloat(overlay, View.ALPHA, 1f, 0f), 520));
        parts.add(timed(color, 620));
        if (glow != null) {
            glow.setVisibility(View.VISIBLE);
            parts.add(timed(ObjectAnimator.ofFloat(glow, View.ALPHA, 0f, 1f, 0f), 900));
            parts.add(timed(ObjectAnimator.ofFloat(glow, View.SCALE_X, 0.7f, 1.35f), 900));
            parts.add(timed(ObjectAnimator.ofFloat(glow, View.SCALE_Y, 0.7f, 1.35f), 900));
        }
        AnimatorSet vanish = new AnimatorSet();
        vanish.playTogether(parts);

        AnimatorSet all = new AnimatorSet();
        all.playSequentially(shake, open, vanish);
        all.setStartDelay(delay);
        all.addListener(new AnimatorListenerAdapter() {
            private boolean finished;

            @Override
            public void onAnimationEnd(Animator animation) {
                if (finished) return;
                finished = true;
                overlay.setVisibility(View.GONE);
                image.clearColorFilter();
                if (glow != null) glow.setAlpha(0f);
                if (onEnd != null) onEnd.run();
            }
        });
        track(all).start();
        return all;
    }
}
