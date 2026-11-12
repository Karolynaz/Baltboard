package com.anysoftkeyboard.ime;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static com.anysoftkeyboard.keyboards.Keyboard.KEYBOARD_ROW_MODE_EMAIL;
import static com.anysoftkeyboard.keyboards.Keyboard.KEYBOARD_ROW_MODE_IM;
import static com.anysoftkeyboard.keyboards.Keyboard.KEYBOARD_ROW_MODE_NORMAL;
import static com.anysoftkeyboard.keyboards.Keyboard.KEYBOARD_ROW_MODE_PASSWORD;
import static com.anysoftkeyboard.keyboards.Keyboard.KEYBOARD_ROW_MODE_URL;

import android.content.res.Configuration;
import android.view.inputmethod.EditorInfo;
import com.anysoftkeyboard.AddOnTestUtils;
import com.anysoftkeyboard.AnySoftKeyboardBaseTest;
import com.anysoftkeyboard.AnySoftKeyboardRobolectricTestRunner;
import com.anysoftkeyboard.TestableAnySoftKeyboard;
import com.anysoftkeyboard.api.KeyCodes;
import com.anysoftkeyboard.keyboards.AnyKeyboard;
import com.anysoftkeyboard.keyboards.Keyboard;
import com.anysoftkeyboard.keyboards.KeyboardSwitcher;
import com.anysoftkeyboard.test.SharedPrefsHelper;
import com.menny.android.anysoftkeyboard.AnyApplication;
import com.menny.android.anysoftkeyboard.R;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

@RunWith(AnySoftKeyboardRobolectricTestRunner.class)
public class AnySoftKeyboardKeyboardSwitcherTest extends AnySoftKeyboardBaseTest {

  @Test
  public void keyboardResizesAcrossFoldableWidthsWithoutChangingKeysOrLanguage() {
    simulateOnStartInputFlow();
    final KeyboardSwitcher switcher = mAnySoftKeyboardUnderTest.getKeyboardSwitcherForTests();
    final float density = mAnySoftKeyboardUnderTest.getResources().getDisplayMetrics().density;
    final int compactWidth = Math.round(400 * density);
    final int wideWidth = Math.round(700 * density);

    layoutKeyboardAtWidth(compactWidth);
    switcher.setKeyboardGeometry(compactWidth, false);
    final AnyKeyboard compact = mAnySoftKeyboardUnderTest.getCurrentAlphabetKeyboard();
    switcher.setKeyboardGeometry(compactWidth, true);
    final AnyKeyboard spaced = mAnySoftKeyboardUnderTest.getCurrentAlphabetKeyboard();
    Assert.assertTrue(key(spaced, 'q').x > key(compact, 'q').x);
    Assert.assertTrue(key(spaced, 'q').height < key(compact, 'q').height);
    Assert.assertTrue(key(spaced, 'a').x > key(spaced, 'q').x);
    Assert.assertTrue(key(spaced, KeyCodes.SHIFT).width < key(compact, KeyCodes.SHIFT).width);
    final int compactQPosition = key(spaced, 'q').x;
    final int compactPWidth = key(spaced, 'p').width;

    layoutKeyboardAtWidth(wideWidth);
    switcher.setKeyboardGeometry(wideWidth, true);
    final AnyKeyboard wide = mAnySoftKeyboardUnderTest.getCurrentAlphabetKeyboard();
    Assert.assertEquals(spaced.getKeyboardId(), wide.getKeyboardId());
    Assert.assertTrue(key(wide, 'q').x >= wideWidth * 0.035f);
    Assert.assertTrue(Keyboard.Key.getEndX(key(wide, 'p')) < wideWidth);
    Assert.assertTrue(key(wide, KeyCodes.KEYBOARD_MODE_CHANGE).x < key(wide, ',').x);
    Assert.assertTrue(key(wide, ',').x < key(wide, KeyCodes.SPACE).x);
    Assert.assertTrue(key(wide, KeyCodes.SPACE).x < key(wide, '.').x);
    Assert.assertTrue(key(wide, '.').x < key(wide, KeyCodes.ENTER).x);

    switcher.setKeyboardGeometry(wideWidth, false);
    final AnyKeyboard wideWithStandardGaps = mAnySoftKeyboardUnderTest.getCurrentAlphabetKeyboard();
    Assert.assertTrue(key(wideWithStandardGaps, 'q').x >= wideWidth * 0.03f);
    Assert.assertTrue(wide.hasWideVisualSpacing());
    Assert.assertFalse(wideWithStandardGaps.hasWideVisualSpacing());
    Assert.assertEquals(key(wideWithStandardGaps, 'q').height, key(wide, 'q').height);
    Assert.assertEquals(key(wideWithStandardGaps, 'q').width, key(wide, 'q').width);
    Assert.assertEquals(key(wideWithStandardGaps, 'p').x, key(wide, 'p').x);

    layoutKeyboardAtWidth(compactWidth);
    switcher.setKeyboardGeometry(compactWidth, true);
    final AnyKeyboard folded = mAnySoftKeyboardUnderTest.getCurrentAlphabetKeyboard();
    Assert.assertEquals(compactQPosition, key(folded, 'q').x);
    Assert.assertEquals(compactPWidth, key(folded, 'p').width);
  }

