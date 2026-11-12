package com.anysoftkeyboard.keyboards.views;

import com.anysoftkeyboard.AnySoftKeyboardRobolectricTestRunner;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

@RunWith(AnySoftKeyboardRobolectricTestRunner.class)
public class QuickKeysKeyboardViewTest {
  @Test
  public void emojiSizeMatchesCategoryIconsRegardlessOfThemeFontSize() {
    QuickKeysKeyboardView view = new QuickKeysKeyboardView(RuntimeEnvironment.getApplication(), null);
    float expected = 26f * view.getResources().getDisplayMetrics().density;
    Assert.assertEquals(expected, view.getEmojiTextSize(14f), 0.01f);
    Assert.assertEquals(expected, view.getEmojiTextSize(48f), 0.01f);
  }
}
