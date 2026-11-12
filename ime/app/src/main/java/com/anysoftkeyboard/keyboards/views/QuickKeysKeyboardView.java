package com.anysoftkeyboard.keyboards.views;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import androidx.annotation.NonNull;
import com.anysoftkeyboard.keyboards.AnyKeyboard;

/**
 * This class will draw a keyboard and will make sure that the keys are split into rows as per the
 * space in the physical view
 */
public class QuickKeysKeyboardView extends AnyKeyboardViewWithMiniKeyboard {
  private int mGridWidth;

  @Override
  protected boolean usesLegacyWidthScaling() {
    return false;
  }

  @Override
  public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    final AnyKeyboard keyboard = getKeyboard();
    final int width = MeasureSpec.getSize(widthMeasureSpec) - getPaddingLeft() - getPaddingRight();
    if (keyboard != null && width > 0 && width != mGridWidth) {
      mGridWidth = width;
      final float widthDp = width / getResources().getDisplayMetrics().density;
      final int columns = widthDp < 600 ? 5 : (int) (widthDp / 48);
      keyboard.layoutQuickTextGrid(width, columns);
      super.setKeyboard(keyboard, 0);
    }
    super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    if (keyboard != null && width > 0) {
      setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), getMeasuredHeight());
    }
  }

  @Override
  protected float getEmojiTextSize(float normalSize) {
    return 26f * getResources().getDisplayMetrics().density;
  }

  public QuickKeysKeyboardView(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public QuickKeysKeyboardView(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
  }

  @SuppressWarnings("deprecation")
  @Override
  public void setBackgroundDrawable(Drawable background) {
    // no background in this class
    super.setBackgroundDrawable(null);
  }

  @Override
  public void setBackground(Drawable background) {
    // no background in this class
    super.setBackground(null);
  }

  @Override
  protected void setKeyboard(@NonNull AnyKeyboard keyboard, float verticalCorrection) {
    super.setKeyboard(keyboard, 0 /*no vertical correct here*/);
  }

  public void setKeyboard(@NonNull AnyKeyboard keyboard) {
    mGridWidth = 0;
    super.setKeyboard(keyboard, 0 /*no vertical correct here*/);
  }
}
