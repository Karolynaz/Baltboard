package lt.klaviatura.advanced;

import com.anysoftkeyboard.AnySoftKeyboardBaseTest;
import com.anysoftkeyboard.TestInputConnection;
import org.junit.*;
import org.robolectric.util.ReflectionHelpers;

public class AdvancedLtTextCaptureTest extends AnySoftKeyboardBaseTest {
  @SuppressWarnings({"unchecked", "rawtypes"})
  private boolean replace(Object capture, String text) {
    return ReflectionHelpers.callInstanceMethod(mAnySoftKeyboardUnderTest, "replaceCapturedText",
        ReflectionHelpers.ClassParameter.from((Class) capture.getClass(), capture),
        ReflectionHelpers.ClassParameter.from(String.class, text));
  }
  @Test public void changedSelectionCannotOverwriteNewText() {
    TestInputConnection connection = getCurrentTestInputConnection();
    connection.commitText("before selected after", 1);
    connection.setSelection(7, 15);
    Object capture = ReflectionHelpers.callInstanceMethod(mAnySoftKeyboardUnderTest, "captureText");
    connection.setSelection(0, 6);
    Assert.assertFalse(replace(capture, "replacement"));
    Assert.assertEquals("before selected after", connection.getCurrentTextInInputConnection());
  }
  @Test public void unchangedFullTextCanBeReplaced() {
    TestInputConnection connection = getCurrentTestInputConnection();
    connection.commitText("original text", 1);
    Object capture = ReflectionHelpers.callInstanceMethod(mAnySoftKeyboardUnderTest, "captureText");
    Assert.assertTrue(replace(capture, "new text"));
    Assert.assertEquals("new text", connection.getCurrentTextInInputConnection());
  }
  @Test public void whitespaceSelectionIsNotSentToAi() {
    TestInputConnection connection = getCurrentTestInputConnection();
    connection.commitText("   ", 1);
    connection.setSelection(0, 3);
    Assert.assertNull(ReflectionHelpers.callInstanceMethod(mAnySoftKeyboardUnderTest, "captureText"));
  }
  @Test public void capturesAllParagraphsNotJustTheCursorParagraph() {
    TestInputConnection connection=getCurrentTestInputConnection();
    String text="First paragraph.\n\nSecond paragraph.\nThird.";
    connection.commitText(text,1);
    connection.setSelection(6,6);
    Object capture=ReflectionHelpers.callInstanceMethod(mAnySoftKeyboardUnderTest,"captureText");
    Assert.assertEquals(text,ReflectionHelpers.getField(capture,"text"));
  }
  @Test public void explicitSelectionTakesPrecedence() {
    TestInputConnection connection=getCurrentTestInputConnection();
    connection.commitText("before selected after",1);
    connection.setSelection(7,15);
    Object capture=ReflectionHelpers.callInstanceMethod(mAnySoftKeyboardUnderTest,"captureText");
    Assert.assertEquals("selected",ReflectionHelpers.getField(capture,"text"));
    Assert.assertEquals(true,ReflectionHelpers.getField(capture,"selection"));
  }
  @Test public void refusesOversizedTextRatherThanSilentlyTruncating() {
    getCurrentTestInputConnection().commitText("x".repeat(10001),1);
    Assert.assertNull(ReflectionHelpers.callInstanceMethod(mAnySoftKeyboardUnderTest,"captureText"));
  }
}
