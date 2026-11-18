package lt.klaviatura.advanced;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.menny.android.anysoftkeyboard.R;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.evendanan.pixel.MainChild;

/** Full-height clipboard history shown in place of the standard keyboard. */
public final class AdvancedLtClipboardView extends FrameLayout implements MainChild {


  public interface Listener {
    void onClose();

    void onPaste(@NonNull CharSequence text);

    default boolean isFavorite(int index) { return false; }

    default boolean toggleFavorite(int index) { return false; }

    void onDelete(@NonNull List<Integer> positions);
  }

  private final List<CharSequence> mEntries = new ArrayList<>();
  private final Set<Integer> mSelectedPositions = new HashSet<>();
  private final List<Integer> mVisiblePositions = new ArrayList<>();
  private boolean mFavoritesOnly;
  private ImageView mFavoritesFilter;
  private RecyclerView mGrid;
  private ClipboardAdapter mAdapter;
  private View mEmptyView;
  private View mNormalToolbar;
  private View mSelectionToolbar;
  private View mDeleteConfirmation;
  private View mBottomFade;
  private TextView mSelectAll;
  private TextView mSelectedCount;
  private TextView mDeleteSelected;
  private TextView mConfirmationText;
  private Listener mListener;
  private boolean mSelectionMode;
  private int mBaseGridBottomPadding;
  private AdvancedLtBackspaceButton mBackspace;

  public void bindBackspace(com.anysoftkeyboard.keyboards.views.OnKeyboardActionListener listener) {
    mBackspace.bind(listener);
  }

  public void refreshTheme() {
    setBackgroundColor(AdvancedLtTheme.panelSurface(getContext()));
    AdvancedLtTheme.tintContents(this);
    int separatorColor = AdvancedLtTheme.isDay(getContext()) ? 0x40333333 : 0x40ffffff;
    for (int id : new int[] {R.id.advanced_lt_clipboard_separator,
        R.id.advanced_lt_clipboard_selection_separator, R.id.advanced_lt_clipboard_count_separator,
        R.id.advanced_lt_clipboard_confirmation_separator}) {
      findViewById(id).setBackgroundColor(separatorColor);
    }
    ((View)mNormalToolbar.getParent()).setBackgroundColor(AdvancedLtTheme.panelSurface(getContext()));
    mBottomFade.setBackground(AdvancedLtTheme.fade(getContext()));
    ((ViewGroup)mDeleteConfirmation).getChildAt(0).setBackground(AdvancedLtTheme.rounded(getContext(), false));
    mDeleteSelected.setTextColor(AdvancedLtTheme.isDay(getContext()) ? 0xffb3261e : 0xffff7373);
    ((TextView)findViewById(R.id.advanced_lt_clipboard_delete_confirm)).setTextColor(mDeleteSelected.getCurrentTextColor());
    if (mBackspace != null) mBackspace.refreshTheme();
    mAdapter.notifyDataSetChanged();
  }

  public AdvancedLtClipboardView(@NonNull Context context) {
    super(context);
  }

  public AdvancedLtClipboardView(@NonNull Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
  }

  public AdvancedLtClipboardView(
      @NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
  }

  @NonNull
  public static AdvancedLtClipboardView create(
      @NonNull Context context, @NonNull ViewGroup parent, @NonNull Listener listener) {
    final AdvancedLtClipboardView view =
        (AdvancedLtClipboardView)
            LayoutInflater.from(context)
                .inflate(R.layout.advanced_lt_clipboard_view, parent, false);
    final ViewGroup.LayoutParams params = view.getLayoutParams();
    params.height = parent.getHeight();
    view.setLayoutParams(params);
    view.mListener = listener;
    return view;
  }

