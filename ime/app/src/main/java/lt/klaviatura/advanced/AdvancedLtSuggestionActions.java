package lt.klaviatura.advanced;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.PopupWindow;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import com.anysoftkeyboard.keyboards.views.KeyboardViewContainerView;
import com.menny.android.anysoftkeyboard.R;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Actions displayed on the left and right sides of the live suggestion strip. */
public final class AdvancedLtSuggestionActions {

  public interface SuggestionPicker {
    void pick(int index, @NonNull CharSequence suggestion);
  }

  private final Runnable mBackAction;
  private final SuggestionPicker mSuggestionPicker;
  private final SideActionProvider mBackProvider;
  private final SideActionProvider mMoreProvider;
  private List<CharSequence> mSuggestions = Collections.emptyList();
  private KeyboardViewContainerView mContainer;
  private View mBackButton;
  private View mMoreButton;
  private PopupWindow mPopup;
  private boolean mSpecialAction;
  private boolean mObscured;
  public void setSpecialAction(boolean active) {
    mSpecialAction = active;
    if (active) dismissPopup();
    updateVisibility();
  }
  private io.reactivex.disposables.Disposable mThemeSubscription;

  public AdvancedLtSuggestionActions(
      @NonNull Runnable backAction, @NonNull SuggestionPicker suggestionPicker) {
    mBackAction = backAction;
    mSuggestionPicker = suggestionPicker;
    mBackProvider =
        new SideActionProvider(
            R.drawable.advanced_lt_ic_arrow_back,
            R.string.advanced_lt_suggestions_back,
            KeyboardViewContainerView.StripActionAlignment.LEFT,
            () -> mBackAction.run(),
            true);
    mMoreProvider =
        new SideActionProvider(
            R.drawable.advanced_lt_ic_more,
            R.string.advanced_lt_suggestions_more,
            KeyboardViewContainerView.StripActionAlignment.RIGHT,
            this::showAllSuggestions,
            false);
  }

  public void attach(@NonNull KeyboardViewContainerView container) {
    mContainer = container;
    container.addStripAction(mBackProvider, true);
    container.addStripAction(mMoreProvider, false);
    mThemeSubscription = com.anysoftkeyboard.theme.KeyboardThemeFactory.observeCurrentTheme(container.getContext())
        .subscribe(theme -> {
          dismissPopup();
          if (mBackButton != null) AdvancedLtTheme.tintContents(mBackButton);
          if (mMoreButton != null) AdvancedLtTheme.tintContents(mMoreButton);
          updateMoreBackground();
        });
    updateVisibility();
  }

  public void detach() {
    if (mThemeSubscription != null) mThemeSubscription.dispose();
    mThemeSubscription = null;
    dismissPopup();
    if (mContainer != null) {
      mContainer.removeStripAction(mBackProvider);
      mContainer.removeStripAction(mMoreProvider);
      mContainer = null;
    }
  }

  public void setSuggestions(@NonNull List<? extends CharSequence> suggestions) {
    dismissPopup();
    mSuggestions = new ArrayList<>(suggestions);
    updateVisibility();
    if (mSuggestions.isEmpty()) {
      dismissPopup();
    }
  }

  private void updateVisibility() {
    final int visibility = mSuggestions.isEmpty() ? View.GONE : View.VISIBLE;
    if (mBackButton != null) {
      mBackButton.setVisibility(mObscured ? View.GONE : mSpecialAction ? View.VISIBLE : visibility);
    }
    if (mMoreButton != null) {
      mMoreButton.setVisibility(mSpecialAction || mObscured ? View.GONE : visibility);
    }
  }

