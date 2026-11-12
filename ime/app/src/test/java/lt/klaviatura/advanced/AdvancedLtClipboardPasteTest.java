package lt.klaviatura.advanced;

import com.anysoftkeyboard.AnySoftKeyboardBaseTest;
import com.anysoftkeyboard.api.KeyCodes;
import org.junit.Assert;
import org.junit.Test;
import org.robolectric.util.ReflectionHelpers;

public class AdvancedLtClipboardPasteTest extends AnySoftKeyboardBaseTest {
  private void paste(String text) {
    Assert.assertTrue(ReflectionHelpers.callInstanceMethod(mAnySoftKeyboardUnderTest,
        "pasteClipboardText", ReflectionHelpers.ClassParameter.from(CharSequence.class, text)));
  }

  @Test public void pasteThenBackspaceDeletesOnlyLastCharacter() {
    paste("Hello world");
    mAnySoftKeyboardUnderTest.simulateKeyPress(KeyCodes.DELETE);
    Assert.assertEquals("Hello worl", getCurrentTestInputConnection().getCurrentTextInInputConnection());
  }

  @Test public void pasteReplacesSelectionAndTypingContinuesAtEnd() {
    getCurrentTestInputConnection().commitText("before old after", 1);
    getCurrentTestInputConnection().setSelection(7, 10);
    paste("new");
    mAnySoftKeyboardUnderTest.simulateKeyPress('x');
    Assert.assertEquals("before newx after", getCurrentTestInputConnection().getCurrentTextInInputConnection());
  }
}
