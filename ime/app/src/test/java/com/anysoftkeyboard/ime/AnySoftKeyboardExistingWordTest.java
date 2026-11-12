package com.anysoftkeyboard.ime;

import com.anysoftkeyboard.AnySoftKeyboardBaseTest;
import com.anysoftkeyboard.TestInputConnection;
import com.anysoftkeyboard.api.KeyCodes;
import com.anysoftkeyboard.rx.TestRxSchedulers;
import com.anysoftkeyboard.test.SharedPrefsHelper;
import com.menny.android.anysoftkeyboard.R;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class AnySoftKeyboardExistingWordTest extends AnySoftKeyboardBaseTest {
  @Before
  public void enableExistingWordSuggestions() {
    SharedPrefsHelper.setPrefsValue(R.string.settings_key_allow_suggestions_restart, true);
    TestRxSchedulers.drainAllTasks();
    Mockito.doReturn(true).when(mAnySoftKeyboardUnderTest.getInputView()).isShown();
  }

  private TestInputConnection editWord(String text, int cursor) {
    TestInputConnection connection = getCurrentTestInputConnection();
    connection.commitText(text, 1);
    connection.setSelection(cursor, cursor);
    TestRxSchedulers.drainAllTasks();
    mAnySoftKeyboardUnderTest.performRestartWordSuggestion(connection);
    return connection;
  }

  @Test
  public void replacingFirstLetterDoesNotDuplicateSuffixAfterCompositionIsLost() {
    TestInputConnection connection = editWord("great game of the year.", 1);
    Assert.assertEquals("great", mAnySoftKeyboardUnderTest.getCurrentComposedWord().getTypedWord().toString());
    mAnySoftKeyboardUnderTest.simulateKeyPress(KeyCodes.DELETE);
    Assert.assertEquals("reat game of the year.", connection.getCurrentTextInInputConnection());
    // Editors may finish composing without moving the cursor before the next key arrives.
    connection.finishComposingText();
    mAnySoftKeyboardUnderTest.simulateKeyPress('G', false);
    Assert.assertEquals("Great game of the year.", connection.getCurrentTextInInputConnection());
    Assert.assertEquals(1, connection.getCurrentStartPosition());
  }

  @Test
  public void suggestionReplacesWholeExistingWordAfterCompositionIsLost() {
    TestInputConnection connection = editWord("great game of the year.", 1);
    connection.finishComposingText();
    mAnySoftKeyboardUnderTest.pickSuggestionManually(1, "Great", false);
    Assert.assertEquals("Great game of the year.", connection.getCurrentTextInInputConnection());
  }

  @Test
  public void highlightedWordOffersSuggestionsAndReplacesOnlyTheSelection() {
    TestInputConnection connection = editWord("It is probab fine.", 12);
    connection.setSelection(6, 12);
    mAnySoftKeyboardUnderTest.onUpdateSelection(12, 12, 6, 12, 6, 12);

    Assert.assertEquals("probab", verifyAndCaptureSuggestion(false).get(0).toString());
    mAnySoftKeyboardUnderTest.pickSuggestionManually(1, "probably", true);
    Assert.assertEquals("It is probably fine.", connection.getCurrentTextInInputConnection());
    Assert.assertEquals(14, connection.getCurrentStartPosition());
  }

  @Test
  public void suggestionForSelectionDoesNotReplaceAnIdenticalWordElsewhere() {
    TestInputConnection connection = editWord("probab and probab", 6);
    connection.setSelection(0, 6);
    mAnySoftKeyboardUnderTest.onUpdateSelection(6, 6, 0, 6, 0, 6);
    connection.setSelection(11, 17);
    mAnySoftKeyboardUnderTest.pickSuggestionManually(1, "probably", true);
    Assert.assertEquals("probab and probab", connection.getCurrentTextInInputConnection());
  }

  @Test
  public void suggestionUsesTheLiveCursorWithinTheOriginalWord() {
    TestInputConnection connection = editWord("It is probab fine.", 7);
    connection.setSelection(12, 12);
    mAnySoftKeyboardUnderTest.pickSuggestionManually(1, "probably", true);
    Assert.assertEquals("It is probably fine.", connection.getCurrentTextInInputConnection());
  }

  @Test
  public void suggestionUsesVerifiedExtractedSelectionWhenSelectedTextIsUnavailable() {
    TestInputConnection connection = editWord("It is probab fine.", 12);
    Mockito.doReturn(null).when(connection).getSelectedText(0);
    mAnySoftKeyboardUnderTest.pickSuggestionManually(1, "probably", true);
    Assert.assertEquals("It is probably fine.", connection.getCurrentTextInInputConnection());
  }

  @Test
  public void suggestionReplacesWholeWordWhenCursorIsBeforeDigit() {
    TestInputConnection connection = editWord("Some pe0ple are here.", 7);
    Assert.assertEquals("pe0ple", mAnySoftKeyboardUnderTest.getCurrentComposedWord().getTypedWord().toString());
    mAnySoftKeyboardUnderTest.pickSuggestionManually(1, "people", true);
    Assert.assertEquals("Some people are here.", connection.getCurrentTextInInputConnection());
    Assert.assertEquals(11, connection.getCurrentStartPosition());
  }

  @Test
  public void suggestionReplacesWholeWordWhenEditorIgnoresComposingRegion() {
    TestInputConnection connection = editWord("Some pe0ple are here.", 8);
    connection.finishComposingText();
    Mockito.doReturn(true).when(connection).setComposingRegion(Mockito.anyInt(), Mockito.anyInt());
    mAnySoftKeyboardUnderTest.pickSuggestionManually(1, "people", true);
    Assert.assertEquals("Some people are here.", connection.getCurrentTextInInputConnection());
  }

  @Test
  public void standaloneNumberDoesNotBecomeAnExistingWord() {
    editWord("Some 123 people.", 6);
    Assert.assertTrue(mAnySoftKeyboardUnderTest.getCurrentComposedWord().isEmpty());
  }

  @Test
  public void backspaceDoesNotInsertWholeWordAfterCompositionIsLost() {
    TestInputConnection connection = editWord("a great game.", 4);
    connection.finishComposingText();
    mAnySoftKeyboardUnderTest.simulateKeyPress(KeyCodes.DELETE, false);
    Assert.assertEquals("a geat game.", connection.getCurrentTextInInputConnection());
    Assert.assertEquals(3, connection.getCurrentStartPosition());
  }

  @Test
  public void typingBeforeDelayedRestartIncludesExistingSuffix() {
    TestInputConnection connection = getCurrentTestInputConnection();
    connection.commitText("reat game of the year.", 1);
    connection.setSelection(0, 0);
    mAnySoftKeyboardUnderTest.simulateKeyPress('G', false);
    Assert.assertEquals("Great game of the year.", connection.getCurrentTextInInputConnection());
    Assert.assertEquals("Great", mAnySoftKeyboardUnderTest.getCurrentComposedWord().getTypedWord().toString());
    mAnySoftKeyboardUnderTest.pickSuggestionManually(1, "Great", false);
    Assert.assertEquals("Great game of the year.", connection.getCurrentTextInInputConnection());
  }

  @Test
  public void disabledRestartKeepsMidWordEditsLiteral() {
    SharedPrefsHelper.setPrefsValue(R.string.settings_key_allow_suggestions_restart, false);
    TestRxSchedulers.drainAllTasks();
    TestInputConnection connection = getCurrentTestInputConnection();
    connection.commitText("reat game of the year.", 1);
    connection.setSelection(0, 0);
    mAnySoftKeyboardUnderTest.simulateKeyPress('G', false);
    Assert.assertEquals("Great game of the year.", connection.getCurrentTextInInputConnection());
    Assert.assertTrue(mAnySoftKeyboardUnderTest.getCurrentComposedWord().isEmpty());
  }

  @Test
  public void lateSelectionNotificationCannotReplaceAnotherWord() {
    TestInputConnection connection = editWord("great game of the year.", 1);
    connection.setSelection(8, 8);
    mAnySoftKeyboardUnderTest.pickSuggestionManually(1, "Great", false);
    Assert.assertEquals("great game of the year.", connection.getCurrentTextInInputConnection());
  }

  @Test
  public void unsupportedComposingRegionKeepsLetterReplacementLiteral() {
    TestInputConnection connection = editWord("great game of the year.", 1);
    mAnySoftKeyboardUnderTest.simulateKeyPress(KeyCodes.DELETE);
    connection.finishComposingText();
    Mockito.doReturn(false).when(connection).setComposingRegion(Mockito.anyInt(), Mockito.anyInt());
    mAnySoftKeyboardUnderTest.simulateKeyPress('G', false);
    Assert.assertEquals("Great game of the year.", connection.getCurrentTextInInputConnection());
    Assert.assertTrue(mAnySoftKeyboardUnderTest.getCurrentComposedWord().isEmpty());
  }

  @Test
  public void editorIgnoringSelectionDoesNotInsertManualSuggestion() {
    TestInputConnection connection = editWord("great game of the year.", 1);
    connection.finishComposingText();
    Mockito.doReturn(true).when(connection).setSelection(0, 5);
    mAnySoftKeyboardUnderTest.pickSuggestionManually(1, "Great", false);
    Assert.assertEquals("great game of the year.", connection.getCurrentTextInInputConnection());
  }

  @Test
  public void composingRegionUsesUtf16OffsetsAfterEmoji() {
    TestInputConnection connection = editWord("🌿 great game.", 4);
    mAnySoftKeyboardUnderTest.simulateKeyPress(KeyCodes.DELETE);
    connection.finishComposingText();
    mAnySoftKeyboardUnderTest.simulateKeyPress('G', false);
    Assert.assertEquals("🌿 Great game.", connection.getCurrentTextInInputConnection());
    Assert.assertEquals(4, connection.getCurrentStartPosition());
  }
}
