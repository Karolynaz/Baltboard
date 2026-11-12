/*
 * Copyright (c) 2013 Menny Even-Danan
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.menny.android.anysoftkeyboard;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import androidx.annotation.NonNull;
import com.anysoftkeyboard.api.KeyCodes;
import com.anysoftkeyboard.keyboards.Keyboard;
import com.anysoftkeyboard.keyboards.views.AnyKeyboardView;
import com.anysoftkeyboard.keyboards.views.KeyboardViewContainerView;
import com.anysoftkeyboard.prefs.DirectBootAwareSharedPreferences;
import com.anysoftkeyboard.saywhat.PublicNotices;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import lt.klaviatura.advanced.AdvancedLtClipboardView;
import lt.klaviatura.advanced.AdvancedLtSettingsActivity;
import lt.klaviatura.advanced.AdvancedLtSuggestionActions;
import lt.klaviatura.advanced.AdvancedLtTheme;
import lt.klaviatura.advanced.AdvancedLtToolbarAction;
import lt.klaviatura.advanced.GeminiClient;
import lt.klaviatura.advanced.SecretStore;

/*
 * Why is this class exists?
 * Well, I first released ASK as SoftKeyboard main class, and then renamed the class, but I can't do that
 * and still support upgrade... so SoftKeyboard inherits from the actual class
 */
public class SoftKeyboard extends PublicNotices {

  /* DEVELOPERS NOTICE:
  This TURNED-OFF code is used to simulate
  a very slow InputConnection updates:
  On some devices and apps, the onUpdateSelection callback will be
  very delayed, and may get com.anysoftkeyboard.ime.AnySoftKeyboardSuggestions.mGlobalCursorPosition
  out-of-sync.
   */
  private static final boolean DELAY_SELECTION_UPDATES = false;
  private Handler mDelayer = null;
  private AdvancedLtToolbarAction mAdvancedLtToolbar;
  private boolean mShowingWordSuggestions;
  private AdvancedLtSuggestionActions mAdvancedLtSuggestionActions;
  private AdvancedLtClipboardView mAdvancedLtClipboardView;
  private GeminiClient mGeminiClient;
  private lt.klaviatura.advanced.AdvancedLtRewriteView mRewriteView;
  private long mRewriteGeneration;
  private boolean mAiRequestInFlight;
  private long mInputSessionId;
  private final Handler mClipboardHintHandler = new Handler(Looper.getMainLooper());
  private long mConsumedClipboardTimestamp;
  private Window mGlassWindow;
  private Drawable mOpaqueWindowBackground;
  private int mOpaqueWindowFormat;
  private final Runnable mRefreshClipboardHint = this::refreshClipboardHint;

  @Override
  public void onStartInput(EditorInfo attribute, boolean restarting) {
    mInputSessionId++;
    super.onStartInput(attribute, restarting);
  }

  @Override
  public void onFinishInput() {
    mInputSessionId++;
    super.onFinishInput();
  }
  private boolean mSuppressSuggestionsForCurrentWord;
  private boolean mSawWordBoundaryAfterSuppression;

  @Override
  public void onCreate() {
    super.onCreate();
    if (DELAY_SELECTION_UPDATES) mDelayer = new Handler(Looper.getMainLooper());
    mGeminiClient = new GeminiClient();
    AdvancedLtSettingsActivity.migrateAiPreferences(this);

    mAdvancedLtToolbar =
        new AdvancedLtToolbarAction(
            this::startAiCorrection,
            () -> startAiTextAction(true),
            this::switchToQuickTextKeyboard,
            this::showAdvancedLtClipboard,
            this::openAdvancedLtSettings,
            () -> onKey(KeyCodes.UNDO, null, 0, null, true));
    mAdvancedLtToolbar.setQuickSettingsAction(this::showQuickSettings);
    mAdvancedLtSuggestionActions =
        new AdvancedLtSuggestionActions(
            this::hideCurrentWordSuggestions, this::pickSuggestionManually);
  }

  @Override
  public void onDestroy() {
    mClipboardHintHandler.removeCallbacks(mRefreshClipboardHint);
    mInputSessionId++;
    if (mGeminiClient != null) mGeminiClient.close();
    closeRewrite(false);
    super.onDestroy();
  }

