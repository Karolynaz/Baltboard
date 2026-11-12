package lt.klaviatura.advanced;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Bundle;
import android.os.Build;
import android.provider.Settings;
import android.text.InputType;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.widget.CompoundButtonCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.graphics.Insets;
import com.anysoftkeyboard.prefs.DirectBootAwareSharedPreferences;
import com.anysoftkeyboard.quicktextkeys.QuickTextKey;
import com.anysoftkeyboard.quicktextkeys.QuickTextKeyFactory;
import com.anysoftkeyboard.theme.KeyboardThemeFactory;
import com.anysoftkeyboard.ui.settings.setup.SetupSupport;
import com.menny.android.anysoftkeyboard.AnyApplication;
import com.menny.android.anysoftkeyboard.BuildConfig;
import com.menny.android.anysoftkeyboard.R;
import java.util.List;
import java.util.Locale;

/** Branded, Samsung One UI-inspired settings for Balticboard. */
public final class AdvancedLtSettingsActivity extends AppCompatActivity {
  public static final String PREFS_NAME = "advanced_lt_keyboard";
  public static final String PREF_MODEL = "gemini_model";
  public static final String PREF_AI_ENABLED = "gemini_ai_enabled";
  public static final String PREF_AI_TRANSLATE_ENABLED = "ai_translate_enabled";
  public static final String PREF_PROMPT = "ai_correction_prompt";
  public static final String PREF_TRANSLATE_PROMPT = "ai_translate_prompt";
  public static final String PREF_PROVIDER = "ai_provider";
  public static String modelPreference(String provider) {
    return "gemini".equals(provider) ? PREF_MODEL : "ai_model_" + provider;
  }
  public static void migrateAiPreferences(android.content.Context context) {
    SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
    if (prefs.contains("languagetool_enabled")) {
      SharedPreferences.Editor edit = prefs.edit().remove("languagetool_enabled");
      if (prefs.getBoolean("languagetool_enabled", false)) edit.putBoolean(PREF_AI_ENABLED, true);
      edit.apply();
    }
  }
  public static final String DEFAULT_MODEL = "gemini-3.5-flash-lite";

  private static final int BACKGROUND = Color.rgb(0, 0, 0);
  private static final int CARD = Color.rgb(28, 28, 31);
  private static final int FIELD = Color.rgb(44, 44, 48);
  private static final int DIVIDER = Color.rgb(55, 55, 60);
  private static final int PRIMARY = Color.WHITE;
  private static final int SECONDARY = Color.rgb(174, 174, 181);
  private static final int ACCENT = Color.rgb(154, 112, 230);

  private static final String THEME_GRAY = "8774f99e-fb4a-49fa-b8d0-4083f762250a";
  private static final String THEME_WHITE = "18c558ef-bc8c-433a-a36e-92c3ca3be4dd";

  private static final int SCREEN_MAIN = 0;
  private static final int SCREEN_THEMES = 1;
  private static final int SCREEN_EMOJI = 2;
  private static final int SCREEN_AI = 3;
  private static final int SCREEN_ABOUT = 4;
  private static final int SCREEN_APPEARANCE = 5;
  private static final int SCREEN_CORRECTION = 6;
  private static final int SCREEN_TYPING = 7;
  private static final int SCREEN_GESTURES = 8;
  private static final int SCREEN_EFFECTS = 9;
  private static final int SCREEN_ADVANCED = 10;
  private static final int SCREEN_SETUP = 11;
  private static final String STATE_SCREEN = "settings_screen";
  private static final int TWO_PANE_WIDTH_DP = 600;

