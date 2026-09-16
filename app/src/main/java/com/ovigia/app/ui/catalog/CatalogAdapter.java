package com.ovigia.app.ui.catalog;

import android.animation.Animator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.ovigia.app.R;
import com.ovigia.app.catalog.CatalogUiState.Item;
import com.ovigia.app.databinding.ItemCatalogHeroBinding;
import com.ovigia.app.ui.Motion;

import java.util.HashSet;
import java.util.Set;

/**
 * Grade de cartas do catálogo. Heróis desbloqueados desde a última visita
 * aparecem ainda com o cadeado, que balança, abre e some enquanto a carta ganha cor.
 */
final class CatalogAdapter extends ListAdapter<Item, CatalogAdapter.Holder> {

    interface OnItemClick {
        void onClick(Item item);
    }

    /** Espera a grade entrar antes da primeira revelação, e espaça as seguintes. */
    private static final long REVEAL_FIRST_DELAY_MS = 520;
    private static final long REVEAL_STEP_MS = 220;
    /** Opacidade do cadeado nas cartas bloqueadas. */
    private static final float LOCKED_ICON_ALPHA = 0.55f;

    private final OnItemClick listener;
    private final Motion motion;
    /** Revelações já tocadas nesta tela (rolar a grade não repete). */
    private final Set<Integer> revealed = new HashSet<>();

    CatalogAdapter(OnItemClick listener, Motion motion) {
        super(DIFF);
        this.listener = listener;
        this.motion = motion;
        setHasStableIds(true);
    }

    @Override
    public long getItemId(int position) {
        return getItem(position).characterId;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(ItemCatalogHeroBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        Item item = getItem(position);
        ItemCatalogHeroBinding b = holder.binding;
        if (holder.reveal != null) {
            // Carta reciclada no meio da revelação: termina na hora antes de reaproveitar.
            holder.reveal.cancel();
            holder.reveal = null;
        }
        resetLock(b);
        b.card.setOnClickListener(v -> listener.onClick(item));

        if (!item.unlocked) {
            Glide.with(b.image).clear(b.image);
            b.image.setImageDrawable(null);
            b.lockedOverlay.setVisibility(View.VISIBLE);
            b.tvName.setText(R.string.catalog_locked_name);
            b.card.setStrokeColor(ContextCompat.getColor(b.card.getContext(), R.color.white_10));
            b.card.setContentDescription(b.card.getContext().getString(R.string.catalog_cd_locked));
            return;
        }

        b.lockedOverlay.setVisibility(View.GONE);
        b.tvName.setText(item.name);
        b.card.setStrokeColor(ContextCompat.getColor(b.card.getContext(), R.color.vigia_gold_soft));
        b.card.setContentDescription(item.name);
        Glide.with(b.image)
                .load(item.imageUrl)
                .placeholder(R.color.vigia_panel_solid)
                .error(R.drawable.ic_character_placeholder)
                .transition(DrawableTransitionOptions.withCrossFade(180))
                .centerCrop()
                .into(b.image);

        if (item.revealNow && revealed.add(item.characterId)) {
            long delay = REVEAL_FIRST_DELAY_MS + (revealed.size() - 1) * REVEAL_STEP_MS;
            holder.reveal = motion.unlock(b.lockedOverlay, b.lockIcon, R.drawable.ic_lock_open, b.image,
                    null, delay, null);
        }
    }

    private static void resetLock(ItemCatalogHeroBinding b) {
        b.lockedOverlay.setAlpha(1f);
        b.lockIcon.setImageResource(R.drawable.ic_lock);
        b.lockIcon.setAlpha(LOCKED_ICON_ALPHA);
        b.lockIcon.setScaleX(1f);
        b.lockIcon.setScaleY(1f);
        b.lockIcon.setRotation(0f);
        Motion.setSaturation(b.image, 1f);
    }

    static final class Holder extends RecyclerView.ViewHolder {
        final ItemCatalogHeroBinding binding;
        Animator reveal;

        Holder(ItemCatalogHeroBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    private static final DiffUtil.ItemCallback<Item> DIFF = new DiffUtil.ItemCallback<Item>() {
        @Override
        public boolean areItemsTheSame(@NonNull Item a, @NonNull Item b) {
            return a.characterId == b.characterId;
        }

        @Override
        public boolean areContentsTheSame(@NonNull Item a, @NonNull Item b) {
            return a.equals(b);
        }
    };
}