  @Override
  public View onCreateInputView() {
    final View container = super.onCreateInputView();
    if (!BuildConfig.TESTING_BUILD && getInputView() instanceof View keyboard) {
      keyboard.addOnLayoutChangeListener(
          (view, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            if (right - left != oldRight - oldLeft) {
              view.post(() -> {
                if (getInputView() == view) updateKeyboardGeometry();
              });
            }
          });
    }
    return container;
  }

  private void updateKeyboardGeometry() {
    if (!(getInputView() instanceof View keyboard)) return;
    final int width = keyboard.getWidth() > 0
        ? keyboard.getWidth() - keyboard.getPaddingLeft() - keyboard.getPaddingRight()
        : getInputView().getThemedKeyboardDimens().getKeyboardMaxWidth();
    final var preferences = DirectBootAwareSharedPreferences.create(this);
    final boolean wider =
        preferences.getBoolean(getString(R.string.settings_key_baltboard_wider_key_spacing), true);
    final boolean split = preferences.getBoolean(
        getString(R.string.settings_key_baltboard_split_wide_keyboard), false);
    getKeyboardSwitcher().setKeyboardGeometry(
        width, wider, split, isInputViewShown() ? getCurrentInputEditorInfo() : null);
  }

  @Override
  public void onConfigurationChanged(Configuration configuration) {
    super.onConfigurationChanged(configuration);
    if (!BuildConfig.TESTING_BUILD && getInputView() instanceof View keyboard) {
      keyboard.post(() -> {
        if (getInputView() == keyboard) updateKeyboardGeometry();
      });
    }
  }

  private void updateGlassBackground() {
    final var container = getInputViewContainer();
    if (container == null || getWindow() == null || getWindow().getWindow() == null) return;
    final Window window = getWindow().getWindow();
    final boolean enabled = DirectBootAwareSharedPreferences.create(this)
        .getBoolean(getString(R.string.settings_key_baltboard_glass), false);
    if (enabled && mGlassWindow != window) {
      mGlassWindow = window;
      mOpaqueWindowBackground = window.getDecorView().getBackground();
      mOpaqueWindowFormat = window.getAttributes().format;
      window.setBackgroundDrawableResource(android.R.color.transparent);
      window.setFormat(PixelFormat.TRANSLUCENT);
    } else if (!enabled && mGlassWindow == window) {
      window.setBackgroundDrawable(mOpaqueWindowBackground);
      window.setFormat(mOpaqueWindowFormat);
      mOpaqueWindowBackground = null;
      mGlassWindow = null;
    }
    container.setGlassBackground(enabled ? AdvancedLtTheme.glassBackground(this) : null);
  }

  @Override
  public void onStartInputView(EditorInfo attribute, boolean restarting) {
    closeRewrite(true);
    closeAdvancedLtClipboard(true);
    mSuppressSuggestionsForCurrentWord = false;
    mSawWordBoundaryAfterSuppression = false;
    if (!BuildConfig.TESTING_BUILD) updateKeyboardGeometry();
    super.onStartInputView(attribute, restarting);
    if (BuildConfig.TESTING_BUILD) {
      return;
    }
    updateGlassBackground();
    mAdvancedLtToolbar.setAiEnabled(
        getSharedPreferences(AdvancedLtSettingsActivity.PREFS_NAME, MODE_PRIVATE)
            .getBoolean(AdvancedLtSettingsActivity.PREF_AI_ENABLED, true));
    mAdvancedLtToolbar.setTranslateEnabled(
        getSharedPreferences(AdvancedLtSettingsActivity.PREFS_NAME, MODE_PRIVATE)
            .getBoolean(AdvancedLtSettingsActivity.PREF_AI_TRANSLATE_ENABLED, false));
    getInputViewContainer().addStripAction(mAdvancedLtToolbar, false);
    mAdvancedLtSuggestionActions.attach(getInputViewContainer());
    mShowingWordSuggestions = false;
    getInputViewContainer().getCandidateView().setActionStateListener(active -> {
      mAdvancedLtToolbar.setVisible(!active && !mShowingWordSuggestions);
      mAdvancedLtSuggestionActions.setSpecialAction(active);
      getInputViewContainer().getCandidateView().setPadding(active || mShowingWordSuggestions ? dp(52) : 0,
          0, mShowingWordSuggestions && !active ? dp(52) : 0, 0);
    });
    mAdvancedLtToolbar.setVisible(true);
    mAdvancedLtSuggestionActions.setSuggestions(Collections.emptyList());
    getInputViewContainer().setActionsStripVisibility(true);
    refreshClipboardHint();
  }

