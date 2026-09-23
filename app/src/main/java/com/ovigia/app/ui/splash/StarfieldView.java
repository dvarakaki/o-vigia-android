package com.ovigia.app.ui.splash;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AnimationUtils;

import androidx.annotation.Nullable;

import java.util.Random;

/**
 * O céu da abertura: pontos brancos e alguns grãos dourados por cima do fundo
 * espacial, cada um piscando no seu ritmo e derivando devagar na diagonal.
 *
 * A semente do sorteio é fixa, então o céu é sempre o mesmo — a abertura do app
 * não muda de uma vez para a outra. As posições ficam guardadas em fração da
 * view (0..1), o que deixa o mesmo céu servir em qualquer tela ou orientação.
 *
 * O quadro seguinte só é pedido enquanto {@link #setRunning(boolean)} estiver
 * ligado, e nem isso acontece se o aparelho estiver com as animações desligadas:
 * aí o céu é desenhado uma vez, parado.
 */
public final class StarfieldView extends View {

    private static final int STAR_COUNT = 120;
    /** Uma em cada tantas estrelas é um grão dourado, com halo em volta. */
    private static final int GOLD_EVERY = 9;
    private static final long SEED = 190261L;

    private static final int STAR_COLOR = 0xFFDCE6FF;
    private static final int GOLD_COLOR = 0xFFE8C468;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float[] x = new float[STAR_COUNT];
    private final float[] y = new float[STAR_COUNT];
    private final float[] radius = new float[STAR_COUNT];
    private final float[] alpha = new float[STAR_COUNT];
    /** Fase inicial da piscada: sem ela o céu inteiro pulsaria junto. */
    private final float[] phase = new float[STAR_COUNT];
    private final float[] pulse = new float[STAR_COUNT];
    private final float[] drift = new float[STAR_COUNT];

    private final float density;
    private long startMs;
    private boolean running;

    public StarfieldView(Context context) {
        this(context, null);
    }

    public StarfieldView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        density = getResources().getDisplayMetrics().density;
        paint.setStyle(Paint.Style.FILL);

        Random random = new Random(SEED);
        for (int i = 0; i < STAR_COUNT; i++) {
            x[i] = random.nextFloat();
            y[i] = random.nextFloat();
            boolean gold = i % GOLD_EVERY == 0;
            radius[i] = gold ? 1.2f + random.nextFloat() * 1.2f : 0.5f + random.nextFloat() * 1.3f;
            alpha[i] = gold ? 0.5f + random.nextFloat() * 0.4f : 0.25f + random.nextFloat() * 0.6f;
            phase[i] = random.nextFloat() * (float) Math.PI * 2f;
            pulse[i] = 0.7f + random.nextFloat() * 1.8f;
            // As estrelas menores derivam menos: a diferença dá profundidade.
            drift[i] = (0.004f + random.nextFloat() * 0.012f) * radius[i];
        }
    }

    /**
     * Liga ou desliga a animação. Com as animações desligadas no aparelho o céu
     * continua desenhado, só não se mexe.
     */
    public void setRunning(boolean running) {
        boolean wanted = running && ValueAnimator.areAnimatorsEnabled();
        if (wanted == this.running) return;
        this.running = wanted;
        if (wanted) startMs = AnimationUtils.currentAnimationTimeMillis();
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int width = getWidth();
        int height = getHeight();
        if (width == 0 || height == 0) return;

        float time = running ? (AnimationUtils.currentAnimationTimeMillis() - startMs) / 1000f : 0f;

        for (int i = 0; i < STAR_COUNT; i++) {
            // Deriva diagonal, dando a volta pela borda oposta.
            float px = wrap(x[i] + drift[i] * time) * width;
            float py = wrap(y[i] - drift[i] * 0.4f * time) * height;
            float twinkle = 0.58f + 0.42f * (float) Math.sin(phase[i] + time * pulse[i]);
            float size = radius[i] * density;
            boolean gold = i % GOLD_EVERY == 0;

            if (gold) {
                paint.setColor(GOLD_COLOR);
                paint.setAlpha(clamp(alpha[i] * twinkle * 0.35f));
                canvas.drawCircle(px, py, size * 3f, paint);
            }
            paint.setColor(gold ? GOLD_COLOR : STAR_COLOR);
            paint.setAlpha(clamp(alpha[i] * twinkle));
            canvas.drawCircle(px, py, size, paint);
        }

        if (running) postInvalidateOnAnimation();
    }

    /** Mantém a fração dentro de 0..1 — a estrela que sai por um lado volta pelo outro. */
    private static float wrap(float value) {
        float wrapped = value % 1f;
        return wrapped < 0f ? wrapped + 1f : wrapped;
    }

    private static int clamp(float value) {
        return Math.max(0, Math.min(255, Math.round(value * 255f)));
    }
}
