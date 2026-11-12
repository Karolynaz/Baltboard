package com.anysoftkeyboard.ime;

import com.anysoftkeyboard.AnySoftKeyboardBaseTest;
import com.anysoftkeyboard.AnySoftKeyboardRobolectricTestRunner;
import com.menny.android.anysoftkeyboard.SoftKeyboard;
import java.lang.reflect.Method;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AnySoftKeyboardRobolectricTestRunner.class)
public class AdvancedLtAiSessionTest extends AnySoftKeyboardBaseTest {
  @Test
  public void resultCannotReplaceSameTextInANewInputSession() throws Exception {
    mAnySoftKeyboardUnderTest.simulateTextTyping("original ");
    Method captureMethod = SoftKeyboard.class.getDeclaredMethod("captureText");
    captureMethod.setAccessible(true);
    Object capture = captureMethod.invoke(mAnySoftKeyboardUnderTest);
    Assert.assertNotNull(capture);
    simulateFinishInputFlow();
    simulateOnStartInputFlow();
    mAnySoftKeyboardUnderTest.simulateTextTyping("original ");
    String before = getCurrentTestInputConnection().getCurrentTextInInputConnection();
    Method replaceMethod = SoftKeyboard.class.getDeclaredMethod(
        "replaceCapturedText", capture.getClass(), String.class);
    replaceMethod.setAccessible(true);
    replaceMethod.invoke(mAnySoftKeyboardUnderTest, capture, "replacement");
    Assert.assertEquals(before, getCurrentTestInputConnection().getCurrentTextInInputConnection());
  }
}
