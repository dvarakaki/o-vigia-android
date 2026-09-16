package com.ovigia.app.translation;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.common.model.RemoteModelManager;
import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.TranslateRemoteModel;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;
import com.ovigia.app.translation.TranslationException.Reason;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Tradutor do ML Kit: roda dentro do aparelho, de graça e sem chave nenhuma.
 * Cada idioma precisa de um pacote de algumas dezenas de MB, baixado uma vez —
 * depois disso a tradução funciona até offline.
 *
 * Tudo aqui é bloqueante e só pode ser chamado fora da main thread.
 */
public final class MlKitTextTranslator implements TextTranslator {

    /** Baixar o pacote depende da rede; a espera é longa de propósito. */
    private static final long DOWNLOAD_TIMEOUT_MINUTES = 15;
    private static final long TRANSLATE_TIMEOUT_SECONDS = 60;

    private final Context app;

    public MlKitTextTranslator(Context context) {
        this.app = context.getApplicationContext();
    }

    @Override
    public boolean supports(String language) {
        return TranslateLanguage.fromLanguageTag(language) != null;
    }

    @Override
    public Session open(String language, boolean allowMeteredDownload) throws TranslationException {
        String target = TranslateLanguage.fromLanguageTag(language);
        if (target == null) throw new TranslationException(Reason.FAILED);
        Translator translator = Translation.getClient(new TranslatorOptions.Builder()
                .setSourceLanguage(TranslateLanguage.ENGLISH)
                .setTargetLanguage(target)
                .build());
        try {
            download(target, translator, allowMeteredDownload);
        } catch (TranslationException e) {
            translator.close();
            throw e;
        }
        return new Session() {
            @Override
            public String translate(String text) throws TranslationException {
                return await(translator.translate(text), TRANSLATE_TIMEOUT_SECONDS, TimeUnit.SECONDS, Reason.FAILED);
            }

            @Override
            public void close() {
                translator.close();
            }
        };
    }

    /**
     * Garante o pacote do idioma no aparelho. Com ele baixado, nem toca na rede
     * (a tradução funciona offline); sem ele, baixa — em rede móvel só com o
     * aval do jogador, porque são dezenas de MB.
     */
    private void download(String language, Translator translator, boolean allowMeteredDownload)
            throws TranslationException {
        TranslateRemoteModel model = new TranslateRemoteModel.Builder(language).build();
        Boolean downloaded = await(RemoteModelManager.getInstance().isModelDownloaded(model),
                30, TimeUnit.SECONDS, Reason.FAILED);
        if (Boolean.TRUE.equals(downloaded)) return;

        NetworkKind network = network();
        if (network == NetworkKind.NONE) throw new TranslationException(Reason.NO_CONNECTION);
        if (network == NetworkKind.METERED && !allowMeteredDownload) {
            throw new TranslationException(Reason.NEEDS_DOWNLOAD);
        }
        await(translator.downloadModelIfNeeded(), DOWNLOAD_TIMEOUT_MINUTES, TimeUnit.MINUTES,
                Reason.NO_CONNECTION);
    }

    private enum NetworkKind { NONE, METERED, UNMETERED }

    private NetworkKind network() {
        ConnectivityManager manager = app.getSystemService(ConnectivityManager.class);
        if (manager == null) return NetworkKind.METERED;
        Network network = manager.getActiveNetwork();
        NetworkCapabilities capabilities = network == null ? null : manager.getNetworkCapabilities(network);
        if (capabilities == null || !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
            return NetworkKind.NONE;
        }
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
                ? NetworkKind.UNMETERED : NetworkKind.METERED;
    }

    private static <T> T await(Task<T> task, long timeout, TimeUnit unit, Reason onFailure)
            throws TranslationException {
        try {
            return Tasks.await(task, timeout, unit);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TranslationException(Reason.FAILED, e);
        } catch (ExecutionException | TimeoutException | RuntimeException e) {
            throw new TranslationException(onFailure, e);
        }
    }
}
