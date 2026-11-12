package lt.klaviatura.advanced;

import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import com.anysoftkeyboard.AnySoftKeyboardRobolectricTestRunner;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

@RunWith(AnySoftKeyboardRobolectricTestRunner.class)
public class AdvancedLtToolbarActionTest {

  @Test
  public void toolbarTintFollowsWhiteAndGrayThemes() {
    final android.content.Context context = RuntimeEnvironment.getApplication();
    final com.anysoftkeyboard.theme.KeyboardThemeFactory factory =
        com.menny.android.anysoftkeyboard.AnyApplication.getKeyboardThemeFactory(context);
    factory.setAddOnEnabled("18c558ef-bc8c-433a-a36e-92c3ca3be4dd", true);
    final AdvancedLtToolbarAction action =
        new AdvancedLtToolbarAction(() -> {}, () -> {}, () -> {}, () -> {}, () -> {});
    final View toolbar = action.inflateActionView(new FrameLayout(context));
    assertTint(toolbar, android.graphics.Color.rgb(51, 51, 51));
    factory.setAddOnEnabled("8774f99e-fb4a-49fa-b8d0-4083f762250a", true);
    org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
    assertTint(toolbar, android.graphics.Color.WHITE);
    action.onRemoved();
  }

  private static void assertTint(View view, int color) {
    if (view instanceof ImageButton) {
      Assert.assertEquals(color, ((ImageButton) view).getImageTintList().getDefaultColor());
    } else if (view instanceof android.view.ViewGroup) {
      android.view.ViewGroup group = (android.view.ViewGroup) view;
      for (int i = 0; i < group.getChildCount(); i++) assertTint(group.getChildAt(i), color);
    }
  }

  @Test
  public void undoButtonRunsUndoAction() {
    java.util.concurrent.atomic.AtomicInteger undoCount = new java.util.concurrent.atomic.AtomicInteger();
    final AdvancedLtToolbarAction action =
        new AdvancedLtToolbarAction(() -> {}, () -> {}, () -> {}, () -> {}, undoCount::incrementAndGet);
    final FrameLayout parent = new FrameLayout(RuntimeEnvironment.getApplication());
    final View toolbar = action.inflateActionView(parent);
    java.util.ArrayList<View> matches = new java.util.ArrayList<>();
    toolbar.findViewsWithText(matches,
        parent.getContext().getString(com.menny.android.anysoftkeyboard.R.string.advanced_lt_toolbar_undo),
        View.FIND_VIEWS_WITH_CONTENT_DESCRIPTION);
    Assert.assertEquals(1, matches.size());
    matches.get(0).performClick();
    Assert.assertEquals(1, undoCount.get());
  }

  @Test
  public void aiButtonVisibilityFollowsSetting() {
    final AdvancedLtToolbarAction action =
        new AdvancedLtToolbarAction(() -> {}, () -> {}, () -> {}, () -> {}, () -> {});
    final FrameLayout parent = new FrameLayout(RuntimeEnvironment.getApplication());
    final View toolbar = action.inflateActionView(parent);

    java.util.ArrayList<View> matches = new java.util.ArrayList<>();
    toolbar.findViewsWithText(matches, toolbar.getResources().getString(com.menny.android.anysoftkeyboard.R.string.advanced_lt_toolbar_ai),
        View.FIND_VIEWS_WITH_CONTENT_DESCRIPTION);
    final ImageButton aiButton = (ImageButton)matches.get(0);
    Assert.assertNotNull(aiButton);
    Assert.assertEquals(View.VISIBLE, aiButton.getVisibility());

    action.setAiEnabled(false);
    Assert.assertEquals(View.GONE, aiButton.getVisibility());

    action.setAiEnabled(true);
    Assert.assertEquals(View.VISIBLE, aiButton.getVisibility());
  }

  @Test
  public void pasteSuggestionKeepsAiAvailableAndDismissesWithoutPasting() {
    final java.util.concurrent.atomic.AtomicInteger pasteCount = new java.util.concurrent.atomic.AtomicInteger();
    final AdvancedLtToolbarAction action =
        new AdvancedLtToolbarAction(() -> {}, () -> {}, () -> {}, () -> {}, () -> {});
    final FrameLayout root =
        (FrameLayout) action.inflateActionView(new FrameLayout(RuntimeEnvironment.getApplication()));
    final android.widget.LinearLayout icons = (android.widget.LinearLayout) root.getChildAt(0);
    final ImageButton emoji = (ImageButton) icons.getChildAt(1);
    final ImageButton ai = (ImageButton) icons.getChildAt(0);
    final ImageButton clipboard = (ImageButton) icons.getChildAt(2);
    final FrameLayout chip = (FrameLayout) root.getChildAt(3);
    final android.widget.TextView preview = (android.widget.TextView) chip.getChildAt(0);
    final ImageButton dismiss = (ImageButton) chip.getChildAt(1);

    action.setClipboardHint("Copied words", pasteCount::incrementAndGet,
        () -> action.setClipboardHint(null, null, null));
    Assert.assertEquals(View.VISIBLE, ai.getVisibility());
    Assert.assertEquals(View.VISIBLE, emoji.getVisibility());
    Assert.assertEquals(View.GONE, clipboard.getVisibility());
    Assert.assertEquals(View.VISIBLE, chip.getVisibility());
    Assert.assertEquals(View.VISIBLE, dismiss.getVisibility());
    Assert.assertEquals("Copied words", preview.getText().toString());
    final int width = Math.round(360 * root.getResources().getDisplayMetrics().density);
    final int height = Math.round(52 * root.getResources().getDisplayMetrics().density);
    root.measure(
        View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
        View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY));
    root.layout(0, 0, width, height);
    Assert.assertTrue(emoji.getRight() + icons.getLeft() <= chip.getLeft());
    final int longWidth = chip.getWidth();
    action.setClipboardHint("Hi", pasteCount::incrementAndGet,
        () -> action.setClipboardHint(null, null, null));
    root.measure(
        View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
        View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY));
    root.layout(0, 0, width, height);
    Assert.assertTrue(chip.getWidth() < longWidth);
    dismiss.performClick();
    Assert.assertEquals(0, pasteCount.get());
    Assert.assertEquals(View.GONE, chip.getVisibility());
    Assert.assertEquals(View.VISIBLE, clipboard.getVisibility());
    Assert.assertEquals(View.VISIBLE, ai.getVisibility());
    Assert.assertEquals(View.VISIBLE, emoji.getVisibility());
  }

  private ImageButton findFirstImageButton(View view) {
    if (view instanceof ImageButton) return (ImageButton) view;
    if (view instanceof android.view.ViewGroup) {
      final android.view.ViewGroup group = (android.view.ViewGroup) view;
      for (int index = 0; index < group.getChildCount(); index++) {
        final ImageButton found = findFirstImageButton(group.getChildAt(index));
        if (found != null) return found;
      }
    }
    return null;
  }
}