  private static Keyboard.Key key(AnyKeyboard keyboard, int code) {
    return keyboard.getKeys().stream()
        .filter(key -> key.getPrimaryCode() == code)
        .findFirst()
        .orElseThrow(() -> new AssertionError("Missing key " + code));
  }

  @Test
  public void cachedWideKeyboardFitsSmallerViewportAndRestoresItsKeyPositions() {
    simulateOnStartInputFlow();
    final var switcher = mAnySoftKeyboardUnderTest.getKeyboardSwitcherForTests();
    final float density = mAnySoftKeyboardUnderTest.getResources().getDisplayMetrics().density;
    final int landscapeWidth = Math.round(870 * density);
    final int portraitWidth = Math.round(657 * density);
    layoutKeyboardAtWidth(landscapeWidth);
    switcher.setKeyboardGeometry(landscapeWidth, true);
    final AnyKeyboard keyboard = mAnySoftKeyboardUnderTest.getCurrentAlphabetKeyboard();
    final int originalP = key(keyboard, 'p').x;
    layoutKeyboardAtWidth(portraitWidth);
    Assert.assertTrue(keyboard.getKeys().stream().allMatch(
        key -> key.x >= 0 && Keyboard.Key.getEndX(key) <= portraitWidth));
    Assert.assertEquals(portraitWidth, keyboard.getMinWidth());
    layoutKeyboardAtWidth(landscapeWidth);
    Assert.assertEquals(originalP, key(keyboard, 'p').x);
  }

  private void layoutKeyboardAtWidth(int width) {
    final android.view.View view = (android.view.View) mAnySoftKeyboardUnderTest.getInputView();
    view.layout(0, 0, width + view.getPaddingLeft() + view.getPaddingRight(), view.getHeight());
  }

  @Test
  @org.robolectric.annotation.Config(qualifiers = "land-450dpi")
  public void switchingBetweenEnglishAndLithuanianKeepsAllLowerKeysInPlace() {
    simulateOnStartInputFlow();
    final var switcher = mAnySoftKeyboardUnderTest.getKeyboardSwitcherForTests();
    final float density = mAnySoftKeyboardUnderTest.getResources().getDisplayMetrics().density;
    for (int widthDp : new int[] {400, 657, 870}) {
      final int width = Math.round(widthDp * density);
      layoutKeyboardAtWidth(width);
      for (boolean spaced : new boolean[] {false, true}) {
        for (boolean split : new boolean[] {false, true}) {
          switcher.setKeyboardGeometry(width, spaced, split);
          final AnyKeyboard english = mAnySoftKeyboardUnderTest.getCurrentAlphabetKeyboard();
          Assert.assertEquals("en", english.getDefaultDictionaryLocale());
          mAnySoftKeyboardUnderTest.simulateKeyPress(KeyCodes.MODE_ALPHABET);
          final AnyKeyboard lithuanian = mAnySoftKeyboardUnderTest.getCurrentAlphabetKeyboard();
          Assert.assertEquals("lt", lithuanian.getDefaultDictionaryLocale());
          Assert.assertEquals(key(english, '1').y, key(lithuanian, 'ą').y);
          final var englishKeys = english.getKeys().stream()
              .filter(key -> key.y >= key(english, 'q').y).toList();
          final var lithuanianKeys = lithuanian.getKeys().stream()
              .filter(key -> key.y >= key(lithuanian, 'q').y).toList();
          Assert.assertEquals(englishKeys.size(), lithuanianKeys.size());
          for (int index = 0; index < englishKeys.size(); index++) {
            final var enKey = englishKeys.get(index);
            final var ltKey = lithuanianKeys.get(index);
            final String layout = widthDp + "dp, spaced=" + spaced + ", split=" + split
                + ", key=" + enKey.getPrimaryCode();
            Assert.assertEquals(layout, enKey.getPrimaryCode(), ltKey.getPrimaryCode());
            Assert.assertEquals(layout, enKey.x, ltKey.x);
            Assert.assertEquals(layout, enKey.y, ltKey.y);
            Assert.assertEquals(layout, enKey.width, ltKey.width);
            Assert.assertEquals(layout, enKey.height, ltKey.height);
          }
          mAnySoftKeyboardUnderTest.simulateKeyPress(KeyCodes.MODE_ALPHABET);
        }
      }
    }
  }

