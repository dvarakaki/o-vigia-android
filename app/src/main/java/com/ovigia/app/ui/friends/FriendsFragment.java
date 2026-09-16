package com.ovigia.app.ui.friends;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.snackbar.Snackbar;
import com.ovigia.app.AppContainer;
import com.ovigia.app.OVigiaApplication;
import com.ovigia.app.R;
import com.ovigia.app.databinding.FragmentFriendsBinding;
import com.ovigia.app.databinding.ItemFriendRowBinding;
import com.ovigia.app.social.FriendRequest;
import com.ovigia.app.social.FriendsHub;
import com.ovigia.app.social.FriendsUiState;
import com.ovigia.app.social.FriendsUiState.Status;
import com.ovigia.app.social.FriendsViewModel;
import com.ovigia.app.social.SocialException;
import com.ovigia.app.social.UserCard;
import com.ovigia.app.social.Username;
import com.ovigia.app.ui.FadeNavOptions;
import com.ovigia.app.ui.Motion;
import com.ovigia.app.ui.SystemBarInsets;
import com.ovigia.app.ui.auth.AuthFragment;

/**
 * Aba de amigos. Leva a conta até ficar online (senha → @usuario) e depois
 * mostra busca por @usuario, pedidos recebidos, amigos e pedidos enviados.
 * Tocar num amigo abre o {@link FriendProfileFragment}.
 */
public class FriendsFragment extends Fragment {

    private FragmentFriendsBinding binding;
    private FriendsViewModel viewModel;
    private final Motion motion = new Motion();
    /** Status da última renderização: a troca de passo anima a entrada do cartão. */
    private Status shownStatus;
    private int rowIconPadding;