  private void showAllSuggestions() {
    if (mContainer == null || mMoreButton == null || mSuggestions.isEmpty()) {
      return;
    }
    dismissPopup();

    final ScrollView scrollView = new ScrollView(mContainer.getContext());
    scrollView.setFillViewport(true);
    scrollView.setBackgroundColor(AdvancedLtTheme.surface(mContainer.getContext()));
    final GridLayout grid = new GridLayout(mContainer.getContext());
    grid.setColumnCount(3);
    grid.setPadding(dp(12), dp(8), dp(12), dp(8));
    scrollView.addView(
        grid,
        new ScrollView.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

    for (int index = 0; index < mSuggestions.size(); index++) {
      final int suggestionIndex = index;
      final CharSequence suggestion = mSuggestions.get(index);
      final TextView suggestionView = new TextView(mContainer.getContext());
      suggestionView.setText(suggestion);
      suggestionView.setTextColor(AdvancedLtTheme.text(mContainer.getContext()));
      suggestionView.setTextSize(19f);
      suggestionView.setGravity(Gravity.CENTER);
      suggestionView.setSingleLine(true);
      suggestionView.setPadding(dp(8), 0, dp(8), 0);
      suggestionView.setOnClickListener(
          ignored -> {
            dismissPopup();
            mSuggestionPicker.pick(suggestionIndex, suggestion);
          });
      final GridLayout.LayoutParams params = new GridLayout.LayoutParams();
      params.width = 0;
      params.height = dp(54);
      params.rowSpec = GridLayout.spec(index / 3);
      params.columnSpec = GridLayout.spec(index % 3, 1f);
      grid.addView(suggestionView, params);
    }

    final int rowCount = (mSuggestions.size() + 2) / 3;
    mPopup =
        new PopupWindow(
            scrollView,
            mContainer.getWidth() > 0
                ? mContainer.getWidth()
                : mContainer.getResources().getDisplayMetrics().widthPixels,
            Math.min(rowCount, 5) * dp(54) + dp(16),
            false);
    // Taking focus ends the editor's composing session and clears these suggestions.
    mPopup.setInputMethodMode(PopupWindow.INPUT_METHOD_NOT_NEEDED);
    mPopup.setBackgroundDrawable(new ColorDrawable(AdvancedLtTheme.surface(mContainer.getContext())));
    mPopup.setOutsideTouchable(true);
    mPopup.setElevation(dp(8));
    mPopup.showAsDropDown(mMoreButton, -mMoreButton.getLeft(), 0);
  }

  private void updateMoreBackground() {
    if (mMoreButton == null) return;
    int surface = AdvancedLtTheme.surface(mMoreButton.getContext());
    mMoreButton.setBackground(new android.graphics.drawable.GradientDrawable(
        android.graphics.drawable.GradientDrawable.Orientation.LEFT_RIGHT,
        new int[] {surface & 0x00ffffff, surface, surface}));
  }

  private void dismissPopup() {
    if (mPopup != null) {
      mPopup.dismiss();
      mPopup = null;
    }
  }

  private int dp(int value) {
    return Math.round(value * mContainer.getResources().getDisplayMetrics().density);
  }

  private final class SideActionProvider
      implements KeyboardViewContainerView.StripActionProvider {
    @Override public boolean isBalticboardNavigation() { return true; }
    @Override public void onOtherActionVisibilityChanged(boolean visible) {
      if (mObscured != visible) { mObscured = visible; updateVisibility(); }
    }

    private final int mIcon;
    private final int mDescription;
    private final KeyboardViewContainerView.StripActionAlignment mAlignment;
    private final Runnable mAction;
    private final boolean mIsBack;

    SideActionProvider(
        int icon,
        int description,
        @NonNull KeyboardViewContainerView.StripActionAlignment alignment,
        @NonNull Runnable action,
        boolean isBack) {
      mIcon = icon;
      mDescription = description;
      mAlignment = alignment;
      mAction = action;
      mIsBack = isBack;
    }

    @Override
    public @NonNull View inflateActionView(@NonNull ViewGroup parent) {
      final ImageButton button = new ImageButton(parent.getContext());
      final int size = parent.getResources().getDimensionPixelSize(R.dimen.candidate_strip_height);
      button.setLayoutParams(new ViewGroup.LayoutParams(mIsBack ? size : size + dp(24), size));
      button.setImageResource(mIcon);
      AdvancedLtTheme.tintContents(button);
      button.setScaleType(ImageButton.ScaleType.CENTER);
      button.setPadding(dp(mIsBack ? 14 : 38), dp(14), dp(14), dp(14));
      button.setContentDescription(parent.getResources().getString(mDescription));
      button.setBackgroundColor(Color.TRANSPARENT);
      button.setOnClickListener(ignored -> mAction.run());
      button.setVisibility(mSuggestions.isEmpty() ? View.GONE : View.VISIBLE);
      if (mIsBack) {
        mBackButton = button;
      } else {
        mMoreButton = button;
        updateMoreBackground();
      }
      return button;
    }

    @Override
    public void onRemoved() {
      if (mIsBack) {
        mBackButton = null;
      } else {
        mMoreButton = null;
      }
    }

    @Override
    public KeyboardViewContainerView.StripActionAlignment getAlignment() {
      return mAlignment;
    }
  }
}
