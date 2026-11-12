package com.anysoftkeyboard.keyboards;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;

import com.anysoftkeyboard.AddOnTestUtils;
import com.anysoftkeyboard.AnySoftKeyboardRobolectricTestRunner;
import com.menny.android.anysoftkeyboard.AnyApplication;
import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AnySoftKeyboardRobolectricTestRunner.class)
public class KeyboardFactoryTest {

  private KeyboardFactory mKeyboardFactory;

  @Before
  public void setUp() throws Exception {
    mKeyboardFactory = AnyApplication.getKeyboardFactory(getApplicationContext());
  }

  @Test
  public void hasMultipleAlphabets() throws Exception {
    Assert.assertTrue(mKeyboardFactory.hasMultipleAlphabets());

    AddOnTestUtils.ensureAddOnAtIndexEnabled(mKeyboardFactory, 1, false);
    Assert.assertFalse(mKeyboardFactory.hasMultipleAlphabets());

    AddOnTestUtils.ensureAddOnAtIndexEnabled(mKeyboardFactory, 1, true);
    Assert.assertTrue(mKeyboardFactory.hasMultipleAlphabets());
  }

  @Test
  public void testDefaultKeyboardId() {
    final List<KeyboardAddOnAndBuilder> allAddOns = mKeyboardFactory.getAllAddOns();
    Assert.assertEquals(2, allAddOns.size());
    KeyboardAddOnAndBuilder addon = mKeyboardFactory.getEnabledAddOn();
    Assert.assertNotNull(addon);
    Assert.assertEquals("c7535083-4fe6-49dc-81aa-c5438a1a343a", addon.getId());

    Assert.assertTrue(
        mKeyboardFactory.isAddOnEnabledByDefault("c7535083-4fe6-49dc-81aa-c5438a1a343a"));
    Assert.assertFalse(
        mKeyboardFactory.isAddOnEnabledByDefault("c7535083-4fe6-49dc-81aa-c5438a1a343b"));
  }

  @Test
  public void testParsesApiLevel() {
    final KeyboardAddOnAndBuilder lithuanian =
        AnyApplication.getKeyboardFactory(getApplicationContext())
            .getAddOnById("aace6147-5687-4ac4-26a6-011d507d90ff");
    Assert.assertNotNull(lithuanian);
    Assert.assertNotEquals(0, lithuanian.getApiVersion());
    Assert.assertEquals(
        getApplicationContext()
            .getResources()
            .getInteger(com.anysoftkeyboard.addons.R.integer.anysoftkeyboard_api_version_code),
        lithuanian.getApiVersion());
  }
}