    public FriendsFragment() {
        super(R.layout.fragment_friends);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding = FragmentFriendsBinding.bind(view);
        SystemBarInsets.padTop(view);
        rowIconPadding = getResources().getDimensionPixelSize(R.dimen.avatar_row_icon_padding);

        AppContainer container = ((OVigiaApplication) requireActivity().getApplication()).container();
        viewModel = new ViewModelProvider(this, new FriendsViewModel.Factory(container.socialRepository,
                container.socialExecutor, container.mainExecutor)).get(FriendsViewModel.class);

        binding.btnBack.setOnClickListener(v -> nav().popBackStack());
        binding.btnRefresh.setOnClickListener(v -> viewModel.refresh());
        binding.btnGate.setOnClickListener(v -> submitGate());
        binding.etPassword.setOnEditorActionListener((v, actionId, event) -> onDone(actionId, this::submitGate));
        binding.etUsername.setOnEditorActionListener((v, actionId, event) -> onDone(actionId, this::submitGate));
        binding.searchLayout.setEndIconOnClickListener(v -> submitSearch());
        binding.etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId != EditorInfo.IME_ACTION_SEARCH && actionId != EditorInfo.IME_ACTION_DONE) return false;
            submitSearch();
            return true;
        });

        viewModel.state().observe(getViewLifecycleOwner(), this::render);
        viewModel.messages().observe(getViewLifecycleOwner(), event -> {
            FriendsUiState.Message message = event.consume();
            if (message != null) Snackbar.make(binding.getRoot(), messageText(message), Snackbar.LENGTH_SHORT).show();
        });
        viewModel.start();
    }

    @Override
    public void onDestroyView() {
        motion.cancelAll();
        super.onDestroyView();
        binding = null;
        shownStatus = null;
    }

    private NavController nav() {
        return NavHostFragment.findNavController(this);
    }

    private boolean isCurrent() {
        NavDestination current = nav().getCurrentDestination();
        return isAdded() && current != null && current.getId() == R.id.friendsFragment;
    }

    private static boolean onDone(int actionId, Runnable action) {
        if (actionId != EditorInfo.IME_ACTION_DONE) return false;
        action.run();
        return true;
    }

    private void submitGate() {
        FriendsUiState state = viewModel.state().getValue();
        if (state == null) return;
        switch (state.status) {
            case NEEDS_CONNECTION:
                hideKeyboard();
                viewModel.connect(text(binding.etPassword));
                break;
            case NEEDS_USERNAME:
                hideKeyboard();
                viewModel.claimUsername(text(binding.etUsername));
                break;
            case ERROR:
                viewModel.refresh();
                break;
            default:
                break;
        }
    }

    private void submitSearch() {
        hideKeyboard();
        viewModel.search(text(binding.etSearch));
    }

    private void hideKeyboard() {
        WindowCompat.getInsetsController(requireActivity().getWindow(), requireView())
                .hide(WindowInsetsCompat.Type.ime());
    }

    private static String text(EditText field) {
        return field.getText() == null ? "" : field.getText().toString();
    }

    // ---------------------------------------------------------------- render

    private void render(FriendsUiState state) {
        if (state.status == Status.SIGNED_OUT) {
            if (!isCurrent()) return;
            Bundle args = new Bundle();
            args.putInt(AuthFragment.ARG_NEXT_DESTINATION, R.id.friendsFragment);
            nav().navigate(R.id.authFragment, args, FadeNavOptions.popUpTo(R.id.friendsFragment, true));
            return;
        }
        boolean stepChanged = shownStatus != state.status;
        shownStatus = state.status;

        binding.progress.setVisibility(state.status == Status.LOADING ? View.VISIBLE : View.GONE);
        boolean ready = state.status == Status.READY;
        binding.readyContent.setVisibility(ready ? View.VISIBLE : View.GONE);
        binding.btnRefresh.setVisibility(ready ? View.VISIBLE : View.INVISIBLE);
        binding.btnRefresh.setEnabled(!state.working);
        boolean gate = state.status != Status.LOADING && !ready;
        binding.gateCard.setVisibility(gate ? View.VISIBLE : View.GONE);

        if (gate) {
            renderGate(state, stepChanged);
        } else if (ready) {
            renderReady(state, stepChanged);
        }
    }

    private void renderGate(FriendsUiState state, boolean stepChanged) {
        boolean connection = state.status == Status.NEEDS_CONNECTION;
        boolean username = state.status == Status.NEEDS_USERNAME;
        binding.passwordLayout.setVisibility(connection ? View.VISIBLE : View.GONE);
        binding.usernameLayout.setVisibility(username ? View.VISIBLE : View.GONE);
        binding.tvGateNote.setVisibility(connection || username ? View.VISIBLE : View.GONE);
        binding.btnGate.setVisibility(state.status == Status.NOT_CONFIGURED ? View.GONE : View.VISIBLE);
        binding.btnGate.setEnabled(!state.working);
        binding.btnGate.setText(state.working ? null : getString(gateAction(state.status)));
        binding.gateProgress.setVisibility(state.working ? View.VISIBLE : View.GONE);
        binding.passwordLayout.setEnabled(!state.working);
        binding.usernameLayout.setEnabled(!state.working);

        switch (state.status) {
            case NOT_CONFIGURED:
                binding.imageGate.setImageResource(R.drawable.ic_group);
                binding.tvGateTitle.setText(R.string.friends_not_configured_title);
                binding.tvGateBody.setText(R.string.friends_not_configured_body);
                break;
            case NEEDS_CONNECTION:
                binding.imageGate.setImageResource(R.drawable.ic_group);
                binding.tvGateTitle.setText(R.string.friends_connect_title);
                binding.tvGateBody.setText(R.string.friends_connect_body);
                break;
            case NEEDS_USERNAME:
                binding.imageGate.setImageResource(R.drawable.ic_at);
                binding.tvGateTitle.setText(R.string.friends_username_title);
                binding.tvGateBody.setText(R.string.friends_username_body);
                if (stepChanged && text(binding.etUsername).isEmpty()) {
                    binding.etUsername.setText(state.suggestedUsername);
                }
                break;
            case ERROR:
            default:
                binding.imageGate.setImageResource(R.drawable.ic_refresh);
                binding.tvGateTitle.setText(R.string.friends_error_title);
                binding.tvGateBody.setText(errorText(state.error));
                break;
        }

        // Erros de formulário: no campo quando dá, senão embaixo do cartão.
        binding.passwordLayout.setError(null);
        binding.usernameLayout.setError(null);
        String formError = state.status == Status.ERROR || state.error == null ? null : formErrorText(state);
        if (formError != null && connection && state.error == SocialException.Error.WRONG_PASSWORD) {
            binding.passwordLayout.setError(formError);
            formError = null;
        } else if (formError != null && username && (state.error == SocialException.Error.USERNAME_INVALID
                || state.error == SocialException.Error.USERNAME_TAKEN)) {
            binding.usernameLayout.setError(formError);
            formError = null;
        }
        binding.tvGateError.setVisibility(formError != null ? View.VISIBLE : View.GONE);
        binding.tvGateError.setText(formError);

        if (stepChanged) motion.fadeUp(binding.gateCard, 0);
    }

    @StringRes
    private static int gateAction(Status status) {
        switch (status) {
            case NEEDS_CONNECTION: return R.string.friends_connect_action;
            case NEEDS_USERNAME: return R.string.friends_username_action;
            case ERROR:
            default: return R.string.btn_retry;
        }
    }

    private String formErrorText(FriendsUiState state) {
        if (state.error == SocialException.Error.USERNAME_INVALID) {
            Username.Problem problem = Username.problemWith(Username.normalize(text(binding.etUsername)));
            if (problem == Username.Problem.TOO_SHORT) return getString(R.string.friends_username_too_short);
            if (problem == Username.Problem.TOO_LONG) return getString(R.string.friends_username_too_long);
            return getString(R.string.friends_username_invalid);
        }
        return getString(errorText(state.error));
    }

    @StringRes
    private static int errorText(@Nullable SocialException.Error error) {
        if (error == null) return R.string.friends_error_generic;
        switch (error) {
            case OFFLINE: return R.string.friends_error_offline;
            case WRONG_PASSWORD: return R.string.friends_error_wrong_password;
            case USERNAME_TAKEN: return R.string.friends_username_taken;
            case USERNAME_INVALID: return R.string.friends_username_invalid;
            case NOT_CONFIGURED: return R.string.friends_not_configured_body;
            default: return R.string.friends_error_generic;
        }
    }

    private void renderReady(FriendsUiState state, boolean stepChanged) {
        UserCard me = state.me;
        if (me != null) {
            binding.tvMeUsername.setText(getString(R.string.friends_username_format, me.username));
            binding.tvMeName.setText(me.name);
            binding.meCard.setContentDescription(getString(R.string.friends_cd_me, me.username));
            SharedImages.bindAvatar(this, binding.imageMe, me.avatar,
                    getResources().getDimensionPixelSize(R.dimen.avatar_me_icon_padding));
        }

        renderSearch(state);

        FriendsHub hub = state.hub;
        binding.tvIncomingTitle.setVisibility(hub.incoming.isEmpty() ? View.GONE : View.VISIBLE);
        binding.tvIncomingTitle.setText(getString(R.string.friends_incoming_title, hub.incoming.size()));
        fillList(binding.incomingList, hub.incoming.size(), (row, i) -> {
            FriendRequest r = hub.incoming.get(i);
            bindRow(row, r.from, state);
            showAction(row, R.string.friends_action_accept, () -> viewModel.accept(r.from.uid));
            showSecondary(row, R.string.friends_action_decline, () -> viewModel.decline(r.from.uid));
            row.tvDetail.setText(getString(R.string.friends_request_received_detail, r.from.username));
        });

        binding.tvFriendsTitle.setText(getString(R.string.friends_list_title, hub.friends.size()));
        binding.tvFriendsEmpty.setVisibility(hub.friends.isEmpty() ? View.VISIBLE : View.GONE);
        fillList(binding.friendsList, hub.friends.size(), (row, i) -> {
            UserCard friend = hub.friends.get(i);
            bindRow(row, friend, state);
            makeOpenable(row, friend);
        });

        binding.tvOutgoingTitle.setVisibility(hub.outgoing.isEmpty() ? View.GONE : View.VISIBLE);
        binding.tvOutgoingTitle.setText(getString(R.string.friends_outgoing_title, hub.outgoing.size()));
        fillList(binding.outgoingList, hub.outgoing.size(), (row, i) -> {
            FriendRequest r = hub.outgoing.get(i);
            bindRow(row, r.to, state);
            row.tvDetail.setText(R.string.friends_request_sent_detail);
            showSecondary(row, R.string.friends_action_cancel, () -> viewModel.cancel(r.to.uid));
        });

        if (stepChanged) {
            motion.staggerIn(0, binding.meCard, binding.searchLayout, binding.tvIncomingTitle, binding.incomingList,
                    binding.tvFriendsTitle, binding.tvFriendsEmpty, binding.friendsList, binding.tvOutgoingTitle,
                    binding.outgoingList);
        }
    }

    private void renderSearch(FriendsUiState state) {
        FriendsUiState.Search search = state.search;
        binding.searchProgress.setVisibility(search.searching ? View.VISIBLE : View.GONE);
        binding.searchLayout.setEnabled(!search.searching);

        String message = null;
        if (search.error != null) {
            switch (search.error) {
                case NOT_FOUND: message = getString(R.string.friends_search_not_found); break;
                case USERNAME_INVALID: message = getString(R.string.friends_search_invalid); break;
                default: message = getString(errorText(search.error)); break;
            }
        }
        binding.tvSearchMessage.setVisibility(message != null ? View.VISIBLE : View.GONE);
        binding.tvSearchMessage.setText(message);

        ItemFriendRowBinding row = binding.searchResult;
        UserCard card = search.result;
        row.getRoot().setVisibility(card != null ? View.VISIBLE : View.GONE);
        if (card == null || search.relationship == null) return;
        bindRow(row, card, state);
        switch (search.relationship) {
            case NONE:
                showAction(row, R.string.friends_action_add, () -> viewModel.sendRequest(card));
                break;
            case REQUEST_RECEIVED:
                showAction(row, R.string.friends_action_accept, () -> viewModel.accept(card.uid));
                break;
            case REQUEST_SENT:
                showStatus(row, R.string.friends_status_sent);
                break;
            case FRIENDS:
                showStatus(row, R.string.friends_status_friends);
                makeOpenable(row, card);
                break;
            case SELF:
            default:
                showStatus(row, R.string.friends_status_you);
                break;
        }
    }

    // ---------------------------------------------------------------- linhas

    private interface RowBinder {
        void bind(ItemFriendRowBinding row, int index);
    }

    /** Refaz a lista reaproveitando as linhas que já existem. */
    private void fillList(LinearLayout list, int count, RowBinder binder) {
        while (list.getChildCount() > count) list.removeViewAt(list.getChildCount() - 1);
        LayoutInflater inflater = LayoutInflater.from(list.getContext());
        for (int i = 0; i < count; i++) {
            ItemFriendRowBinding row = i < list.getChildCount()
                    ? ItemFriendRowBinding.bind(list.getChildAt(i))
                    : ItemFriendRowBinding.inflate(inflater, list, true);
            binder.bind(row, i);
        }
    }

    /** Foto, nome e @usuario; esconde as ações (cada lista mostra as suas). */
    private void bindRow(ItemFriendRowBinding row, UserCard card, FriendsUiState state) {
        row.tvName.setText(card.name);
        row.tvDetail.setText(getString(R.string.friends_username_format, card.username));
        SharedImages.bindAvatar(this, row.imageAvatar, card.avatar, rowIconPadding);
        boolean busy = state.busyUids.contains(card.uid);
        row.rowProgress.setVisibility(busy ? View.VISIBLE : View.GONE);
        row.btnAction.setVisibility(View.GONE);
        row.btnSecondary.setVisibility(View.GONE);
        row.tvStatus.setVisibility(View.GONE);
        row.imageChevron.setVisibility(View.GONE);
        row.getRoot().setOnClickListener(null);
        row.getRoot().setClickable(false);
        row.getRoot().setForeground(null);
        row.getRoot().setContentDescription(null);
        row.getRoot().setTag(busy);
    }

    private static boolean isBusy(ItemFriendRowBinding row) {
        return Boolean.TRUE.equals(row.getRoot().getTag());
    }

    private void showAction(ItemFriendRowBinding row, @StringRes int label, Runnable action) {
        if (isBusy(row)) return;
        row.btnAction.setVisibility(View.VISIBLE);
        row.btnAction.setText(label);
        row.btnAction.setOnClickListener(v -> action.run());
    }

    private void showSecondary(ItemFriendRowBinding row, @StringRes int description, Runnable action) {
        if (isBusy(row)) return;
        row.btnSecondary.setVisibility(View.VISIBLE);
        row.btnSecondary.setImageResource(R.drawable.ic_close);
        row.btnSecondary.setContentDescription(getString(description));
        row.btnSecondary.setOnClickListener(v -> action.run());
    }

    private void showStatus(ItemFriendRowBinding row, @StringRes int label) {
        row.tvStatus.setVisibility(View.VISIBLE);
        row.tvStatus.setText(label);
    }

    private void makeOpenable(ItemFriendRowBinding row, UserCard friend) {
        row.imageChevron.setVisibility(View.VISIBLE);
        row.getRoot().setForeground(AppCompatResources.getDrawable(requireContext(), R.drawable.ripple_card));
        row.getRoot().setContentDescription(getString(R.string.friends_cd_open_profile, friend.name, friend.username));
        row.getRoot().setOnClickListener(v -> {
            if (!isCurrent()) return;
            Bundle args = new Bundle();
            args.putString(FriendProfileFragment.ARG_FRIEND_UID, friend.uid);
            nav().navigate(R.id.friendProfileFragment, args, FadeNavOptions.builder().build());
        });
    }

    @StringRes
    private static int messageText(FriendsUiState.Message message) {
        switch (message) {
            case REQUEST_SENT: return R.string.friends_message_request_sent;
            case BECAME_FRIENDS: return R.string.friends_message_became_friends;
            case REQUEST_DECLINED: return R.string.friends_message_declined;
            case REQUEST_CANCELED: return R.string.friends_message_canceled;
            case USERNAME_SAVED: return R.string.friends_message_username_saved;
            case CONNECTED: return R.string.friends_message_connected;
            case ACTION_FAILED_OFFLINE: return R.string.friends_error_offline;
            case ACTION_FAILED:
            default: return R.string.friends_error_generic;
        }
    }
}
