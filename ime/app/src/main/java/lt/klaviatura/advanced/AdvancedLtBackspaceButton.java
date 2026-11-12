package lt.klaviatura.advanced;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import com.anysoftkeyboard.api.KeyCodes;
import com.anysoftkeyboard.keyboards.views.OnKeyboardActionListener;
import com.anysoftkeyboard.prefs.DirectBootAwareSharedPreferences;
import com.menny.android.anysoftkeyboard.R;

/** Uses the IME's regular DELETE path, including its haptics and deletion acceleration. */
public final class AdvancedLtBackspaceButton extends ImageButton {
  private OnKeyboardActionListener mListener;
  private boolean mHolding;
  private int mPointerId = -1;
  private final Runnable mRepeat = new Runnable() {
    @Override public void run() {
      if (!mHolding || mListener == null || !isShown() || !isEnabled()) { stop(); return; }
      sendDelete();
      if (mHolding) postDelayed(this, 50);
    }
  };
  public AdvancedLtBackspaceButton(Context context) {
    super(context);
    setId(R.id.advanced_lt_floating_backspace);
    setImageResource(R.drawable.advanced_lt_floating_backspace_icon);
    setContentDescription(context.getString(R.string.swipe_action_name_backspace));
    int padding = dp(14);
    setPadding(padding,padding,padding,padding);
    setScaleType(ScaleType.FIT_CENTER);
    setElevation(dp(6));
    setTranslationX(-5f);
    refreshTheme();
  }
  public void bind(OnKeyboardActionListener listener) { stop(); mListener = listener; }
  public void refreshTheme() {
    AdvancedLtTheme.tintContents(this);
    android.graphics.drawable.GradientDrawable background = AdvancedLtTheme.rounded(getContext(), false);
    background.setCornerRadius(dp(28));
    background.setColor(AdvancedLtTheme.isDay(getContext()) ? 0xffdddddd : 0xff444444);
    background.setStroke(0, android.graphics.Color.TRANSPARENT);
    android.graphics.drawable.StateListDrawable states = new android.graphics.drawable.StateListDrawable();
    android.graphics.drawable.GradientDrawable pressed = AdvancedLtTheme.rounded(getContext(), true);
    pressed.setCornerRadius(dp(28));
    pressed.setColor(getContext().getColor(AdvancedLtTheme.isDay(getContext())
        ? R.color.advanced_lt_pressed_light_color : R.color.advanced_lt_pressed_color));
    states.addState(new int[] {android.R.attr.state_pressed}, pressed);
    states.addState(new int[0], background);
    setBackground(states);
  }
  private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
  private void sendDelete() { mListener.onKey(KeyCodes.DELETE, null, 0, null, true); }
  @Override public boolean onTouchEvent(MotionEvent event) {
    if (mListener == null || !isEnabled()) return false;
    switch (event.getActionMasked()) {
      case MotionEvent.ACTION_DOWN:
        stop();
        mHolding = true;
        mPointerId = event.getPointerId(0);
        setPressed(true);
        getParent().requestDisallowInterceptTouchEvent(true);
        mListener.onPress(KeyCodes.DELETE);
        sendDelete();
        SharedPreferences prefs = DirectBootAwareSharedPreferences.create(getContext());
        if (prefs.getBoolean(getContext().getString(R.string.settings_key_use_key_repeat),
            getResources().getBoolean(R.bool.settings_default_use_key_repeat))) {
          int delay = 350;
          try { delay = Integer.parseInt(prefs.getString(getContext().getString(R.string.settings_key_long_press_timeout),
              getContext().getString(R.string.settings_default_long_press_timeout))); }
          catch (NumberFormatException ignored) { }
          postDelayed(mRepeat, Math.max(1, delay));
        }
        return true;
      case MotionEvent.ACTION_MOVE:
        int index = event.findPointerIndex(mPointerId);
        if (index < 0 || event.getX(index) < 0 || event.getY(index) < 0
            || event.getX(index) >= getWidth() || event.getY(index) >= getHeight()) stop();
        return true;
      case MotionEvent.ACTION_POINTER_UP:
        if (event.getPointerId(event.getActionIndex()) == mPointerId) stop();
        return true;
      case MotionEvent.ACTION_UP:
      case MotionEvent.ACTION_CANCEL:
        stop();
        return true;
      default: return true;
    }
  }
  @Override public boolean performClick() {
    super.performClick();
    if (mListener != null) {
      mListener.onPress(KeyCodes.DELETE);
      sendDelete();
      mListener.onRelease(KeyCodes.DELETE);
    }
    return true;
  }
  private void stop() {
    if (mRepeat != null) removeCallbacks(mRepeat);
    if (mHolding && mListener != null) mListener.onRelease(KeyCodes.DELETE);
    mHolding = false;
    mPointerId = -1;
    setPressed(false);
  }
  @Override protected void onDetachedFromWindow() { stop(); super.onDetachedFromWindow(); }
  @Override public void onWindowFocusChanged(boolean focused) {
    if (!focused) stop();
    super.onWindowFocusChanged(focused);
  }
  @Override protected void onWindowVisibilityChanged(int visibility) {
    if (visibility != View.VISIBLE) stop();
    super.onWindowVisibilityChanged(visibility);
  }
}
