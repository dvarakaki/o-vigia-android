package com.ovigia.app.game;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * LiveData para eventos de "dispare uma vez só" (navegação, fim de jogo).
 * Sem isso, um LiveData comum reentrega o último valor pra todo observador
 * novo — o que faria a tela reagir de novo a uma navegação antiga só por
 * causa de uma rotação de tela. Padrão conhecido dos exemplos de
 * arquitetura do Android.
 */
public class SingleLiveEvent<T> extends MutableLiveData<T> {

    private final AtomicBoolean pending = new AtomicBoolean(false);

    @MainThread
    @Override
    public void observe(@NonNull LifecycleOwner owner, @NonNull Observer<? super T> observer) {
        super.observe(owner, t -> {
            if (pending.compareAndSet(true, false)) {
                observer.onChanged(t);
            }
        });
    }

    @MainThread
    @Override
    public void setValue(T t) {
        pending.set(true);
        super.setValue(t);
    }
}
