package lt.klaviatura.advanced;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;

import com.anysoftkeyboard.AnySoftKeyboardRobolectricTestRunner;
import com.anysoftkeyboard.prefs.DirectBootAwareSharedPreferences;
import com.anysoftkeyboard.theme.KeyboardThemeFactory;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AnySoftKeyboardRobolectricTestRunner.class)
public class AdvancedLtThemeMigrationTest {
  @Test public void autoFollowsSystemWhileManualThemeIsPreserved() {
    android.content.Context context = getApplicationContext();
    DirectBootAwareSharedPreferences.create(context).edit().putString(AdvancedLtTheme.MODE, "auto").apply();
    KeyboardThemeFactory factory = new KeyboardThemeFactory(context);
    android.content.res.Configuration config = new android.content.res.Configuration();
    config.uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES;
    factory.applyAutomaticTheme(config);
    Assert.assertEquals(AdvancedLtTheme.NIGHT, factory.getEnabledAddOn().getId());
    config.uiMode = android.content.res.Configuration.UI_MODE_NIGHT_NO;
    factory.applyAutomaticTheme(config);
    Assert.assertEquals(AdvancedLtTheme.DAY, factory.getEnabledAddOn().getId());
    DirectBootAwareSharedPreferences.create(context).edit().putString(AdvancedLtTheme.MODE, "manual").apply();
    config.uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES;
    factory.applyAutomaticTheme(config);
    Assert.assertEquals(AdvancedLtTheme.DAY, factory.getEnabledAddOn().getId());
  }
  @Test
  public void removedBlackThemeFallsBackToGray() {
    DirectBootAwareSharedPreferences.create(getApplicationContext()).edit()
        .putBoolean("theme_EC93C810-4FC5-4FC3-8CE4-1CDA44680A0D", true)
        .putBoolean("theme_8774f99e-fb4a-49fa-b8d0-4083f762250a", false)
        .apply();
    KeyboardThemeFactory factory = new KeyboardThemeFactory(getApplicationContext());
    Assert.assertNull(factory.getAddOnById("EC93C810-4FC5-4FC3-8CE4-1CDA44680A0D"));
    Assert.assertEquals("8774f99e-fb4a-49fa-b8d0-4083f762250a", factory.getEnabledAddOn().getId());
  }
}
