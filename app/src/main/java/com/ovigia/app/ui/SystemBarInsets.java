package com.ovigia.app.ui;

import android.view.View;
import android.view.ViewGroup;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * Espaço da barra de status, aplicado por tela. A {@code MainActivity} cuida das
 * laterais, da base e do teclado; o topo fica com cada tela para que as de
 * cabeçalho imersivo (perfil, ficha do herói) desenhem a imagem por trás da
 * barra de status enquanto as demais só recuam o conteúdo.
 */
public final class SystemBarInsets {

    private static int top(WindowInsetsCompat insets) {
        Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
        return bars.top;
    }

    /** Soma a altura da barra de status ao padding de topo original da view. */
    public static void padTop(View view) {
        int base = view.getPaddingTop();
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            v.setPadding(v.getPaddingLeft(), base + top(insets), v.getPaddingRight(), v.getPaddingBottom());
            return insets;
        });
        ViewCompat.requestApplyInsets(view);
    }

    /** Soma a altura da barra de status à margem de topo original da view. */
    public static void marginTop(View view) {
        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
        int base = params.topMargin;
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            ViewGroup.MarginLayoutParams p = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
            p.topMargin = base + top(insets);
            v.setLayoutParams(p);
            return insets;
        });
        ViewCompat.requestApplyInsets(view);
    }

    /** Aumenta a altura fixa da view (ex.: banner) para continuar por trás da barra de status. */
    public static void extendHeight(View view) {
        int base = view.getLayoutParams().height;
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            ViewGroup.LayoutParams p = v.getLayoutParams();
            p.height = base + top(insets);
            v.setLayoutParams(p);
            return insets;
        });
        ViewCompat.requestApplyInsets(view);
    }

    private SystemBarInsets() { }
}
