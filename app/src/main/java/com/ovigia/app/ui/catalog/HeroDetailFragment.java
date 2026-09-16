package com.ovigia.app.ui.catalog;

import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;
import androidx.core.text.HtmlCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.fragment.NavHostFragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.ovigia.app.AppContainer;
import com.ovigia.app.OVigiaApplication;
import com.ovigia.app.R;
import com.ovigia.app.catalog.HeroDetailUiState;
import com.ovigia.app.catalog.HeroDetailUiState.TranslationStatus;
import com.ovigia.app.catalog.HeroDetailViewModel;
import com.ovigia.app.databinding.FragmentHeroDetailBinding;
import com.ovigia.app.databinding.ItemDetailRowBinding;
import com.ovigia.app.databinding.ItemProfileStatBinding;
import com.ovigia.app.databinding.ViewDetailSectionBinding;
import com.ovigia.app.model.ApiRef;
import com.ovigia.app.model.CharacterDetail;
import com.ovigia.app.model.ImageData;
import com.ovigia.app.translation.ComicVineGlossary;
import com.ovigia.app.translation.ComicVineTexts;
import com.ovigia.app.translation.HeroTranslation;
import com.ovigia.app.ui.FadeNavOptions;
import com.ovigia.app.ui.Motion;
import com.ovigia.app.ui.SystemBarInsets;
import com.ovigia.app.ui.game.GameFragment;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Ficha completa de um herói desbloqueado, com tudo o que a Comic Vine fornece:
 * identidade, biografia, poderes, equipes, aliados, inimigos, criadores, filmes,
 * aparições (edições, arcos, volumes, mortes), imagem e registro. Seções vazias
 * não aparecem; listas longas abrem numa gaveta com busca.
 */
public class HeroDetailFragment extends Fragment {

    public static final String ARG_CHARACTER_ID = "characterId";

    /** Etiquetas mostradas direto na seção; o resto fica no "Ver todos". */
    private static final int CHIP_LIMIT = 24;
    private static final String COMIC_VINE_URL = "https://comicvine.gamespot.com/";
    private static final Pattern CHARACTER_LINK = Pattern.compile("/4005-(\\d+)/?");
    private static final Pattern IMAGE_LINK = Pattern.compile("(?i).+\\.(jpe?g|png|gif|webp)(\\?.*)?$");

    private FragmentHeroDetailBinding binding;
    private HeroDetailViewModel viewModel;
    private CharacterDetail boundDetail;
    private WebView bioWebView;
    private TextView bioText;
    /** Peças que mudam quando a tradução chega (ou quando o jogador pede o original). */
    private ItemDetailRowBinding birthRow;
    private ChipGroup galleryGroup;
    private String boundBiography;
    private HeroTranslation boundTranslation;
    private boolean boundShowOriginal;
    private Set<Integer> unlockedIds = Collections.emptySet();
    private static final String STATE_ENTERED = "entered";
    private final Motion motion = new Motion();
    /** Entrada animada só na primeira abertura (girar a tela não repete). */
    private boolean animateHeader;
    private boolean animateDetail;

    public HeroDetailFragment() {
        super(R.layout.fragment_hero_detail);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding = FragmentHeroDetailBinding.bind(view);
        boolean entered = savedInstanceState != null && savedInstanceState.getBoolean(STATE_ENTERED, false);
        animateHeader = !entered;
        animateDetail = !entered;
        SystemBarInsets.padTop(binding.topBar);
        SystemBarInsets.extendHeight(binding.imageBackdrop);
        SystemBarInsets.marginTop(binding.content);

        int characterId = requireArguments().getInt(ARG_CHARACTER_ID);
        AppContainer container = ((OVigiaApplication) requireActivity().getApplication()).container();
        viewModel = new ViewModelProvider(this, new HeroDetailViewModel.Factory(characterId,
                container.accountStore, container.collectionStore, container.heroDetailRepository,
                container.heroTranslationRepository, getString(R.string.content_language),
                container.ioExecutor, container.mainExecutor)).get(HeroDetailViewModel.class);

        binding.btnBack.setOnClickListener(v -> nav().popBackStack());
        binding.btnRetry.setOnClickListener(v -> viewModel.retry());
        binding.topBar.getBackground().mutate().setAlpha(0);
        binding.scroll.setOnScrollChangeListener((View.OnScrollChangeListener) (v, x, y, oldX, oldY) -> onScrolled(y));

        viewModel.state().observe(getViewLifecycleOwner(), this::render);
        viewModel.start();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(STATE_ENTERED, !animateDetail);
    }

