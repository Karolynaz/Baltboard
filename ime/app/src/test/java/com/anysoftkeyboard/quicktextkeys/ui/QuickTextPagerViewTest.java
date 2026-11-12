package com.anysoftkeyboard.quicktextkeys.ui;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import com.anysoftkeyboard.AnySoftKeyboardRobolectricTestRunner;
import com.anysoftkeyboard.keyboards.views.OnKeyboardActionListener;
import com.anysoftkeyboard.quicktextkeys.QuickKeyHistoryRecords;
import com.anysoftkeyboard.quicktextkeys.QuickTextKeyFactory;
import com.anysoftkeyboard.remote.MediaType;
import com.anysoftkeyboard.theme.KeyboardTheme;
import com.menny.android.anysoftkeyboard.AnyApplication;
import com.menny.android.anysoftkeyboard.R;
import java.util.Collections;
import net.evendanan.pixel.ViewPagerWithDisable;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowView;

@RunWith(AnySoftKeyboardRobolectricTestRunner.class)
public class QuickTextPagerViewTest {

  private QuickTextPagerView mUnderTest;
  private KeyboardTheme mKeyboardTheme;

  @Before
  public void setup() {
    Context context = getApplicationContext();
    mKeyboardTheme = AnyApplication.getKeyboardThemeFactory(context).getEnabledAddOn();
    mUnderTest =
        (QuickTextPagerView)
            LayoutInflater.from(getApplicationContext())
                .inflate(R.layout.quick_text_popup_root_view, null, false);
    mUnderTest.setQuickKeyHistoryRecords(
        new QuickKeyHistoryRecords(AnyApplication.prefs(getApplicationContext())));
    mUnderTest.setEmojiVariantsPrefTrackers(
        Mockito.mock(DefaultSkinTonePrefTracker.class),
        Mockito.mock(DefaultGenderPrefTracker.class));
    mUnderTest.setThemeValues(
        mKeyboardTheme,
        10f,
        new ColorStateList(new int[][] {{0}}, new int[] {Color.WHITE}),
        context.getDrawable(R.drawable.ic_cancel),
        context.getDrawable(R.drawable.sym_keyboard_delete_light),
        context.getDrawable(R.drawable.ic_action_settings),
        context.getDrawable(R.drawable.dark_background),
        context.getDrawable(R.drawable.ic_media_insertion),
        context.getDrawable(R.drawable.ic_delete_forever_dark),
        10,
        Collections.singleton(MediaType.Image));
  }

  @Test
  public void emojiFadeReservesSystemBarAndDoesNotInterceptTouches() {
    View fade = mUnderTest.findViewById(R.id.quick_text_bottom_fade);
    int expected = 10 + Math.round(48 * getApplicationContext().getResources().getDisplayMetrics().density);
    Assert.assertEquals(expected, fade.getLayoutParams().height);
    Assert.assertFalse(fade.isClickable());
    Assert.assertFalse(fade.isFocusable());
  }

  @Test
  public void testShowsCategoryBar() throws Exception {
    OnKeyboardActionListener listener = Mockito.mock(OnKeyboardActionListener.class);
    mUnderTest.setOnKeyboardActionListener(listener);
    Assert.assertNotNull(mUnderTest.findViewById(R.id.quick_text_category_bar));
    Assert.assertNotNull(mUnderTest.findViewById(R.id.quick_text_category_scroller));
  }

  @Test
  public void testStartsOnRecentsAndCreatesScrollableCategoryIcons() throws Exception {
    OnKeyboardActionListener listener = Mockito.mock(OnKeyboardActionListener.class);
    mUnderTest.setOnKeyboardActionListener(listener);

    ViewPagerWithDisable pager = mUnderTest.findViewById(R.id.quick_text_keyboards_pager);
    Assert.assertNotNull(pager);
    Assert.assertEquals(0, pager.getCurrentItem());
    android.widget.LinearLayout tabs = mUnderTest.findViewById(R.id.quick_text_category_tabs);
    Assert.assertEquals(15, tabs.getChildCount());
    tabs.getChildAt(1).performClick();
    Assert.assertEquals(1, pager.getCurrentItem());
  }

  @Test
  public void testSetOnKeyboardActionListener() throws Exception {
    OnKeyboardActionListener listener = Mockito.mock(OnKeyboardActionListener.class);

    ShadowView shadowView =
        Shadows.shadowOf((View) mUnderTest.findViewById(R.id.quick_keys_popup_close));
    Assert.assertNull(shadowView.getOnClickListener());
    ViewPagerWithDisable pager = mUnderTest.findViewById(R.id.quick_text_keyboards_pager);
    Assert.assertNull(pager.getAdapter());

    mUnderTest.setOnKeyboardActionListener(listener);

    Assert.assertNotNull(shadowView.getOnClickListener());
    Assert.assertNotNull(pager.getAdapter());
  }

