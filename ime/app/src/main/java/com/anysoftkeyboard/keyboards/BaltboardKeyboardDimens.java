package com.anysoftkeyboard.keyboards;

/** Geometry of the two reference layouts, measured relative to the available keyboard width. */
record BaltboardKeyboardDimens(
    KeyboardDimens theme, int width, boolean wide, boolean spaced, boolean split)
    implements KeyboardDimens {
  static final int WIDE_WINDOW_DP = 600;

  @Override
  public int getKeyboardMaxWidth() {
    return width;
  }

  @Override
  public int getKeyboardSideInset() {
    return Math.round(width * (wide ? 0.033f : spaced ? 0.01f : 0f));
  }

  @Override
  public float getKeyHorizontalGap() {
    if (!spaced || wide) return theme.getKeyHorizontalGap();
    return Math.max(theme.getKeyHorizontalGap() + 1, width * 0.015f);
  }

  int getOuterLetterRowFunctionKeyWidth() {
    return Math.round(width * (wide ? 0.108f : 0.112f));
  }

  @Override
  public float getRowVerticalGap() {
    if (!spaced || wide) return theme.getRowVerticalGap();
    // The reference gap is roughly one fifth of the distance between row starts.
    return Math.max(theme.getRowVerticalGap(),
        Math.round((theme.getNormalKeyHeight() + theme.getRowVerticalGap()) / 5f));
  }

  @Override
  public int getNormalKeyHeight() {
    return heightWithRowGap(theme.getNormalKeyHeight());
  }

  @Override
  public int getSmallKeyHeight() {
    return heightWithRowGap(theme.getSmallKeyHeight());
  }

  @Override
  public int getLargeKeyHeight() {
    return heightWithRowGap(theme.getLargeKeyHeight());
  }

  @Override
  public float getPaddingBottom() {
    return theme.getPaddingBottom();
  }

  private int heightWithRowGap(int height) {
    return Math.max(1, height - Math.round(getRowVerticalGap() - theme.getRowVerticalGap()));
  }
}