  @Override
  public void onFinishInputView(boolean finishingInput) {
    mClipboardHintHandler.removeCallbacks(mRefreshClipboardHint);
    mInputSessionId++;
    closeRewrite(false);
    closeAdvancedLtClipboard(false);
    if (!BuildConfig.TESTING_BUILD) {
      if (getInputViewContainer() != null && getInputViewContainer().getCandidateView() != null)
        getInputViewContainer().getCandidateView().setActionStateListener(null);
      mAdvancedLtSuggestionActions.detach();
      getInputViewContainer().removeStripAction(mAdvancedLtToolbar);
    }
    super.onFinishInputView(finishingInput);
  }

  private void showAdvancedLtClipboard() {
    final KeyboardViewContainerView inputViewContainer = getInputViewContainer();
    if (inputViewContainer == null) return;
    if (mAdvancedLtClipboardView != null) {
      mAdvancedLtClipboardView.setEntries(getClipboardHistoryEntries());
      return;
    }

    abortCorrectionAndResetPredictionState(false);
    cleanUpQuickTextKeyboard(false);
    final AnyKeyboardView actualInputView = (AnyKeyboardView) getInputView();
    if (actualInputView == null) return;

    final AdvancedLtClipboardView clipboardView =
        AdvancedLtClipboardView.create(
            this,
            inputViewContainer,
            new AdvancedLtClipboardView.Listener() {
              @Override
              public void onClose() {
                closeAdvancedLtClipboard(true);
              }

              @Override
              public void onPaste(@NonNull CharSequence text) {
                pasteClipboardText(text);
              }

              @Override
              public void onDelete(@NonNull List<Integer> positions) {
                deleteClipboardHistoryEntries(positions);
              }

            });
    clipboardView.bindBackspace(this);
    clipboardView.setEntries(getClipboardHistoryEntries());
    actualInputView.resetInputView();
    actualInputView.setVisibility(View.GONE);
    mAdvancedLtClipboardView = clipboardView;
    inputViewContainer.addView(clipboardView);
    inputViewContainer.setActionsStripVisibility(false);
    updateBackCallbackState();
  }

  private boolean closeAdvancedLtClipboard(boolean reshowStandardKeyboard) {
    final KeyboardViewContainerView inputViewContainer = getInputViewContainer();
    if (inputViewContainer == null || mAdvancedLtClipboardView == null) return false;
    if (reshowStandardKeyboard && getInputView() != null) {
      ((View) getInputView()).setVisibility(View.VISIBLE);
    }
    inputViewContainer.removeView(mAdvancedLtClipboardView);
    mAdvancedLtClipboardView = null;
    inputViewContainer.setActionsStripVisibility(true);
    updateBackCallbackState();
    return true;
  }

  @Override
  protected void onClipboardHistoryChanged() {
    if (mAdvancedLtClipboardView != null) {
      mAdvancedLtClipboardView.setEntries(getClipboardHistoryEntries());
    }
    if (mAdvancedLtToolbar != null) refreshClipboardHint();
  }

  private boolean pasteClipboardText(CharSequence text) {
    InputConnection connection = getCurrentInputConnection();
    if (connection == null || TextUtils.isEmpty(text)) return false;
    connection.beginBatchEdit();
    try {
      // Clipboard insertion is not a composing word or a whole-span backspace undo.
      abortCorrectionAndResetPredictionState(false);
      return connection.commitText(text.toString(), 1);
    } finally {
      connection.endBatchEdit();
    }
  }