  @Override
  protected void onFinishInflate() {
    super.onFinishInflate();
    mGrid = findViewById(R.id.advanced_lt_clipboard_grid);
    mEmptyView = findViewById(R.id.advanced_lt_clipboard_empty);
    mNormalToolbar = findViewById(R.id.advanced_lt_clipboard_normal_toolbar);
    mSelectionToolbar = findViewById(R.id.advanced_lt_clipboard_selection_toolbar);
    mDeleteConfirmation = findViewById(R.id.advanced_lt_clipboard_delete_confirmation);
    mBottomFade = findViewById(R.id.advanced_lt_clipboard_bottom_fade);
    mSelectAll = findViewById(R.id.advanced_lt_clipboard_select_all);
    mSelectedCount = findViewById(R.id.advanced_lt_clipboard_selected_count);
    mDeleteSelected = findViewById(R.id.advanced_lt_clipboard_delete_selected);
    mConfirmationText =
        findViewById(R.id.advanced_lt_clipboard_delete_confirmation_text);

    mFavoritesFilter = findViewById(R.id.advanced_lt_clipboard_favorites);
    mFavoritesFilter.setOnClickListener(ignored -> {
      mFavoritesOnly = !mFavoritesOnly;
      refreshVisibleEntries();
      mGrid.scrollToPosition(0);
    });
    mAdapter = new ClipboardAdapter();
    mGrid.setLayoutManager(new GridLayoutManager(getContext(), 2));
    mGrid.setAdapter(mAdapter);
    mGrid.addItemDecoration(new ClipboardItemSpacing(dp(8)));
    mBaseGridBottomPadding = mGrid.getPaddingBottom();
    mBackspace = new AdvancedLtBackspaceButton(getContext());
    FrameLayout panel = (FrameLayout)mGrid.getParent();
    FrameLayout.LayoutParams backspaceParams = new FrameLayout.LayoutParams(dp(56), dp(56),
        android.view.Gravity.BOTTOM | android.view.Gravity.END);
    backspaceParams.setMarginEnd(dp(16));
    panel.addView(mBackspace, backspaceParams);
    setBottomOffset(0);

    findViewById(R.id.advanced_lt_clipboard_close)
        .setOnClickListener(ignored -> listener().onClose());
    findViewById(R.id.advanced_lt_clipboard_start_delete)
        .setOnClickListener(ignored -> enterSelectionMode());
    findViewById(R.id.advanced_lt_clipboard_cancel_selection)
        .setOnClickListener(ignored -> leaveSelectionMode());
    mSelectAll.setOnClickListener(ignored -> toggleSelectAll());
    mDeleteSelected.setOnClickListener(ignored -> showDeleteConfirmation());
    findViewById(R.id.advanced_lt_clipboard_delete_cancel)
        .setOnClickListener(ignored -> hideDeleteConfirmation());
    findViewById(R.id.advanced_lt_clipboard_delete_confirm)
        .setOnClickListener(ignored -> confirmDelete());
    updateUi();
    refreshTheme();
  }

  public void setEntries(@NonNull List<? extends CharSequence> entries) {
    final List<CharSequence> updated = new ArrayList<>();
    for (CharSequence entry : entries) updated.add(entry.toString());
    // Indices no longer identify the same clips after a copy moves history around.
    if (!mEntries.equals(updated)) {
      hideDeleteConfirmation();
      leaveSelectionMode();
    }
    mEntries.clear();
    mEntries.addAll(updated);
    if (mEntries.isEmpty()) {
      leaveSelectionMode();
    }
    refreshVisibleEntries();
  }

  private void refreshVisibleEntries() {
    mVisiblePositions.clear();
    for (int index = 0; index < mEntries.size(); index++) {
      if (!mFavoritesOnly || listener().isFavorite(index)) mVisiblePositions.add(index);
    }
    mFavoritesFilter.setSelected(mFavoritesOnly);
    mFavoritesFilter.setImageTintList(android.content.res.ColorStateList.valueOf(AdvancedLtTheme.text(getContext())));
    mFavoritesFilter.setImageResource(mFavoritesOnly
        ? R.drawable.advanced_lt_bookmark_filled : R.drawable.advanced_lt_bookmark_outline);
    mAdapter.notifyDataSetChanged();
    updateUi();
  }

  /** Consumes back for the confirmation or selection layer, before the whole view closes. */
  public boolean handleBack() {
    if (mDeleteConfirmation.getVisibility() == View.VISIBLE) {
      hideDeleteConfirmation();
      return true;
    }
    if (mSelectionMode) {
      leaveSelectionMode();
      return true;
    }
    return false;
  }

  boolean isSelectionMode() {
    return mSelectionMode;
  }

  int getSelectedCount() {
    return mSelectedPositions.size();
  }

