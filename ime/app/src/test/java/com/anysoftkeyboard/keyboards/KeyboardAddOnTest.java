package com.anysoftkeyboard.keyboards;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.anysoftkeyboard.AnySoftKeyboardRobolectricTestRunner;
import com.menny.android.anysoftkeyboard.AnyApplication;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AnySoftKeyboardRobolectricTestRunner.class)
public class KeyboardAddOnTest {

  private static final String ASK_ENGLISH_1_ID = "c7535083-4fe6-49dc-81aa-c5438a1a343a";
  private static final String LITHUANIAN_ID = "aace6147-5687-4ac4-26a6-011d507d90ff";

  @Test
  public void testGetKeyboardDefaultEnabled() throws Exception {
    List<KeyboardAddOnAndBuilder> enabledKeyboards =
        AnyApplication.getKeyboardFactory(getApplicationContext()).getEnabledAddOns();
    // checking that ASK English is enabled
    boolean askEnglishEnabled = false;
    for (KeyboardAddOnAndBuilder addOnAndBuilder : enabledKeyboards) {
      if (addOnAndBuilder.getId().equals(ASK_ENGLISH_1_ID)) {
        assertTrue(addOnAndBuilder.getKeyboardDefaultEnabled());
        assertEquals(addOnAndBuilder.getPackageName(), getApplicationContext().getPackageName());
        askEnglishEnabled = true;
      }
    }
    assertTrue(askEnglishEnabled);
    // This product intentionally enables exactly English and Lithuanian.
    Assert.assertEquals(2, enabledKeyboards.size());
  }

  @Test
  public void testGetEnabledDefaultFromAllKeyboards() throws Exception {
    List<KeyboardAddOnAndBuilder> allAvailableKeyboards =
        AnyApplication.getKeyboardFactory(getApplicationContext()).getAllAddOns();

    Map<String, Boolean> keyboardsEnabled = new HashMap<>();
    for (KeyboardAddOnAndBuilder addOnAndBuilder : allAvailableKeyboards) {
      keyboardsEnabled.put(addOnAndBuilder.getId(), addOnAndBuilder.getKeyboardDefaultEnabled());
    }

    Assert.assertEquals(2, keyboardsEnabled.size());
    Assert.assertTrue(keyboardsEnabled.containsKey(ASK_ENGLISH_1_ID));
    Assert.assertTrue(keyboardsEnabled.get(ASK_ENGLISH_1_ID));
    Assert.assertTrue(keyboardsEnabled.containsKey(LITHUANIAN_ID));
    Assert.assertTrue(keyboardsEnabled.get(LITHUANIAN_ID));
  }

  private KeyboardAddOnAndBuilder getKeyboardFromFactory(String id) {
    List<KeyboardAddOnAndBuilder> keyboards =
        AnyApplication.getKeyboardFactory(getApplicationContext()).getAllAddOns();

    for (KeyboardAddOnAndBuilder addOnAndBuilder : keyboards) {
      if (addOnAndBuilder.getId().equals(id)) {
        return addOnAndBuilder;
      }
    }

    return null;
  }

  @Test
  public void testGetKeyboardLocale() throws Exception {
    KeyboardAddOnAndBuilder askEnglish = getKeyboardFromFactory(ASK_ENGLISH_1_ID);
    assertNotNull(askEnglish);
    assertEquals("en", askEnglish.getKeyboardLocale());

    KeyboardAddOnAndBuilder lithuanian = getKeyboardFromFactory(LITHUANIAN_ID);
    assertNotNull(lithuanian);
    assertEquals("lt", lithuanian.getKeyboardLocale());
  }

  @Test
  public void testCreateKeyboard() throws Exception {}
}