  private void refreshClipboardHint() {
    mClipboardHintHandler.removeCallbacks(mRefreshClipboardHint);
    if (mAdvancedLtToolbar == null) return;
    mAdvancedLtToolbar.setClipboardHint(null, null, null);
    if (!isInputViewShown()) return;
    EditorInfo editor = getCurrentInputEditorInfo();
    if (editor == null || isTextPassword(editor) || isNumberPassword(editor)
        || getSuggest().isIncognitoMode() || android.os.Build.VERSION.SDK_INT < 26) return;
    ClipboardManager manager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
    try {
      android.content.ClipDescription description = manager.getPrimaryClipDescription();
      if (description == null) return;
      long copiedAt = description.getTimestamp();
      long age = System.currentTimeMillis() - copiedAt;
      if (copiedAt <= 0 || copiedAt == mConsumedClipboardTimestamp || age < 0 || age >= 300_000L) return;
      if (description.getExtras() != null
          && description.getExtras().getBoolean("android.content.extra.IS_SENSITIVE", false)) return;
      ClipData clip = manager.getPrimaryClip();
      if (clip == null || clip.getItemCount() == 0) return;
      CharSequence text = clip.getItemAt(0).getText();
      if (TextUtils.isEmpty(text)) return;
      String pasteText = text.toString();
      mAdvancedLtToolbar.setClipboardHint(pasteText, () -> {
        if (pasteClipboardText(pasteText)) {
          mConsumedClipboardTimestamp = copiedAt;
          refreshClipboardHint();
        }
      }, () -> {
        mConsumedClipboardTimestamp = copiedAt;
        refreshClipboardHint();
      });
      mClipboardHintHandler.postDelayed(mRefreshClipboardHint, 300_000L - age);
    } catch (SecurityException ignored) {
      // The editor may have lost focus while Android was granting clipboard access.
    }
  }

  @Override
  protected boolean shouldShowClipboardSuggestion() {
    return BuildConfig.TESTING_BUILD;
  }

  @Override
  protected boolean handleCloseRequest() {
    if (mRewriteView != null) { closeRewrite(true); return true; }
    if (mAdvancedLtClipboardView != null) {
      if (mAdvancedLtClipboardView.handleBack()) return true;
      return closeAdvancedLtClipboard(true);
    }
    return super.handleCloseRequest();
  }

  @Override
  protected boolean hasStuffToClose() {
    return mRewriteView != null || mAdvancedLtClipboardView != null || super.hasStuffToClose();
  }

  @Override
  public void onKey(
      int primaryCode,
      Keyboard.Key key,
      int multiTapIndex,
      int[] nearByKeyCodes,
      boolean fromUI) {
    if (mSuppressSuggestionsForCurrentWord) {
      if (mSawWordBoundaryAfterSuppression
          && primaryCode > 0
          && Character.isLetterOrDigit(primaryCode)) {
        mSuppressSuggestionsForCurrentWord = false;
        mSawWordBoundaryAfterSuppression = false;
      } else if (isWordBoundary(primaryCode)) {
        mSawWordBoundaryAfterSuppression = true;
      }
    }
    super.onKey(primaryCode, key, multiTapIndex, nearByKeyCodes, fromUI);
  }

  @Override
  protected void onThemeChanged(@NonNull com.anysoftkeyboard.theme.KeyboardTheme theme) {
    super.onThemeChanged(theme);
    if (!BuildConfig.TESTING_BUILD) updateGlassBackground();
    if (mAdvancedLtClipboardView != null) mAdvancedLtClipboardView.refreshTheme();
  }

