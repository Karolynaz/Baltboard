/*
 * Copyright (c) 2013 Menny Even-Danan
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.anysoftkeyboard.devicespecific;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import androidx.annotation.Nullable;
import com.menny.android.anysoftkeyboard.BuildConfig;
import java.util.ArrayList;
import java.util.List;
import java.text.Normalizer;
import org.json.JSONArray;
import org.json.JSONException;

public class ClipboardV11 implements Clipboard {
  private static final int MAX_ENTRIES_COUNT = 20;
  private static final String HISTORY_PREFS_NAME = "advanced_lt_clipboard_history";
  private static final String HISTORY_PREFS_KEY = "entries";

  protected final List<CharSequence> mEntries = new ArrayList<>(MAX_ENTRIES_COUNT);
  protected final ClipboardManager mClipboardManager;
  protected final Context mContext;
  private final SharedPreferences mHistoryPreferences;
  private boolean mListenerWasConfigured;
  @Nullable private ClipboardUpdatedListener mClipboardEntryAddedListener;
  private final ClipboardManager.OnPrimaryClipChangedListener mOsClipboardChangedListener =
      this::onPrimaryClipChanged;

  ClipboardV11(Context context) {
    mContext = context;
    mClipboardManager = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
    mHistoryPreferences =
        context.getSharedPreferences(HISTORY_PREFS_NAME, Context.MODE_PRIVATE);
    if (!BuildConfig.TESTING_BUILD) {
      loadHistory();
    }
  }

  @Override
  public void setClipboardUpdatedListener(@Nullable ClipboardUpdatedListener listener) {
    if (mListenerWasConfigured && mClipboardEntryAddedListener != listener) {
      mEntries.clear();
      persistHistory();
    }
    if (listener == null) {
      mClipboardManager.removePrimaryClipChangedListener(mOsClipboardChangedListener);
    } else if (mClipboardEntryAddedListener != listener) {
      mClipboardManager.addPrimaryClipChangedListener(mOsClipboardChangedListener);
    }
    mClipboardEntryAddedListener = listener;
    mListenerWasConfigured = true;
  }

  @Override
  public CharSequence getText(int entryIndex) {
    return mEntries.get(entryIndex);
  }

  @Override
  public boolean isOsClipboardEmpty() {
    return !mClipboardManager.hasPrimaryClip();
  }

  @Override
  public int getClipboardEntriesCount() {
    return mEntries.size();
  }

  @Override
  public void deleteEntry(int entryIndex) {
    mEntries.remove(entryIndex);
    persistHistory();
    if (entryIndex == 0) {
      clearPrimaryClip();
    }
  }

  protected void clearPrimaryClip() {
    mClipboardManager.setPrimaryClip(ClipData.newPlainText("", ""));
  }

  @Override
  public void deleteAllEntries() {
    mEntries.clear();
    persistHistory();
    clearPrimaryClip();
  }

  protected CharSequence getTextFromClipItem(ClipData.Item item) {
    return item.getText();
  }

  private synchronized void onPrimaryClipChanged() {
    // this is call on every change in the clip:
    // new item, clear clip, updated clip, etc.
    // we will only notify the listener if the clip is new or cleared.
    final var addedListener = mClipboardEntryAddedListener;
    if (addedListener != null) {
      var cp = mClipboardManager.hasPrimaryClip() ? mClipboardManager.getPrimaryClip() : null;
      if (cp != null && cp.getItemCount() > 0) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N
            && cp.getDescription().getExtras() != null
            && cp.getDescription().getExtras().getBoolean("android.content.extra.IS_SENSITIVE", false)) {
          addedListener.onClipboardCleared();
          return;
        }
        // we're only taking the first item!
        // complex clips (multiple items) means that it was a multi-selection
        // copy. This only works if we have the OS doing the paste.
        final var text = getTextFromClipItem(cp.getItemAt(0));
        if (!TextUtils.isEmpty(text)) {
          final String storedText = text.toString();
          for (int index = mEntries.size() - 1; index >= 0; index--) {
            if (entriesMatch(mEntries.get(index), storedText)) mEntries.remove(index);
          }
          mEntries.add(0, storedText);

          while (mEntries.size() > MAX_ENTRIES_COUNT) {
            mEntries.remove(MAX_ENTRIES_COUNT);
          }

          persistHistory();
          addedListener.onClipboardEntryAdded(text);
        } else {
          addedListener.onClipboardCleared();
        }
      } else {
        addedListener.onClipboardCleared();
      }
    }
  }

  private int findEntry(CharSequence text) {
    for (int index = 0; index < mEntries.size(); index++) {
      if (entriesMatch(mEntries.get(index), text)) {
        return index;
      }
    }
    return -1;
  }

  private static boolean entriesMatch(CharSequence first, CharSequence second) {
    return normalizedText(first).equals(normalizedText(second));
  }

  private static String normalizedText(CharSequence text) {
    return Normalizer.normalize(text.toString().replace("\r\n", "\n"), Normalizer.Form.NFC);
  }

  private void loadHistory() {
    final String storedHistory = mHistoryPreferences.getString(HISTORY_PREFS_KEY, "[]");
    try {
      final JSONArray entries = new JSONArray(storedHistory);
      for (int index = 0;
          index < entries.length() && mEntries.size() < MAX_ENTRIES_COUNT;
          index++) {
        final String text = entries.optString(index, "");
        if (!TextUtils.isEmpty(text) && findEntry(text) < 0) {
          mEntries.add(text);
        }
      }
    } catch (JSONException ignored) {
      mHistoryPreferences.edit().remove(HISTORY_PREFS_KEY).apply();
    }
  }

  private void persistHistory() {
    if (BuildConfig.TESTING_BUILD) return;
    final JSONArray entries = new JSONArray();
    for (CharSequence entry : mEntries) {
      entries.put(entry.toString());
    }
    mHistoryPreferences.edit().putString(HISTORY_PREFS_KEY, entries.toString()).apply();
  }
}