  @Test
  @org.robolectric.annotation.Config(qualifiers = "land")
  public void splitWideLayoutStaysInBoundsAndReturnsToContinuousInPortrait() {
    simulateOnStartInputFlow();
    final KeyboardSwitcher switcher = mAnySoftKeyboardUnderTest.getKeyboardSwitcherForTests();
    final int width = Math.round(
        740 * mAnySoftKeyboardUnderTest.getResources().getDisplayMetrics().density);
    layoutKeyboardAtWidth(width);
    switcher.setKeyboardGeometry(width, true, true);
    final AnyKeyboard split = mAnySoftKeyboardUnderTest.getCurrentAlphabetKeyboard();
    Assert.assertEquals(2, split.getKeys().stream()
        .filter(key -> key.getPrimaryCode() == KeyCodes.SPACE).count());
    Assert.assertEquals(" ", split.getKeys().stream()
        .filter(key -> key.getPrimaryCode() == KeyCodes.SPACE).skip(1)
        .findFirst().orElseThrow().label.toString());
    Assert.assertEquals(KeyCodes.SPACE, split.getKeys().stream()
        .filter(key -> key.getPrimaryCode() == KeyCodes.SPACE).skip(1)
        .findFirst().orElseThrow().getCodeAtIndex(0, true));
    Assert.assertTrue(key(split, 'y').x - Keyboard.Key.getEndX(key(split, 't')) > width / 7);
    Assert.assertTrue(split.getKeys().stream().allMatch(
        key -> key.x >= 0 && Keyboard.Key.getEndX(key) <= width));
    Assert.assertTrue(key(split, KeyCodes.KEYBOARD_MODE_CHANGE).x < key(split, ',').x);

    org.robolectric.RuntimeEnvironment.setQualifiers("+port");
    switcher.setKeyboardGeometry(width, true, true);
    final AnyKeyboard continuous = mAnySoftKeyboardUnderTest.getCurrentAlphabetKeyboard();
    Assert.assertEquals(1, continuous.getKeys().stream()
        .filter(key -> key.getPrimaryCode() == KeyCodes.SPACE).count());
    Assert.assertTrue(key(continuous, 'y').x - Keyboard.Key.getEndX(key(continuous, 't')) < width / 20);
  }

  @Test
  public void lastLanguageSurvivesSwitcherRecreation() {
    AddOnTestUtils.ensureKeyboardAtIndexEnabled(0, true);
    AddOnTestUtils.ensureKeyboardAtIndexEnabled(1, true);
    simulateOnStartInputFlow();
    mAnySoftKeyboardUnderTest.simulateKeyPress(KeyCodes.MODE_ALPHABET);
    String selectedId =
        mAnySoftKeyboardUnderTest.getCurrentAlphabetKeyboard().getKeyboardAddOn().getId();
    com.anysoftkeyboard.keyboards.KeyboardSwitcher.KeyboardSwitchedListener listener =
        org.mockito.Mockito.mock(
            com.anysoftkeyboard.keyboards.KeyboardSwitcher.KeyboardSwitchedListener.class);
    com.anysoftkeyboard.keyboards.KeyboardSwitcher recreated =
        new com.anysoftkeyboard.keyboards.KeyboardSwitcher(listener, getApplicationContext());
    try {
      recreated.setKeyboardMode(com.anysoftkeyboard.keyboards.KeyboardSwitcher.INPUT_MODE_TEXT,
          new EditorInfo(), false);
      org.mockito.ArgumentCaptor<AnyKeyboard> captured =
          org.mockito.ArgumentCaptor.forClass(AnyKeyboard.class);
      org.mockito.Mockito.verify(listener).onAlphabetKeyboardSet(captured.capture());
      Assert.assertEquals(selectedId, captured.getValue().getKeyboardAddOn().getId());
    } finally {
      recreated.destroy();
    }
  }