  private final GeminiClient mGeminiClient = new GeminiClient();
  private final SparseArray<View> mNavigationRows = new SparseArray<>();
  private int mCurrentScreen = SCREEN_MAIN;
  private FrameLayout mSettingsRoot;
  private LinearLayout mPanes;
  private ScrollView mNavigation;
  private TextView mNavigationHeading;
  @Nullable private ScrollView mDetail;
  @Nullable private TextView mThemeSummary;
  @Nullable private EditText mApiKey;
  @Nullable private EditText mModel;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    migrateAiPreferences(this);
    getWindow().setStatusBarColor(BACKGROUND);
    getWindow().setNavigationBarColor(BACKGROUND);
    getWindow().getDecorView().setSystemUiVisibility(0);
    WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
    getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
    getOnBackPressedDispatcher().addCallback(this,
        new androidx.activity.OnBackPressedCallback(true) {
          @Override
          public void handleOnBackPressed() {
            if (mCurrentScreen != SCREEN_MAIN) {
              showMain();
            } else {
              setEnabled(false);
              getOnBackPressedDispatcher().onBackPressed();
            }
          }
        });
    createSettingsLayout();
    showMain();
    if (savedInstanceState != null) {
      openScreen(savedInstanceState.getInt(STATE_SCREEN, SCREEN_MAIN));
    }
  }

  @Override
  protected void onSaveInstanceState(@NonNull Bundle outState) {
    outState.putInt(STATE_SCREEN, mCurrentScreen);
    super.onSaveInstanceState(outState);
  }

  @Override
  protected void onResume() {
    super.onResume();
    if (mCurrentScreen == SCREEN_SETUP) showSetup();
  }

  private void showMain() {
    mCurrentScreen = SCREEN_MAIN;
    if (mNavigation != null) {
      if (mDetail != null) {
        final var controller = ViewCompat.getWindowInsetsController(mDetail);
        if (controller != null) controller.hide(WindowInsetsCompat.Type.ime());
        mPanes.removeView(mDetail);
        mDetail = null;
      }
      setTitle(tr("Nustatymai", "Settings"));
      refreshPanes();
      return;
    }
    final LinearLayout content = page(tr("Nustatymai", "Settings"), false);

    addSectionLabel(content, tr("Išvaizda", "Appearance"));
    content.addView(
        card(
            navigationRow(
                R.id.advanced_lt_settings_themes,
                tr("Temos", "Themes"),
                currentThemeName(),
                SCREEN_THEMES),
            navigationRow(
                R.id.advanced_lt_settings_emoji,
                "Emoji",
                tr("Rodomos kategorijos ir elgsena", "Visible categories and behavior"),
                SCREEN_EMOJI),
            navigationRow(
                R.id.advanced_lt_settings_appearance,
                tr("Daugiau parinkčių", "More details"),
                tr("Tarpai, klavišai ir klaviatūros aukštis", "Spacing, keys and keyboard height"),
                SCREEN_APPEARANCE)));

    addSectionLabel(content, tr("Rašymas", "Typing"));
    content.addView(
        card(
            navigationRow(
                R.id.advanced_lt_settings_correction,
                tr("Teksto taisymas", "Text correction"),
                tr("Pasiūlymai, žodynai ir automatinis taisymas", "Suggestions, dictionaries and autocorrect"),
                SCREEN_CORRECTION),
            navigationRow(
                R.id.advanced_lt_settings_typing,
                tr("Rašymas ir kalbos", "Typing and languages"),
                tr("Lietuvių ir anglų kalbų elgsena", "Lithuanian and English typing behavior"),
                SCREEN_TYPING),
            navigationRow(
                R.id.advanced_lt_settings_gestures,
                tr("Gestai", "Gestures"),
                tr("Braukimas ir kiti gestai", "Swipe typing and other gestures"),
                SCREEN_GESTURES)));

    addSectionLabel(content, tr("Klaviatūra", "Keyboard"));
    content.addView(
        card(
            navigationRow(
                R.id.advanced_lt_settings_effects,
                tr("Garsas ir vibracija", "Sound and vibration"),
                tr("Paspaudimų garsai ir vibracija", "Key press sounds and vibration"),
                SCREEN_EFFECTS),
            navigationRow(
                R.id.advanced_lt_settings_advanced,
                tr("Papildomi nustatymai", "Advanced settings"),
                tr("Elgsena ir išplėstinės parinktys", "Behavior and advanced options"),
                SCREEN_ADVANCED)));

    addSectionLabel(content, "AI");
    content.addView(
        card(
            navigationRow(
                R.id.advanced_lt_settings_ai,
                tr("AI taisymas ir vertimas", "AI rewrite & translate"),
                tr("Tiekėjas, API raktas ir taisymo instrukcija", "Provider, API key and correction instructions"),
                SCREEN_AI)));

    addSectionLabel(content, tr("Informacija", "Information"));
    content.addView(
        card(
            navigationRow(
                R.id.advanced_lt_settings_setup,
                tr("Klaviatūros įjungimas", "Enable keyboard"),
                tr("Įjungimo ir pasirinkimo vedlys", "Setup and selection guide"),
                SCREEN_SETUP),
            navigationRow(
                R.id.advanced_lt_settings_about,
                tr("Apie", "About"),
                "Baltboard",
                SCREEN_ABOUT)));
    addBottomSpace(content);
  }

  private void openScreen(int screen) {
    if (screen == mCurrentScreen) return;
    switch (screen) {
      case SCREEN_THEMES -> showThemes();
      case SCREEN_EMOJI -> showEmoji();
      case SCREEN_AI -> showAi();
      case SCREEN_ABOUT -> showAbout();
      case SCREEN_APPEARANCE -> showAppearance();
      case SCREEN_CORRECTION -> showCorrection();
      case SCREEN_TYPING -> showTyping();
      case SCREEN_GESTURES -> showGestures();
      case SCREEN_EFFECTS -> showEffects();
      case SCREEN_ADVANCED -> showAdvanced();
      case SCREEN_SETUP -> showSetup();
    }
  }

  private View navigationRow(int id, String title, String summary, int screen) {
    final View item = row(id, title, summary, ignored -> openScreen(screen));
    final StateListDrawable background = new StateListDrawable();
    background.addState(new int[] {android.R.attr.state_activated},
        rounded(Color.rgb(58, 43, 82), 18, 0, Color.TRANSPARENT));
    background.addState(new int[0], new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
    item.setBackground(background);
    mNavigationRows.put(screen, item);
    return item;
  }

  private void showThemes() {
    mCurrentScreen = SCREEN_THEMES;
    final LinearLayout content = page(tr("Temos", "Themes"), true);
    addIntro(content, tr("Pasirinkite klaviatūros spalvą.", "Choose the keyboard color."));

    final KeyboardThemeFactory factory = AnyApplication.getKeyboardThemeFactory(this);
    String currentId = factory.getEnabledAddOn().getId();
    if (!THEME_GRAY.equals(currentId)
        && !THEME_WHITE.equals(currentId)
        && !AdvancedLtTheme.STEVE_LIGHT.equals(currentId)
        && !AdvancedLtTheme.STEVE_DARK.equals(currentId)) {
      factory.setAddOnEnabled(THEME_GRAY, true);
      currentId = THEME_GRAY;
    }
    final RadioGroup choices = new RadioGroup(this);
    choices.setOrientation(LinearLayout.VERTICAL);
    choices.setBackground(rounded(CARD, 28, 0, Color.TRANSPARENT));
    choices.setPadding(0, dp(8), 0, dp(8));

    boolean automatic = "auto".equals(com.anysoftkeyboard.prefs.DirectBootAwareSharedPreferences.create(this)
        .getString(AdvancedLtTheme.MODE, "manual"));
    if (automatic) currentId = "auto";
    choices.addView(themeChoice(R.id.advanced_lt_theme_gray, "Night", THEME_GRAY, currentId));
    choices.addView(themeChoice(R.id.advanced_lt_theme_white, "Day", THEME_WHITE, currentId));
    choices.addView(themeChoice(R.id.advanced_lt_theme_auto, "Auto", "auto", currentId));
    choices.addView(themeChoice(R.id.advanced_lt_theme_steve_light,
        "Steve Light", AdvancedLtTheme.STEVE_LIGHT, currentId));
    choices.addView(themeChoice(R.id.advanced_lt_theme_steve_dark,
        "Steve Dark", AdvancedLtTheme.STEVE_DARK, currentId));
    choices.setOnCheckedChangeListener(
        (group, checkedId) -> {
          boolean auto = checkedId == R.id.advanced_lt_theme_auto;
          com.anysoftkeyboard.prefs.DirectBootAwareSharedPreferences.create(this).edit()
              .putString(AdvancedLtTheme.MODE, auto ? "auto" : "manual").apply();
          if (auto) {
            factory.applyAutomaticTheme(getResources().getConfiguration());
            if (mThemeSummary != null) mThemeSummary.setText(currentThemeName());
            return;
          }
          final String themeId;
          if (checkedId == R.id.advanced_lt_theme_gray) {
            themeId = THEME_GRAY;
          } else if (checkedId == R.id.advanced_lt_theme_white) {
            themeId = THEME_WHITE;
          } else if (checkedId == R.id.advanced_lt_theme_steve_light) {
            themeId = AdvancedLtTheme.STEVE_LIGHT;
          } else if (checkedId == R.id.advanced_lt_theme_steve_dark) {
            themeId = AdvancedLtTheme.STEVE_DARK;
          } else {
            themeId = THEME_GRAY;
          }
          factory.setAddOnEnabled(themeId, true);
          if (mThemeSummary != null) mThemeSummary.setText(currentThemeName());
        });
    content.addView(choices, sectionParams());
    final SharedPreferences preferences = keyboardPreferences();
    final String glassKey = getString(R.string.settings_key_baltboard_glass);
    content.addView(card(switchRow(
        tr("Permatomas stiklo fonas", "Translucent glass background"),
        tr("Pusiau permatomas klaviatūros fonas", "Use a translucent keyboard background"),
        preferences.getBoolean(glassKey, false),
        R.id.advanced_lt_glass,
        (ignored, checked) -> preferences.edit().putBoolean(glassKey, checked).apply())));
    addBottomSpace(content);
  }

  private void showEmoji() {
    mCurrentScreen = SCREEN_EMOJI;
    final LinearLayout content = page("Emoji", true);
    addIntro(
        content,
        tr(
            "Pažymėkite kategorijas, kurias norite matyti emoji pasirinkime.",
            "Select the categories you want to see in the emoji picker."));

    final QuickTextKeyFactory factory = AnyApplication.getQuickTextKeyFactory(this);
    final List<String> enabledIds = factory.getEnabledIds();
    final LinearLayout list = new LinearLayout(this);
    list.setId(R.id.advanced_lt_emoji_categories);
    list.setOrientation(LinearLayout.VERTICAL);
    list.setBackground(rounded(CARD, 28, 0, Color.TRANSPARENT));
    final List<QuickTextKey> addOns = factory.getAllAddOns();
    for (int index = 0; index < addOns.size(); index++) {
      final QuickTextKey addOn = addOns.get(index);
      final CheckBox checkBox = new CheckBox(this);
      checkBox.setText(addOn.getName());
      checkBox.setTextColor(PRIMARY);
      checkBox.setTextSize(17f);
      checkBox.setGravity(Gravity.CENTER_VERTICAL);
      checkBox.setPadding(dp(20), dp(6), dp(20), dp(6));
      checkBox.setMinHeight(dp(58));
      CompoundButtonCompat.setButtonTintList(checkBox, checkTint());
      checkBox.setChecked(enabledIds.contains(addOn.getId()));
      checkBox.setOnCheckedChangeListener(
          (button, checked) -> factory.setAddOnEnabled(addOn.getId(), checked));
      list.addView(checkBox, matchWrap());
      if (index < addOns.size() - 1) list.addView(divider(dp(56)));
    }
    content.addView(list, sectionParams());

    final SharedPreferences keyboardPreferences =
        DirectBootAwareSharedPreferences.create(getApplicationContext());
    final String returnAfterEmojiKey =
        getString(R.string.settings_key_one_shot_quick_text_popup);
    final SwitchCompat returnAfterEmoji = new SwitchCompat(this);
    returnAfterEmoji.setText(
        tr("Pasirinkus emoji grįžtu į klaviatūrą", "Return to keyboard after selecting an emoji"));
    returnAfterEmoji.setTextColor(PRIMARY);
    returnAfterEmoji.setTextSize(17f);
    returnAfterEmoji.setPadding(dp(20), dp(8), dp(20), dp(8));
    returnAfterEmoji.setMinHeight(dp(64));
    returnAfterEmoji.setBackground(rounded(CARD, 28, 0, Color.TRANSPARENT));
    returnAfterEmoji.setChecked(keyboardPreferences.getBoolean(returnAfterEmojiKey, false));
    returnAfterEmoji.setOnCheckedChangeListener(
        (ignored, checked) ->
            keyboardPreferences.edit().putBoolean(returnAfterEmojiKey, checked).apply());
    content.addView(returnAfterEmoji, sectionParams());
    addBottomSpace(content);
  }

  private void showAppearance() {
    mCurrentScreen = SCREEN_APPEARANCE;
    final LinearLayout content = page(tr("Daugiau parinkčių", "More details"), true);
    addIntro(
        content,
        tr(
            "Pritaikykite klavišų informaciją ir klaviatūros dydį.",
            "Customize key information and keyboard size."));

    addSectionLabel(content, tr("Tarpai", "Spacing"));
    final SharedPreferences preferences = keyboardPreferences();
    final String spacingKey = getString(R.string.settings_key_baltboard_wider_key_spacing);
    content.addView(
        card(
            switchRow(
                tr("Didesni tarpai tarp klavišų", "Wider key spacing"),
                tr("Padidina tarpus tarp klavišų ir jų eilių",
                    "Adds more space between keys and rows"),
                preferences.getBoolean(spacingKey, true),
                R.id.advanced_lt_wider_key_spacing,
                (ignored, checked) -> preferences.edit().putBoolean(spacingKey, checked).apply())));

    addSectionLabel(content, tr("Plataus ekrano išdėstymas", "Wide-screen layout"));
    final String splitKey = getString(R.string.settings_key_baltboard_split_wide_keyboard);
    final String layout = preferences.getBoolean(splitKey, false) ? "split" : "continuous";
    final RadioGroup layoutChoices = new RadioGroup(this);
    layoutChoices.setOrientation(LinearLayout.VERTICAL);
    layoutChoices.setPadding(0, dp(8), 0, dp(8));
    layoutChoices.addView(themeChoice(R.id.advanced_lt_fold_continuous,
        tr("Vientisa", "Continuous"), "continuous", layout));
    layoutChoices.addView(themeChoice(R.id.advanced_lt_fold_split,
        tr("Padalyta", "Split"), "split", layout));
    layoutChoices.setOnCheckedChangeListener((group, checkedId) ->
        preferences.edit().putBoolean(splitKey, checkedId == R.id.advanced_lt_fold_split).apply());
    content.addView(card(layoutChoices));
    addIntro(content, tr("Padalytas išdėstymas rodomas plačiame ekrane laikant telefoną horizontaliai.",
        "Split layout is available on a wide screen in landscape."));

    addSectionLabel(content, tr("Klavišai", "Keys"));
    content.addView(
        card(
            settingSwitch(
                tr("Rodyti paspausto klavišo peržiūrą", "Show key press preview"),
                tr("Virš klavišo trumpam parodoma paspausta raidė", "Briefly show the pressed character above the key"),
                R.string.settings_key_key_press_shows_preview_popup,
                R.bool.settings_default_key_press_shows_preview_popup),
            settingSwitch(
                tr("Rodyti papildomus simbolius", "Show secondary symbols"),
                tr("Ant klavišų rodomos ilgo paspaudimo užuominos", "Show long-press hints on keys"),
                R.string.settings_key_show_hint_text_key,
                R.bool.settings_default_show_hint_text_value),
            settingSwitch(
                tr("Rodyti klaviatūros pavadinimą", "Show keyboard name"),
                tr("Pavadinimas rodomas ant kalbos klavišo", "Show the name on the language key"),
                R.string.settings_key_show_keyboard_name_text_key,
                R.bool.settings_default_show_keyboard_name_text_value)));

    addSectionLabel(content, tr("Dydis", "Size"));
    content.addView(
        card(
            intSlider(
                tr("Klaviatūros dydis vertikaliai", "Portrait keyboard size"),
                tr("Keičia klavišų aukštį laikant telefoną stačiai", "Adjust key height while holding the phone upright"),
                R.string.settings_key_zoom_percent_in_portrait,
                R.integer.settings_default_zoom_percent_in_portrait,
                70,
                130,
                "%"),
            intSlider(
                tr("Klaviatūros dydis horizontaliai", "Landscape keyboard size"),
                tr("Keičia klavišų aukštį pasukus telefoną", "Adjust key height when the phone is rotated"),
                R.string.settings_key_zoom_percent_in_landscape,
                R.integer.settings_default_zoom_percent_in_landscape,
                70,
                130,
                "%")));
    addBottomSpace(content);
  }

  private void showCorrection() {
    mCurrentScreen = SCREEN_CORRECTION;
    final LinearLayout content = page(tr("Teksto taisymas", "Text correction"), true);
    addIntro(
        content,
        tr(
            "Valdykite žodžių pasiūlymus ir automatinį klaidų taisymą.",
            "Control word suggestions and automatic corrections."));

    addSectionLabel(content, tr("Pasiūlymai", "Suggestions"));
    content.addView(
        card(
            settingSwitch(
                tr("Rodyti žodžių pasiūlymus", "Show word suggestions"),
                tr("Virš klaviatūros rodomi siūlomi žodžiai", "Show suggested words above the keyboard"),
                R.string.settings_key_show_suggestions,
                R.bool.settings_default_show_suggestions),
            settingSwitch(
                tr("Automatinis taisymas", "Autocorrect"),
                tr("Rašybos klaidos taisomos vedant tekstą", "Correct spelling mistakes while typing"),
                R.string.settings_key_quick_fix,
                R.bool.settings_default_quick_fix),
            choiceSetting(
                tr("Automatinio taisymo stiprumas", "Autocorrect strength"),
                R.string.settings_key_auto_pick_suggestion_aggressiveness,
                R.string.settings_default_auto_pick_suggestion_aggressiveness,
                new String[] {
                  tr("Išjungtas", "Off"),
                  tr("Silpnas", "Low"),
                  tr("Vidutinis", "Medium"),
                  tr("Stiprus", "High"),
                  tr("Labai stiprus", "Very high")
                },
                new String[] {
                  "none", "minimal_aggressiveness", "medium_aggressiveness",
                  "high_aggressiveness", "extreme_aggressiveness"
                }),
            settingSwitch(
                tr("Didžioji raidė sakinio pradžioje", "Auto-capitalization"),
                tr("Naujas sakinys automatiškai pradedamas didžiąja raide", "Automatically capitalize the start of a sentence"),
                R.string.settings_key_auto_capitalization,
                R.bool.settings_default_auto_capitalization)));

    addSectionLabel(content, tr("Žodynas", "Dictionary"));
    content.addView(
        card(
            settingSwitch(
                tr("Naudoti asmeninį žodyną", "Use personal dictionary"),
                tr("Siūlomi anksčiau jūsų naudoti žodžiai", "Suggest words you have used before"),
                R.string.settings_key_use_user_dictionary,
                R.bool.settings_default_user_dictionary),
            settingSwitch(
                tr("Taisyti sulipusius žodžius", "Split joined words"),
                tr("Bandoma atskirti netyčia be tarpo parašytus žodžius", "Try to split words accidentally typed without a space"),
                R.string.settings_key_try_splitting_words_for_correction,
                R.bool.settings_default_try_splitting_words_for_correction),
            settingSwitch(
                tr("Atnaujinti pasiūlymus perkėlus žymeklį", "Refresh suggestions after moving the cursor"),
                tr("Pasiūlymai perskaičiuojami redaguojant žodį", "Recalculate suggestions while editing a word"),
                R.string.settings_key_allow_suggestions_restart,
                R.bool.settings_default_allow_suggestions_restart)));
    addBottomSpace(content);
  }

  private void showTyping() {
    mCurrentScreen = SCREEN_TYPING;
    final LinearLayout content = page(tr("Rašymas ir kalbos", "Typing and languages"), true);
    addIntro(
        content,
        tr(
            "Lietuvių ir anglų QWERTY išdėstymai įjungti pagal numatymą.",
            "Lithuanian and English QWERTY layouts are enabled by default."));

    addSectionLabel(content, tr("Rašymas", "Typing"));
    content.addView(
        card(
            settingSwitch(
                tr("Tarpas pasirinkus pasiūlymą", "Space after selecting a suggestion"),
                tr("Po pasirinkto žodžio automatiškai įrašomas tarpas", "Automatically insert a space after the selected word"),
                R.string.settings_key_auto_space,
                R.bool.settings_default_auto_space),
            settingSwitch(
                tr("Taškas dukart paspaudus tarpą", "Double-space period"),
                tr("Du tarpai pakeičiami tašku ir tarpu", "Replace two spaces with a period and a space"),
                R.string.settings_key_double_space_to_period,
                R.bool.settings_default_double_space_to_period),
            settingSwitch(
                tr("Sinchronizuoti sistemos iškarpinę", "Sync system clipboard"),
                tr("Nukopijuotas tekstas patenka į klaviatūros iškarpinę", "Add copied text to the keyboard clipboard"),
                R.string.settings_key_os_clipboard_sync,
                R.bool.settings_default_os_clipboard_sync)));

    addSectionLabel(content, tr("Kalbos", "Languages"));
    content.addView(
        card(
            settingSwitch(
                tr("Kalbos pasirinkimas ilgu paspaudimu", "Language picker on long press"),
                tr("Ilgai paspaudus kalbos klavišą rodomas pasirinkimas", "Show language choices after a long press"),
                R.string.settings_key_lang_key_shows_popup,
                R.bool.settings_default_lang_key_shows_popup),
            settingSwitch(
                tr("Slėpti atskirą kalbos klavišą", "Hide the separate language key"),
                tr("Kalba keičiama braukiant per tarpo klavišą", "Switch languages by swiping on the space bar"),
                R.string.settings_key_always_hide_language_key,
                R.bool.settings_default_always_hide_language_key),
            settingSwitch(
                tr("Rodyti visus simbolių puslapius", "Cycle through all symbol pages"),
                tr("Simbolių mygtukas perjungia visus galimus puslapius", "The symbols key cycles through all available pages"),
                R.string.settings_key_cycle_all_symbols,
                R.bool.settings_default_cycle_all_symbols)));
    addBottomSpace(content);
  }

  private void showGestures() {
    mCurrentScreen = SCREEN_GESTURES;
    final LinearLayout content = page(tr("Gestai", "Gestures"), true);
    addIntro(
        content,
        tr(
            "Nustatykite rašymą braukiant ir gestų jautrumą.",
            "Configure swipe typing and gesture sensitivity."));

    addSectionLabel(content, tr("Rašymas braukiant", "Swipe typing"));
    content.addView(
        card(
            settingSwitch(
                tr("Rašyti braukiant per raides", "Type by swiping over letters"),
                tr("Žodis įvedamas neatkeliant piršto nuo klaviatūros", "Enter a word without lifting your finger"),
                R.string.settings_key_gesture_typing,
                R.bool.settings_default_gesture_typing),
            settingSwitch(
                tr("Tikslesnė piršto trajektorija", "Improve touch trajectory"),
                tr("Koreguojami nedideli braukimo netikslumai", "Correct small inaccuracies while swiping"),
                R.string.settings_key_touch_trajectory_correction,
                R.bool.settings_default_touch_trajectory_correction)));

    addSectionLabel(content, tr("Jautrumas", "Sensitivity"));
    content.addView(
        card(
            stringSlider(
                tr("Minimalus gesto greitis", "Minimum gesture speed"),
                tr("Mažesnė reikšmė lengviau atpažįsta lėtą braukimą", "A lower value recognizes slower swipes"),
                R.string.settings_key_swipe_velocity_threshold,
                R.string.settings_default_swipe_velocity_threshold,
                new String[] {"220", "300", "380", "460", "540"}),
            stringSlider(
                tr("Minimalus gesto atstumas", "Minimum gesture distance"),
                tr("Mažesnė reikšmė atpažįsta trumpesnį braukimą", "A lower value recognizes shorter swipes"),
                R.string.settings_key_swipe_distance_threshold,
                R.string.settings_default_swipe_distance_threshold,
                new String[] {"30", "80", "140", "180", "240"})));

    addSectionLabel(content, tr("Tarpo klavišas", "Space bar"));
    content.addView(
        infoCard(
            tr(
                "Braukiant į kairę arba dešinę per tarpo klavišą perjungiamos LT ir EN kalbos.",
                "Swipe left or right on the space bar to switch between LT and EN.")));
    addBottomSpace(content);
  }

  private void showEffects() {
    mCurrentScreen = SCREEN_EFFECTS;
    final LinearLayout content = page(tr("Garsas ir vibracija", "Sound and vibration"), true);
    addIntro(
        content,
        tr(
            "Pasirinkite klavišų paspaudimo grįžtamąjį ryšį.",
            "Choose feedback for key presses."));

    addSectionLabel(content, tr("Paspaudimai", "Key presses"));
    content.addView(
        card(
            settingSwitch(
                tr("Klavišų garsas", "Key press sound"),
                tr("Paspaudus klavišą atkuriamas trumpas garsas", "Play a short sound when a key is pressed"),
                R.string.settings_key_sound_on,
                R.bool.settings_default_sound_on),
            settingSwitch(
                tr("Naudoti sistemos vibraciją", "Use system vibration"),
                tr("Vibracijos stiprumą nustato telefonas", "Let Android control vibration strength"),
                R.string.settings_key_use_system_vibration,
                R.bool.settings_default_use_system_vibration),
            settingSwitch(
                tr("Vibruoti ilgai paspaudus", "Vibrate on long press"),
                tr("Trumpa vibracija aktyvavus papildomą klavišo veiksmą", "Vibrate when a secondary key action is triggered"),
                R.string.settings_key_vibrate_on_long_press,
                R.bool.settings_default_vibrate_on_long_press),
            intSlider(
                tr("Klavišo vibracijos trukmė", "Key vibration duration"),
                tr("Naudojama, kai sistemos vibracija išjungta", "Used when system vibration is disabled"),
                R.string.settings_key_vibrate_on_key_press_duration_int,
                R.integer.settings_default_vibrate_on_key_press_duration_int,
                0,
                100,
                " ms")));
    addSectionLabel(content, tr("Energijos taupymas", "Power saving"));
    final SharedPreferences preferences = keyboardPreferences();
    final String powerKey = getString(R.string.settings_key_power_save_mode);
    content.addView(card(switchRow(
        tr("Taupyti klaviatūros energiją", "Keyboard power saving"),
        tr("Esant mažai baterijai ar energijos taupymo režimui keisti temą ir išjungti vibraciją. Išjungus paliekamos įprastos nuostatos.",
            "Change the theme and disable vibration on low battery or Battery Saver. When off, keep your normal keyboard settings."),
        !"never".equals(preferences.getString(powerKey,
            getString(R.string.settings_default_power_save_mode_value))),
        R.id.advanced_lt_power_saving,
        (ignored, checked) -> preferences.edit()
            .putString(powerKey, checked ? "on_low_battery" : "never").apply())));
    addBottomSpace(content);
  }

  private void showAdvanced() {
    mCurrentScreen = SCREEN_ADVANCED;
    final LinearLayout content = page(tr("Papildomi nustatymai", "Advanced settings"), true);
    addIntro(
        content,
        tr(
            "Rečiau naudojamos klaviatūros elgsenos parinktys.",
            "Less frequently used keyboard behavior options."));

    addSectionLabel(content, tr("Sąsaja", "Interface"));
    content.addView(
        card(
            settingSwitch(
                tr("Papildoma klavišų eilutė", "Extension key row"),
                tr("Virš pagrindinės klaviatūros leidžiama papildoma eilutė", "Allow an extra row above the main keyboard"),
                R.string.settings_key_extension_keyboard_enabled,
                R.bool.settings_default_extension_keyboard_enabled),
            settingSwitch(
                tr("Visas ekranas horizontaliai", "Fullscreen in landscape"),
                tr("Pasukus telefoną teksto laukas užima visą ekraną", "Use a fullscreen text field when the phone is rotated"),
                R.string.settings_key_landscape_fullscreen,
                R.bool.settings_default_landscape_fullscreen),
            settingSwitch(
                tr("Visas ekranas vertikaliai", "Fullscreen in portrait"),
                tr("Laikant telefoną stačiai teksto laukas užima visą ekraną", "Use a fullscreen text field while holding the phone upright"),
                R.string.settings_key_portrait_fullscreen,
                R.bool.settings_default_portrait_fullscreen),
            settingSwitch(
                tr("Derinti navigacijos juostos spalvą", "Match navigation bar color"),
                tr("Apatinė sistemos juosta priderinama prie klaviatūros", "Match the bottom system bar to the keyboard"),
                R.string.settings_key_colorize_nav_bar,
                R.bool.settings_default_colorize_nav_bar)));

    addSectionLabel(content, tr("Redagavimas", "Editing"));
    content.addView(
        card(
            settingSwitch(
                tr("Trinti visą žodį", "Delete whole words"),
                tr("Ilgai laikant trynimo klavišą trinami ištisi žodžiai", "Delete complete words while holding backspace"),
                R.string.settings_key_use_backword,
                R.bool.settings_default_use_backword),
            settingSwitch(
                tr("Rodyti klaidų pranešimus", "Show crash notifications"),
                tr("Leisti pranešti, jei klaviatūra netikėtai sustoja", "Notify you if the keyboard stops unexpectedly"),
                R.string.settings_key_show_chewbacca,
                R.bool.settings_default_show_chewbacca)));
    addBottomSpace(content);
  }

  private void showSetup() {
    mCurrentScreen = SCREEN_SETUP;
    final LinearLayout content = page(tr("Klaviatūros įjungimas", "Enable keyboard"), true);
    addIntro(
        content,
        tr(
            "Du trumpi žingsniai, kad Baltboard būtų paruošta naudoti.",
            "Complete two short steps to start using Baltboard."));

    final boolean enabled = SetupSupport.isThisKeyboardEnabled(this);
    final boolean selected = SetupSupport.isThisKeyboardSetAsDefaultIME(this);
    addSectionLabel(content, tr("1 žingsnis", "Step 1"));
    content.addView(
        guideCard(
            tr("Įjunkite klaviatūrą", "Enable the keyboard"),
            enabled
                ? tr("Įjungta", "Enabled")
                : tr("Android sąraše įjunkite „Baltboard“.", "Enable Baltboard in the Android keyboard list."),
            enabled
                ? tr("Atidaryti klaviatūrų sąrašą", "Open keyboard list")
                : tr("Įjungti klaviatūrą", "Enable keyboard"),
            ignored -> startActivity(new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)),
            enabled));

    addSectionLabel(content, tr("2 žingsnis", "Step 2"));
    content.addView(
        guideCard(
            tr("Pasirinkite klaviatūrą", "Select the keyboard"),
            selected
                ? tr("Pasirinkta kaip dabartinė klaviatūra", "Selected as the current keyboard")
                : tr("Pasirinkite ją Android klaviatūrų lange.", "Select it in the Android keyboard dialog."),
            selected
                ? tr("Keisti klaviatūrą", "Change keyboard")
                : tr("Pasirinkti klaviatūrą", "Select keyboard"),
            ignored ->
                ((android.view.inputmethod.InputMethodManager)
                        getSystemService(Context.INPUT_METHOD_SERVICE))
                    .showInputMethodPicker(),
            selected));
    addBottomSpace(content);
  }

  private void showAi() {
    mCurrentScreen = SCREEN_AI;
    final LinearLayout content = page(tr("AI taisymas ir vertimas", "AI rewrite & translate"), true);
    final SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
    final String[] providers = {"gemini", "openai", "claude", "grok"};
    final String provider = preferences.getString(PREF_PROVIDER, "gemini");
    content.addView(label(tr("AI tiekėjas", "AI provider")));
    android.widget.Spinner selector = new android.widget.Spinner(this);
    android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(this,
        android.R.layout.simple_spinner_item, new String[] {"Google Gemini", "OpenAI", "Anthropic Claude", "xAI Grok"});
    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    selector.setAdapter(adapter);
    selector.setSelection(Math.max(0, java.util.Arrays.asList(providers).indexOf(provider)));
    content.addView(selector, fieldParams());
    selector.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
      @Override public void onNothingSelected(android.widget.AdapterView<?> parent) { }
      @Override public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
        if (!providers[position].equals(provider)) {
          preferences.edit().putString(PREF_PROVIDER, providers[position]).apply();
          showAi();
        }
      }
    });
    addIntro(
        content,
        tr(
            "Tekstas siunčiamas pasirinktam tiekėjui tik paspaudus ✦. Raktai saugomi užšifruoti. Taisymui ir vertimui naudojamos atskiros instrukcijos.",
            "Text is sent to the selected provider only when you tap ✦. Keys are stored encrypted. Rewrite and translation use separate instructions."));

    content.addView(
        card(
            preferenceSwitch(
                tr("Įjungti AI taisymą", "Enable AI rewrite"),
                tr(
                    "Rodyti AI taisymo veiksmą klaviatūros įrankių juostoje",
                    "Show the AI rewrite action in the keyboard toolbar"),
                getSharedPreferences(PREFS_NAME, MODE_PRIVATE),
                PREF_AI_ENABLED,
                true),
            preferenceSwitch(
                tr("Įjungti AI vertimą", "Enable AI translate"),
                tr(
                    "Rodyti AI vertimo veiksmą klaviatūros įrankių juostoje",
                    "Show the AI translation action in the keyboard toolbar"),
                getSharedPreferences(PREFS_NAME, MODE_PRIVATE),
                PREF_AI_TRANSLATE_ENABLED,
                false)));

    final LinearLayout form = new LinearLayout(this);
    form.setOrientation(LinearLayout.VERTICAL);
    form.setPadding(dp(20), dp(18), dp(20), dp(20));
    form.setBackground(rounded(CARD, 28, 0, Color.TRANSPARENT));
    final SecretStore secretStore = new SecretStore(this, provider);

    form.addView(label(tr("API raktas", "API key")));
    mApiKey =
        field(
            secretStore.exists()
                ? tr("API raktas jau išsaugotas", "API key is already saved")
                : tr("Įveskite API raktą", "Enter API key"));
    mApiKey.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
    final FrameLayout apiKeyRow = new FrameLayout(this);
    final FrameLayout.LayoutParams apiKeyParams = new FrameLayout.LayoutParams(-1, dp(56));
    mApiKey.setPadding(dp(14), 0, dp(56), 0);
    apiKeyRow.addView(mApiKey, apiKeyParams);

    final android.widget.ImageButton apiKeyMenu = new android.widget.ImageButton(this);
    apiKeyMenu.setImageResource(R.drawable.advanced_lt_ic_more);
    apiKeyMenu.setColorFilter(PRIMARY);
    apiKeyMenu.setContentDescription(tr("API rakto veiksmai", "API key actions"));
    apiKeyMenu.setBackgroundColor(Color.TRANSPARENT);
    apiKeyMenu.setPadding(dp(12), dp(12), dp(12), dp(12));
    apiKeyRow.addView(apiKeyMenu, new FrameLayout.LayoutParams(dp(48), dp(56), Gravity.END | Gravity.CENTER_VERTICAL));
    form.addView(apiKeyRow, matchWrap());

    form.addView(labelWithTopMargin(tr("Modelis", "Model")));
    mModel = field("gemini".equals(provider) ? DEFAULT_MODEL
        : tr("Modelio ID iš tiekėjo dokumentacijos", "Model ID from your provider's documentation"));
    mModel.setText(preferences.getString(modelPreference(provider), "gemini".equals(provider) ? DEFAULT_MODEL : ""));
    form.addView(mModel, fieldParams());

    final EditText instruction = instructionEditor(
        form,
        tr("Taisymo instrukcija", "Correction instructions"),
        preferences.getString(PREF_PROMPT, GeminiClient.DEFAULT_PROMPT),
        tr("Atkurti numatytąją taisymo instrukciją", "Restore default correction instructions"),
        GeminiClient.DEFAULT_PROMPT);
    final EditText translateInstruction = instructionEditor(
        form,
        tr("Vertimo instrukcija", "Translate instructions"),
        preferences.getString(PREF_TRANSLATE_PROMPT, GeminiClient.DEFAULT_TRANSLATE_PROMPT),
        tr("Atkurti numatytąją vertimo instrukciją", "Restore default translation instructions"),
        GeminiClient.DEFAULT_TRANSLATE_PROMPT);

    final Button save = primaryButton(tr("Išsaugoti", "Save"));
    save.setOnClickListener(
        ignored -> {
          try {
            final String key = mApiKey.getText().toString().trim();
            if (!key.isEmpty()) {
              secretStore.save(key);
              mApiKey.setText("");
              mApiKey.setHint(tr("API raktas jau išsaugotas", "API key is already saved"));
            }
            preferences.edit().putString(modelPreference(provider), mModel.getText().toString().trim())
                .putString(PREF_PROMPT, instruction.getText().toString().isBlank()
                    ? GeminiClient.DEFAULT_PROMPT : instruction.getText().toString())
                .putString(PREF_TRANSLATE_PROMPT, translateInstruction.getText().toString().isBlank()
                    ? GeminiClient.DEFAULT_TRANSLATE_PROMPT : translateInstruction.getText().toString())
                .apply();
            toast(tr("Nustatymai išsaugoti", "Settings saved"));
          } catch (Exception exception) {
            toast(tr("Nepavyko saugiai išsaugoti API rakto", "Could not securely save the API key"));
          }
        });
    form.addView(save, buttonParams());

    apiKeyMenu.setOnClickListener(
        ignored -> {
          final PopupMenu menu = new PopupMenu(this, apiKeyMenu);
          final android.view.MenuItem testItem =
              menu.getMenu().add(tr("Patikrinti ryšį", "Test connection"));
          final android.view.MenuItem removeItem =
              menu.getMenu().add(tr("Pašalinti API raktą", "Remove API key"));
          removeItem.setIcon(android.R.drawable.ic_menu_delete);
          testItem.setOnMenuItemClickListener(
              item -> {
                final String typedKey = mApiKey.getText().toString().trim();
                final String key = typedKey.isEmpty() ? secretStore.get() : typedKey;
                if (key == null || key.isEmpty()) {
                  toast(tr("Pirmiausia įveskite API raktą", "Enter an API key first"));
                  return true;
                }
                item.setEnabled(false);
                item.setTitle(tr("Tikrinama…", "Testing…"));
                mGeminiClient.correct(
                    "Test sentence.",
                    "English",
                    key,
                    mModel.getText().toString().trim(),
                    instruction.getText().toString(),
                    provider,
                    result -> {
                      if (isFinishing() || (Build.VERSION.SDK_INT >= 17 && isDestroyed())) return;
                      item.setEnabled(true);
                      item.setTitle(result.isSuccess() ? tr("Ryšys veikia", "Connection works")
                          : tr("Patikrinti ryšį", "Test connection"));
                      if (result.isSuccess()) toast(tr("Ryšys veikia", "Connection works"));
                      else new android.app.AlertDialog.Builder(this)
                          .setTitle(tr("Ryšio klaida", "Connection error"))
                          .setMessage(result.error).setPositiveButton("OK", null).show();
                    });
                return true;
              });
          removeItem.setOnMenuItemClickListener(
              item -> {
                secretStore.remove();
                mApiKey.setText("");
                mApiKey.setHint(tr("Įveskite API raktą", "Enter API key"));
                toast(tr("API raktas pašalintas", "API key removed"));
                return true;
              });
          menu.show();
        });
    content.addView(form, sectionParams());
    addBottomSpace(content);
  }

  private void showAbout() {
    mCurrentScreen = SCREEN_ABOUT;
    final LinearLayout content = page(tr("Apie", "About"), true);
    final LinearLayout about = new LinearLayout(this);
    about.setOrientation(LinearLayout.VERTICAL);
    about.setPadding(dp(24), dp(24), dp(24), dp(24));
    about.setBackground(rounded(CARD, 28, 0, Color.TRANSPARENT));

    final TextView aboutHeading = text(tr("Apie", "About"), 21f, PRIMARY);
    aboutHeading.setTypeface(Typeface.DEFAULT_BOLD);
    about.addView(aboutHeading);
    final TextView description =
        text(
            tr(
                "Baltboard yra patobulinta ir individualiai pritaikyta klaviatūros versija su moderniu dizainu, specializuotomis lietuviško išdėstymo funkcijomis ir papildomu AI teksto perrašymo palaikymu.",
                "Baltboard is a customized and enhanced keyboard version featuring modern visual designs and specialized functions for Lithuanian layouts, as well as added AI rewrite support."),
            16f,
            PRIMARY);
    description.setPadding(0, dp(10), 0, dp(22));
    about.addView(description);

    final TextView creditsHeading = text(tr("Atvirasis kodas ir autoriai", "Open Source & Credits"), 21f, PRIMARY);
    creditsHeading.setTypeface(Typeface.DEFAULT_BOLD);
    about.addView(creditsHeading);
    final TextView credits =
        text(
            tr(
                "Ši programa sukurta naudojant atvirojo kodo projekto AnySoftKeyboard kodą. Labai vertiname originalių AnySoftKeyboard autorių ir bendruomenės darbą.\n\nPrograma licencijuojama pagal Apache License, Version 2.0 (toliau – Licencija).",
                "This application is based on and includes code from the open-source project AnySoftKeyboard. We highly appreciate the work of the original AnySoftKeyboard contributors.\n\nLicensed under the Apache License, Version 2.0 (the \"License\")."),
            16f,
            PRIMARY);
    credits.setPadding(0, dp(10), 0, dp(18));
    about.addView(credits);

    final TextView repository =
        text(
            tr(
                "Originalus projektas: AnySoftKeyboard\nPirminis kodas: https://github.com/AnySoftKeyboard/AnySoftKeyboard",
                "Original repository: AnySoftKeyboard\nSource code: https://github.com/AnySoftKeyboard/AnySoftKeyboard"),
            15f,
            ACCENT);
    Linkify.addLinks(repository, Linkify.WEB_URLS);
    repository.setMovementMethod(LinkMovementMethod.getInstance());
    about.addView(repository);

    final TextView version =
        text(tr("Versija ", "Version ") + BuildConfig.VERSION_NAME, 14f, SECONDARY);
    version.setPadding(0, dp(4), 0, dp(20));
    about.addView(version);
    content.addView(about, sectionParams());
    addBottomSpace(content);
  }

  private void createSettingsLayout() {
    mSettingsRoot = new FrameLayout(this);
    mSettingsRoot.setId(R.id.advanced_lt_settings_root);
    mSettingsRoot.setBackgroundColor(BACKGROUND);
    mPanes = new LinearLayout(this);
    mPanes.setOrientation(LinearLayout.HORIZONTAL);
    mSettingsRoot.addView(mPanes, new FrameLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT,
        Gravity.TOP | Gravity.CENTER_HORIZONTAL));
    ViewCompat.setOnApplyWindowInsetsListener(mSettingsRoot, (view, windowInsets) -> {
      final Insets safe = windowInsets.getInsets(
          WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout()
              | WindowInsetsCompat.Type.ime());
      view.setPadding(safe.left, safe.top, safe.right, safe.bottom);
      refreshPanes();
      final ScrollView detail = mDetail;
      if (detail != null && windowInsets.isVisible(WindowInsetsCompat.Type.ime())) {
        final View focused = detail.findFocus();
        if (focused instanceof EditText) focused.post(() -> {
          if (detail != mDetail || !focused.hasFocus()) return;
          android.graphics.Rect bounds = new android.graphics.Rect();
          focused.getDrawingRect(bounds);
          detail.offsetDescendantRectToMyCoords(focused, bounds);
          detail.smoothScrollTo(0, Math.max(0, bounds.top - dp(16)));
        });
      }
      return windowInsets;
    });
    mSettingsRoot.addOnLayoutChangeListener(
        (view, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
          if (right - left != oldRight - oldLeft) refreshPanes();
        });
    setContentView(mSettingsRoot);
    ViewCompat.requestApplyInsets(mSettingsRoot);
  }

  private void refreshPanes() {
    if (mNavigation == null) return;
    final int windowWidth = mSettingsRoot.getWidth() > 0
        ? mSettingsRoot.getWidth() : dp(getResources().getConfiguration().screenWidthDp);
    final int availableWidth =
        windowWidth - mSettingsRoot.getPaddingLeft() - mSettingsRoot.getPaddingRight();
    final boolean wide = availableWidth >= dp(TWO_PANE_WIDTH_DP);
    final boolean split = wide && mDetail != null;
    final int margin = dp(wide ? 24 : 16);
    final int width = Math.max(1, Math.min(availableWidth - 2 * margin, dp(split ? 1120 : 600)));
    final var paneParams = (FrameLayout.LayoutParams) mPanes.getLayoutParams();
    if (paneParams.width != width) {
      paneParams.width = width;
      mPanes.setLayoutParams(paneParams);
    }
    final int navigationWidth = split ? Math.min(dp(300), Math.round(width * 0.38f)) : width;
    sizePane(mNavigation, navigationWidth, 0);
    mNavigationHeading.setTextSize(split ? 29f : 36f);
    mNavigationHeading.setMinimumHeight(dp(split ? 72 : 100));
    mNavigation.setVisibility(mDetail == null || split ? View.VISIBLE : View.GONE);
    if (mDetail != null) {
      final int gap = split ? dp(20) : 0;
      sizePane(mDetail, split ? width - navigationWidth - gap : width, gap);
    }
    for (int index = 0; index < mNavigationRows.size(); index++) {
      final View row = mNavigationRows.valueAt(index);
      final boolean selected = split && mNavigationRows.keyAt(index) == mCurrentScreen;
      row.setActivated(selected);
      row.setSelected(selected);
    }
    if (mThemeSummary != null) mThemeSummary.setText(currentThemeName());
  }

  private static void sizePane(ScrollView pane, int width, int gap) {
    final var params = (LinearLayout.LayoutParams) pane.getLayoutParams();
    if (params.width == width && params.getMarginStart() == gap) return;
    params.width = width;
    params.setMarginStart(gap);
    pane.setLayoutParams(params);
  }

  @NonNull
  private LinearLayout page(@NonNull String title, boolean showBack) {
    final boolean navigation = mCurrentScreen == SCREEN_MAIN;
    final ScrollView scroll = new ScrollView(this);
    scroll.setId(navigation ? R.id.advanced_lt_settings_main : R.id.advanced_lt_settings_detail);
    scroll.setFillViewport(true);
    scroll.setClipToPadding(true);
    scroll.setVerticalScrollBarEnabled(false);
    scroll.setFocusableInTouchMode(true);
    final LinearLayout content = new LinearLayout(this);
    content.setOrientation(LinearLayout.VERTICAL);
    content.setPadding(0, dp(8), 0, 0);
    scroll.addView(content, matchWrap());

    final LinearLayout header = new LinearLayout(this);
    header.setGravity(Gravity.CENTER_VERTICAL);
    if (showBack) {
      final android.widget.ImageButton back = new android.widget.ImageButton(this);
      back.setImageResource(R.drawable.advanced_lt_ic_arrow_back);
      back.setColorFilter(PRIMARY);
      back.setBackgroundColor(Color.TRANSPARENT);
      back.setScaleType(android.widget.ImageButton.ScaleType.FIT_CENTER);
      back.setPadding(dp(12), dp(24), dp(12), dp(24));
      back.setContentDescription(tr("Atgal", "Back"));
      back.setOnClickListener(ignored -> showMain());
      header.addView(back, new LinearLayout.LayoutParams(dp(48), dp(72)));
    }
    final TextView heading = text(title, showBack ? 29f : 36f, PRIMARY);
    heading.setGravity(Gravity.CENTER_VERTICAL);
    heading.setTypeface(Typeface.DEFAULT_BOLD);
    heading.setMinimumHeight(dp(showBack ? 72 : 100));
    header.addView(heading, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
    content.addView(header, matchWrap());
    setTitle(title);
    if (navigation) {
      mNavigation = scroll;
      mNavigationHeading = heading;
    } else {
      if (mDetail != null) {
        if (mDetail.findFocus() instanceof EditText) {
          final var controller = ViewCompat.getWindowInsetsController(mDetail);
          if (controller != null) controller.hide(WindowInsetsCompat.Type.ime());
        }
        mPanes.removeView(mDetail);
      }
      mDetail = scroll;
    }
    mPanes.addView(scroll, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT));
    refreshPanes();
    return content;
  }

  private View row(
      int id,
      @NonNull String title,
      @NonNull String summary,
      @NonNull View.OnClickListener listener) {
    final LinearLayout row = new LinearLayout(this);
    if (id != View.NO_ID) row.setId(id);
    row.setOrientation(LinearLayout.HORIZONTAL);
    row.setGravity(Gravity.CENTER_VERTICAL);
    row.setPadding(0, dp(12), 0, dp(12));
    row.setMinimumHeight(dp(78));
    row.setClickable(true);
    row.setFocusable(true);
    row.setOnClickListener(listener);

    final LinearLayout labels = new LinearLayout(this);
    labels.setOrientation(LinearLayout.VERTICAL);
    labels.addView(text(title, 18f, PRIMARY));
    final TextView detail = text(summary, 14f, SECONDARY);
    if (id == R.id.advanced_lt_settings_themes) mThemeSummary = detail;
    detail.setPadding(0, dp(3), 0, 0);
    labels.addView(detail);
    final var labelParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
    labelParams.setMarginStart(dp(20));
    labelParams.setMarginEnd(dp(8));
    row.addView(labels, labelParams);
    final TextView arrow = text("›", 31f, SECONDARY);
    arrow.setGravity(Gravity.CENTER);
    final var arrowParams = new LinearLayout.LayoutParams(dp(26), dp(50));
    arrowParams.setMarginEnd(dp(14));
    row.addView(arrow, arrowParams);
    return row;
  }

  private View settingSwitch(
      @NonNull String title, @NonNull String summary, int keyResource, int defaultResource) {
    return preferenceSwitch(
        title,
        summary,
        keyboardPreferences(),
        getString(keyResource),
        getResources().getBoolean(defaultResource));
  }

  private View preferenceSwitch(
      @NonNull String title,
      @NonNull String summary,
      @NonNull SharedPreferences preferences,
      @NonNull String key,
      boolean defaultValue) {
    return switchRow(title, summary, preferences.getBoolean(key, defaultValue), View.NO_ID,
        (ignored, checked) -> preferences.edit().putBoolean(key, checked).apply());
  }

  private View switchRow(String title, String summary, boolean checked, int id,
      android.widget.CompoundButton.OnCheckedChangeListener listener) {
    final LinearLayout container = new LinearLayout(this);
    container.setOrientation(LinearLayout.HORIZONTAL);
    container.setGravity(Gravity.CENTER_VERTICAL);
    container.setPadding(0, dp(12), 0, dp(12));
    container.setMinimumHeight(dp(78));

    final LinearLayout labels = new LinearLayout(this);
    labels.setOrientation(LinearLayout.VERTICAL);
    labels.addView(text(title, 17f, PRIMARY));
    final TextView detail = text(summary, 14f, SECONDARY);
    detail.setPadding(0, dp(3), 0, 0);
    labels.addView(detail);
    final var labelParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
    labelParams.setMarginStart(dp(20));
    labelParams.setMarginEnd(dp(12));
    container.addView(labels, labelParams);

    final SwitchCompat toggle = new SwitchCompat(this);
    toggle.setId(id);
    toggle.setShowText(false);
    toggle.setChecked(checked);
    toggle.setOnCheckedChangeListener(listener);
    final var toggleParams = new LinearLayout.LayoutParams(dp(58), dp(48));
    toggleParams.setMarginEnd(dp(16));
    container.addView(toggle, toggleParams);
    container.setClickable(true);
    container.setFocusable(true);
    container.setOnClickListener(ignored -> toggle.setChecked(!toggle.isChecked()));
    return container;
  }

  private View choiceSetting(
      @NonNull String title,
      int keyResource,
      int defaultResource,
      @NonNull String[] labels,
      @NonNull String[] values) {
    final SharedPreferences preferences = keyboardPreferences();
    final String key = getString(keyResource);
    final String current = preferences.getString(key, getString(defaultResource));
    int selected = 0;
    for (int index = 0; index < values.length; index++) {
      if (values[index].equals(current)) selected = index;
    }
    final int checked = selected;
    return row(
        View.NO_ID,
        title,
        labels[selected],
        ignored ->
            new AlertDialog.Builder(this)
                .setTitle(title)
                .setSingleChoiceItems(
                    labels,
                    checked,
                    (dialog, which) -> {
                      preferences.edit().putString(key, values[which]).apply();
                      dialog.dismiss();
                      showCorrection();
                    })
                .setNegativeButton(tr("Atšaukti", "Cancel"), null)
                .show());
  }

  private View intSlider(
      @NonNull String title,
      @NonNull String summary,
      int keyResource,
      int defaultResource,
      int minimum,
      int maximum,
      @NonNull String suffix) {
    final SharedPreferences preferences = keyboardPreferences();
    final String key = getString(keyResource);
    final LinearLayout container = sliderContainer(title, summary);
    final TextView value = text("", 14f, SECONDARY);
    value.setGravity(Gravity.END);
    container.addView(value, insetMatchWrap(20, 20));
    final SeekBar seekBar = new SeekBar(this);
    seekBar.setMax(maximum - minimum);
    seekBar.setProgress(
        Math.max(
            0,
            Math.min(
                maximum - minimum,
                preferences.getInt(key, getResources().getInteger(defaultResource)) - minimum)));
    value.setText((seekBar.getProgress() + minimum) + suffix);
    seekBar.setOnSeekBarChangeListener(
        new SeekBar.OnSeekBarChangeListener() {
          @Override
          public void onProgressChanged(SeekBar ignored, int progress, boolean fromUser) {
            final int actual = progress + minimum;
            value.setText(actual + suffix);
            if (fromUser) preferences.edit().putInt(key, actual).apply();
          }

          @Override
          public void onStartTrackingTouch(SeekBar ignored) {}

          @Override
          public void onStopTrackingTouch(SeekBar ignored) {}
        });
    container.addView(seekBar, insetMatchWrap(20, 20));
    return container;
  }

  private View stringSlider(
      @NonNull String title,
      @NonNull String summary,
      int keyResource,
      int defaultResource,
      @NonNull String[] values) {
    final SharedPreferences preferences = keyboardPreferences();
    final String key = getString(keyResource);
    final String current = preferences.getString(key, getString(defaultResource));
    int selected = 0;
    for (int index = 0; index < values.length; index++) {
      if (values[index].equals(current)) selected = index;
    }
    final LinearLayout container = sliderContainer(title, summary);
    final TextView value = text(values[selected], 14f, SECONDARY);
    value.setGravity(Gravity.END);
    container.addView(value, insetMatchWrap(20, 20));
    final SeekBar seekBar = new SeekBar(this);
    seekBar.setMax(values.length - 1);
    seekBar.setProgress(selected);
    seekBar.setOnSeekBarChangeListener(
        new SeekBar.OnSeekBarChangeListener() {
          @Override
          public void onProgressChanged(SeekBar ignored, int progress, boolean fromUser) {
            value.setText(values[progress]);
            if (fromUser) preferences.edit().putString(key, values[progress]).apply();
          }

          @Override
          public void onStartTrackingTouch(SeekBar ignored) {}

          @Override
          public void onStopTrackingTouch(SeekBar ignored) {}
        });
    container.addView(seekBar, insetMatchWrap(20, 20));
    return container;
  }

  private LinearLayout sliderContainer(@NonNull String title, @NonNull String summary) {
    final LinearLayout container = new LinearLayout(this);
    container.setOrientation(LinearLayout.VERTICAL);
    container.setPadding(0, dp(14), 0, dp(10));
    container.addView(text(title, 17f, PRIMARY), insetMatchWrap(20, 20));
    final TextView detail = text(summary, 14f, SECONDARY);
    detail.setPadding(0, dp(3), 0, 0);
    container.addView(detail, insetMatchWrap(20, 20));
    return container;
  }

  private View infoCard(@NonNull String message) {
    final TextView info = text(message, 16f, PRIMARY);
    info.setPadding(dp(22), dp(20), dp(22), dp(20));
    info.setBackground(rounded(CARD, 28, 0, Color.TRANSPARENT));
    info.setLayoutParams(sectionParams());
    return info;
  }

  private View guideCard(
      @NonNull String title,
      @NonNull String summary,
      @NonNull String buttonTitle,
      @NonNull View.OnClickListener listener,
      boolean complete) {
    final LinearLayout guide = new LinearLayout(this);
    guide.setOrientation(LinearLayout.VERTICAL);
    guide.setPadding(dp(22), dp(20), dp(22), dp(20));
    guide.setBackground(rounded(CARD, 28, 0, Color.TRANSPARENT));
    final TextView heading = text((complete ? "✓  " : "") + title, 19f, PRIMARY);
    heading.setTypeface(Typeface.DEFAULT_BOLD);
    guide.addView(heading, matchWrap());
    final TextView detail = text(summary, 15f, complete ? Color.rgb(127, 214, 154) : SECONDARY);
    detail.setPadding(0, dp(7), 0, dp(8));
    guide.addView(detail, matchWrap());
    final Button action = secondaryButton(buttonTitle);
    action.setOnClickListener(listener);
    guide.addView(action, buttonParams());
    guide.setLayoutParams(sectionParams());
    return guide;
  }

  private SharedPreferences keyboardPreferences() {
    return DirectBootAwareSharedPreferences.create(getApplicationContext());
  }

  private View card(View... rows) {
    final LinearLayout card = new LinearLayout(this);
    card.setOrientation(LinearLayout.VERTICAL);
    card.setBackground(rounded(CARD, 28, 0, Color.TRANSPARENT));
    for (int index = 0; index < rows.length; index++) {
      card.addView(rows[index], matchWrap());
      if (index < rows.length - 1) card.addView(divider(dp(22)));
    }
    card.setLayoutParams(sectionParams());
    return card;
  }

  private void addSectionLabel(LinearLayout content, String label) {
    final TextView section = text(label, 14f, SECONDARY);
    section.setTypeface(Typeface.DEFAULT_BOLD);
    final var params = insetMatchWrap(8, 8);
    params.topMargin = dp(16);
    params.bottomMargin = dp(7);
    content.addView(section, params);
  }

  private void addIntro(LinearLayout content, String intro) {
    final TextView view = text(intro, 16f, SECONDARY);
    final var params = insetMatchWrap(8, 8);
    params.bottomMargin = dp(12);
    content.addView(view, params);
  }

  private void addBottomSpace(LinearLayout content) {
    content.addView(new View(this), new LinearLayout.LayoutParams(1, dp(32)));
  }

  private View divider(int startMargin) {
    final View divider = new View(this);
    divider.setBackgroundColor(DIVIDER);
    final LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(1));
    params.leftMargin = startMargin;
    params.rightMargin = dp(18);
    divider.setLayoutParams(params);
    return divider;
  }

  private RadioButton themeChoice(int id, String label, String themeId, String currentId) {
    final RadioButton choice = new RadioButton(this);
    choice.setId(id);
    choice.setText(label);
    choice.setTextColor(PRIMARY);
    choice.setTextSize(18f);
    choice.setGravity(Gravity.CENTER_VERTICAL);
    choice.setPadding(0, dp(8), 0, dp(8));
    final var params = new RadioGroup.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    params.setMarginStart(dp(16));
    params.setMarginEnd(dp(16));
    choice.setLayoutParams(params);
    choice.setMinHeight(dp(68));
    CompoundButtonCompat.setButtonTintList(choice, checkTint());
    choice.setChecked(themeId.equals(currentId));
    return choice;
  }

  private ColorStateList checkTint() {
    return new ColorStateList(
        new int[][] {new int[] {android.R.attr.state_checked}, new int[] {}},
        new int[] {ACCENT, SECONDARY});
  }

  private TextView label(String value) {
    final TextView label = text(value, 15f, SECONDARY);
    label.setTypeface(Typeface.DEFAULT_BOLD);
    return label;
  }

  private TextView labelWithTopMargin(String value) {
    final TextView label = label(value);
    label.setPadding(0, dp(16), 0, 0);
    return label;
  }

  private EditText field(String hint) {
    final EditText field = new EditText(this);
    field.setSingleLine(true);
    field.setTextColor(PRIMARY);
    field.setHintTextColor(SECONDARY);
    field.setTextSize(16f);
    field.setHint(hint);
    field.setPadding(dp(14), 0, dp(14), 0);
    field.setBackground(rounded(FIELD, 14, 1, DIVIDER));
    return field;
  }

  private EditText instructionEditor(
      LinearLayout form,
      String title,
      String value,
      String restoreTitle,
      String defaultValue) {
    form.addView(labelWithTopMargin(title));
    final EditText instruction = field(title);
    instruction.setSingleLine(false);
    instruction.setMinLines(3);
    instruction.setMaxLines(3);
    instruction.setEllipsize(TextUtils.TruncateAt.END);
    instruction.setVerticalFadingEdgeEnabled(true);
    instruction.setFadingEdgeLength(dp(20));
    instruction.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
    instruction.setVerticalScrollBarEnabled(true);
    instruction.setText(value);
    instruction.setGravity(Gravity.TOP | Gravity.START);
    instruction.setPadding(dp(14), dp(14), dp(14), dp(14));

    final FrameLayout.LayoutParams instructionParams = new FrameLayout.LayoutParams(-1, -2);
    final FrameLayout instructionFrame = new FrameLayout(this);
    instructionFrame.addView(instruction, instructionParams);
    final View instructionFade = new View(this);
    instructionFade.setBackground(
        new GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            new int[] {Color.TRANSPARENT, FIELD}));
    instructionFade.setClickable(false);
    final FrameLayout.LayoutParams fadeParams =
        new FrameLayout.LayoutParams(-1, instruction.getLineHeight(), Gravity.BOTTOM);
    fadeParams.setMargins(dp(14), 0, dp(14), dp(14));
    instructionFrame.addView(instructionFade, fadeParams);
    form.addView(instructionFrame, matchWrap());

    instruction.setOnFocusChangeListener((view, focused) -> {
      if (focused) {
        instructionFade.setVisibility(View.GONE);
        instruction.setMaxLines(Integer.MAX_VALUE);
        instruction.setEllipsize(null);
        instruction.setMinLines(6);
        instructionParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        instruction.setLayoutParams(instructionParams);
        view.postDelayed(() -> {
          if (!view.hasFocus()) return;
          ((android.view.inputmethod.InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE))
              .showSoftInput(view, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
          view.requestRectangleOnScreen(
              new android.graphics.Rect(0, 0, view.getWidth(), view.getHeight()), false);
        }, 250);
      } else {
        instructionFade.setVisibility(View.VISIBLE);
        instruction.setMaxLines(3);
        instruction.setEllipsize(TextUtils.TruncateAt.END);
        instruction.setMinLines(3);
        instructionParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        instruction.scrollTo(0, 0);
        instruction.setLayoutParams(instructionParams);
      }
    });
    instruction.setOnClickListener(view -> {
      view.requestFocus();
      ((android.view.inputmethod.InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE))
          .showSoftInput(view, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
    });

    final Button restore = secondaryButton(restoreTitle);
    restore.setOnClickListener(v -> instruction.setText(defaultValue));
    form.addView(restore, buttonParams());
    return instruction;
  }

  private Button primaryButton(String title) {
    final Button button = button(title);
    button.setTextColor(PRIMARY);
    button.setBackground(rounded(ACCENT, 16, 0, Color.TRANSPARENT));
    return button;
  }

  private Button secondaryButton(String title) {
    final Button button = button(title);
    button.setTextColor(PRIMARY);
    button.setBackground(rounded(FIELD, 16, 1, DIVIDER));
    return button;
  }

  private Button button(String title) {
    final Button button = new Button(this);
    button.setText(title);
    button.setTextSize(16f);
    button.setAllCaps(false);
    button.setGravity(Gravity.CENTER);
    button.setMinHeight(0);
    button.setMinimumHeight(0);
    return button;
  }

  private TextView text(String value, float size, int color) {
    final TextView text = new TextView(this);
    text.setText(value);
    text.setTextSize(size);
    text.setTextColor(color);
    text.setLineSpacing(0f, 1.12f);
    return text;
  }

  private GradientDrawable rounded(int color, int radiusDp, int strokeDp, int strokeColor) {
    final GradientDrawable drawable = new GradientDrawable();
    drawable.setColor(color);
    drawable.setCornerRadius(dp(radiusDp));
    if (strokeDp > 0) drawable.setStroke(dp(strokeDp), strokeColor);
    return drawable;
  }

  private LinearLayout.LayoutParams matchWrap() {
    return new LinearLayout.LayoutParams(-1, -2);
  }

  private LinearLayout.LayoutParams sectionParams() {
    final LinearLayout.LayoutParams params = matchWrap();
    params.bottomMargin = dp(12);
    return params;
  }

  private LinearLayout.LayoutParams insetMatchWrap(int start, int end) {
    final var params = matchWrap();
    params.setMarginStart(dp(start));
    params.setMarginEnd(dp(end));
    return params;
  }

  private LinearLayout.LayoutParams fieldParams() {
    final LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(52));
    params.topMargin = dp(7);
    return params;
  }

  private LinearLayout.LayoutParams buttonParams() {
    final LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(52));
    params.topMargin = dp(12);
    return params;
  }

  private String currentThemeName() {
    final String current = AnyApplication.getKeyboardThemeFactory(this).getEnabledAddOn().getId();
    if ("auto".equals(com.anysoftkeyboard.prefs.DirectBootAwareSharedPreferences.create(this)
        .getString(AdvancedLtTheme.MODE, "manual"))) return "Auto";
    if (AdvancedLtTheme.STEVE_LIGHT.equals(current)) return "Steve Light";
    if (AdvancedLtTheme.STEVE_DARK.equals(current)) return "Steve Dark";
    return THEME_WHITE.equals(current) ? "Day" : "Night";
  }

  @NonNull
  private String normalizedModel() {
    final String model = mModel == null ? "" : mModel.getText().toString().trim();
    return model.isEmpty() ? DEFAULT_MODEL : model;
  }

  private int dp(int value) {
    return Math.round(value * getResources().getDisplayMetrics().density);
  }

  private void toast(@NonNull String message) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
  }

  private boolean isLithuanian() {
    final Locale locale;
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
      locale = getResources().getConfiguration().getLocales().get(0);
    } else {
      locale = getResources().getConfiguration().locale;
    }
    return "lt".equalsIgnoreCase(locale.getLanguage());
  }

  @NonNull
  private String tr(@NonNull String lithuanian, @NonNull String english) {
    return isLithuanian() ? lithuanian : english;
  }

  @Override
  protected void onDestroy() {
    mGeminiClient.close();
    super.onDestroy();
  }
}