  @Override
  public void setBottomOffset(int offset) {
    mGrid.setPadding(
        mGrid.getPaddingLeft(),
        mGrid.getPaddingTop(),
        mGrid.getPaddingRight(),
        mBaseGridBottomPadding + Math.max(0, offset) + dp(48));
    final ViewGroup.LayoutParams params = mBottomFade.getLayoutParams();
    params.height = Math.max(0, offset) + dp(48);
    mBottomFade.setLayoutParams(params);
    if (mBackspace != null) {
      FrameLayout.LayoutParams buttonParams = (FrameLayout.LayoutParams)mBackspace.getLayoutParams();
      buttonParams.bottomMargin = Math.max(0, offset) + dp(32);
      mBackspace.setLayoutParams(buttonParams);
    }
  }

  void onEntryClicked(int position) {
    if (position < 0 || position >= mEntries.size()) return;
    if (mSelectionMode) {
      if (!mSelectedPositions.add(position)) {
        mSelectedPositions.remove(position);
      }
      mAdapter.notifyItemChanged(mVisiblePositions.indexOf(position));
      updateSelectionToolbar();
    } else {
      listener().onPaste(mEntries.get(position));
    }
  }

  private void enterSelectionMode() {
    if (mVisiblePositions.isEmpty()) return;
    mSelectionMode = true;
    mSelectedPositions.clear();
    mAdapter.notifyDataSetChanged();
    updateUi();
  }

  private void leaveSelectionMode() {
    mSelectionMode = false;
    mSelectedPositions.clear();
    if (mAdapter != null) mAdapter.notifyDataSetChanged();
    updateUi();
  }

  private void toggleSelectAll() {
    final boolean allSelected = mSelectedPositions.size() == mVisiblePositions.size();
    mSelectedPositions.clear();
    if (!allSelected) mSelectedPositions.addAll(mVisiblePositions);
    mAdapter.notifyDataSetChanged();
    updateSelectionToolbar();
  }

  private void showDeleteConfirmation() {
    final int count = mSelectedPositions.size();
    if (count == 0) return;
    mConfirmationText.setText(
        getResources()
            .getString(
                count == 1
                    ? R.string.advanced_lt_clipboard_delete_confirmation
                    : R.string.advanced_lt_clipboard_delete_confirmation_plural,
                count));
    mDeleteConfirmation.setVisibility(View.VISIBLE);
  }

  private void hideDeleteConfirmation() {
    mDeleteConfirmation.setVisibility(View.GONE);
  }

  private void confirmDelete() {
    final List<Integer> positions = new ArrayList<>(mSelectedPositions);
    Collections.sort(positions);
    hideDeleteConfirmation();
    listener().onDelete(positions);
    leaveSelectionMode();
  }

  private void updateUi() {
    if (mNormalToolbar == null) return;
    mNormalToolbar.setVisibility(mSelectionMode ? View.GONE : View.VISIBLE);
    mSelectionToolbar.setVisibility(mSelectionMode ? View.VISIBLE : View.GONE);
    mEmptyView.setVisibility(mVisiblePositions.isEmpty() ? View.VISIBLE : View.GONE);
    mGrid.setVisibility(mVisiblePositions.isEmpty() ? View.GONE : View.VISIBLE);
    findViewById(R.id.advanced_lt_clipboard_start_delete)
        .setEnabled(!mVisiblePositions.isEmpty());
    updateSelectionToolbar();
  }

  private void updateSelectionToolbar() {
    if (mSelectedCount == null) return;
    final int selectedCount = mSelectedPositions.size();
    mSelectedCount.setText(String.valueOf(selectedCount));
    mDeleteSelected.setEnabled(selectedCount > 0);
    mDeleteSelected.setAlpha(selectedCount > 0 ? 1f : 0.35f);
    final boolean allSelected = !mVisiblePositions.isEmpty() && selectedCount == mVisiblePositions.size();
    mSelectAll.setText(
        (allSelected ? "✓  " : "○  ")
            + getContext().getString(R.string.advanced_lt_clipboard_all));
  }

  @NonNull
  private Listener listener() {
    if (mListener == null) {
      throw new IllegalStateException("Clipboard listener was not attached.");
    }
    return mListener;
  }

