package com.ovigia.app.ui.friends;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;

import com.ovigia.app.R;
import com.ovigia.app.databinding.ItemAchievementBinding;
import com.ovigia.app.social.Achievement;
import com.ovigia.app.social.AchievementProgress;
import com.ovigia.app.social.Achievements;

import java.util.ArrayList;
import java.util.List;

/** Lista de conquistas, igual no próprio perfil e no perfil de um amigo. */
public final class AchievementViews {

    /** Conquistas mostradas antes de "Ver todas". */
    private static final int PREVIEW = 4;

    /**
     * Desbloqueadas primeiro (na ordem do enum), depois as que faltam, da mais
     * perto para a mais longe. Recolhida, mostra só as primeiras e o botão
     * {@code showAll} abre o resto.
     */
    public static void fill(LinearLayout list, Button showAll, List<AchievementProgress> progress, boolean expanded) {
        list.removeAllViews();
        Context context = list.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);
        List<AchievementProgress> ordered = new ArrayList<>(progress);
        ordered.sort((a, b) -> {
            if (a.isUnlocked() != b.isUnlocked()) return a.isUnlocked() ? -1 : 1;
            if (a.isUnlocked()) return 0;
            return Integer.compare(b.percent(), a.percent());
        });
        boolean collapsed = !expanded && ordered.size() > PREVIEW;
        showAll.setVisibility(collapsed ? View.VISIBLE : View.GONE);
        showAll.setText(context.getString(R.string.achievements_show_all, ordered.size()));
        if (collapsed) ordered = ordered.subList(0, PREVIEW);
        for (AchievementProgress p : ordered) {
            ItemAchievementBinding row = ItemAchievementBinding.inflate(inflater, list, true);
            boolean unlocked = p.isUnlocked();
            String title = context.getString(titleOf(p.achievement));
            String description = context.getString(descriptionOf(p.achievement));
            row.tvTitle.setText(title);
            row.tvTitle.setTextColor(ContextCompat.getColor(context, unlocked ? R.color.white : R.color.white_70));
            row.tvDescription.setText(description);
            row.tvCount.setText(context.getString(R.string.achievement_count, p.current, p.achievement.target));
            row.progressBar.setProgressCompat(p.percent(), false);
            row.imageIcon.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(context,
                    unlocked ? R.color.vigia_gold : R.color.white_20)));
            row.iconFrame.setAlpha(unlocked ? 1f : 0.6f);
            row.getRoot().setContentDescription(context.getString(unlocked
                            ? R.string.achievement_cd_unlocked : R.string.achievement_cd_locked,
                    title, description, p.current, p.achievement.target));
        }
    }

    public static String countText(Context context, List<AchievementProgress> progress) {
        return context.getString(R.string.achievement_count, Achievements.unlockedCount(progress), progress.size());
    }

    @StringRes
    static int titleOf(Achievement a) {
        switch (a) {
            case FIRST_HERO: return R.string.achievement_first_hero;
            case HEROES_10: return R.string.achievement_heroes_10;
            case HEROES_25: return R.string.achievement_heroes_25;
            case HEROES_50: return R.string.achievement_heroes_50;
            case AVENGERS_5: return R.string.achievement_avengers_5;
            case XMEN_5: return R.string.achievement_xmen_5;
            case GUARDIANS_3: return R.string.achievement_guardians_3;
            case FANTASTIC_FOUR: return R.string.achievement_fantastic_four;
            case VILLAINS_5: return R.string.achievement_villains_5;
            case GAMES_10: return R.string.achievement_games_10;
            case GAMES_50: return R.string.achievement_games_50;
            case BEAT_WATCHER: return R.string.achievement_beat_watcher;
            case BEAT_WATCHER_10:
            default: return R.string.achievement_beat_watcher_10;
        }
    }

    @StringRes
    static int descriptionOf(Achievement a) {
        switch (a) {
            case FIRST_HERO: return R.string.achievement_first_hero_desc;
            case HEROES_10: return R.string.achievement_heroes_10_desc;
            case HEROES_25: return R.string.achievement_heroes_25_desc;
            case HEROES_50: return R.string.achievement_heroes_50_desc;
            case AVENGERS_5: return R.string.achievement_avengers_5_desc;
            case XMEN_5: return R.string.achievement_xmen_5_desc;
            case GUARDIANS_3: return R.string.achievement_guardians_3_desc;
            case FANTASTIC_FOUR: return R.string.achievement_fantastic_four_desc;
            case VILLAINS_5: return R.string.achievement_villains_5_desc;
            case GAMES_10: return R.string.achievement_games_10_desc;
            case GAMES_50: return R.string.achievement_games_50_desc;
            case BEAT_WATCHER: return R.string.achievement_beat_watcher_desc;
            case BEAT_WATCHER_10:
            default: return R.string.achievement_beat_watcher_10_desc;
        }
    }

    private AchievementViews() { }
}
