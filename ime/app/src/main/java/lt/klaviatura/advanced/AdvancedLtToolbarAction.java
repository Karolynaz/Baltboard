package lt.klaviatura.advanced;

import android.graphics.Color;
import android.content.res.ColorStateList;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import com.anysoftkeyboard.keyboards.views.KeyboardViewContainerView;
import com.menny.android.anysoftkeyboard.R;
import com.anysoftkeyboard.theme.KeyboardThemeFactory;
import io.reactivex.disposables.Disposable;

/** The idle toolbar shown before the user starts typing a word. */
public final class AdvancedLtToolbarAction
    implements KeyboardViewContainerView.StripActionProvider {

  private final Runnable mAiAction;
  private final Runnable mTranslateAction;
  private ImageButton mTranslateButton;
  private boolean mTranslateEnabled;
  private final Runnable mEmojiAction;
  private final Runnable mClipboardAction;
  private final Runnable mSettingsAction;
  private View.OnLongClickListener mQuickSettingsAction;
  private final Runnable mUndoAction;
  private View mRootView;
  private ImageButton mEmojiButton;
  private ImageButton mClipboardButton;
  private ImageButton mAiButton;
  private Disposable mThemeSubscription;
  private boolean mVisible = true;
  private boolean mObscured;
  private LinearLayout mLeftActions;
  private android.widget.TextView mClipboardHint;
  private FrameLayout mClipboardHintContainer;
  private ImageButton mDismissClipboardHint;
  private String mClipboardText;
  private Runnable mPasteAction;
  private Runnable mDismissAction;
  @Override public boolean isBalticboardNavigation() { return true; }
  @Override public void onOtherActionVisibilityChanged(boolean visible) {
    mObscured = visible;
    if (mRootView != null) mRootView.setVisibility(mVisible && !mObscured ? View.VISIBLE : View.GONE);
  }
  private boolean mAiEnabled = true;

  public AdvancedLtToolbarAction(
      Runnable aiAction,
      Runnable emojiAction,
      Runnable clipboardAction,
      Runnable settingsAction,
      Runnable undoAction) {
    this(aiAction, null, emojiAction, clipboardAction, settingsAction, undoAction);
  }

  public AdvancedLtToolbarAction(Runnable aiAction, Runnable translateAction,
      Runnable emojiAction, Runnable clipboardAction, Runnable settingsAction, Runnable undoAction) {
    mAiAction = aiAction;
    mTranslateAction = translateAction;
    mEmojiAction = emojiAction;
    mClipboardAction = clipboardAction;
    mSettingsAction = settingsAction;
    mUndoAction = undoAction;
  }

  public void setQuickSettingsAction(@NonNull View.OnLongClickListener listener) {
    mQuickSettingsAction = listener;
  }

  @Override
  public @NonNull View inflateActionView(@NonNull ViewGroup parent) {
    final FrameLayout toolbar = new FrameLayout(parent.getContext());
    toolbar.setLayoutParams(
        new ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            parent.getResources().getDimensionPixelSize(R.dimen.candidate_strip_height)));
    toolbar.setPadding(dp(parent, 8), 0, dp(parent, 8), 0);

    final LinearLayout leftActions = new LinearLayout(parent.getContext());
    mLeftActions = leftActions;
    leftActions.setOrientation(LinearLayout.HORIZONTAL);
    leftActions.setGravity(Gravity.CENTER_VERTICAL);
    toolbar.addView(
        leftActions,
        new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
            Gravity.START | Gravity.CENTER_VERTICAL));

    mAiButton =
        addButton(
            leftActions,
            R.drawable.advanced_lt_ic_ai_check,
            R.string.advanced_lt_toolbar_ai,
            mAiAction);
    mAiButton.setVisibility(mAiEnabled ? View.VISIBLE : View.GONE);
    if (mTranslateAction != null) {
      mTranslateButton = addButton(leftActions, R.drawable.advanced_lt_ic_translate,
          R.string.advanced_lt_toolbar_translate, mTranslateAction);
      mTranslateButton.setVisibility(mTranslateEnabled ? View.VISIBLE : View.GONE);
    }
    mEmojiButton = addButton(
        leftActions,
        R.drawable.advanced_lt_ic_emoji,
        R.string.advanced_lt_toolbar_emoji,
        mEmojiAction);
    mClipboardButton = addButton(
        leftActions,
        R.drawable.advanced_lt_ic_clipboard,
        R.string.advanced_lt_toolbar_clipboard,
        mClipboardAction);

    final ImageButton settings =
        createButton(
            toolbar,
            R.drawable.advanced_lt_ic_settings,
            R.string.advanced_lt_toolbar_settings,
            mSettingsAction);
    settings.setOnLongClickListener(mQuickSettingsAction);
    final FrameLayout.LayoutParams settingsParams =
        new FrameLayout.LayoutParams(
            dp(parent, 44), dp(parent, 44), Gravity.END | Gravity.CENTER_VERTICAL);
    settingsParams.setMargins(dp(parent, 3), 0, dp(parent, 3), 0);
    toolbar.addView(settings, settingsParams);
    final ImageButton undo = createButton(
        toolbar, R.drawable.advanced_lt_ic_undo, R.string.advanced_lt_toolbar_undo, mUndoAction);
    final FrameLayout.LayoutParams undoParams = new FrameLayout.LayoutParams(
        dp(parent, 44), dp(parent, 44), Gravity.END | Gravity.CENTER_VERTICAL);
    undoParams.setMarginEnd(dp(parent, 53));
    toolbar.addView(undo, undoParams);

    mClipboardHint = new android.widget.TextView(parent.getContext());
    mClipboardHint.setSingleLine(true);
    mClipboardHint.setEllipsize(android.text.TextUtils.TruncateAt.END);
    mClipboardHint.setTextSize(15);
    mClipboardHint.setGravity(Gravity.CENTER_VERTICAL);
    mClipboardHint.setPadding(dp(parent, 12), dp(parent, 6), dp(parent, 40), dp(parent, 6));
    mClipboardHint.setOnClickListener(view -> { if (mPasteAction != null) mPasteAction.run(); });
    mClipboardHintContainer = new FrameLayout(parent.getContext());
    FrameLayout.LayoutParams hintParams = new FrameLayout.LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT, dp(parent, 40), Gravity.START | Gravity.CENTER_VERTICAL);
    hintParams.setMarginEnd(dp(parent, 104));
    hintParams.setMarginStart(dp(parent, hintStartMargin()));
    toolbar.addView(mClipboardHintContainer, hintParams);
    mClipboardHintContainer.addView(mClipboardHint, new FrameLayout.LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT));
    mDismissClipboardHint = createButton(
        toolbar,
        R.drawable.advanced_lt_ic_close,
        R.string.advanced_lt_toolbar_dismiss_paste,
        () -> { if (mDismissAction != null) mDismissAction.run(); });
    mDismissClipboardHint.setPadding(dp(parent, 8), dp(parent, 8), dp(parent, 8), dp(parent, 8));
    final FrameLayout.LayoutParams dismissParams =
        new FrameLayout.LayoutParams(dp(parent, 36), dp(parent, 40), Gravity.END | Gravity.CENTER_VERTICAL);
    mClipboardHintContainer.addView(mDismissClipboardHint, dismissParams);
    updateClipboardHint();

    mRootView = toolbar;
    if (mThemeSubscription != null) mThemeSubscription.dispose();
    mThemeSubscription = KeyboardThemeFactory.observeCurrentTheme(parent.getContext())
        .subscribe(theme -> tintButtons(toolbar,
            AdvancedLtTheme.isLightTheme(theme.getId())
                ? Color.rgb(51, 51, 51) : Color.WHITE));
    toolbar.setVisibility(mVisible ? View.VISIBLE : View.GONE);
    return toolbar;
  }

  public void setVisible(boolean visible) {
    mVisible = visible;
    if (mRootView != null) {
      mRootView.setVisibility(visible && !mObscured ? View.VISIBLE : View.GONE);
    }
  }

  public void setAiEnabled(boolean enabled) {
    mAiEnabled = enabled;
    if (mAiButton != null) mAiButton.setVisibility(enabled ? View.VISIBLE : View.GONE);
    updateClipboardHintMargins();
  }

  public void setTranslateEnabled(boolean enabled) {
    mTranslateEnabled = enabled;
    if (mTranslateButton != null) mTranslateButton.setVisibility(enabled ? View.VISIBLE : View.GONE);
    updateClipboardHintMargins();
  }

  private int hintStartMargin() {
    return 61 + (mAiEnabled ? 47 : 0) + (mTranslateEnabled && mTranslateAction != null ? 50 : 0);
  }

  public void setClipboardHint(String text, Runnable pasteAction, Runnable dismissAction) {
    mClipboardText = text;
    mPasteAction = pasteAction;
    mDismissAction = dismissAction;
    updateClipboardHint();
  }

  private void updateClipboardHintMargins() {
    if (mClipboardHintContainer == null) return;
    FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) mClipboardHintContainer.getLayoutParams();
    params.setMarginStart(dp(mClipboardHint, hintStartMargin()));
    mClipboardHintContainer.setLayoutParams(params);
  }

  private void updateClipboardHint() {
    if (mClipboardHint == null) return;
    boolean visible = mClipboardText != null && mPasteAction != null;
    if (mClipboardButton != null) mClipboardButton.setVisibility(visible ? View.GONE : View.VISIBLE);
    mClipboardHintContainer.setVisibility(visible ? View.VISIBLE : View.GONE);
    if (visible) {
      String preview = mClipboardText.substring(0, Math.min(mClipboardText.length(), 160))
          .replace('\n', ' ').replace('\r', ' ');
      mClipboardHint.setText(preview);
      boolean lt = "lt".equals(mClipboardHint.getResources().getConfiguration().locale.getLanguage());
      mClipboardHint.setContentDescription((lt ? "Įklijuoti: " : "Paste: ") + preview);
    }
  }

  @NonNull
  private static ImageButton addButton(
      @NonNull LinearLayout toolbar,
      @DrawableRes int icon,
      @StringRes int description,
      @NonNull Runnable action) {
    final ImageButton button = createButton(toolbar, icon, description, action);
    final LinearLayout.LayoutParams params =
        new LinearLayout.LayoutParams(dp(toolbar, 44), dp(toolbar, 44));
    params.setMargins(dp(toolbar, 3), 0, dp(toolbar, 3), 0);
    toolbar.addView(button, params);
    return button;
  }

  @NonNull
  private static ImageButton createButton(
      @NonNull View parent,
      @DrawableRes int icon,
      @StringRes int description,
      @NonNull Runnable action) {
    final ImageButton button = new ImageButton(parent.getContext());
    button.setImageResource(icon);
    button.setPadding(dp(parent, 11), dp(parent, 11), dp(parent, 11), dp(parent, 11));
    button.setScaleType(ImageButton.ScaleType.CENTER_INSIDE);
    button.setContentDescription(parent.getResources().getString(description));
    button.setClickable(true);
    button.setFocusable(true);
    button.setBackgroundColor(Color.TRANSPARENT);
    button.setOnClickListener(ignored -> action.run());
    return button;
  }

  private static int dp(@NonNull View view, int value) {
    return Math.round(value * view.getResources().getDisplayMetrics().density);
  }

  private static void tintButtons(View view, int color) {
    if (view instanceof ImageButton) {
      ((ImageButton) view).setImageTintList(ColorStateList.valueOf(color));
    } else if (view instanceof android.widget.TextView) {
      ((android.widget.TextView) view).setTextColor(color);
      android.graphics.drawable.GradientDrawable background = new android.graphics.drawable.GradientDrawable();
      background.setCornerRadius(dp(view, 18));
      background.setColor(color == Color.WHITE ? 0xff404040 : 0xffdddddd);
      view.setBackground(background);
    } else if (view instanceof ViewGroup) {
      ViewGroup group = (ViewGroup) view;
      for (int i = 0; i < group.getChildCount(); i++) tintButtons(group.getChildAt(i), color);
    }
  }

  @Override
  public void onRemoved() {
    if (mThemeSubscription != null) mThemeSubscription.dispose();
    mThemeSubscription = null;
    mRootView = null;
    mEmojiButton = null;
    mClipboardButton = null;
    mAiButton = null;
    mTranslateButton = null;
    mLeftActions = null;
    mClipboardHint = null;
    mClipboardHintContainer = null;
    mDismissClipboardHint = null;
  }
}
