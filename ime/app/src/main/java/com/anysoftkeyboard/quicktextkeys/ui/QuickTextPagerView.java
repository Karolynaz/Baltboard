package com.anysoftkeyboard.quicktextkeys.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;
import com.anysoftkeyboard.ime.InputViewActionsProvider;
import com.anysoftkeyboard.keyboards.views.OnKeyboardActionListener;
import com.anysoftkeyboard.keyboards.views.ThemeableChild;
import com.anysoftkeyboard.overlay.OverlayData;
import com.anysoftkeyboard.overlay.OverlayDataImpl;
import com.anysoftkeyboard.quicktextkeys.HistoryQuickTextKey;
import com.anysoftkeyboard.quicktextkeys.QuickKeyHistoryRecords;
import com.anysoftkeyboard.quicktextkeys.QuickTextKey;
import com.anysoftkeyboard.remote.MediaType;
import com.anysoftkeyboard.theme.KeyboardTheme;
import com.menny.android.anysoftkeyboard.AnyApplication;
import com.menny.android.anysoftkeyboard.R;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.evendanan.pixel.ViewPagerWithDisable;

public class QuickTextPagerView extends LinearLayout
    implements InputViewActionsProvider, ThemeableChild {

  private KeyboardTheme mKeyboardTheme;
  @NonNull private OverlayData mOverlayData = new OverlayDataImpl();
  private QuickKeysKeyboardPagerAdapter mPagerAdapter;
  private float mTabTitleTextSize;
  private ColorStateList mTabTitleTextColor;
  private Drawable mCloseKeyboardIcon;
  private Drawable mBackspaceIcon;
  private Drawable mSettingsIcon;
  private Drawable mMediaInsertionDrawable;
  private Drawable mDeleteRecentlyUsedDrawable;
  private int mBottomPadding;
  private QuickKeyHistoryRecords mQuickKeyHistoryRecords;
  private DefaultSkinTonePrefTracker mDefaultSkinTonePrefTracker;
  private DefaultGenderPrefTracker mDefaultGenderPrefTracker;
  private lt.klaviatura.advanced.AdvancedLtBackspaceButton mFloatingBackspace;

  public QuickTextPagerView(Context context) {
    super(context);
  }

  public QuickTextPagerView(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public QuickTextPagerView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
  }

  private void setupCategoryTabs(
      View rootView,
      ColorStateList tabTitleTextColor,
      ViewPager pager,
      PagerAdapter adapter,
      List<QuickTextKey> categories,
      ViewPager.OnPageChangeListener onPageChangeListener,
      int startIndex) {
    pager.setAdapter(adapter);
    pager.setCurrentItem(startIndex);
    pager.addOnPageChangeListener(onPageChangeListener);

    LinearLayout tabs = rootView.findViewById(R.id.quick_text_category_tabs);
    tabs.removeAllViews();
    for (int position = 0; position < adapter.getCount(); position++) {
      final int tabPosition = position;
      ImageButton tab = new ImageButton(getContext());
      tab.setImageResource(categoryIconFor(categories.get(position)));
      tab.setContentDescription(adapter.getPageTitle(position));
      tab.setBackgroundColor(android.graphics.Color.TRANSPARENT);
      tab.setPadding(dp(13), dp(13), dp(13), dp(13));
      tab.setScaleType(ImageView.ScaleType.FIT_CENTER);
      tab.setOnClickListener(ignored -> pager.setCurrentItem(tabPosition, true));
      tabs.addView(tab, new LinearLayout.LayoutParams(dp(52), dp(52)));
    }
    updateSelectedCategory(startIndex, tabTitleTextColor.getDefaultColor());
  }

  private int categoryIconFor(QuickTextKey category) {
    if (category instanceof HistoryQuickTextKey) {
      return R.drawable.advanced_lt_ic_emoji_recents;
    }
    final int popup = category.getPopupKeyboardResId();
    if (popup == R.xml.quick_text_unicode_emoticons) {
      return R.drawable.advanced_lt_ic_emoji_all;
    } else if (popup == R.xml.quick_text_unicode_people) {
      return R.drawable.advanced_lt_ic_emoji_people;
    } else if (popup == R.xml.quick_text_unicode_gestures) {
      return R.drawable.advanced_lt_ic_emoji_gestures;
    } else if (popup == R.xml.quick_text_unicode_accessories) {
      return R.drawable.advanced_lt_ic_emoji_accessories;
    } else if (popup == R.xml.quick_text_unicode_animals
        || popup == R.xml.quick_text_unicode_nature) {
      return R.drawable.advanced_lt_ic_emoji_animals;
    } else if (popup == R.xml.quick_text_unicode_food) {
      return R.drawable.advanced_lt_ic_emoji_food;
    } else if (popup == R.xml.quick_text_unicode_activity) {
      return R.drawable.advanced_lt_ic_emoji_activity;
    } else if (popup == R.xml.quick_text_unicode_office) {
      return R.drawable.advanced_lt_ic_emoji_objects;
    } else if (popup == R.xml.quick_text_unicode_signs
        || popup == R.xml.quick_text_unicode_occasions) {
      return R.drawable.advanced_lt_ic_emoji_symbols;
    } else if (popup == R.xml.quick_text_unicode_flags) {
      return R.drawable.advanced_lt_ic_emoji_flags;
    } else {
      return R.drawable.advanced_lt_ic_emoji_home;
    }
  }

  private void updateSelectedCategory(int selectedPosition, int normalColor) {
    LinearLayout tabs = findViewById(R.id.quick_text_category_tabs);
    final int selectedColor =
        lt.klaviatura.advanced.AdvancedLtTheme.accent(getContext());
    for (int position = 0; position < tabs.getChildCount(); position++) {
      ImageView tab = (ImageView) tabs.getChildAt(position);
      tab.setColorFilter(
          position == selectedPosition ? selectedColor : normalColor, PorterDuff.Mode.SRC_IN);
    }
    View selected = tabs.getChildAt(selectedPosition);
    HorizontalScrollView scroller = findViewById(R.id.quick_text_category_scroller);
    if (selected != null) {
      scroller.post(() -> scroller.smoothScrollTo(selected.getLeft() - dp(52), 0));
    }
  }

  public void setThemeValues(
      @NonNull KeyboardTheme keyboardTheme,
      float tabTextSize,
      ColorStateList tabTextColor,
      Drawable closeKeyboardIcon,
      Drawable backspaceIcon,
      Drawable settingsIcon,
      Drawable keyboardDrawable,
      Drawable mediaInsertionDrawable,
      Drawable deleteRecentlyUsedDrawable,
      int bottomPadding,
      Set<MediaType> supportedMediaTypes) {
    mKeyboardTheme = keyboardTheme;
    mTabTitleTextSize = tabTextSize;
    mTabTitleTextColor = tabTextColor;
    mCloseKeyboardIcon = closeKeyboardIcon;
    mBackspaceIcon = backspaceIcon;
    mSettingsIcon = settingsIcon;
    mMediaInsertionDrawable = mediaInsertionDrawable;
    mDeleteRecentlyUsedDrawable = deleteRecentlyUsedDrawable;
    mBottomPadding = Math.max(0, bottomPadding) + dp(48);
    if (mFloatingBackspace != null) {
      android.widget.FrameLayout.LayoutParams buttonParams =
          (android.widget.FrameLayout.LayoutParams) mFloatingBackspace.getLayoutParams();
      buttonParams.bottomMargin = Math.max(0, bottomPadding) + dp(32);
      mFloatingBackspace.setLayoutParams(buttonParams);
    }
    View fade = findViewById(R.id.quick_text_bottom_fade);
    android.view.ViewGroup.LayoutParams fadeParams = fade.getLayoutParams();
    fadeParams.height = mBottomPadding;
    fade.setLayoutParams(fadeParams);
    setBackground(keyboardDrawable);
    refreshPanelTheme();
  }

  private void refreshPanelTheme() {
    setBackgroundColor(lt.klaviatura.advanced.AdvancedLtTheme.surface(getContext()));
    findViewById(R.id.quick_text_bottom_fade).setBackground(lt.klaviatura.advanced.AdvancedLtTheme.fade(getContext()));
    mTabTitleTextColor = ColorStateList.valueOf(lt.klaviatura.advanced.AdvancedLtTheme.text(getContext()));
    ((ImageView)findViewById(R.id.quick_keys_popup_close)).setImageTintList(mTabTitleTextColor);
    findViewById(R.id.quick_text_category_separator).setBackgroundColor(
        lt.klaviatura.advanced.AdvancedLtTheme.isDay(getContext()) ? 0x40333333 : 0x40ffffff);
    ViewPager pager = findViewById(R.id.quick_text_keyboards_pager);
    updateSelectedCategory(pager.getCurrentItem(), mTabTitleTextColor.getDefaultColor());
    if (mFloatingBackspace != null) mFloatingBackspace.refreshTheme();
  }

  @Override
  public void setOnKeyboardActionListener(OnKeyboardActionListener keyboardActionListener) {
    FrameKeyboardViewClickListener frameKeyboardViewClickListener =
        new FrameKeyboardViewClickListener(keyboardActionListener);
    frameKeyboardViewClickListener.registerOnViews(this);
    if (mFloatingBackspace == null) {
      mFloatingBackspace = new lt.klaviatura.advanced.AdvancedLtBackspaceButton(getContext());
      android.widget.FrameLayout panel = (android.widget.FrameLayout)findViewById(R.id.quick_text_keyboards_pager).getParent();
      android.widget.FrameLayout.LayoutParams params = new android.widget.FrameLayout.LayoutParams(dp(56), dp(56),
          android.view.Gravity.BOTTOM | android.view.Gravity.END);
      params.setMarginEnd(dp(16));
      params.bottomMargin = Math.max(0, mBottomPadding - dp(48)) + dp(32);
      panel.addView(mFloatingBackspace, params);
    }
    mFloatingBackspace.bind(keyboardActionListener);

    final Context context = getContext();
    final List<QuickTextKey> list = new ArrayList<>();
    // always starting with Recent
    final HistoryQuickTextKey historyQuickTextKey =
        new HistoryQuickTextKey(context, mQuickKeyHistoryRecords);
    list.add(historyQuickTextKey);
    // Then the Unicode emoji categories. Legacy text smileys and kaomoji do not belong here.
    for (QuickTextKey key : AnyApplication.getQuickTextKeyFactory(context).getEnabledAddOns()) {
      if (isEmojiCategory(key)) list.add(key);
    }

    final QuickTextUserPrefs quickTextUserPrefs = new QuickTextUserPrefs(context);

    final ViewPagerWithDisable pager = findViewById(R.id.quick_text_keyboards_pager);
    mPagerAdapter =
        new QuickKeysKeyboardPagerAdapter(
            context,
            pager,
            list,
            new RecordHistoryKeyboardActionListener(
                historyQuickTextKey,
                keyboardActionListener,
                quickTextUserPrefs.isOneShotQuickTextPopup()),
            mDefaultSkinTonePrefTracker,
            mDefaultGenderPrefTracker,
            mKeyboardTheme,
            mBottomPadding);
    if (mOverlayData.isValid()) {
      mPagerAdapter.setThemeOverlay(mOverlayData);
    }

    ViewPager.SimpleOnPageChangeListener onPageChangeListener =
        new ViewPager.SimpleOnPageChangeListener() {
          @Override
          public void onPageSelected(int position) {
            super.onPageSelected(position);
            QuickTextKey selectedKey = list.get(position);
            quickTextUserPrefs.setLastSelectedAddOnId(selectedKey.getId());
            updateSelectedCategory(position, mTabTitleTextColor.getDefaultColor());
          }
        };
    int startPageIndex = 0;
    setupCategoryTabs(
        this,
        mTabTitleTextColor,
        pager,
        mPagerAdapter,
        list,
        onPageChangeListener,
        startPageIndex);

    // setting up icons from theme
    ((ImageView) findViewById(R.id.quick_keys_popup_close)).setImageResource(R.drawable.advanced_lt_ic_keyboard);
    refreshPanelTheme();
    applyThemeOverlayToUi();
  }

  @Override
  public void setKeyboardTheme(@NonNull KeyboardTheme theme) {
    mKeyboardTheme = theme;
    if (mPagerAdapter != null) {
      mPagerAdapter.setKeyboardTheme(theme);
    }
    refreshPanelTheme();
  }

  @Override
  public void setThemeOverlay(OverlayData overlay) {
    mOverlayData = overlay;
    if (mPagerAdapter != null) {
      mPagerAdapter.setThemeOverlay(overlay);
    }
    applyThemeOverlayToUi();
  }

  private void applyThemeOverlayToUi() {
    if (mOverlayData.isValid()) {
      OverlayData normalizedOverlay = getNormalizedOverlayData(mOverlayData);
      int iconColor = normalizedOverlay.getPrimaryTextColor();
      applyIconColorFilter(R.id.quick_keys_popup_close, iconColor);
      ViewPager pager = findViewById(R.id.quick_text_keyboards_pager);
      updateSelectedCategory(pager.getCurrentItem(), iconColor);
    }
  }

  private static OverlayData getNormalizedOverlayData(OverlayData overlay) {
    if (overlay.getPrimaryDarkColor() != android.graphics.Color.TRANSPARENT
        || overlay.getSecondaryTextColor() != android.graphics.Color.TRANSPARENT) {
      return com.anysoftkeyboard.overlay.OverlayDataNormalizer.normalize(
          overlay, 96, overlay.getPrimaryDarkColor(), overlay.getSecondaryTextColor());
    } else {
      return overlay;
    }
  }

  private void applyIconColorFilter(int viewId, int color) {
    ImageView imageView = findViewById(viewId);
    if (imageView != null && imageView.getDrawable() != null) {
      imageView.getDrawable().setColorFilter(color, PorterDuff.Mode.SRC_IN);
    }
  }

  private boolean isEmojiCategory(QuickTextKey key) {
    final int popup = key.getPopupKeyboardResId();
    return popup == R.xml.quick_text_unicode_emoticons
        || popup == R.xml.quick_text_unicode_people
        || popup == R.xml.quick_text_unicode_gestures
        || popup == R.xml.quick_text_unicode_accessories
        || popup == R.xml.quick_text_unicode_animals
        || popup == R.xml.quick_text_unicode_nature
        || popup == R.xml.quick_text_unicode_food
        || popup == R.xml.quick_text_unicode_transport
        || popup == R.xml.quick_text_unicode_signs
        || popup == R.xml.quick_text_unicode_city
        || popup == R.xml.quick_text_unicode_activity
        || popup == R.xml.quick_text_unicode_office
        || popup == R.xml.quick_text_unicode_occasions
        || popup == R.xml.quick_text_unicode_flags;
  }

  private int dp(int value) {
    return Math.round(value * getResources().getDisplayMetrics().density);
  }

  public void setQuickKeyHistoryRecords(QuickKeyHistoryRecords quickKeyHistoryRecords) {
    mQuickKeyHistoryRecords = quickKeyHistoryRecords;
  }

  public void setEmojiVariantsPrefTrackers(
      DefaultSkinTonePrefTracker defaultSkinTonePrefTracker,
      DefaultGenderPrefTracker defaultGenderPrefTracker) {
    mDefaultSkinTonePrefTracker = defaultSkinTonePrefTracker;
    mDefaultGenderPrefTracker = defaultGenderPrefTracker;
  }
}