  @Override
  protected void setSuggestions(
      @NonNull List<? extends CharSequence> suggestions, int highlightedSuggestionIndex) {
    if (BuildConfig.TESTING_BUILD) {
      super.setSuggestions(suggestions, highlightedSuggestionIndex);
      return;
    }
    final boolean showWordSuggestions =
        !mSuppressSuggestionsForCurrentWord
            && isPredictionOn()
            && hasSuggestionWord()
            && !suggestions.isEmpty();
    final List<? extends CharSequence> visibleSuggestions;
    if (showWordSuggestions) {
      visibleSuggestions = lt.klaviatura.advanced.AdvancedLtSuggestions.firstThree(suggestions);
      if (highlightedSuggestionIndex >= visibleSuggestions.size()) {
        highlightedSuggestionIndex = -1;
      }
    } else {
      visibleSuggestions = Collections.emptyList();
      highlightedSuggestionIndex = -1;
    }
    mShowingWordSuggestions = showWordSuggestions;
    super.setSuggestions(visibleSuggestions, highlightedSuggestionIndex);

    if (mAdvancedLtToolbar != null) {
      mAdvancedLtToolbar.setVisible(!showWordSuggestions);
    }
    if (mAdvancedLtSuggestionActions != null) {
      mAdvancedLtSuggestionActions.setSuggestions(
          showWordSuggestions ? suggestions : Collections.emptyList());
    }
    if (getInputViewContainer() != null && getInputViewContainer().getCandidateView() != null) {
      final int sidePadding = showWordSuggestions ? dp(52) : 0;
      getInputViewContainer().getCandidateView().setPadding(sidePadding, 0, sidePadding, 0);
    }
  }

  @Override
  protected boolean useDefaultCancelSuggestionsAction() {
    return BuildConfig.TESTING_BUILD;
  }

  @Override
  protected boolean shouldShowActionsStrip() {
    if (BuildConfig.TESTING_BUILD) return super.shouldShowActionsStrip();
    // A restarted editor can disable predictions without reopening the input view.
    // Baltboard's tools are still useful in password, number and no-suggestions fields.
    return mRewriteView == null && mAdvancedLtClipboardView == null;
  }

  private void hideCurrentWordSuggestions() {
    mSuppressSuggestionsForCurrentWord = true;
    mSawWordBoundaryAfterSuppression = false;
    clearSuggestions();
    if (getInputViewContainer() != null && getInputViewContainer().getCandidateView() != null)
      getInputViewContainer().getCandidateView().clear();
  }

  private static boolean isWordBoundary(int primaryCode) {
    return primaryCode == KeyCodes.SPACE
        || primaryCode == KeyCodes.ENTER
        || (primaryCode > 0 && !Character.isLetterOrDigit(primaryCode));
  }

  private int dp(int value) {
    return Math.round(value * getResources().getDisplayMetrics().density);
  }

  @Override
  public void onUpdateSelection(
      int oldSelStart,
      int oldSelEnd,
      int newSelStart,
      int newSelEnd,
      int candidatesStart,
      int candidatesEnd) {
    if (newSelStart != oldSelStart
        && (newSelStart < candidatesStart || newSelStart > candidatesEnd)) {
      mSuppressSuggestionsForCurrentWord = false;
      mSawWordBoundaryAfterSuppression = false;
    }
    if (DELAY_SELECTION_UPDATES) {
      mDelayer.postDelayed(
          () ->
              SoftKeyboard.super.onUpdateSelection(
                  oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd),
          1025);
    } else {
      super.onUpdateSelection(
          oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd);
    }
  }

  @Override
  protected String getSettingsInputMethodId() {
    return new ComponentName(getApplication(), getClass()).flattenToShortString();
  }

