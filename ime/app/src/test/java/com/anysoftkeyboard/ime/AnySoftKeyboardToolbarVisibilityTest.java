package com.anysoftkeyboard.ime;

import android.view.View;
import android.view.inputmethod.EditorInfo;
import com.anysoftkeyboard.AnySoftKeyboardBaseTest;
import com.anysoftkeyboard.TestableAnySoftKeyboard;
import org.junit.Assert;
import org.junit.Test;

public class AnySoftKeyboardToolbarVisibilityTest extends AnySoftKeyboardBaseTest {
  public static class ToolbarKeyboard extends TestableAnySoftKeyboard {
    @Override
    protected boolean shouldShowActionsStrip() { return true; }
  }

  @Override
  protected Class<? extends TestableAnySoftKeyboard> getServiceClass() {
    return ToolbarKeyboard.class;
  }

  @Test
  public void editorRestartDoesNotHideIndependentToolbarWhenPredictionIsDisabled() {
    mAnySoftKeyboardUnderTest.getInputViewContainer().setActionsStripVisibility(false);
    simulateOnStartInputFlow(true, TestableAnySoftKeyboard.createEditorInfo(
        EditorInfo.IME_ACTION_NONE, EditorInfo.TYPE_CLASS_TEXT | EditorInfo.TYPE_TEXT_FLAG_NO_SUGGESTIONS));
    Assert.assertFalse(mAnySoftKeyboardUnderTest.isPredictionOn());
    Assert.assertEquals(View.VISIBLE,
        mAnySoftKeyboardUnderTest.getInputViewContainer().getCandidateView().getVisibility());
  }
}
