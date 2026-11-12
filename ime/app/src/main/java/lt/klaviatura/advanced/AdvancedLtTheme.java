package lt.klaviatura.advanced;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.content.res.ColorStateList;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.menny.android.anysoftkeyboard.AnyApplication;
import com.menny.android.anysoftkeyboard.R;

/** Shared colors for the keyboard's auxiliary panels, independent of the host app theme. */
public final class AdvancedLtTheme {
  public static final String DAY = "18c558ef-bc8c-433a-a36e-92c3ca3be4dd";
  public static final String NIGHT = "8774f99e-fb4a-49fa-b8d0-4083f762250a";
  public static final String STEVE_LIGHT = "201a874a-3db8-4acf-904f-83055b0ab327";
  public static final String STEVE_DARK = "3e1e38c1-89c2-480b-a81f-e75aa373b0c5";
  public static final String MODE = "advanced_lt_theme_mode";
  private AdvancedLtTheme() {}

  public static boolean isDay(Context context) {
    return isLightTheme(AnyApplication.getKeyboardThemeFactory(context).getEnabledAddOn().getId());
  }
  public static boolean isLightTheme(String id) {
    return DAY.equals(id) || STEVE_LIGHT.equals(id);
  }
  public static int text(Context context) { return isDay(context) ? Color.rgb(51,51,51) : Color.WHITE; }
  public static int surface(Context context) {
    final String id = AnyApplication.getKeyboardThemeFactory(context).getEnabledAddOn().getId();
    if (STEVE_LIGHT.equals(id)) return context.getColor(R.color.steve_light_surface);
    if (STEVE_DARK.equals(id)) return context.getColor(R.color.steve_dark_surface);
    return isLightTheme(id) ? Color.rgb(221, 221, 221) : Color.rgb(34, 34, 34);
  }
  public static int card(Context context) {
    final String id = AnyApplication.getKeyboardThemeFactory(context).getEnabledAddOn().getId();
    if (STEVE_LIGHT.equals(id)) return context.getColor(R.color.steve_light_key);
    if (STEVE_DARK.equals(id)) return context.getColor(R.color.steve_dark_key);
    return isLightTheme(id) ? Color.rgb(250, 250, 250) : Color.rgb(42, 42, 42);
  }
  public static int accent(Context context) {
    return context.getColor(isDay(context) ? R.color.advanced_lt_pressed_color : R.color.advanced_lt_pressed_light_color);
  }
  public static GradientDrawable glassBackground(Context context) {
    final int color = surface(context);
    final int tint = color & 0x00ffffff;
    final GradientDrawable glass = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
        new int[] {(isDay(context) ? 0xc9000000 : 0xbd000000) | tint, 0xe6000000 | tint});
    final float radius = 18 * context.getResources().getDisplayMetrics().density;
    glass.setCornerRadii(new float[] {radius, radius, radius, radius, 0, 0, 0, 0});
    return glass;
  }
  public static GradientDrawable fade(Context context) {
    int color = surface(context);
    return new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP,
        new int[] {color, (color & 0x00ffffff) | 0xe6000000, color & 0x00ffffff});
  }
  public static GradientDrawable rounded(Context context, boolean selected) {
    GradientDrawable shape = new GradientDrawable();
    shape.setColor(card(context));
    shape.setCornerRadius(16 * context.getResources().getDisplayMetrics().density);
    shape.setStroke(Math.round(context.getResources().getDisplayMetrics().density),
        selected ? accent(context) : (isDay(context) ? Color.LTGRAY : Color.GRAY));
    return shape;
  }
  public static void tintContents(View view) {
    int color = text(view.getContext());
    if (view instanceof ImageView) ((ImageView)view).setImageTintList(ColorStateList.valueOf(color));
    if (view instanceof TextView) ((TextView)view).setTextColor(color);
    if (view instanceof ViewGroup) {
      ViewGroup group = (ViewGroup)view;
      for (int i=0;i<group.getChildCount();i++) tintContents(group.getChildAt(i));
    }
  }
}
