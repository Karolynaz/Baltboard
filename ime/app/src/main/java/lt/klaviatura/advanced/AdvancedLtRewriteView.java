package lt.klaviatura.advanced;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import com.menny.android.anysoftkeyboard.R;
import net.evendanan.pixel.MainChild;

/** Non-focusable IME panel: opening a preview must never change the editor selection. */
public final class AdvancedLtRewriteView extends LinearLayout implements MainChild {
  private final TextView body;
  private final Button cancel, copy, replace;
  private final boolean lt;
  private final Runnable close;
  private final boolean translate;
  public AdvancedLtRewriteView(Context context, Runnable close) {
    this(context, close, false);
  }
  public AdvancedLtRewriteView(Context context, Runnable close, boolean translate) {
    super(context);
    this.translate = translate;
    this.close = close;
    lt = "lt".equals(getResources().getConfiguration().locale.getLanguage());
    setOrientation(VERTICAL);
    setBackgroundColor(AdvancedLtTheme.surface(context));
    LinearLayout toolbar = new LinearLayout(context);
    toolbar.setGravity(Gravity.CENTER_VERTICAL);
    ImageButton keyboard = new ImageButton(context);
    keyboard.setImageResource(R.drawable.advanced_lt_ic_keyboard);
    keyboard.setBackgroundColor(android.graphics.Color.TRANSPARENT);
    keyboard.setPadding(dp(10), dp(10), dp(10), dp(10));
    keyboard.setImageTintList(android.content.res.ColorStateList.valueOf(AdvancedLtTheme.text(context)));
    keyboard.setContentDescription(tr("Grįžti į klaviatūrą", "Back to keyboard"));
    keyboard.setOnClickListener(v -> close.run());
    toolbar.addView(keyboard, new LayoutParams(dp(48), dp(48)));
    View divider = new View(context);
    divider.setBackgroundColor(AdvancedLtTheme.isDay(context) ? 0x40333333 : 0x40ffffff);
    LayoutParams dividerParams = new LayoutParams(dp(1), dp(30));
    dividerParams.setMargins(dp(8),0,dp(16),0);
    toolbar.addView(divider, dividerParams);
    TextView title = new TextView(context);
    title.setText(translate ? tr("Vertimas į anglų kalbą", "Translate to English")
        : tr("Rašyba ir gramatika", "Spelling and grammar"));
    title.setTextColor(AdvancedLtTheme.text(context));
    title.setTextSize(19);
    title.setTypeface(null, android.graphics.Typeface.BOLD);
    toolbar.addView(title, new LayoutParams(0, LayoutParams.WRAP_CONTENT,1));
    addView(toolbar, new LayoutParams(LayoutParams.MATCH_PARENT,dp(52)));
    LinearLayout card = new LinearLayout(context);
    card.setOrientation(VERTICAL);
    card.setPadding(dp(16),dp(16),dp(16),dp(8));
    card.setBackground(AdvancedLtTheme.rounded(context,false));
    LayoutParams cardParams = new LayoutParams(LayoutParams.MATCH_PARENT,0,1);
    cardParams.setMargins(dp(12),dp(14),dp(12),dp(12));
    addView(card,cardParams);
    ScrollView scroll = new ScrollView(context);
    body = new TextView(context);
    body.setTextSize(18);
    body.setTextColor(AdvancedLtTheme.text(context));
    body.setPadding(0,0,0,dp(16));
    scroll.addView(body);
    card.addView(scroll,new LayoutParams(LayoutParams.MATCH_PARENT,0,1));
    LinearLayout actions = new LinearLayout(context);
    actions.setGravity(Gravity.CENTER_VERTICAL);
    cancel = button(tr("Atšaukti","Cancel"));
    copy = button(tr("Kopijuoti","Copy"));
    replace = button(tr("Pakeisti","Replace"));
    actions.addView(cancel,new LayoutParams(0,dp(48),1));
    actions.addView(copy,new LayoutParams(0,dp(48),1));
    actions.addView(replace,new LayoutParams(0,dp(48),1));
    card.addView(actions);
    loading();
  }
  private Button button(String label) {
    Button button = new Button(getContext());
    button.setText(label);
    button.setTextSize(14);
    button.setAllCaps(false);
    button.setMinWidth(0);
    button.setMinimumWidth(0);
    button.setPadding(dp(2),0,dp(2),0);
    button.setTextColor(AdvancedLtTheme.text(getContext()));
    button.setBackgroundColor(android.graphics.Color.TRANSPARENT);
    return button;
  }
  private String tr(String lithuanian,String english) { return lt ? lithuanian : english; }
  private int dp(int value) { return Math.round(value*getResources().getDisplayMetrics().density); }
  private void reset() {
    cancel.setText(tr("Atšaukti","Cancel"));
    cancel.setTextColor(AdvancedLtTheme.isDay(getContext()) ? 0xffb3261e : 0xffff7373);
    cancel.setOnClickListener(v -> close.run());
    copy.setVisibility(GONE); replace.setVisibility(GONE);
    copy.setOnClickListener(null); replace.setOnClickListener(null);
  }
  public void loading() { reset(); body.setText(translate ? tr("Verčiama…", "Translating…") : tr("Tikrinama…","Checking…")); }
  public void message(String message) { reset(); body.setText(message); }
  public void error(String message,Runnable retry) {
    reset(); body.setText(message);
    cancel.setText(tr("Bandyti darsyk","Try again"));
    cancel.setTextColor(AdvancedLtTheme.isDay(getContext()) ? 0xff1565c0 : 0xff90caf9);
    cancel.setOnClickListener(v -> retry.run());
  }
  public void result(String text,Runnable onCopy,Runnable onReplace) {
    reset(); body.setText(text);
    copy.setVisibility(VISIBLE); replace.setVisibility(VISIBLE);
    copy.setOnClickListener(v -> onCopy.run());
    replace.setOnClickListener(v -> onReplace.run());
  }
  @Override public void setBottomOffset(int offset) { setPadding(0,0,0,Math.max(0,offset)+dp(48)); }
}