  @Test
  public void testOnLowMemoryAlphabet() {
    AddOnTestUtils.ensureKeyboardAtIndexEnabled(0, true);
    AddOnTestUtils.ensureKeyboardAtIndexEnabled(1, true);

    simulateOnStartInputFlow();

    // creating all keyboards
    mAnySoftKeyboardUnderTest.simulateKeyPress(KeyCodes.MODE_ALPHABET);
    mAnySoftKeyboardUnderTest.simulateKeyPress(KeyCodes.MODE_ALPHABET);
    mAnySoftKeyboardUnderTest.simulateKeyPress(KeyCodes.MODE_ALPHABET);
    mAnySoftKeyboardUnderTest.simulateKeyPress(KeyCodes.MODE_ALPHABET);
    mAnySoftKeyboardUnderTest.simulateKeyPress(KeyCodes.MODE_ALPHABET);
    mAnySoftKeyboardUnderTest.simulateKeyPress(KeyCodes.MODE_SYMBOLS);
    mAnySoftKeyboardUnderTest.simulateKeyPress(KeyCodes.MODE_SYMBOLS);
    mAnySoftKeyboardUnderTest.simulateKeyPress(KeyCodes.MODE_SYMBOLS);
    mAnySoftKeyboardUnderTest.simulateKeyPress(KeyCodes.MODE_SYMBOLS);
    mAnySoftKeyboardUnderTest.simulateKeyPress(KeyCodes.MODE_SYMBOLS);
    mAnySoftKeyboardUnderTest.simulateKeyPress(KeyCodes.MODE_ALPHABET);

    Assert.assertEquals(
        2,
        mAnySoftKeyboardUnderTest
            .getKeyboardSwitcherForTests()
            .getCachedAlphabetKeyboards()
            .size());
    for (AnyKeyboard keyboard :
        mAnySoftKeyboardUnderTest.getKeyboardSwitcherForTests().getCachedAlphabetKeyboards()) {
      Assert.assertNotNull(keyboard);
    }

    Assert.assertEquals(
        6,
        mAnySoftKeyboardUnderTest.getKeyboardSwitcherForTests().getCachedSymbolsKeyboards().size());
    Assert.assertNotNull(
        mAnySoftKeyboardUnderTest.getKeyboardSwitcherForTests().getCachedSymbolsKeyboards().get(0));
    Assert.assertNotNull(
        mAnySoftKeyboardUnderTest.getKeyboardSwitcherForTests().getCachedSymbolsKeyboards().get(1));
    Assert.assertNotNull(
        mAnySoftKeyboardUnderTest.getKeyboardSwitcherForTests().getCachedSymbolsKeyboards().get(2));
    // special modes keyboards which were not created yet
    Assert.assertNull(
        mAnySoftKeyboardUnderTest.getKeyboardSwitcherForTests().getCachedSymbolsKeyboards().get(3));
    Assert.assertNull(
        mAnySoftKeyboardUnderTest.getKeyboardSwitcherForTests().getCachedSymbolsKeyboards().get(4));
    Assert.assertNull(
        mAnySoftKeyboardUnderTest.getKeyboardSwitcherForTests().getCachedSymbolsKeyboards().get(5));

    mAnySoftKeyboardUnderTest.onLowMemory();

    Assert.assertEquals(
        2,
        mAnySoftKeyboardUnderTest
            .getKeyboardSwitcherForTests()
            .getCachedAlphabetKeyboards()
            .size());
    java.util.List<AnyKeyboard> remaining =
        mAnySoftKeyboardUnderTest.getKeyboardSwitcherForTests().getCachedAlphabetKeyboards();
    Assert.assertEquals(1, remaining.stream().filter(java.util.Objects::nonNull).count());
    Assert.assertTrue(remaining.contains(mAnySoftKeyboardUnderTest.getCurrentAlphabetKeyboard()));

    Assert.assertEquals(
        6,
        mAnySoftKeyboardUnderTest.getKeyboardSwitcherForTests().getCachedSymbolsKeyboards().size());
    Assert.assertNull(
        mAnySoftKeyboardUnderTest.getKeyboardSwitcherForTests().getCachedSymbolsKeyboards().get(0));
    Assert.assertNull(
        mAnySoftKeyboardUnderTest.getKeyboardSwitcherForTests().getCachedSymbolsKeyboards().get(1));
    Assert.assertNull(
        mAnySoftKeyboardUnderTest.getKeyboardSwitcherForTests().getCachedSymbolsKeyboards().get(2));
    // special modes keyboards which were not created yet
    Assert.assertNull(
        mAnySoftKeyboardUnderTest.getKeyboardSwitcherForTests().getCachedSymbolsKeyboards().get(3));
    Assert.assertNull(
        mAnySoftKeyboardUnderTest.getKeyboardSwitcherForTests().getCachedSymbolsKeyboards().get(4));
    Assert.assertNull(
        mAnySoftKeyboardUnderTest.getKeyboardSwitcherForTests().getCachedSymbolsKeyboards().get(5));
  }

