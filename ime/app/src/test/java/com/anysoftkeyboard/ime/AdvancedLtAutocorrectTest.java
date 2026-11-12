package com.anysoftkeyboard.ime;

import com.anysoftkeyboard.AnySoftKeyboardBaseTest;
import com.anysoftkeyboard.AnySoftKeyboardRobolectricTestRunner;
import com.anysoftkeyboard.test.SharedPrefsHelper;
import com.menny.android.anysoftkeyboard.R;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AnySoftKeyboardRobolectricTestRunner.class)
public class AdvancedLtAutocorrectTest extends AnySoftKeyboardBaseTest {
  @Test
  public void disabledSwitchPreservesTypedWordDespiteHighStrength() {
    SharedPrefsHelper.setPrefsValue(R.string.settings_key_auto_pick_suggestion_aggressiveness,
        "high_aggressiveness");
    SharedPrefsHelper.setPrefsValue(R.string.settings_key_quick_fix, false);
    mAnySoftKeyboardUnderTest.simulateTextTyping("hellp");
    Assert.assertTrue(mAnySoftKeyboardUnderTest.isPredictionOn());
    Assert.assertFalse(((AnySoftKeyboardSuggestions) mAnySoftKeyboardUnderTest).isAutoCorrect());
    mAnySoftKeyboardUnderTest.getCurrentComposedWord().setPreferredWord("hello");
    mAnySoftKeyboardUnderTest.simulateKeyPress(' ');
    Assert.assertEquals("hellp ", getCurrentTestInputConnection().getCurrentTextInInputConnection());
  }

  @Test
  public void disablingWhileComposingStopsPendingCorrection() {
    SharedPrefsHelper.setPrefsValue(R.string.settings_key_quick_fix, true);
    SharedPrefsHelper.setPrefsValue(R.string.settings_key_auto_pick_suggestion_aggressiveness,
        "high_aggressiveness");
    mAnySoftKeyboardUnderTest.simulateTextTyping("hellp");
    Assert.assertTrue(((AnySoftKeyboardSuggestions) mAnySoftKeyboardUnderTest).isAutoCorrect());
    SharedPrefsHelper.setPrefsValue(R.string.settings_key_quick_fix, false);
    Assert.assertFalse(((AnySoftKeyboardSuggestions) mAnySoftKeyboardUnderTest).isAutoCorrect());
    mAnySoftKeyboardUnderTest.getCurrentComposedWord().setPreferredWord("hello");
    mAnySoftKeyboardUnderTest.simulateKeyPress(' ');
    Assert.assertEquals("hellp ", getCurrentTestInputConnection().getCurrentTextInInputConnection());
    SharedPrefsHelper.setPrefsValue(R.string.settings_key_quick_fix, true);
    mAnySoftKeyboardUnderTest.simulateTextTyping("hellp");
    Assert.assertTrue(((AnySoftKeyboardSuggestions) mAnySoftKeyboardUnderTest).isAutoCorrect());
  }

  @Test
  public void strengthOffStillDisablesCorrectionWithSwitchOn() {
    SharedPrefsHelper.setPrefsValue(R.string.settings_key_quick_fix, true);
    SharedPrefsHelper.setPrefsValue(R.string.settings_key_auto_pick_suggestion_aggressiveness, "none");
    mAnySoftKeyboardUnderTest.simulateTextTyping("hellp");
    Assert.assertFalse(((AnySoftKeyboardSuggestions) mAnySoftKeyboardUnderTest).isAutoCorrect());
  }
}
