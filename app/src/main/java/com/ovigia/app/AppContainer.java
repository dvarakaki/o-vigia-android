package com.ovigia.app;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.ovigia.app.api.ApiClient;
import com.ovigia.app.api.ComicVineService;
import com.ovigia.app.auth.AccountStore;
import com.ovigia.app.collection.CollectionStore;
import com.ovigia.app.data.CharacterRepository;
import com.ovigia.app.data.ComicVineCharacterRepository;
import com.ovigia.app.data.ComicVineHeroDetailRepository;
import com.ovigia.app.data.HeroDetailRepository;
import com.ovigia.app.data.QuestionTexts;
import com.ovigia.app.data.roster.RosterCatalog;
import com.ovigia.app.learning.LearningStore;
import com.ovigia.app.profile.AndroidProfileImages;
import com.ovigia.app.profile.ProfileImages;
import com.ovigia.app.settings.AndroidAppCache;
import com.ovigia.app.settings.AppCache;
import com.ovigia.app.settings.AppLocales;
import com.ovigia.app.settings.SettingsStore;
import com.ovigia.app.social.FirebaseSocialBackend;
import com.ovigia.app.social.SocialRepository;
import com.ovigia.app.translation.CachedHeroTranslationRepository;
import com.ovigia.app.translation.HeroTranslationRepository;
import com.ovigia.app.translation.MlKitTextTranslator;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

/**
 * Injeção de dependências manual: cria uma vez, no escopo do app, tudo que as
 * telas precisam. Mantém as classes de domínio livres de singletons e fáceis
 * de testar com fakes.
 */
public final class AppContainer {

    /** Uma thread só: serializa as gravações em disco e evita corridas entre elas. */
    public final ExecutorService ioExecutor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "ovigia-io");
        thread.setPriority(Thread.NORM_PRIORITY - 1);
        return thread;
    });
    /** Rede dos amigos online: fora do I/O de disco, para uma rede lenta não travar gravações. */
    public final ExecutorService socialExecutor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "ovigia-social");
        thread.setPriority(Thread.NORM_PRIORITY - 1);
        return thread;
    });
    /** Tradução das fichas: pesada e demorada, com a menor prioridade de todas. */
    public final ExecutorService translationExecutor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "ovigia-translate");
        thread.setPriority(Thread.MIN_PRIORITY);
        return thread;
    });
    public final Executor mainExecutor;
    public final LearningStore learningStore;
    public final CharacterRepository characterRepository;
    public final AccountStore accountStore;
    public final CollectionStore collectionStore;
    public final ProfileImages profileImages;
    public final HeroDetailRepository heroDetailRepository;
    public final HeroTranslationRepository heroTranslationRepository;
    public final SettingsStore settingsStore;
    public final AppCache appCache;
    public final SocialRepository socialRepository;

    private final Context appContext;
    private RosterCatalog rosterCatalog;
    private boolean rosterLoaded = false;

    AppContainer(Context context) {
        Context app = context.getApplicationContext();
        appContext = app;
        Handler mainHandler = new Handler(Looper.getMainLooper());
        mainExecutor = mainHandler::post;

        // getFilesDir() toca o disco: os caminhos só são resolvidos no executor de I/O.
        learningStore = new LearningStore(() -> new File(app.getFilesDir(), "learning_store.json"), ioExecutor);
        accountStore = new AccountStore(() -> new File(app.getFilesDir(), "accounts.json"),
                AccountStore.DEFAULT_ITERATIONS);
        ComicVineService comicVine = ApiClient.create();
        characterRepository = new ComicVineCharacterRepository(
                comicVine,
                ApiClient.apiKey(),
                ApiClient.isConfigured(),
                () -> {
                    try (Reader reader = new InputStreamReader(app.getAssets().open("roster.json"),
                            StandardCharsets.UTF_8)) {
                        return RosterCatalog.parse(reader);
                    }
                },
                () -> QuestionTexts.load(AppLocales.resources(app)),
                learningStore,
                () -> {
                    AccountStore.Account current = accountStore.currentAccount();
                    return current == null ? null : current.id;
                },
                () -> new File(app.getFilesDir(), "characters_cache.json"),
                ioExecutor,
                mainExecutor);

        collectionStore = new CollectionStore(() -> new File(app.getFilesDir(), "collection.json"));
        profileImages = new AndroidProfileImages(app.getContentResolver(),
                () -> new File(app.getFilesDir(), "profile_media"));
        Supplier<File> heroDetailsDirectory = () -> new File(app.getFilesDir(), "hero_details");
        heroDetailRepository = new ComicVineHeroDetailRepository(
                ComicVineHeroDetailRepository.remote(comicVine, ApiClient.apiKey()),
                ApiClient.isConfigured(),
                heroDetailsDirectory,
                ioExecutor,
                mainExecutor);
        Supplier<File> heroTranslationsDirectory = () -> new File(app.getFilesDir(), "hero_translations");
        heroTranslationRepository = new CachedHeroTranslationRepository(
                new MlKitTextTranslator(app),
                heroTranslationsDirectory,
                ioExecutor,
                translationExecutor,
                mainExecutor,
                System::currentTimeMillis);
        settingsStore = new SettingsStore(() -> new File(app.getFilesDir(), "settings.json"), ioExecutor);
        appCache = new AndroidAppCache(app, heroDetailsDirectory, heroTranslationsDirectory);
        socialRepository = new SocialRepository(
                new FirebaseSocialBackend(app, BuildConfig.FIREBASE_EMULATOR_HOST),
                accountStore, collectionStore, learningStore, profileImages, this::rosterCatalog,
                socialExecutor, System::currentTimeMillis);

        // Aquece o aprendizado, a sessão e as preferências fora da main thread antes da primeira tela que precisa deles.
        ioExecutor.execute(learningStore::ensureLoaded);
        ioExecutor.execute(accountStore::ensureLoaded);
        ioExecutor.execute(settingsStore::ensureLoaded);
    }

    /**
     * Equipes e vilania do roster.json, lidas uma vez (conquistas). Bloqueante na
     * primeira chamada: fora da main thread. {@code null} se o arquivo não pôde ser lido.
     */
    public synchronized RosterCatalog rosterCatalog() {
        if (!rosterLoaded) {
            rosterLoaded = true;
            try (Reader reader = new InputStreamReader(appContext.getAssets().open("roster.json"),
                    StandardCharsets.UTF_8)) {
                rosterCatalog = RosterCatalog.parse(reader);
            } catch (IOException | RuntimeException e) {
                rosterCatalog = null;
            }
        }
        return rosterCatalog;
    }
}
