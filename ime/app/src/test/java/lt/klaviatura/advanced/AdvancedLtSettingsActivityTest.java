package lt.klaviatura.advanced;

import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import androidx.appcompat.widget.SwitchCompat;
import com.anysoftkeyboard.AnySoftKeyboardRobolectricTestRunner;
import com.menny.android.anysoftkeyboard.AnyApplication;
import com.menny.android.anysoftkeyboard.R;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;

@RunWith(AnySoftKeyboardRobolectricTestRunner.class)
public class AdvancedLtSettingsActivityTest {

  @Test
  public void widerKeySpacingIsEnabledByDefaultAndPersistsWhenDisabled() {
    try (ActivityController<AdvancedLtSettingsActivity> controller =
        Robolectric.buildActivity(AdvancedLtSettingsActivity.class).setup()) {
      final AdvancedLtSettingsActivity activity = controller.get();
      activity.findViewById(R.id.advanced_lt_settings_appearance).performClick();
      SwitchCompat spacing = activity.findViewById(R.id.advanced_lt_wider_key_spacing);
      Assert.assertTrue(spacing.isChecked());
      spacing.setChecked(false);
      activity.getOnBackPressedDispatcher().onBackPressed();
      activity.findViewById(R.id.advanced_lt_settings_appearance).performClick();
      spacing = activity.findViewById(R.id.advanced_lt_wider_key_spacing);
      Assert.assertFalse(spacing.isChecked());
    }
  }

  @Test
  public void powerSavingSwitchPersistsAndRestoresBothStates() {
    try (ActivityController<AdvancedLtSettingsActivity> controller =
        Robolectric.buildActivity(AdvancedLtSettingsActivity.class).setup()) {
      AdvancedLtSettingsActivity activity = controller.get();
      activity.findViewById(R.id.advanced_lt_settings_effects).performClick();
      SwitchCompat toggle = activity.findViewById(R.id.advanced_lt_power_saving);
      android.content.SharedPreferences prefs = com.anysoftkeyboard.prefs.DirectBootAwareSharedPreferences.create(activity);
      String key = activity.getString(R.string.settings_key_power_save_mode);
      toggle.setChecked(false);
      Assert.assertEquals("never", prefs.getString(key, ""));
      activity.getOnBackPressedDispatcher().onBackPressed();
      activity.findViewById(R.id.advanced_lt_settings_effects).performClick();
      toggle = activity.findViewById(R.id.advanced_lt_power_saving);
      Assert.assertFalse(toggle.isChecked());
      toggle.setChecked(true);
      Assert.assertEquals("on_low_battery", prefs.getString(key, ""));
    }
  }

  @Test
  public void systemBackReturnsFromSectionThenExitsSettings() {
    try (ActivityController<AdvancedLtSettingsActivity> controller =
        Robolectric.buildActivity(AdvancedLtSettingsActivity.class).setup()) {
      final AdvancedLtSettingsActivity activity = controller.get();
      activity.findViewById(R.id.advanced_lt_settings_correction).performClick();
      activity.getOnBackPressedDispatcher().onBackPressed();
      Assert.assertNotNull(activity.findViewById(R.id.advanced_lt_settings_main));
      Assert.assertFalse(activity.isFinishing());
      activity.getOnBackPressedDispatcher().onBackPressed();
      Assert.assertTrue(activity.isFinishing());
    }
  }

  @Test
  public void mainScreenLinksToBrandedSettingsSections() {
    try (ActivityController<AdvancedLtSettingsActivity> controller =
        Robolectric.buildActivity(AdvancedLtSettingsActivity.class).setup()) {
      final AdvancedLtSettingsActivity activity = controller.get();

      Assert.assertNotNull(activity.findViewById(R.id.advanced_lt_settings_main));
      Assert.assertNotNull(activity.findViewById(R.id.advanced_lt_settings_themes));
      Assert.assertNotNull(activity.findViewById(R.id.advanced_lt_settings_emoji));
      Assert.assertNotNull(activity.findViewById(R.id.advanced_lt_settings_ai));
      Assert.assertNotNull(activity.findViewById(R.id.advanced_lt_settings_about));
      Assert.assertNotNull(activity.findViewById(R.id.advanced_lt_settings_appearance));
      Assert.assertNotNull(activity.findViewById(R.id.advanced_lt_settings_correction));
      Assert.assertNotNull(activity.findViewById(R.id.advanced_lt_settings_typing));
      Assert.assertNotNull(activity.findViewById(R.id.advanced_lt_settings_gestures));
      Assert.assertNotNull(activity.findViewById(R.id.advanced_lt_settings_effects));
      Assert.assertNotNull(activity.findViewById(R.id.advanced_lt_settings_advanced));
      Assert.assertNotNull(activity.findViewById(R.id.advanced_lt_settings_setup));

      final LinearLayout themesRow =
          (LinearLayout) activity.findViewById(R.id.advanced_lt_settings_themes);
      Assert.assertEquals(2, themesRow.getChildCount());
      Assert.assertTrue(themesRow.getChildAt(0) instanceof LinearLayout);
    }
  }