  private void openAdvancedLtSettings() {
    hideWindow();
    startActivity(
        new Intent(this, AdvancedLtSettingsActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
  }

  private boolean showQuickSettings(View anchor) {
    final var preferences = DirectBootAwareSharedPreferences.create(this);
    final String key = getString(R.string.settings_key_baltboard_split_wide_keyboard);
    final var popup = new android.widget.PopupMenu(anchor.getContext(), anchor);
    popup.getMenu().add(R.string.advanced_lt_quick_settings).setEnabled(false);
    popup.getMenu().add(R.string.advanced_lt_quick_split_keyboard)
        .setCheckable(true).setChecked(preferences.getBoolean(key, false));
    popup.setOnMenuItemClickListener(item -> {
      if (!item.isCheckable()) return false;
      final boolean split = !item.isChecked();
      preferences.edit().putBoolean(key, split).apply();
      item.setChecked(split);
      updateKeyboardGeometry();
      return true;
    });
    popup.show();
    return true;
  }

  private String checkText(String lithuanian, String english) {
    return "lt".equals(getResources().getConfiguration().locale.getLanguage()) ? lithuanian : english;
  }

  private void closeRewrite(boolean reshow) {
    mRewriteGeneration++;
    mAiRequestInFlight = false;
    if (mGeminiClient != null) mGeminiClient.cancel();
    if (mRewriteView == null) return;
    KeyboardViewContainerView container = getInputViewContainer();
    if (container != null) {
      container.removeView(mRewriteView);
    }
    mRewriteView = null;
    if (reshow && getInputView() != null) ((View)getInputView()).setVisibility(View.VISIBLE);
    // The strip's visibility is derived from its visible keyboard child. Restore that
    // child first, otherwise the container removes the toolbar until the IME reopens.
    if (container != null && reshow) container.setActionsStripVisibility(true);
    updateBackCallbackState();
  }

  private void startAiCorrection() {
    startAiTextAction(false);
  }

  private void startAiTextAction(boolean translate) {
    if (!getSharedPreferences(AdvancedLtSettingsActivity.PREFS_NAME, MODE_PRIVATE)
        .getBoolean(translate ? AdvancedLtSettingsActivity.PREF_AI_TRANSLATE_ENABLED
            : AdvancedLtSettingsActivity.PREF_AI_ENABLED, !translate)) return;
    EditorInfo editor = getCurrentInputEditorInfo();
    if (isTextPassword(editor) || isNumberPassword(editor) || getSuggest().isIncognitoMode()) {
      showToastMessage(checkText("Šiame lauke AI tikrinimas išjungtas.", "AI checking is disabled in this field."), true);
      return;
    }
    if (mAiRequestInFlight) return;
    if (mRewriteView == null) {
      KeyboardViewContainerView container = getInputViewContainer();
      if (container == null || getInputView() == null) return;
      closeAdvancedLtClipboard(true);
      cleanUpQuickTextKeyboard(false);
      abortCorrectionAndResetPredictionState(false);
      AnyKeyboardView keyboard = (AnyKeyboardView)getInputView();
      keyboard.resetInputView();
      mRewriteView = new lt.klaviatura.advanced.AdvancedLtRewriteView(this, () -> closeRewrite(true), translate);
      container.addView(mRewriteView, new android.view.ViewGroup.LayoutParams(
          android.view.ViewGroup.LayoutParams.MATCH_PARENT, Math.max(container.getHeight(), keyboard.getHeight())));
      keyboard.setVisibility(View.GONE);
      container.setActionsStripVisibility(false);
      updateBackCallbackState();
    }
    final lt.klaviatura.advanced.AdvancedLtRewriteView panel = mRewriteView;
    android.content.SharedPreferences prefs = getSharedPreferences(AdvancedLtSettingsActivity.PREFS_NAME, MODE_PRIVATE);
    final String provider = prefs.getString(AdvancedLtSettingsActivity.PREF_PROVIDER, "gemini");
    final String key = new SecretStore(this, provider).get();
    if (TextUtils.isEmpty(key)) {
      panel.message(checkText("Įveskite pasirinkto tiekėjo API raktą nustatymuose.", "Enter the selected provider's API key in settings."));
      return;
    }
    final TextCapture capture = captureText();
    if (capture == null) {
      panel.message(checkText("Nėra teksto arba programa jo nepateikia. Pasirinkite tekstą (iki 10 000 ženklų).",
          "No text is available, or the app did not provide it. Select the text (up to 10,000 characters)."));
      return;
    }
    final long generation = ++mRewriteGeneration;
    String locale = getCurrentAlphabetKeyboard() == null ? "" : getCurrentAlphabetKeyboard().getDefaultDictionaryLocale();
    mAiRequestInFlight = true;
    panel.loading();
    String instructions = prefs.getString(
        translate ? AdvancedLtSettingsActivity.PREF_TRANSLATE_PROMPT : AdvancedLtSettingsActivity.PREF_PROMPT,
        translate ? GeminiClient.DEFAULT_TRANSLATE_PROMPT : GeminiClient.DEFAULT_PROMPT);
    if (instructions == null || instructions.isBlank()) {
      instructions = translate ? GeminiClient.DEFAULT_TRANSLATE_PROMPT : GeminiClient.DEFAULT_PROMPT;
    }
    if (translate) instructions = "Task: translate the supplied text into English.\n" + instructions;
    mGeminiClient.correct(capture.text, locale == null ? "" : locale, key,
        prefs.getString(AdvancedLtSettingsActivity.modelPreference(provider), "gemini".equals(provider) ? AdvancedLtSettingsActivity.DEFAULT_MODEL : ""),
        instructions,
        provider,
        result -> {
          if (generation != mRewriteGeneration || panel != mRewriteView || capture.sessionId != mInputSessionId) return;
          mAiRequestInFlight = false;
          if (!result.isSuccess()) {
            panel.error((translate ? checkText("Negaliu išversti šio teksto. ", "Could not translate this text. ")
                : checkText("Negaliu pataisyti šio teksto. ", "Could not correct this text. ")) + result.error,
                () -> startAiTextAction(translate));
          } else if (capture.text.equals(result.text)) {
            panel.message(translate ? checkText("Tekstas jau tinkamas anglų kalba.", "The text is already suitable English.")
                : checkText("Klaidų neradau.", "No errors found."));
          } else {
            panel.result(result.text, () -> {
              ClipboardManager clipboard = (ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE);
              clipboard.setPrimaryClip(ClipData.newPlainText("Corrected text", result.text));
              showToastMessage(checkText("Nukopijuota", "Copied"), true);
            }, () -> {
              if (replaceCapturedText(capture, result.text)) closeRewrite(true);
            });
          }
        });
  }

  private TextCapture captureText() {
    final InputConnection connection = getCurrentInputConnection();
    if (connection == null) return null;
    final CharSequence selected = connection.getSelectedText(0);
    if (!TextUtils.isEmpty(selected)) {
      if (selected.length() > 10000 || selected.toString().isBlank()) return null;
      return new TextCapture(selected.toString(), "", "", true, mInputSessionId);
    }
    final CharSequence beforeCursor = connection.getTextBeforeCursor(10001, 0);
    final CharSequence afterCursor = connection.getTextAfterCursor(10001, 0);
    if (beforeCursor == null || afterCursor == null) return null;
    final String before = beforeCursor.toString(), after = afterCursor.toString();
    String text = before + after;
    return text.isBlank() || text.length() > 10000 ? null
        : new TextCapture(text, before, after, false, mInputSessionId);
  }

  private boolean replaceCapturedText(@NonNull TextCapture capture, @NonNull String correctedText) {
    if (capture.sessionId != mInputSessionId) return false;
    final InputConnection connection = getCurrentInputConnection();
    if (connection == null) return false;
    if (capture.selection) {
      final CharSequence selected = connection.getSelectedText(0);
      if (!Objects.equals(capture.text, selected == null ? null : selected.toString())) {
        showToastMessage(checkText("Tekstas pasikeitė. Paleiskite veiksmą dar kartą.", "The text changed. Run the action again."), false);
        return false;
      }
      return connection.commitText(correctedText, 1);
    }

    final CharSequence beforeCursor = connection.getTextBeforeCursor(10001, 0);
    final CharSequence afterCursor = connection.getTextAfterCursor(10001, 0);
    final String before = beforeCursor == null ? "" : beforeCursor.toString();
    final String after = afterCursor == null ? "" : afterCursor.toString();
    final String currentLeft = before;
    final String currentRight = after;
    if (!capture.left.equals(currentLeft) || !capture.right.equals(currentRight)) {
      showToastMessage(checkText("Tekstas pasikeitė. Paleiskite veiksmą dar kartą.", "The text changed. Run the action again."), false);
      return false;
    }
    connection.beginBatchEdit();
    try {
      return connection.deleteSurroundingText(capture.left.length(), capture.right.length())
          && connection.commitText(correctedText, 1);
    } finally { connection.endBatchEdit(); }
  }

  private static final class TextCapture {
    final String text;
    final String left;
    final String right;
    final boolean selection;
    final long sessionId;

    TextCapture(String text, String left, String right, boolean selection, long sessionId) {
      this.text = text;
      this.left = left;
      this.right = right;
      this.selection = selection;
      this.sessionId = sessionId;
    }
  }
}
