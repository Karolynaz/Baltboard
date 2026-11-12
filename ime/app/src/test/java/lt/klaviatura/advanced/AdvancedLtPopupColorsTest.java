package lt.klaviatura.advanced;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import com.anysoftkeyboard.AnySoftKeyboardRobolectricTestRunner;
import com.menny.android.anysoftkeyboard.R;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

@RunWith(AnySoftKeyboardRobolectricTestRunner.class)
@Config(sdk = {28, 32})
public class AdvancedLtPopupColorsTest {
  @Test
  public void popupBackgroundsUseEightyPercentOfMatchingAccent() {
    assertPopup(R.drawable.yochees_dark_popup_background, R.color.advanced_lt_pressed_color);
    assertPopup(R.drawable.blacktheme_popup_background, R.color.advanced_lt_pressed_color);
    assertPopup(R.drawable.yochees_light_popup_background, R.color.advanced_lt_pressed_light_color);
  }

  @Test
  public void lightAndDarkPopupTextRemainReadable() {
    assertContrast(Color.BLACK, R.drawable.yochees_light_popup_background, Color.WHITE);
    assertContrast(Color.WHITE, R.drawable.yochees_dark_popup_background, Color.rgb(34, 34, 34));
    assertContrast(Color.WHITE, R.drawable.blacktheme_popup_background, Color.rgb(9, 9, 10));
  }

  @Test
  public void suggestionHighlightsMatchPressedKeys() {
    Assert.assertEquals(getApplicationContext().getColor(R.color.advanced_lt_pressed_color),
        backgroundColor(R.drawable.dark_candidate_selected_background));
    Assert.assertEquals(getApplicationContext().getColor(R.color.advanced_lt_pressed_light_color),
        backgroundColor(R.drawable.light_candidate_selected_background));
  }

  private static void assertPopup(int drawable, int accent) {
    Assert.assertEquals(
        ColorUtils.setAlphaComponent(getApplicationContext().getColor(accent), 204),
        backgroundColor(drawable));
  }

  private static void assertContrast(int text, int drawable, int surface) {
    int background = ColorUtils.compositeColors(backgroundColor(drawable), surface);
    Assert.assertTrue("Popup text contrast must be at least 4.5:1",
        ColorUtils.calculateContrast(text, background) >= 4.5);
  }

  private static int backgroundColor(int drawable) {
    GradientDrawable background =
        (GradientDrawable) ContextCompat.getDrawable(getApplicationContext(), drawable);
    return background.getColor().getDefaultColor();
  }
}
