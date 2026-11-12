package lt.klaviatura.advanced;

import android.app.Activity;
import android.os.Looper;
import android.view.MotionEvent;
import android.widget.FrameLayout;
import com.anysoftkeyboard.AnySoftKeyboardRobolectricTestRunner;
import com.anysoftkeyboard.api.KeyCodes;
import com.anysoftkeyboard.keyboards.views.OnKeyboardActionListener;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.Robolectric;
import org.robolectric.Shadows;
import java.util.concurrent.TimeUnit;

@RunWith(AnySoftKeyboardRobolectricTestRunner.class)
public class AdvancedLtBackspaceButtonTest {
  @Test public void holdRepeatsAndReleaseStops() {
    try (org.robolectric.android.controller.ActivityController<Activity> controller = Robolectric.buildActivity(Activity.class).setup()) {
      Activity activity = controller.get();
      FrameLayout parent = new FrameLayout(activity);
      activity.setContentView(parent);
      AdvancedLtBackspaceButton button = new AdvancedLtBackspaceButton(activity);
      parent.addView(button, new FrameLayout.LayoutParams(56,56));
      button.layout(0,0,56,56);
      org.junit.Assert.assertEquals(-5f, button.getTranslationX(), 0f);
      OnKeyboardActionListener listener = Mockito.mock(OnKeyboardActionListener.class);
      button.bind(listener);
      touch(button, MotionEvent.ACTION_DOWN);
      Mockito.verify(listener).onPress(KeyCodes.DELETE);
      Mockito.verify(listener).onKey(KeyCodes.DELETE, null, 0, null, true);
      Shadows.shadowOf(Looper.getMainLooper()).idleFor(460, TimeUnit.MILLISECONDS);
      Mockito.verify(listener, Mockito.atLeast(2)).onKey(KeyCodes.DELETE, null, 0, null, true);
      touch(button, MotionEvent.ACTION_UP);
      Mockito.verify(listener).onRelease(KeyCodes.DELETE);
      Mockito.clearInvocations(listener);
      Shadows.shadowOf(Looper.getMainLooper()).idleFor(1000, TimeUnit.MILLISECONDS);
      Mockito.verifyZeroInteractions(listener);
      touch(button, MotionEvent.ACTION_DOWN);
      parent.removeView(button);
      Mockito.clearInvocations(listener);
      Shadows.shadowOf(Looper.getMainLooper()).idleFor(1000, TimeUnit.MILLISECONDS);
      Mockito.verifyZeroInteractions(listener);
    }
  }
  @Test
  @org.robolectric.annotation.Config(sdk = 28)
  public void idleButtonHasFilledGrayBackground() {
    AdvancedLtBackspaceButton button = new AdvancedLtBackspaceButton(
        org.robolectric.RuntimeEnvironment.getApplication());
    android.graphics.drawable.GradientDrawable background =
        (android.graphics.drawable.GradientDrawable)button.getBackground().getCurrent();
    org.junit.Assert.assertEquals(0xff444444, background.getColor().getDefaultColor());
  }
  private static void touch(AdvancedLtBackspaceButton button, int action) {
    MotionEvent event = MotionEvent.obtain(0,0,action,20,20,0);
    button.onTouchEvent(event);
    event.recycle();
  }
}
