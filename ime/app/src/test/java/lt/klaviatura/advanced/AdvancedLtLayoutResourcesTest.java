package lt.klaviatura.advanced;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;

import android.content.res.XmlResourceParser;
import androidx.core.content.ContextCompat;
import com.anysoftkeyboard.AnySoftKeyboardRobolectricTestRunner;
import com.menny.android.anysoftkeyboard.R;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.xmlpull.v1.XmlPullParser;

@RunWith(AnySoftKeyboardRobolectricTestRunner.class)
public class AdvancedLtLayoutResourcesTest {

  @Test
  @org.robolectric.annotation.Config(sdk = 32)
  public void pressedColorsFollowSystemPaletteOnAndroid12() {
    Assert.assertEquals(
        getApplicationContext().getColor(android.R.color.system_accent1_600),
        getApplicationContext().getColor(R.color.advanced_lt_pressed_color));
    Assert.assertEquals(
        getApplicationContext().getColor(android.R.color.system_accent1_200),
        getApplicationContext().getColor(R.color.advanced_lt_pressed_light_color));
  }

  @Test
  public void lithuanianTopRowUsesNormalHeight() throws Exception {
    try (XmlResourceParser parser =
        getApplicationContext().getResources().getXml(R.xml.lithuanian_qwerty_4rows)) {
      int normalHeight = 0;
      while (parser.next() != XmlPullParser.END_DOCUMENT) {
        if (parser.getEventType() != XmlPullParser.START_TAG) continue;
        if ("Keyboard".equals(parser.getName())) {
          normalHeight = parser.getAttributeResourceValue(
              "http://schemas.android.com/apk/res/android", "keyHeight", 0);
        } else if ("Row".equals(parser.getName())) {
          Assert.assertNotEquals(0, normalHeight);
          Assert.assertEquals(normalHeight, parser.getAttributeResourceValue(
              "http://schemas.android.com/apk/res/android", "keyHeight", 0));
          return;
        }
      }
      Assert.fail("Missing top row");
    }
  }

  @Test
  public void lithuanianTopRowContainsExactlyNineLithuanianLetters() throws Exception {
    Assert.assertEquals(
        Arrays.asList("ą", "č", "ę", "ė", "į", "š", "ų", "ū", "ž"),
        firstRowAttribute(R.xml.lithuanian_qwerty_4rows, "codes"));
  }

  @Test
  public void symbolsTopRowContainsExactlyDigits() throws Exception {
    Assert.assertEquals(
        Arrays.asList("1", "2", "3", "4", "5", "6", "7", "8", "9", "0"),
        firstRowAttribute(R.xml.symbols, "keyLabel"));
  }

  @Test
  public void englishNumberRowUsesNormalHeight() throws Exception {
    try (XmlResourceParser parser =
        getApplicationContext().getResources().getXml(R.xml.ext_kbd_top_row_numbers_simple)) {
      while (parser.next() != XmlPullParser.END_DOCUMENT) {
        if (parser.getEventType() == XmlPullParser.START_TAG && "Row".equals(parser.getName())) {
          Assert.assertEquals(R.integer.key_normal_height, parser.getAttributeResourceValue(
              "http://schemas.android.com/apk/res/android", "keyHeight", 0));
          return;
        }
      }
      Assert.fail("Missing number row");
    }
  }

  @Test
  public void spacebarUsesShortLanguageLabels() {
    Assert.assertEquals("LT", getApplicationContext().getString(R.string.advanced_lt_language_lithuanian));
    Assert.assertEquals("EN", getApplicationContext().getString(R.string.advanced_lt_language_english));
  }

  @Test
  public void allProvidedToolbarIconsAreStoredAsVectorResources() {
    int[] icons = {
      R.drawable.advanced_lt_ic_emoji,
      R.drawable.advanced_lt_ic_clipboard,
      R.drawable.advanced_lt_ic_settings,
      R.drawable.advanced_lt_ic_ai_check,
      R.drawable.advanced_lt_ic_arrow_back,
      R.drawable.advanced_lt_ic_more,
      R.drawable.advanced_lt_ic_keyboard,
      R.drawable.advanced_lt_ic_delete,
      R.drawable.advanced_lt_ic_emoji_recents,
      R.drawable.advanced_lt_ic_emoji_all,
      R.drawable.advanced_lt_ic_emoji_gestures,
      R.drawable.advanced_lt_ic_emoji_people,
      R.drawable.advanced_lt_ic_emoji_animals,
      R.drawable.advanced_lt_ic_emoji_food,
      R.drawable.advanced_lt_ic_emoji_accessories,
      R.drawable.advanced_lt_ic_emoji_home,
      R.drawable.advanced_lt_ic_emoji_activity,
      R.drawable.advanced_lt_ic_emoji_objects,
      R.drawable.advanced_lt_ic_emoji_symbols,
      R.drawable.advanced_lt_ic_emoji_flags
    };
    for (int icon : icons) {
      Assert.assertNotNull(ContextCompat.getDrawable(getApplicationContext(), icon));
    }
  }

  private static List<String> firstRowAttribute(int xmlResource, String attribute)
      throws Exception {
    final List<String> values = new ArrayList<>();
    try (XmlResourceParser parser =
        getApplicationContext().getResources().getXml(xmlResource)) {
      boolean insideFirstRow = false;
      int event;
      while ((event = parser.next()) != XmlPullParser.END_DOCUMENT) {
        if (event == XmlPullParser.START_TAG && "Row".equals(parser.getName())) {
          insideFirstRow = true;
        } else if (event == XmlPullParser.START_TAG
            && insideFirstRow
            && "Key".equals(parser.getName())) {
          values.add(
              parser.getAttributeValue("http://schemas.android.com/apk/res/android", attribute));
        } else if (event == XmlPullParser.END_TAG
            && insideFirstRow
            && "Row".equals(parser.getName())) {
          break;
        }
      }
    }
    return values;
  }
}