    /** Traduzir é caro: só acontece com a ficha à vista. */
    @Override
    public void onStart() {
        super.onStart();
        viewModel.onVisible();
    }

    @Override
    public void onStop() {
        viewModel.onHidden();
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        motion.cancelAll();
        if (bioWebView != null) {
            ViewGroup parent = (ViewGroup) bioWebView.getParent();
            if (parent != null) parent.removeView(bioWebView);
            bioWebView.destroy();
            bioWebView = null;
        }
        super.onDestroyView();
        binding = null;
        boundDetail = null;
        bioText = null;
        birthRow = null;
        galleryGroup = null;
        boundBiography = null;
        boundTranslation = null;
    }

    private NavController nav() {
        return NavHostFragment.findNavController(this);
    }

    private boolean isCurrent() {
        NavDestination current = nav().getCurrentDestination();
        return isAdded() && current != null && current.getId() == R.id.heroDetailFragment
                && nav().getCurrentBackStackEntry() != null
                && nav().getCurrentBackStackEntry().getArguments() != null
                && nav().getCurrentBackStackEntry().getArguments().getInt(ARG_CHARACTER_ID)
                == requireArguments().getInt(ARG_CHARACTER_ID);
    }

    /** Barra superior fica sólida e mostra o nome conforme o pôster sai de cena; o fundo sobe mais devagar. */
    private void onScrolled(int scrollY) {
        if (binding == null) return;
        float limit = getResources().getDimension(R.dimen.hero_poster_top)
                + getResources().getDimension(R.dimen.hero_poster_height) * 0.6f;
        float fraction = Math.min(1f, Math.max(0f, scrollY / limit));
        binding.topBar.getBackground().setAlpha(Math.round(fraction * 255));
        binding.tvBarTitle.setAlpha(fraction < 0.8f ? 0f : (fraction - 0.8f) / 0.2f);
        binding.imageBackdrop.setTranslationY(-scrollY * 0.5f);
        binding.backdropScrim.setTranslationY(-scrollY * 0.5f);
    }

    // ------------------------------------------------------------ estado

    private void render(HeroDetailUiState state) {
        if (state.status != HeroDetailUiState.Status.READY) binding.translationBar.setVisibility(View.GONE);
        switch (state.status) {
            case SIGNED_OUT:
                nav().popBackStack(R.id.homeFragment, false);
                return;
            case LOCKED:
                showHeader(getString(R.string.catalog_locked_name), null);
                binding.posterCard.setClickable(false);
                binding.progress.setVisibility(View.GONE);
                showMessage(getString(R.string.hero_locked_message), false);
                return;
            case LOADING:
                showHeader(state.previewName, state.previewImageUrl);
                playHeaderEntrance();
                binding.progress.setVisibility(View.VISIBLE);
                binding.messageBox.setVisibility(View.GONE);
                return;
            case ERROR:
                showHeader(state.previewName, state.previewImageUrl);
                binding.progress.setVisibility(View.GONE);
                showMessage(getString(GameFragment.messageFor(state.error)), true);
                return;
            case READY:
            default:
                binding.progress.setVisibility(View.GONE);
                binding.messageBox.setVisibility(View.GONE);
                unlockedIds = state.unlockedIds;
                if (state.detail != boundDetail) {
                    playHeaderEntrance();
                    bindDetail(state);
                } else if (state.translation != boundTranslation || state.showOriginal != boundShowOriginal) {
                    bindTranslatedTexts(state);
                }
                bindTranslationBar(state);
        }
    }

    /** Pôster cresce e aparece; o nome sobe logo depois. */
    private void playHeaderEntrance() {
        if (!animateHeader) return;
        animateHeader = false;
        motion.popIn(binding.posterCard, 80);
        motion.fadeUp(binding.tvName, 200);
    }

    private void showMessage(String message, boolean retry) {
        binding.messageBox.setVisibility(View.VISIBLE);
        binding.tvMessage.setText(message);
        binding.btnRetry.setVisibility(retry ? View.VISIBLE : View.GONE);
    }

    private void showHeader(@Nullable String name, @Nullable String imageUrl) {
        binding.tvName.setText(name);
        binding.tvBarTitle.setText(name);
        loadPoster(imageUrl);
    }