  @Test
  public void correctionScreenIsBrandedAndDoesNotOpenLegacySettings() {
    try (ActivityController<AdvancedLtSettingsActivity> controller =
        Robolectric.buildActivity(AdvancedLtSettingsActivity.class).setup()) {
      final AdvancedLtSettingsActivity activity = controller.get();

      activity.findViewById(R.id.advanced_lt_settings_correction).performClick();

      Assert.assertNull(
          org.robolectric.Shadows.shadowOf(activity).getNextStartedActivity());
      Assert.assertTrue(countSwitches(activity.findViewById(android.R.id.content)) >= 6);
    }
  }

  @Test
  @Config(qualifiers = "lt")
  public void themeScreenOffersOriginalAndSteveThemesWithOptionalGlass() {
    try (ActivityController<AdvancedLtSettingsActivity> controller =
        Robolectric.buildActivity(AdvancedLtSettingsActivity.class).setup()) {
      final AdvancedLtSettingsActivity activity = controller.get();
      activity.findViewById(R.id.advanced_lt_settings_themes).performClick();

      final RadioButton black = activity.findViewById(R.id.advanced_lt_theme_black);
      final RadioButton gray = activity.findViewById(R.id.advanced_lt_theme_gray);
      final RadioButton white = activity.findViewById(R.id.advanced_lt_theme_white);
      Assert.assertNull(black);
      Assert.assertEquals(5, ((android.widget.RadioGroup) gray.getParent()).getChildCount());
      Assert.assertEquals("Night", gray.getText().toString());
      Assert.assertEquals("Day", white.getText().toString());
      Assert.assertEquals("Auto", ((RadioButton) activity.findViewById(R.id.advanced_lt_theme_auto)).getText().toString());

      activity.findViewById(R.id.advanced_lt_theme_steve_light).performClick();
      Assert.assertEquals(
          AdvancedLtTheme.STEVE_LIGHT,
          AnyApplication.getKeyboardThemeFactory(activity).getEnabledAddOn().getId());
      final SwitchCompat glass = activity.findViewById(R.id.advanced_lt_glass);
      Assert.assertFalse(glass.isChecked());
      glass.setChecked(true);
      activity.getOnBackPressedDispatcher().onBackPressed();
      activity.findViewById(R.id.advanced_lt_settings_themes).performClick();
      Assert.assertTrue(((RadioButton) activity.findViewById(R.id.advanced_lt_theme_steve_light)).isChecked());
      Assert.assertTrue(((SwitchCompat) activity.findViewById(R.id.advanced_lt_glass)).isChecked());
    }
  }

  @Test
  @Config(qualifiers = "en")
  public void settingsUseEnglishWhenAndroidLanguageIsEnglish() {
    try (ActivityController<AdvancedLtSettingsActivity> controller =
        Robolectric.buildActivity(AdvancedLtSettingsActivity.class).setup()) {
      final AdvancedLtSettingsActivity activity = controller.get();
      activity.findViewById(R.id.advanced_lt_settings_themes).performClick();

      final RadioButton black = activity.findViewById(R.id.advanced_lt_theme_black);
      final RadioButton gray = activity.findViewById(R.id.advanced_lt_theme_gray);
      final RadioButton white = activity.findViewById(R.id.advanced_lt_theme_white);
      Assert.assertNull(black);
      Assert.assertEquals("Night", gray.getText().toString());
      Assert.assertEquals("Day", white.getText().toString());
    }
  }

  @Test
  public void emojiScreenUsesCheckboxListWithoutPreviewImages() {
    try (ActivityController<AdvancedLtSettingsActivity> controller =
        Robolectric.buildActivity(AdvancedLtSettingsActivity.class).setup()) {
      final AdvancedLtSettingsActivity activity = controller.get();
      activity.findViewById(R.id.advanced_lt_settings_emoji).performClick();

      final LinearLayout categories =
          activity.findViewById(R.id.advanced_lt_emoji_categories);
      int checkboxCount = 0;
      for (int index = 0; index < categories.getChildCount(); index++) {
        final View child = categories.getChildAt(index);
        if (child instanceof CheckBox) checkboxCount++;
      }
      Assert.assertEquals(
          AnyApplication.getQuickTextKeyFactory(activity).getAllAddOns().size(), checkboxCount);
    }
  }