  @Test
  public void testOnLowMemorySymbols() {
    AddOnTestUtils.ensureKeyboardAtIndexEnabled(0, true);
    AddOnTestUtils.ensureKeyboardAtIndexEnabled(1, true);

    simulateOnStartInputFlow();

    // creating all keyboards
    mAnySoftKeyboardUnderTest.simulateKeyPress(KeyCodes.MODE_ALPHABET);
    mAnySoftKeyboardUnderTest.simulateKeyPress(KeyCodes.MODE_ALPHABET);
    mAnySoftKeyboardUnderTest.simulateKeyPress(KeyCodes.MODE_ALPHABET);
    mAnySoftKeyboardUnderTest.simulateKeyPress(KeyCodes.MODE_ALPHABET);
    mAnySoftKeyboardUnderTest.simulateKeyPress(KeyCodes.MODE_ALPHABET);
    mAnySoftKeyboardUnderTest.simulateKeyPress(KeyCodes.MODE_SYMBOLS);
    mAnySoftKeyboardUnderTest.simulateKeyPress(KeyCodes.MODE_SYMBOLS);
    mAnySoftKeyboardUnderTest.simulateKeyPress(KeyCodes.MODE_SYMBOLS);
    mAnySoftKeyboardUnderTest.simulateKeyPress(KeyCodes.MODE_SYMBOLS);
    mAnySoftKeyboardUnderTest.simulateKeyPress(KeyCodes.MODE_SYMBOLS);

    Assert.assertEquals(
        2,
        mAnySoftKeyboardUnderTest
            .getKeyboardSwitcherForTests()
            .getCachedAlphabetKeyboards()
            .size());
    for (AnyKeyboard keyboard :
        mAnySoftKeyboardUnderTest.getKeyboardSwitcherForTests().getCachedAlphabetKeyboards()) {
      Assert.assertNotNull(keyboard);
    }

    Assert.assertEquals(
        6,
        mAnySoftKeyboardUnderTest.getKeyboardSwitcherForTests().getCachedSymbolsKeyboards().size());
    Assert.assertNotNull(
        mAnySoftKeyboardUnderTest.getKeyboardSwitcherForTests().getCachedSymbolsKeyboards().get(0));
    Assert.assertNotNull(
        mAnySoftKeyboardUnderTest.getKeyboardSwitcherForTests().getCachedSymbolsKeyboards().get(1));
    Assert.assertNotNull(
        mAnySoftKeyboardUnderTest.getKeyboardSwitcherForTests().getCachedSymbolsKeyboards().get(2));
    // special modes keyboards which were not created yet
    Assert.assertNull(
        mAnySoftKeyboardUnderTest.getKeyboardSwitcherForTests().getCachedSymbolsKeyboards().get(3));
    Assert.assertNull(
        mAnySoftKeyboardUnderTest.getKeyboardSwitcherForTests().getCachedSymbolsKeyboards().get(4));
    Assert.assertNull(
        mAnySoftKeyboardUnderTest.getKeyboardSwitcherForTests().getCachedSymbolsKeyboards().get(5));

    mAnySoftKeyboardUnderTest.onLowMemory();

    Assert.assertEquals(
        2,
        mAnySoftKeyboardUnderTest
            .getKeyboardSwitcherForTests()
            .getCachedAlphabetKeyboards()
            .size());
    java.util.List<AnyKeyboard> remaining =
        mAnySoftKeyboardUnderTest.getKeyboardSwitcherForTests().getCachedAlphabetKeyboards();
    Assert.assertEquals(1, remaining.stream().filter(java.util.Objects::nonNull).count());
    Assert.assertTrue(remaining.contains(mAnySoftKeyboardUnderTest.getCurrentAlphabetKeyboard()));

    Assert.assertEquals(
        6,
        mAnySoftKeyboardUnderTest.getKeyboardSwitcherForTests().getCachedSymbolsKeyboards().size());
    Assert.assertNull(
        mAnySoftKeyboardUnderTest.getKeyboardSwitcherForTests().getCachedSymbolsKeyboards().get(0));
    Assert.assertNotNull(
        mAnySoftKeyboardUnderTest.getKeyboardSwitcherForTests().getCachedSymbolsKeyboards().get(1));
    Assert.assertNull(
        mAnySoftKeyboardUnderTest.getKeyboardSwitcherForTests().getCachedSymbolsKeyboards().get(2));
    // special modes keyboards which were not created yet
    Assert.assertNull(
        mAnySoftKeyboardUnderTest.getKeyboardSwitcherForTests().getCachedSymbolsKeyboards().get(3));
    Assert.assertNull(
        mAnySoftKeyboardUnderTest.getKeyboardSwitcherForTests().getCachedSymbolsKeyboards().get(4));
    Assert.assertNull(
        mAnySoftKeyboardUnderTest.getKeyboardSwitcherForTests().getCachedSymbolsKeyboards().get(5));
  }

  @Test
  public void testForceRecreateKeyboardOnSettingKeyboardView() {
    final EditorInfo editorInfo = TestableAnySoftKeyboard.createEditorInfoTextWithSuggestions();
    mAnySoftKeyboardUnderTest.onStartInput(editorInfo, false);
    mAnySoftKeyboardUnderTest.onCreateInputView();
    mAnySoftKeyboardUnderTest.getKeyboardSwitcherForTests().verifyNewViewSet();
    mAnySoftKeyboardUnderTest.getKeyboardSwitcherForTests().verifyKeyboardsFlushed();
  }

  @Test
  public void testCreatedPhoneKeyboard() {
    final EditorInfo editorInfo =
        TestableAnySoftKeyboard.createEditorInfo(
            EditorInfo.IME_ACTION_NONE, EditorInfo.TYPE_CLASS_PHONE);
    mAnySoftKeyboardUnderTest.onStartInput(editorInfo, false);
    mAnySoftKeyboardUnderTest.onCreateInputView();
    mAnySoftKeyboardUnderTest.onStartInputView(editorInfo, false);

    Assert.assertEquals(
        "phone_symbols_keyboard",
        mAnySoftKeyboardUnderTest.getCurrentKeyboardForTests().getKeyboardId().toString());
    Assert.assertEquals(
        KeyboardSwitcher.INPUT_MODE_PHONE,
        mAnySoftKeyboardUnderTest.getKeyboardSwitcherForTests().getInputModeId());
  }