    private void loadPoster(@Nullable String url) {
        Glide.with(this).load(url)
                .placeholder(R.color.vigia_panel_solid)
                .error(R.drawable.ic_character_placeholder)
                .transition(DrawableTransitionOptions.withCrossFade(200))
                .centerCrop()
                .into(binding.imagePoster);
        // Fundo: a mesma arte, pequena (fica naturalmente borrada ao ampliar) e desfocada no Android 12+.
        Glide.with(this).load(url)
                .override(96)
                .centerCrop()
                .transition(DrawableTransitionOptions.withCrossFade(300))
                .into(binding.imageBackdrop);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            binding.imageBackdrop.setRenderEffect(RenderEffect.createBlurEffect(28f, 28f, Shader.TileMode.CLAMP));
        }
    }

    // ------------------------------------------------------------ ficha

    private void bindDetail(HeroDetailUiState state) {
        CharacterDetail d = state.detail;
        boundDetail = d;
        birthRow = null;
        galleryGroup = null;
        boundBiography = null;
        String heroImage = d.image != null ? d.image.bestForHero() : state.previewImageUrl;
        showHeader(DetailFormat.isBlank(d.name) ? state.previewName : d.name, heroImage);

        String original = d.image != null && !DetailFormat.isBlank(d.image.originalUrl) ? d.image.originalUrl : heroImage;
        binding.posterCard.setOnClickListener(original == null ? null : v -> showImage(original));

        boolean showRealName = !DetailFormat.isBlank(d.realName) && !d.realName.trim().equalsIgnoreCase(String.valueOf(d.name).trim());
        binding.tvRealName.setVisibility(showRealName ? View.VISIBLE : View.GONE);
        binding.tvRealName.setText(showRealName ? d.realName.trim() : null);
        binding.tvOffline.setVisibility(state.offlineCopy ? View.VISIBLE : View.GONE);
        binding.btnOpenWeb.setVisibility(DetailFormat.isBlank(d.siteDetailUrl) ? View.GONE : View.VISIBLE);
        binding.btnOpenWeb.setOnClickListener(v -> openUrl(d.siteDetailUrl));
        binding.tvAttribution.setVisibility(View.VISIBLE);

        bindBadges(d, state.unlockedAt);
        bindStats(d);

        binding.sections.removeAllViews();
        addIdentitySection(state);
        addBiographySection(d);
        addChipSection(R.string.hero_section_powers, ComicVineGlossary.powers(getResources(), d.powers), ChipKind.PLAIN);
        addChipSection(R.string.hero_section_teams, d.teams, ChipKind.LINK);
        addChipSection(R.string.hero_section_allies, d.characterFriends, ChipKind.HERO);
        addChipSection(R.string.hero_section_enemies, d.characterEnemies, ChipKind.HERO);
        addChipSection(R.string.hero_section_team_allies, d.teamFriends, ChipKind.LINK);
        addChipSection(R.string.hero_section_team_enemies, d.teamEnemies, ChipKind.LINK);
        addChipSection(R.string.hero_section_creators, d.creators, ChipKind.LINK);
        addChipSection(R.string.hero_section_movies, d.movies, ChipKind.LINK);
        addAppearancesSection(d);
        addImageSection(d.image);
        addRecordSection(d);
        bindTranslatedTexts(state);

        if (animateDetail) {
            animateDetail = false;
            // Selos, resumo, números e seções entram em cascata.
            View[] views = new View[5 + binding.sections.getChildCount()];
            views[0] = binding.tvRealName;
            views[1] = binding.badges;
            views[2] = binding.tvDeck;
            views[3] = binding.tvOffline;
            views[4] = binding.statsRow;
            for (int i = 0; i < binding.sections.getChildCount(); i++) views[5 + i] = binding.sections.getChildAt(i);
            motion.staggerIn(260, views);
            motion.countUp(binding.statAppearances.tvValue, d.countOfIssueAppearances, DetailFormat::number, 420);
            motion.countUp(binding.statTeams.tvValue, DetailFormat.size(d.teams), DetailFormat::number, 480);
            motion.countUp(binding.statPowers.tvValue, DetailFormat.size(d.powers), DetailFormat::number, 540);
        }
    }

    private void bindBadges(CharacterDetail d, long unlockedAt) {
        ChipGroup badges = binding.badges;
        badges.removeAllViews();
        Chip unlocked = addChip(badges, getString(R.string.hero_badge_unlocked));
        unlocked.setChipIconResource(R.drawable.ic_lock_open);
        unlocked.setChipIconVisible(true);
        unlocked.setChipBackgroundColorResource(R.color.vigia_gold_bg);
        unlocked.setChipStrokeColorResource(R.color.vigia_gold);
        unlocked.setTextColor(ContextCompat.getColor(requireContext(), R.color.vigia_gold));
        unlocked.setContentDescription(getString(R.string.hero_unlocked_on, DetailFormat.date(unlockedAt)));
        if (d.publisher != null && !DetailFormat.isBlank(d.publisher.name)) addChip(badges, d.publisher.name.trim());
        String origin = ComicVineGlossary.origin(getResources(), d.origin);
        if (!DetailFormat.isBlank(origin)) addChip(badges, origin.trim());
        String gender = DetailFormat.gender(requireContext(), d.gender);
        if (gender != null) addChip(badges, gender);
    }

    private void bindStats(CharacterDetail d) {
        binding.statsRow.setVisibility(View.VISIBLE);
        bindStat(binding.statAppearances, DetailFormat.number(d.countOfIssueAppearances), R.string.hero_stat_appearances);
        bindStat(binding.statTeams, DetailFormat.number(DetailFormat.size(d.teams)), R.string.hero_stat_teams);
        bindStat(binding.statPowers, DetailFormat.number(DetailFormat.size(d.powers)), R.string.hero_stat_powers);
    }

    private void bindStat(ItemProfileStatBinding stat, String value, @StringRes int label) {
        stat.tvValue.setText(value);
        stat.tvLabel.setText(label);
        stat.getRoot().setContentDescription(value + " " + getString(label));
    }

    // ------------------------------------------------------------ tradução

    /**
     * Os textos que a Comic Vine só tem em inglês (resumo, nascimento, galerias
     * e biografia), trocados conforme a tradução vai chegando.
     */
    private void bindTranslatedTexts(HeroDetailUiState state) {
        boundTranslation = state.translation;
        boundShowOriginal = state.showOriginal;
        String deck = translated(state, state.detail.deck);
        binding.tvDeck.setVisibility(DetailFormat.isBlank(deck) ? View.GONE : View.VISIBLE);
        binding.tvDeck.setText(DetailFormat.isBlank(deck) ? null : deck.trim());
        if (birthRow != null) {
            String birth = birth(state);
            birthRow.tvValue.setText(birth);
            birthRow.getRoot().setContentDescription(getString(R.string.hero_field_birth) + ": " + birth);
        }
        if (galleryGroup != null) bindGalleries(state);
        bindBiography(state);
    }

    private void bindGalleries(HeroDetailUiState state) {
        galleryGroup.removeAllViews();
        ImageData image = state.detail.image;
        for (String gallery : ComicVineTexts.galleries(image == null ? null : image.imageTags)) {
            String name = ComicVineGlossary.gallery(getResources(), gallery);
            addChip(galleryGroup, name != null ? name : translated(state, gallery)).setClickable(false);
        }
    }

    /** Aviso de tradução automática, com o andamento e o atalho para o texto original. */
    private void bindTranslationBar(HeroDetailUiState state) {
        TranslationStatus status = state.translationStatus;
        boolean nothingToSay = status == TranslationStatus.NONE
                || (status == TranslationStatus.DONE && !state.hasTranslation());
        binding.translationBar.setVisibility(nothingToSay ? View.GONE : View.VISIBLE);
        if (nothingToSay) return;

        binding.translationProgress.setVisibility(
                status == TranslationStatus.TRANSLATING && !state.showOriginal ? View.VISIBLE : View.GONE);
        binding.tvTranslation.setText(translationMessage(state));

        Button action = binding.btnTranslationAction;
        if (status == TranslationStatus.NEEDS_DOWNLOAD) {
            action.setVisibility(View.VISIBLE);
            action.setText(R.string.hero_translation_download);
            action.setOnClickListener(v -> viewModel.downloadTranslator());
        } else if (status == TranslationStatus.NO_CONNECTION || status == TranslationStatus.FAILED) {
            action.setVisibility(View.VISIBLE);
            action.setText(R.string.btn_retry);
            action.setOnClickListener(v -> viewModel.retryTranslation());
        } else if (state.hasTranslation()) {
            action.setVisibility(View.VISIBLE);
            action.setText(state.showOriginal
                    ? R.string.hero_translation_show_translation : R.string.hero_translation_show_original);
            action.setOnClickListener(v -> viewModel.setShowOriginal(!state.showOriginal));
        } else {
            action.setVisibility(View.GONE);
        }
    }

    private String translationMessage(HeroDetailUiState state) {
        if (state.showOriginal) return getString(R.string.hero_translation_original);
        switch (state.translationStatus) {
            case TRANSLATING:
                float progress = state.translation == null ? 0f : state.translation.biographyProgress;
                return progress <= 0f || progress >= 1f
                        ? getString(R.string.hero_translation_working)
                        : getString(R.string.hero_translation_working_biography, Math.round(progress * 100));
            case NEEDS_DOWNLOAD: return getString(R.string.hero_translation_needs_download);
            case NO_CONNECTION: return getString(R.string.hero_translation_no_connection);
            case FAILED: return getString(R.string.hero_translation_failed);
            case DONE:
            default: return getString(R.string.hero_translation_done);
        }
    }

    /** O texto no idioma do app; o original quando não há tradução ou o jogador pediu para vê-lo. */
    private String translated(HeroDetailUiState state, String original) {
        if (state.showOriginal || state.translation == null || DetailFormat.isBlank(original)) return original;
        String translated = state.translation.text(original);
        return DetailFormat.isBlank(translated) ? original : translated;
    }

    private String biography(HeroDetailUiState state) {
        String description = state.detail.description;
        if (state.showOriginal || state.translation == null || state.translation.description == null) {
            return description;
        }
        return state.translation.description;
    }

    /** Nascimento: data no formato do idioma quando a Comic Vine escreveu uma; senão, texto traduzido. */
    private String birth(HeroDetailUiState state) {
        String date = DetailFormat.birth(state.detail.birth);
        return date != null ? date : translated(state, state.detail.birth);
    }

    // ------------------------------------------------------------ seções

    private void addIdentitySection(HeroDetailUiState state) {
        CharacterDetail d = state.detail;
        long unlockedAt = state.unlockedAt;
        ViewDetailSectionBinding s = newSection(R.string.hero_section_identity, -1);
        addRow(s, R.string.hero_field_name, d.name);
        addRow(s, R.string.hero_field_real_name, d.realName);
        List<String> aliases = DetailFormat.aliases(d.aliases);
        if (!aliases.isEmpty()) addRow(s, getString(R.string.hero_field_aliases_count, aliases.size()), String.join("\n", aliases));
        addRow(s, R.string.hero_field_gender, DetailFormat.gender(requireContext(), d.gender));
        // O nascimento vem escrito em inglês ("Oct 14, 1962"): vira data ou, em texto livre, tradução.
        birthRow = addRow(s, R.string.hero_field_birth, birth(state));
        addRow(s, R.string.hero_field_origin, ComicVineGlossary.origin(getResources(), d.origin));
        addRow(s, R.string.hero_field_publisher, d.publisher != null ? d.publisher.name : null);
        addRow(s, R.string.hero_field_first_appearance, DetailFormat.issue(requireContext(), d.firstAppearedInIssue));
        addRow(s, R.string.hero_field_unlocked_on, DetailFormat.date(unlockedAt));
        commit(s);
    }

    private void addBiographySection(CharacterDetail d) {
        if (DetailFormat.isBlank(d.description)) return;
        ViewDetailSectionBinding s = newSection(R.string.hero_section_biography, -1);
        int collapsed = getResources().getDimensionPixelSize(R.dimen.hero_bio_collapsed_height);

        FrameLayout frame = new FrameLayout(requireContext());
        frame.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, collapsed));
        View content = createBiographyView();
        frame.addView(content, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        View fade = new View(requireContext());
        fade.setBackgroundResource(R.drawable.bg_bio_fade);
        FrameLayout.LayoutParams fadeParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                getResources().getDimensionPixelSize(R.dimen.hero_bio_fade_height), Gravity.BOTTOM);
        frame.addView(fade, fadeParams);
        s.sectionBody.addView(frame);

        Button toggle = (Button) getLayoutInflater().inflate(R.layout.item_detail_text_button, s.sectionBody, false);
        toggle.setText(R.string.hero_bio_expand);
        toggle.setOnClickListener(v -> {
            boolean expand = frame.getLayoutParams().height != ViewGroup.LayoutParams.WRAP_CONTENT;
            frame.getLayoutParams().height = expand ? ViewGroup.LayoutParams.WRAP_CONTENT : collapsed;
            frame.requestLayout();
            fade.setVisibility(expand ? View.GONE : View.VISIBLE);
            toggle.setText(expand ? R.string.hero_bio_collapse : R.string.hero_bio_expand);
            if (!expand) binding.scroll.smoothScrollTo(0, Math.max(0, s.getRoot().getTop()));
        });
        s.sectionBody.addView(toggle);
        commit(s);
    }

    /** WebView com o HTML estilizado; sem WebView disponível, cai para texto simples. */
    private View createBiographyView() {
        try {
            WebView web = new WebView(requireContext());
            WebSettings settings = web.getSettings();
            settings.setJavaScriptEnabled(false);
            settings.setAllowFileAccess(false);
            settings.setAllowContentAccess(false);
            settings.setDomStorageEnabled(false);
            settings.setTextZoom(Math.round(getResources().getConfiguration().fontScale * 100));
            web.setBackgroundColor(Color.TRANSPARENT);
            web.setVerticalScrollBarEnabled(false);
            web.setHorizontalScrollBarEnabled(false);
            web.setWebViewClient(new WebViewClient() {
                @Override
                public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                    onLinkClicked(request.getUrl().toString());
                    return true;
                }

                @Override
                public void onPageFinished(WebView view, String url) {
                    // A altura segurada durante a troca de texto já pode ser solta.
                    view.postDelayed(() -> view.setMinimumHeight(0), 250);
                }
            });
            bioWebView = web;
            return web;
        } catch (RuntimeException e) {
            TextView text = new TextView(requireContext());
            text.setTextColor(Color.WHITE);
            text.setTextSize(16);
            text.setLineSpacing(0, 1.2f);
            bioText = text;
            return text;
        }
    }

    /**
     * Mostra a biografia — traduzida, original ou o meio do caminho enquanto o
     * tradutor trabalha. Recarregar zera a altura do WebView, então a anterior
     * fica segurada até a página nova assentar: a tela não dá um pulo na leitura.
     */
    private void bindBiography(HeroDetailUiState state) {
        String html = biography(state);
        if (DetailFormat.isBlank(html) || html.equals(boundBiography)) return;
        boundBiography = html;
        if (bioWebView != null) {
            bioWebView.setMinimumHeight(bioWebView.getHeight());
            bioWebView.loadDataWithBaseURL(COMIC_VINE_URL, HeroHtml.document(html), "text/html", "utf-8", null);
        } else if (bioText != null) {
            bioText.setText(HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_COMPACT));
        }
    }

    private enum ChipKind {
        /** Sem ação. */
        PLAIN,
        /** Abre a página na Comic Vine. */
        LINK,
        /** Personagem: abre a ficha se estiver desbloqueado; senão, a página na Comic Vine. */
        HERO
    }

    private void addChipSection(@StringRes int title, @Nullable List<ApiRef> refs, ChipKind kind) {
        if (refs == null || refs.isEmpty()) return;
        ViewDetailSectionBinding s = newSection(title, refs.size());
        ChipGroup group = newChipGroup(s.sectionBody);
        int shown = Math.min(CHIP_LIMIT, refs.size());
        for (int i = 0; i < shown; i++) addRefChip(group, refs.get(i), kind);
        if (refs.size() > shown) addSeeAll(s, getString(title), refs);
        commit(s);
    }

    private void addRefChip(ChipGroup group, ApiRef ref, ChipKind kind) {
        Chip chip = addChip(group, DetailFormat.refName(requireContext(), ref));
        if (kind == ChipKind.HERO && ref != null && unlockedIds.contains(ref.id)) {
            chip.setChipStrokeColorResource(R.color.vigia_gold);
            chip.setChipIconResource(R.drawable.ic_lock_open);
            chip.setChipIconVisible(true);
            chip.setOnClickListener(v -> openHero(ref.id));
        } else if (kind != ChipKind.PLAIN && ref != null && !DetailFormat.isBlank(ref.siteDetailUrl)) {
            chip.setOnClickListener(v -> openUrl(ref.siteDetailUrl));
        } else {
            chip.setClickable(false);
        }
    }

    private void addAppearancesSection(CharacterDetail d) {
        int issues = DetailFormat.size(d.issueCredits);
        int arcs = DetailFormat.size(d.storyArcCredits);
        int volumes = DetailFormat.size(d.volumeCredits);
        int deaths = DetailFormat.size(d.issuesDiedIn);
        if (d.countOfIssueAppearances == 0 && issues + arcs + volumes + deaths == 0 && d.firstAppearedInIssue == null) {
            return;
        }
        ViewDetailSectionBinding s = newSection(R.string.hero_section_appearances, -1);
        addRow(s, R.string.hero_field_issue_appearances, DetailFormat.number(d.countOfIssueAppearances));
        addRow(s, R.string.hero_field_first_appearance, DetailFormat.issue(requireContext(), d.firstAppearedInIssue));
        addListLink(s, R.string.hero_list_issues, d.issueCredits);
        addListLink(s, R.string.hero_list_story_arcs, d.storyArcCredits);
        addListLink(s, R.string.hero_list_volumes, d.volumeCredits);
        if (deaths > 0) {
            TextView label = (TextView) getLayoutInflater().inflate(R.layout.item_detail_label, s.sectionBody, false);
            label.setText(getString(R.string.hero_field_died_in, deaths));
            s.sectionBody.addView(label);
            ChipGroup group = newChipGroup(s.sectionBody);
            int shown = Math.min(CHIP_LIMIT, deaths);
            for (int i = 0; i < shown; i++) addRefChip(group, d.issuesDiedIn.get(i), ChipKind.LINK);
            if (deaths > shown) addSeeAll(s, getString(R.string.hero_list_deaths), d.issuesDiedIn);
        }
        commit(s);
    }

    /** Linha "Edições · 1.085 ›" que abre a lista completa. */
    private void addListLink(ViewDetailSectionBinding s, @StringRes int title, @Nullable List<ApiRef> refs) {
        if (refs == null || refs.isEmpty()) return;
        View row = getLayoutInflater().inflate(R.layout.item_detail_list_link, s.sectionBody, false);
        ((TextView) row.findViewById(R.id.tvLinkTitle)).setText(title);
        ((TextView) row.findViewById(R.id.tvLinkCount)).setText(DetailFormat.number(refs.size()));
        row.setContentDescription(getString(title) + ", " + DetailFormat.number(refs.size()));
        row.setOnClickListener(v -> RefListSheet.show(requireContext(), getString(title), refs, ref -> openRef(ref)));
        s.sectionBody.addView(row);
    }

    private void addImageSection(@Nullable ImageData image) {
        if (image == null) return;
        String original = !DetailFormat.isBlank(image.originalUrl) ? image.originalUrl : image.bestForHero();
        if (original == null && DetailFormat.isBlank(image.imageTags)) return;
        ViewDetailSectionBinding s = newSection(R.string.hero_section_image, -1);
        if (!DetailFormat.isBlank(image.imageTags)) {
            TextView label = (TextView) getLayoutInflater().inflate(R.layout.item_detail_label, s.sectionBody, false);
            label.setText(R.string.hero_field_image_tags);
            s.sectionBody.addView(label);
            // As etiquetas entram em bindTranslatedTexts: os nomes das galerias vêm em inglês.
            galleryGroup = newChipGroup(s.sectionBody);
        }
        addRow(s, R.string.hero_field_image_sizes, availableSizes(image));
        if (original != null) {
            Button open = (Button) getLayoutInflater().inflate(R.layout.item_detail_text_button, s.sectionBody, false);
            open.setText(R.string.hero_view_original_image);
            open.setOnClickListener(v -> showImage(original));
            s.sectionBody.addView(open);
        }
        commit(s);
    }

    private String availableSizes(ImageData image) {
        StringBuilder sb = new StringBuilder();
        appendSize(sb, image.iconUrl, R.string.hero_size_icon);
        appendSize(sb, image.tinyUrl, R.string.hero_size_tiny);
        appendSize(sb, image.thumbUrl, R.string.hero_size_thumb);
        appendSize(sb, image.smallUrl, R.string.hero_size_small);
        appendSize(sb, image.mediumUrl, R.string.hero_size_medium);
        appendSize(sb, image.screenUrl, R.string.hero_size_screen);
        appendSize(sb, image.screenLargeUrl, R.string.hero_size_screen_large);
        appendSize(sb, image.superUrl, R.string.hero_size_super);
        appendSize(sb, image.originalUrl, R.string.hero_size_original);
        return sb.length() == 0 ? null : sb.toString();
    }

    private void appendSize(StringBuilder sb, String url, @StringRes int name) {
        if (DetailFormat.isBlank(url)) return;
        if (sb.length() > 0) sb.append(" · ");
        sb.append(getString(name));
    }

    private void addRecordSection(CharacterDetail d) {
        ViewDetailSectionBinding s = newSection(R.string.hero_section_record, -1);
        addRow(s, R.string.hero_field_cv_id, String.valueOf(d.id));
        addRow(s, R.string.hero_field_date_added, DetailFormat.date(d.dateAdded));
        addRow(s, R.string.hero_field_date_updated, DetailFormat.date(d.dateLastUpdated));
        ItemDetailRowBinding page = addRow(s, R.string.hero_field_site_url, d.siteDetailUrl);
        if (page != null) {
            page.tvValue.setTextColor(ContextCompat.getColor(requireContext(), R.color.vigia_blue_light));
            page.tvValue.setTextIsSelectable(false);
            page.getRoot().setOnClickListener(v -> openUrl(d.siteDetailUrl));
        }
        ItemDetailRowBinding api = addRow(s, R.string.hero_field_api_url, d.apiDetailUrl);
        if (api != null) {
            api.tvValue.setTypeface(Typeface.MONOSPACE);
            api.tvValue.setTextSize(13);
        }
        commit(s);
    }

    // ------------------------------------------------------------ peças

    private ViewDetailSectionBinding newSection(@StringRes int title, int count) {
        ViewDetailSectionBinding s = ViewDetailSectionBinding.inflate(getLayoutInflater(), binding.sections, false);
        s.tvSectionTitle.setText(title);
        if (count >= 0) {
            s.tvSectionCount.setVisibility(View.VISIBLE);
            s.tvSectionCount.setText(DetailFormat.number(count));
        }
        return s;
    }

    /** Adiciona a seção só se ela ganhou conteúdo. */
    private void commit(ViewDetailSectionBinding s) {
        if (s.sectionBody.getChildCount() > 0) binding.sections.addView(s.getRoot());
    }

    private ItemDetailRowBinding addRow(ViewDetailSectionBinding s, @StringRes int label, @Nullable String value) {
        return addRow(s, getString(label), value);
    }

    private ItemDetailRowBinding addRow(ViewDetailSectionBinding s, String label, @Nullable String value) {
        if (DetailFormat.isBlank(value)) return null;
        ItemDetailRowBinding row = ItemDetailRowBinding.inflate(getLayoutInflater(), s.sectionBody, true);
        row.tvLabel.setText(label);
        row.tvValue.setText(value.trim());
        row.getRoot().setContentDescription(label + ": " + value.trim());
        return row;
    }

    private ChipGroup newChipGroup(LinearLayout parent) {
        ChipGroup group = new ChipGroup(requireContext());
        int spacing = getResources().getDimensionPixelSize(R.dimen.hero_chip_spacing);
        group.setChipSpacingHorizontal(spacing);
        group.setChipSpacingVertical(spacing);
        parent.addView(group, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        return group;
    }

    private Chip addChip(ChipGroup group, String text) {
        Chip chip = (Chip) LayoutInflater.from(requireContext()).inflate(R.layout.item_detail_chip, group, false);
        chip.setText(text);
        group.addView(chip);
        return chip;
    }

    private void addSeeAll(ViewDetailSectionBinding s, String title, List<ApiRef> refs) {
        Button seeAll = (Button) getLayoutInflater().inflate(R.layout.item_detail_text_button, s.sectionBody, false);
        seeAll.setText(getString(R.string.hero_see_all, DetailFormat.number(refs.size())));
        seeAll.setOnClickListener(v -> RefListSheet.show(requireContext(), title, refs, this::openRef));
        s.sectionBody.addView(seeAll);
    }

    // ------------------------------------------------------------ ações

    private void openRef(ApiRef ref) {
        if (ref == null) return;
        if (!DetailFormat.isBlank(ref.siteDetailUrl)) openUrl(ref.siteDetailUrl);
    }

    private void openHero(int characterId) {
        if (!isCurrent() || characterId == requireArguments().getInt(ARG_CHARACTER_ID)) return;
        Bundle args = new Bundle();
        args.putInt(ARG_CHARACTER_ID, characterId);
        nav().navigate(R.id.heroDetailFragment, args, FadeNavOptions.builder().build());
    }

    /** Links da biografia: herói desbloqueado abre a ficha, imagem abre ampliada, o resto vai para o navegador. */
    private void onLinkClicked(String url) {
        Matcher hero = CHARACTER_LINK.matcher(url);
        if (hero.find()) {
            int id = Integer.parseInt(hero.group(1));
            if (unlockedIds.contains(id)) {
                openHero(id);
                return;
            }
        }
        if (IMAGE_LINK.matcher(url).matches()) {
            showImage(url);
            return;
        }
        openUrl(url);
    }

    private void openUrl(@Nullable String url) {
        if (DetailFormat.isBlank(url)) return;
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (ActivityNotFoundException e) {
            // Sem navegador: nada a fazer.
        }
    }

    /** Imagem em tela cheia; toque fecha. */
    private void showImage(String url) {
        Dialog dialog = new Dialog(requireContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        FrameLayout root = new FrameLayout(requireContext());
        root.setBackgroundColor(Color.BLACK);
        CircularProgressIndicator progress = new CircularProgressIndicator(requireContext());
        progress.setIndeterminate(true);
        progress.setIndicatorColor(ContextCompat.getColor(requireContext(), R.color.vigia_gold));
        root.addView(progress, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
        ImageView image = new ImageView(requireContext());
        image.setScaleType(ImageView.ScaleType.FIT_CENTER);
        image.setContentDescription(binding != null ? binding.tvName.getText() : null);
        root.addView(image, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        root.setOnClickListener(v -> dialog.dismiss());
        dialog.setContentView(root);
        Glide.with(this).load(url).transition(DrawableTransitionOptions.withCrossFade(200)).into(image);
        dialog.show();
    }
}
