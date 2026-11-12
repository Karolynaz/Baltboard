package lt.klaviatura.advanced;

import com.anysoftkeyboard.AnySoftKeyboardRobolectricTestRunner;
import com.menny.android.anysoftkeyboard.R;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(AnySoftKeyboardRobolectricTestRunner.class)
public class AdvancedLtInterfaceLanguageTest {
  @Test @Config(qualifiers = "en") public void englishSystemShowsEnglishClipboard() {
    Assert.assertEquals("Clipboard", RuntimeEnvironment.getApplication().getString(R.string.advanced_lt_toolbar_clipboard));
    Assert.assertEquals("Delete", RuntimeEnvironment.getApplication().getString(R.string.advanced_lt_clipboard_delete));
  }
  @Test @Config(qualifiers = "lt") public void lithuanianSystemShowsLithuanianClipboard() {
    Assert.assertEquals("Iškarpinė", RuntimeEnvironment.getApplication().getString(R.string.advanced_lt_toolbar_clipboard));
    Assert.assertEquals("Trinti", RuntimeEnvironment.getApplication().getString(R.string.advanced_lt_clipboard_delete));
  }
}