  private GradientDrawable selectionCircle(boolean checked) {
    final GradientDrawable drawable = new GradientDrawable();
    drawable.setShape(GradientDrawable.OVAL);
    drawable.setColor(checked ? AdvancedLtTheme.accent(getContext()) : Color.TRANSPARENT);
    drawable.setStroke(dp(2), checked ? AdvancedLtTheme.accent(getContext()) : AdvancedLtTheme.text(getContext()));
    return drawable;
  }

  private int dp(int value) {
    return Math.round(value * getResources().getDisplayMetrics().density);
  }

  private final class ClipboardAdapter extends RecyclerView.Adapter<ClipboardViewHolder> {

    @NonNull
    @Override
    public ClipboardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
      return new ClipboardViewHolder(
          LayoutInflater.from(parent.getContext())
              .inflate(R.layout.advanced_lt_clipboard_item, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ClipboardViewHolder holder, int position) {
      final int historyIndex = mVisiblePositions.get(position);
      final boolean selected = mSelectedPositions.contains(historyIndex);
      holder.favorite.animate().cancel();
      holder.favorite.setAlpha(1f);
      holder.favorite.setVisibility(listener().isFavorite(historyIndex) ? View.VISIBLE : View.GONE);
      holder.favorite.setColorFilter(AdvancedLtTheme.text(getContext()));
      holder.text.setText(mEntries.get(historyIndex));
      holder.text.setPadding(mSelectionMode ? dp(32) : 0, 0, holder.favorite.getVisibility() == View.VISIBLE ? dp(24) : 0, 0);
      holder.selection.setVisibility(mSelectionMode ? View.VISIBLE : View.GONE);
      holder.selection.setText(selected ? "✓" : "");
      holder.selection.setBackground(selectionCircle(selected));
      holder.text.setTextColor(AdvancedLtTheme.text(getContext()));
      holder.selection.setTextColor(selected ? (AdvancedLtTheme.isDay(getContext()) ? Color.WHITE : Color.BLACK)
          : AdvancedLtTheme.text(getContext()));
      holder.itemView.setBackground(AdvancedLtTheme.rounded(getContext(), selected));
      holder.itemView.setOnClickListener(
          ignored -> {
            final int row = holder.getBindingAdapterPosition();
            if (row != RecyclerView.NO_POSITION) onEntryClicked(mVisiblePositions.get(row));
          });
      holder.itemView.setOnLongClickListener(ignored -> {
        final int row = holder.getBindingAdapterPosition();
        if (mSelectionMode || row == RecyclerView.NO_POSITION) return false;
        final boolean favorite = listener().toggleFavorite(mVisiblePositions.get(row));
        holder.favorite.animate().cancel();
        holder.favorite.setVisibility(View.VISIBLE);
        holder.favorite.setAlpha(favorite ? 0f : 1f);
        if (favorite) holder.text.setPadding(0, 0, dp(24), 0);
        holder.favorite.animate().alpha(favorite ? 1f : 0f).setDuration(150).withEndAction(() -> {
          if (!favorite) refreshVisibleEntries();
        }).start();
        return true;
      });
    }

    @Override
    public int getItemCount() {
      return mVisiblePositions.size();
    }
  }

  private static final class ClipboardViewHolder extends RecyclerView.ViewHolder {
    private final TextView text;
    private final TextView selection;
    private final ImageView favorite;

    ClipboardViewHolder(@NonNull View itemView) {
      super(itemView);
      text = itemView.findViewById(R.id.advanced_lt_clipboard_item_text);
      favorite = itemView.findViewById(R.id.advanced_lt_clipboard_item_favorite);
      selection = itemView.findViewById(R.id.advanced_lt_clipboard_item_selection);
    }
  }

  private static final class ClipboardItemSpacing extends RecyclerView.ItemDecoration {
    private final int mSpacing;

    ClipboardItemSpacing(int spacing) {
      mSpacing = spacing;
    }

    @Override
    public void getItemOffsets(
        @NonNull Rect outRect,
        @NonNull View view,
        @NonNull RecyclerView parent,
        @NonNull RecyclerView.State state) {
      final int position = parent.getChildAdapterPosition(view);
      outRect.left = position % 2 == 0 ? mSpacing : mSpacing / 2;
      outRect.right = position % 2 == 0 ? mSpacing / 2 : mSpacing;
      outRect.top = mSpacing;
      outRect.bottom = mSpacing;
    }
  }
}
