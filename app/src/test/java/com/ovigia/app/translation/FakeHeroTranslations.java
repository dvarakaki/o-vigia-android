package com.ovigia.app.translation;

import com.ovigia.app.model.CharacterDetail;

import java.util.HashMap;
import java.util.Map;

/**
 * Tradução de mentira para os testes de ViewModel: guarda o pedido e publica o
 * que o teste mandar, como o repositório de verdade faria.
 */
public final class FakeHeroTranslations implements HeroTranslationRepository {

    public boolean supportsLanguage = true;
    public int jobs = 0;
    public int cancels = 0;
    public boolean lastAllowMeteredDownload = false;
    public CharacterDetail lastDetail;

    private Callback callback;

    @Override
    public boolean supports(String language) {
        return supportsLanguage && !ComicVineTexts.isSourceLanguage(language);
    }

    @Override
    public Job translate(CharacterDetail detail, String language, boolean allowMeteredDownload, Callback callback) {
        jobs++;
        lastDetail = detail;
        lastAllowMeteredDownload = allowMeteredDownload;
        this.callback = callback;
        return () -> cancels++;
    }

    /** Uma parte pronta (ou tudo, com {@code complete}). */
    public void publish(String original, String translated, String description, boolean complete) {
        Map<String, String> texts = new HashMap<>();
        if (original != null) texts.put(ComicVineTexts.normalize(original), translated);
        callback.onUpdate(new HeroTranslation("pt", texts, description, complete ? 1f : 0.5f, complete));
    }

    public void fail(TranslationException.Reason reason) {
        callback.onError(reason, new HeroTranslation("pt", new HashMap<>(), null, 0f, false));
    }
}