  @Test
  public void testCreatedDateTimeKeyboard() {
    final EditorInfo editorInfo =
        TestableAnySoftKeyboard.createEditorInfo(
            EditorInfo.IME_ACTION_NONE, EditorInfo.TYPE_CLASS_DATETIME);
    mAnySoftKeyboardUnderTest.onStartInput(editorInfo, false);
    mAnySoftKeyboardUnderTest.onCreateInputView();
    mAnySoftKeyboardUnderTest.onStartInputView(editorInfo, false);

    Assert.assertEquals(
        "datetime_symbols_keyboard",
        mAnySoftKeyboardUnderTest.getCurrentKeyboardForTests().getKeyboardId().toString());
    Assert.assertEquals(
        KeyboardSwitcher.INPUT_MODE_DATETIME,
        mAnySoftKeyboardUnderTest.getKeyboardSwitcherForTests().getInputModeId());
  }

  @Test
  public void testCreatedNumbersKeyboard() {
    final EditorInfo editorInfo =
        TestableAnySoftKeyboard.createEditorInfo(
            EditorInfo.IME_ACTION_NONE, EditorInfo.TYPE_CLASS_NUMBER);
    mAnySoftKeyboardUnderTest.onStartInput(editorInfo, true);
    mAnySoftKeyboardUnderTest.onCreateInputView();
    mAnySoftKeyboardUnderTest.onStartInputView(editorInfo, true);

    Assert.assertEquals(
        "numbers_symbols_keyboard",
        mAnySoftKeyboardUnderTest.getCurrentKeyboardForTests().getKeyboardId().toString());
    Assert.assertEquals(
        KeyboardSwitcher.INPUT_MODE_NUMBERS,
        mAnySoftKeyboardUnderTest.getKeyboardSwitcherForTests().getInputModeId());
  }

  @Test
  public void testCreatedTextInputKeyboard() {
    final EditorInfo editorInfo = TestableAnySoftKeyboard.createEditorInfoTextWithSuggestions();
    mAnySoftKeyboardUnderTest.onStartInput(editorInfo, true);
    mAnySoftKeyboardUnderTest.onCreateInputView();
    mAnySoftKeyboardUnderTest.onStartInputView(editorInfo, true);

    Assert.assertEquals(
        KeyboardSwitcher.INPUT_MODE_TEXT,
        mAnySoftKeyboardUnderTest.getKeyboardSwitcherForTests().getInputModeId());
  }