  @Test
  public void testPassesOnlyEnabledAddOns() throws Exception {
    final QuickTextKeyFactory quickTextKeyFactory =
        AnyApplication.getQuickTextKeyFactory(getApplicationContext());

    Assert.assertEquals(18, quickTextKeyFactory.getAllAddOns().size());
    Assert.assertEquals(18, quickTextKeyFactory.getEnabledAddOns().size());
    quickTextKeyFactory.setAddOnEnabled(quickTextKeyFactory.getAllAddOns().get(0).getId(), false);
    Assert.assertEquals(18, quickTextKeyFactory.getAllAddOns().size());
    Assert.assertEquals(17, quickTextKeyFactory.getEnabledAddOns().size());

    OnKeyboardActionListener listener = Mockito.mock(OnKeyboardActionListener.class);

    mUnderTest.setOnKeyboardActionListener(listener);
    ViewPagerWithDisable pager = mUnderTest.findViewById(R.id.quick_text_keyboards_pager);

    Assert.assertEquals(13 + 1 /* history */, pager.getAdapter().getCount());

    quickTextKeyFactory.setAddOnEnabled(quickTextKeyFactory.getAllAddOns().get(1).getId(), false);

    mUnderTest.setOnKeyboardActionListener(listener);
    pager = mUnderTest.findViewById(R.id.quick_text_keyboards_pager);

    Assert.assertEquals(12 + 1 /* history */, pager.getAdapter().getCount());
  }

  @Test
  public void testSetThemeValues() throws Exception {
    mUnderTest.setOnKeyboardActionListener(Mockito.mock(OnKeyboardActionListener.class));

    Assert.assertEquals(
        R.drawable.advanced_lt_ic_keyboard,
        Shadows.shadowOf(
                ((ImageView) mUnderTest.findViewById(R.id.quick_keys_popup_close)).getDrawable())
            .getCreatedFromResId());
    Assert.assertEquals(
        lt.klaviatura.advanced.AdvancedLtTheme.surface(mUnderTest.getContext()),
        ((android.graphics.drawable.ColorDrawable)mUnderTest.getBackground()).getColor());
  }

  @Test
  public void testSetThemeOverlay_forwardsOverlayToPagerAdapter() throws Exception {
    mUnderTest.setOnKeyboardActionListener(Mockito.mock(OnKeyboardActionListener.class));
    com.anysoftkeyboard.overlay.OverlayData overlayData =
        new com.anysoftkeyboard.overlay.OverlayDataImpl(
            Color.WHITE, Color.BLACK, Color.BLUE, Color.BLACK, Color.GRAY);
    mUnderTest.setThemeOverlay(overlayData);
    ViewPagerWithDisable pager = mUnderTest.findViewById(R.id.quick_text_keyboards_pager);
    Assert.assertNotNull(pager.getAdapter());
  }

  @Test
  public void testSetThemeOverlay_normalizesAndKeepsCategoryIcons() throws Exception {
    mUnderTest.setOnKeyboardActionListener(Mockito.mock(OnKeyboardActionListener.class));
    com.anysoftkeyboard.overlay.OverlayData overlayData =
        new com.anysoftkeyboard.overlay.OverlayDataImpl(
            Color.WHITE, Color.BLACK, Color.BLUE, Color.BLACK, Color.BLACK);
    mUnderTest.setThemeOverlay(overlayData);

    android.widget.LinearLayout tabs = mUnderTest.findViewById(R.id.quick_text_category_tabs);
    Assert.assertEquals(15, tabs.getChildCount());
  }

  @Test
  public void testSetThemeOverlay_invalidOverlayData_doesNotApplyNormalizedColors()
      throws Exception {
    mUnderTest.setOnKeyboardActionListener(Mockito.mock(OnKeyboardActionListener.class));
    com.anysoftkeyboard.overlay.OverlayData invalidOverlay =
        new com.anysoftkeyboard.overlay.OverlayDataImpl();
    mUnderTest.setThemeOverlay(invalidOverlay);
    Assert.assertEquals(
        15,
        ((android.widget.LinearLayout)
                mUnderTest.findViewById(R.id.quick_text_category_tabs))
            .getChildCount());
  }

  @Test
  public void
      testSetThemeOverlay_lightPrimaryColor_darkPrimaryDarkColor_normalizesAgainstPrimaryDarkColor()
          throws Exception {
    mUnderTest.setOnKeyboardActionListener(Mockito.mock(OnKeyboardActionListener.class));
    // primaryColor is WHITE (light), primaryDarkColor is BLACK (dark), secondaryTextColor is BLACK
    com.anysoftkeyboard.overlay.OverlayData overlayData =
        new com.anysoftkeyboard.overlay.OverlayDataImpl(
            Color.WHITE, Color.BLACK, Color.BLUE, Color.BLACK, Color.BLACK);
    mUnderTest.setThemeOverlay(overlayData);

    Assert.assertEquals(
        15,
        ((android.widget.LinearLayout)
                mUnderTest.findViewById(R.id.quick_text_category_tabs))
            .getChildCount());
  }
}
