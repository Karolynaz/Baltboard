package lt.klaviatura.advanced;

import android.app.Activity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.PopupWindow;
import android.widget.ScrollView;
import com.anysoftkeyboard.AnySoftKeyboardRobolectricTestRunner;
import com.anysoftkeyboard.keyboards.views.KeyboardViewContainerView;
import com.menny.android.anysoftkeyboard.R;
import java.util.ArrayList;
import java.util.Arrays;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.Robolectric;
import org.robolectric.util.ReflectionHelpers;

@RunWith(AnySoftKeyboardRobolectricTestRunner.class)
public class AdvancedLtSuggestionActionsTest {
  @Test
  public void morePopupDoesNotStealEditorFocusAndStillPicksSuggestions() {
    try (org.robolectric.android.controller.ActivityController<Activity> controller =
        Robolectric.buildActivity(Activity.class).setup().visible()) {
      Activity activity = controller.get();
      FrameLayout root = new FrameLayout(activity);
      activity.setContentView(root);
      KeyboardViewContainerView container = Mockito.mock(KeyboardViewContainerView.class);
      Mockito.when(container.getContext()).thenReturn(activity);
      Mockito.when(container.getResources()).thenReturn(activity.getResources());
      Mockito.when(container.getWidth()).thenReturn(600);
      Mockito.doAnswer(invocation -> {
        KeyboardViewContainerView.StripActionProvider provider = invocation.getArgument(0);
        root.addView(provider.inflateActionView(root));
        return null;
      }).when(container).addStripAction(Mockito.any(), Mockito.anyBoolean());
      ArrayList<CharSequence> picked = new ArrayList<>();
      AdvancedLtSuggestionActions actions = new AdvancedLtSuggestionActions(
          () -> {}, (index, text) -> picked.add(text));
      actions.attach(container);
      actions.setSuggestions(Arrays.asList("mok", "mokykla", "mokyklos", "mokyti"));
      ArrayList<View> buttons = new ArrayList<>();
      root.findViewsWithText(buttons, activity.getString(R.string.advanced_lt_suggestions_more),
          View.FIND_VIEWS_WITH_CONTENT_DESCRIPTION);
      buttons.get(0).performClick();
      PopupWindow popup = ReflectionHelpers.getField(actions, "mPopup");
      Assert.assertTrue(popup.isShowing());
      Assert.assertFalse(popup.isFocusable());
      Assert.assertEquals(PopupWindow.INPUT_METHOD_NOT_NEEDED, popup.getInputMethodMode());
      GridLayout grid = (GridLayout)((ScrollView)popup.getContentView()).getChildAt(0);
      grid.getChildAt(1).performClick();
      Assert.assertEquals(Arrays.asList("mokykla"), picked);
      Assert.assertFalse(popup.isShowing());
      Assert.assertTrue(buttons.get(0).getBackground() instanceof android.graphics.drawable.GradientDrawable);
      actions.setSpecialAction(true);
      Assert.assertEquals(View.GONE,buttons.get(0).getVisibility());
      ArrayList<View> back = new ArrayList<>();
      root.findViewsWithText(back,activity.getString(R.string.advanced_lt_suggestions_back),View.FIND_VIEWS_WITH_CONTENT_DESCRIPTION);
      Assert.assertEquals(View.VISIBLE,back.get(0).getVisibility());
      actions.setSpecialAction(false);
      Assert.assertEquals(View.VISIBLE,buttons.get(0).getVisibility());
      actions.detach();
    }
  }
}
