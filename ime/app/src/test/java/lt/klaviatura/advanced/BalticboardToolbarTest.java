package lt.klaviatura.advanced;
import com.anysoftkeyboard.AnySoftKeyboardRobolectricTestRunner;
import android.widget.*;
import android.view.*;
import com.menny.android.anysoftkeyboard.R;
import org.junit.*;
import org.junit.runner.RunWith;
@RunWith(AnySoftKeyboardRobolectricTestRunner.class)
public class BalticboardToolbarTest {
  @Test public void translateIsIndependentlyEnabledAfterRewrite() {
    int[] calls = {0};
    android.content.Context context = androidx.test.core.app.ApplicationProvider.getApplicationContext();
    AdvancedLtToolbarAction action = new AdvancedLtToolbarAction(()->{}, ()->calls[0]++, ()->{}, ()->{}, ()->{}, ()->{});
    FrameLayout root = (FrameLayout) action.inflateActionView(new FrameLayout(context));
    LinearLayout left = (LinearLayout) root.getChildAt(0);
    Assert.assertEquals(context.getString(R.string.advanced_lt_toolbar_translate), left.getChildAt(1).getContentDescription());
    Assert.assertEquals(View.GONE, left.getChildAt(1).getVisibility());
    action.setAiEnabled(false);
    action.setTranslateEnabled(true);
    Assert.assertEquals(View.GONE, left.getChildAt(0).getVisibility());
    Assert.assertEquals(View.VISIBLE, left.getChildAt(1).getVisibility());
    left.getChildAt(1).performClick();
    Assert.assertEquals(1, calls[0]);
  }
  @Test public void toolbarOrderIsAiEmojiClipboard() {
    android.content.Context context=androidx.test.core.app.ApplicationProvider.getApplicationContext();
    AdvancedLtToolbarAction action=new AdvancedLtToolbarAction(()->{},()->{},()->{},()->{},()->{});
    FrameLayout root=(FrameLayout)action.inflateActionView(new FrameLayout(context));
    LinearLayout left=(LinearLayout)root.getChildAt(0);
    Assert.assertEquals(context.getString(R.string.advanced_lt_toolbar_ai),left.getChildAt(0).getContentDescription());
    Assert.assertEquals(context.getString(R.string.advanced_lt_toolbar_emoji),left.getChildAt(1).getContentDescription());
    Assert.assertEquals(context.getString(R.string.advanced_lt_toolbar_clipboard),left.getChildAt(2).getContentDescription());
    action.setAiEnabled(false);
    Assert.assertEquals(View.GONE,left.getChildAt(0).getVisibility());
    Assert.assertEquals(View.VISIBLE,left.getChildAt(1).getVisibility());
    action.onOtherActionVisibilityChanged(true);
    Assert.assertEquals(View.GONE,root.getVisibility());
    action.setVisible(true);
    Assert.assertEquals(View.GONE,root.getVisibility());
    action.onOtherActionVisibilityChanged(false);
    Assert.assertEquals(View.VISIBLE,root.getVisibility());
    action.onRemoved();
  }
}