  @Test
  @Config(sdk = 32)
  public void everySectionAppliesSafeInsetsToItsViewport() {
    int[] sections = {
      R.id.advanced_lt_settings_themes, R.id.advanced_lt_settings_emoji,
      R.id.advanced_lt_settings_ai, R.id.advanced_lt_settings_about,
      R.id.advanced_lt_settings_appearance, R.id.advanced_lt_settings_correction,
      R.id.advanced_lt_settings_typing, R.id.advanced_lt_settings_gestures,
      R.id.advanced_lt_settings_effects, R.id.advanced_lt_settings_advanced,
      R.id.advanced_lt_settings_setup
    };
    for (int section : sections) {
      try (ActivityController<AdvancedLtSettingsActivity> controller =
          Robolectric.buildActivity(AdvancedLtSettingsActivity.class).setup()) {
        final AdvancedLtSettingsActivity activity = controller.get();
        activity.findViewById(section).performClick();
        View root = activity.findViewById(R.id.advanced_lt_settings_root);
        android.widget.ScrollView viewport = activity.findViewById(R.id.advanced_lt_settings_detail);
        androidx.core.view.WindowInsetsCompat insets =
            new androidx.core.view.WindowInsetsCompat.Builder()
                .setInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars(),
                    androidx.core.graphics.Insets.of(0, 32, 0, 24))
                .setInsets(androidx.core.view.WindowInsetsCompat.Type.displayCutout(),
                    androidx.core.graphics.Insets.of(0, 48, 0, 0))
                .build();
        androidx.core.view.ViewCompat.dispatchApplyWindowInsets(root, insets);
        androidx.core.view.ViewCompat.dispatchApplyWindowInsets(root, insets);
        Assert.assertEquals(48, root.getPaddingTop());
        Assert.assertEquals(24, root.getPaddingBottom());
        Assert.assertTrue(viewport.getClipToPadding());
        android.widget.LinearLayout content =
            (android.widget.LinearLayout) viewport.getChildAt(0);
        android.widget.LinearLayout header =
            (android.widget.LinearLayout) content.getChildAt(0);
        android.widget.TextView title = (android.widget.TextView) header.getChildAt(1);
        Assert.assertEquals(android.view.Gravity.CENTER_VERTICAL,
            title.getGravity() & android.view.Gravity.VERTICAL_GRAVITY_MASK);
        Assert.assertEquals(header.getChildAt(0).getLayoutParams().height,
            title.getMinimumHeight());
      }
    }
  }

  @Test
  @Config(qualifiers = "w870dp-h657dp-land")
  public void wideSettingsKeepsNavigationBesideDetailAndAdaptsToNarrowWidth() {
    try (ActivityController<AdvancedLtSettingsActivity> controller =
        Robolectric.buildActivity(AdvancedLtSettingsActivity.class).setup()) {
      final AdvancedLtSettingsActivity activity = controller.get();
      final View root = activity.findViewById(R.id.advanced_lt_settings_root);
      final View navigation = activity.findViewById(R.id.advanced_lt_settings_main);
      final View appearance = activity.findViewById(R.id.advanced_lt_settings_appearance);
      appearance.performClick();
      final View detail = activity.findViewById(R.id.advanced_lt_settings_detail);
      final float density = activity.getResources().getDisplayMetrics().density;
      final int wideWidth = Math.round(870 * density);
      measureSettings(root, wideWidth);

      Assert.assertEquals(View.VISIBLE, navigation.getVisibility());
      Assert.assertTrue(appearance.isActivated());
      Assert.assertTrue(detail.getLeft() > navigation.getRight());
      Assert.assertTrue(detail.getWidth() > navigation.getWidth());
      Assert.assertTrue(((View) detail.getParent()).getLeft() >= Math.round(24 * density));
      Assert.assertTrue(((LinearLayout) appearance).getChildAt(0).getLeft() >= Math.round(20 * density));
      final SwitchCompat spacing = activity.findViewById(R.id.advanced_lt_wider_key_spacing);
      final LinearLayout spacingRow = (LinearLayout) spacing.getParent();
      Assert.assertTrue(spacingRow.getChildAt(0).getLeft() >= Math.round(20 * density));
      Assert.assertTrue(spacingRow.getWidth() - spacing.getRight() >= Math.round(16 * density));

      measureSettings(root, Math.round(390 * density));
      Assert.assertEquals(View.GONE, navigation.getVisibility());
      Assert.assertSame(detail, activity.findViewById(R.id.advanced_lt_settings_detail));

      measureSettings(root, wideWidth);
      Assert.assertEquals(View.VISIBLE, navigation.getVisibility());
      activity.findViewById(R.id.advanced_lt_settings_themes).performClick();
      Assert.assertFalse(appearance.isActivated());
      Assert.assertTrue(activity.findViewById(R.id.advanced_lt_settings_themes).isActivated());
      Assert.assertNotNull(activity.findViewById(R.id.advanced_lt_theme_steve_light));
    }
  }

  private static void measureSettings(View root, int width) {
    for (int pass = 0; pass < 2; pass++) {
      root.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
          View.MeasureSpec.makeMeasureSpec(600, View.MeasureSpec.EXACTLY));
      root.layout(0, 0, width, 600);
    }
  }

  private int countSwitches(View view) {
    int count = view instanceof SwitchCompat ? 1 : 0;
    if (view instanceof android.view.ViewGroup) {
      final android.view.ViewGroup group = (android.view.ViewGroup) view;
      for (int index = 0; index < group.getChildCount(); index++) {
        count += countSwitches(group.getChildAt(index));
      }
    }
    return count;
  }
}