  @Test
  public void testCreatedEmailTextInputKeyboard() {
    final EditorInfo editorInfo =
        TestableAnySoftKeyboard.createEditorInfo(
            EditorInfo.IME_ACTION_NONE,
            EditorInfo.TYPE_CLASS_TEXT + EditorInfo.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
    mAnySoftKeyboardUnderTest.onStartInput(editorInfo, true);
    mAnySoftKeyboardUnderTest.onCreateInputView();
    mAnySoftKeyboardUnderTest.onStartInputView(editorInfo, true);

    Assert.assertEquals(
        KeyboardSwitcher.INPUT_MODE_EMAIL,
        mAnySoftKeyboardUnderTest.getKeyboardSwitcherForTests().getInputModeId());
  }

  @Test
  public void testCreatedPasswordTextInputKeyboard() {
    mAnySoftKeyboardUnderTest.onFinishInputView(true);
    mAnySoftKeyboardUnderTest.onFinishInput();

    final EditorInfo editorInfo =
        TestableAnySoftKeyboard.createEditorInfo(
            EditorInfo.IME_ACTION_NONE,
            EditorInfo.TYPE_CLASS_TEXT + EditorInfo.TYPE_TEXT_VARIATION_PASSWORD);
    mAnySoftKeyboardUnderTest.onStartInput(editorInfo, false);
    mAnySoftKeyboardUnderTest.onStartInputView(editorInfo, true);

    // just a normal text keyboard
    Assert.assertEquals(
        KeyboardSwitcher.INPUT_MODE_TEXT,
        mAnySoftKeyboardUnderTest.getKeyboardSwitcherForTests().getInputModeId());
    // with password row mode
    Assert.assertEquals(
        KEYBOARD_ROW_MODE_PASSWORD,
        mAnySoftKeyboardUnderTest.getCurrentKeyboardForTests().getKeyboardMode());
  }

  private void verifyMaskedKeyboardRow(
      @Keyboard.KeyboardRowModeId int modeId, int inputModeId, int variant) {
    SharedPrefsHelper.setPrefsValue(Keyboard.getPrefKeyForEnabledRowMode(modeId), false);

    mAnySoftKeyboardUnderTest.onFinishInputView(true);
    mAnySoftKeyboardUnderTest.onFinishInput();

    final EditorInfo editorInfo =
        TestableAnySoftKeyboard.createEditorInfo(
            EditorInfo.IME_ACTION_NONE, EditorInfo.TYPE_CLASS_TEXT + variant);
    mAnySoftKeyboardUnderTest.onStartInput(editorInfo, false);
    mAnySoftKeyboardUnderTest.onStartInputView(editorInfo, true);

    // just a normal text keyboard
    Assert.assertEquals(
        inputModeId, mAnySoftKeyboardUnderTest.getKeyboardSwitcherForTests().getInputModeId());
    // with NORMAL row mode, since the pref is false
    Assert.assertEquals(
        KEYBOARD_ROW_MODE_NORMAL,
        mAnySoftKeyboardUnderTest.getCurrentKeyboardForTests().getKeyboardMode());
  }

  @Test
  public void testCreatedNormalTextInputKeyboardWhenPasswordFieldButOptionDisabled() {
    verifyMaskedKeyboardRow(
        KEYBOARD_ROW_MODE_PASSWORD,
        KeyboardSwitcher.INPUT_MODE_TEXT,
        EditorInfo.TYPE_TEXT_VARIATION_PASSWORD);
  }

  @Test
  public void
      testCreatedNormalTextInputKeyboardWhenPasswordFieldButOptionDisabledVisiblePassword() {
    verifyMaskedKeyboardRow(
        KEYBOARD_ROW_MODE_PASSWORD,
        KeyboardSwitcher.INPUT_MODE_TEXT,
        EditorInfo.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
  }

  @Test
  public void testCreatedNormalTextInputKeyboardWhenPasswordFieldButOptionDisabledWeb() {
    verifyMaskedKeyboardRow(
        KEYBOARD_ROW_MODE_PASSWORD,
        KeyboardSwitcher.INPUT_MODE_TEXT,
        EditorInfo.TYPE_TEXT_VARIATION_WEB_PASSWORD);
  }

  @Test
  public void testCreatedNormalTextInputKeyboardWhenUrlFieldButOptionDisabled() {
    verifyMaskedKeyboardRow(
        KEYBOARD_ROW_MODE_URL, KeyboardSwitcher.INPUT_MODE_URL, EditorInfo.TYPE_TEXT_VARIATION_URI);
  }

  @Test
  public void testCreatedNormalTextInputKeyboardWhenEmailAddressFieldButOptionDisabled() {
    verifyMaskedKeyboardRow(
        KEYBOARD_ROW_MODE_EMAIL,
        KeyboardSwitcher.INPUT_MODE_EMAIL,
        EditorInfo.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
  }

  @Test
  public void testCreatedNormalTextInputKeyboardWhenWebEmailAddressFieldButOptionDisabled() {
    verifyMaskedKeyboardRow(
        KEYBOARD_ROW_MODE_EMAIL,
        KeyboardSwitcher.INPUT_MODE_EMAIL,
        EditorInfo.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS);
  }

  @Test
  public void testCreatedNormalTextInputKeyboardWhenShortMessageFieldButOptionDisabled() {
    verifyMaskedKeyboardRow(
        KEYBOARD_ROW_MODE_IM,
        KeyboardSwitcher.INPUT_MODE_IM,
        EditorInfo.TYPE_TEXT_VARIATION_SHORT_MESSAGE);
  }

  @Test
  public void
      testCreatedNormalTextInputKeyboardWhenShortMessageFieldButOptionDisabledEmailSubject() {
    verifyMaskedKeyboardRow(
        KEYBOARD_ROW_MODE_IM,
        KeyboardSwitcher.INPUT_MODE_TEXT,
        EditorInfo.TYPE_TEXT_VARIATION_EMAIL_SUBJECT);
  }

  @Test
  public void
      testCreatedNormalTextInputKeyboardWhenShortMessageFieldButOptionDisabledLongMessage() {
    verifyMaskedKeyboardRow(
        KEYBOARD_ROW_MODE_IM,
        KeyboardSwitcher.INPUT_MODE_TEXT,
        EditorInfo.TYPE_TEXT_VARIATION_LONG_MESSAGE);
  }

  @Test
  public void testKeyboardsRecycledOnPasswordRowSupportPrefChange() {
    mAnySoftKeyboardUnderTest
        .getKeyboardSwitcherForTests()
        .verifyKeyboardsFlushed(); // initial. It will reset flush state

    SharedPrefsHelper.setPrefsValue(
        Keyboard.getPrefKeyForEnabledRowMode(Keyboard.KEYBOARD_ROW_MODE_EMAIL), false);
    mAnySoftKeyboardUnderTest.getKeyboardSwitcherForTests().verifyKeyboardsFlushed();

    SharedPrefsHelper.setPrefsValue(
        Keyboard.getPrefKeyForEnabledRowMode(Keyboard.KEYBOARD_ROW_MODE_EMAIL), true);
    mAnySoftKeyboardUnderTest.getKeyboardSwitcherForTests().verifyKeyboardsFlushed();

    // same value
    SharedPrefsHelper.setPrefsValue(
        Keyboard.getPrefKeyForEnabledRowMode(Keyboard.KEYBOARD_ROW_MODE_EMAIL), true);
    mAnySoftKeyboardUnderTest.getKeyboardSwitcherForTests().verifyKeyboardsNotFlushed();
  }

  @Test
  public void testForceMakeKeyboardsOnOrientationChange() {
    mAnySoftKeyboardUnderTest
        .getKeyboardSwitcherForTests()
        .verifyKeyboardsFlushed(); // initial. It will reset flush state

    Assert.assertFalse(mAnySoftKeyboardUnderTest.isKeyboardViewHidden());
    final Configuration configuration = getApplicationContext().getResources().getConfiguration();
    configuration.orientation = Configuration.ORIENTATION_LANDSCAPE;
    mAnySoftKeyboardUnderTest.onConfigurationChanged(configuration);
    mAnySoftKeyboardUnderTest.getKeyboardSwitcherForTests().verifyKeyboardsFlushed();
    // same orientation
    configuration.orientation = Configuration.ORIENTATION_LANDSCAPE;
    mAnySoftKeyboardUnderTest.onConfigurationChanged(configuration);
    mAnySoftKeyboardUnderTest.getKeyboardSwitcherForTests().verifyKeyboardsFlushed();
  }

  @Test
  public void testForceMakeKeyboardsOnAddOnsPrefChange() {
    AddOnTestUtils.ensureKeyboardAtIndexEnabled(1, true);
    mAnySoftKeyboardUnderTest.getKeyboardSwitcherForTests().verifyKeyboardsFlushed();
    mAnySoftKeyboardUnderTest.getKeyboardSwitcherForTests().verifyNewViewSet();
    Mockito.reset(mAnySoftKeyboardUnderTest.getSuggest());
    SharedPrefsHelper.setPrefsValue("keyboard_some-id_override_dictionary", "dictionary_id");
    mAnySoftKeyboardUnderTest.getKeyboardSwitcherForTests().verifyKeyboardsNotFlushed();
    mAnySoftKeyboardUnderTest.getKeyboardSwitcherForTests().verifyNewViewNotSet();
    Mockito.verify(mAnySoftKeyboardUnderTest.getSuggest()).resetNextWordSentence();
    // no UI, no setup of suggestions dictionaries
    Mockito.verify(mAnySoftKeyboardUnderTest.getSuggest(), Mockito.never())
        .setupSuggestionsForKeyboard(Mockito.anyList(), Mockito.any());
    Mockito.reset(mAnySoftKeyboardUnderTest.getSuggest());
    AnyApplication.getQuickTextKeyFactory(getApplicationContext())
        .setAddOnEnabled(
            AnyApplication.getQuickTextKeyFactory(getApplicationContext())
                .getAllAddOns()
                .get(1)
                .getId(),
            true);
    mAnySoftKeyboardUnderTest.getKeyboardSwitcherForTests().verifyKeyboardsFlushed();
    mAnySoftKeyboardUnderTest.getKeyboardSwitcherForTests().verifyNewViewNotSet();
    AnyApplication.getTopRowFactory(getApplicationContext())
        .setAddOnEnabled(
            AnyApplication.getTopRowFactory(getApplicationContext()).getAllAddOns().get(1).getId(),
            true);
    mAnySoftKeyboardUnderTest.getKeyboardSwitcherForTests().verifyKeyboardsFlushed();
    mAnySoftKeyboardUnderTest.getKeyboardSwitcherForTests().verifyNewViewNotSet();
    AnyApplication.getBottomRowFactory(getApplicationContext())
        .setAddOnEnabled(
            AnyApplication.getBottomRowFactory(getApplicationContext())
                .getAllAddOns()
                .get(1)
                .getId(),
            true);
    mAnySoftKeyboardUnderTest.getKeyboardSwitcherForTests().verifyKeyboardsFlushed();
    mAnySoftKeyboardUnderTest.getKeyboardSwitcherForTests().verifyNewViewNotSet();
    SharedPrefsHelper.setPrefsValue(
        getApplicationContext().getString(R.string.settings_key_always_hide_language_key), true);
    mAnySoftKeyboardUnderTest.getKeyboardSwitcherForTests().verifyKeyboardsFlushed();
    mAnySoftKeyboardUnderTest.getKeyboardSwitcherForTests().verifyNewViewNotSet();

    // sanity
    SharedPrefsHelper.setPrefsValue("random", "dummy");
    mAnySoftKeyboardUnderTest.getKeyboardSwitcherForTests().verifyKeyboardsNotFlushed();
    mAnySoftKeyboardUnderTest.getKeyboardSwitcherForTests().verifyNewViewNotSet();
  }
}
