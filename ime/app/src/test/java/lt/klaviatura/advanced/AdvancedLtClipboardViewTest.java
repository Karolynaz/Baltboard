package lt.klaviatura.advanced;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.anysoftkeyboard.AnySoftKeyboardRobolectricTestRunner;
import com.menny.android.anysoftkeyboard.R;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AnySoftKeyboardRobolectricTestRunner.class)
public class AdvancedLtClipboardViewTest {

  private FakeListener mListener;
  private AdvancedLtClipboardView mView;

  @Before
  public void setUp() {
    final FrameLayout parent = new FrameLayout(getApplicationContext());
    parent.layout(0, 0, 1080, 600);
    mListener = new FakeListener();
    mView = AdvancedLtClipboardView.create(getApplicationContext(), parent, mListener);
    mView.setEntries(Arrays.asList("Pirmas tekstas", "Antras tekstas", "Trečias tekstas"));
  }

  @Test
  public void dayThemeAppliesToClipboardAndFade() {
    com.menny.android.anysoftkeyboard.AnyApplication.getKeyboardThemeFactory(getApplicationContext())
        .setAddOnEnabled(AdvancedLtTheme.DAY, true);
    mView.refreshTheme();
    View toolbar = (View)mView.findViewById(R.id.advanced_lt_clipboard_normal_toolbar).getParent();
    Assert.assertEquals(AdvancedLtTheme.surface(getApplicationContext()),
        ((android.graphics.drawable.ColorDrawable)toolbar.getBackground()).getColor());
    Assert.assertEquals(AdvancedLtTheme.surface(getApplicationContext()),
        ((android.graphics.drawable.ColorDrawable)mView.getBackground()).getColor());
    android.widget.ImageButton close = mView.findViewById(R.id.advanced_lt_clipboard_close);
    Assert.assertEquals(android.graphics.Color.rgb(51,51,51), close.getImageTintList().getDefaultColor());
    Assert.assertNotNull(mView.findViewById(R.id.advanced_lt_floating_backspace));
    for (int id : new int[] {R.id.advanced_lt_clipboard_separator,
        R.id.advanced_lt_clipboard_selection_separator, R.id.advanced_lt_clipboard_count_separator,
        R.id.advanced_lt_clipboard_confirmation_separator}) {
      Assert.assertEquals(0x40333333,
          ((android.graphics.drawable.ColorDrawable)mView.findViewById(id).getBackground()).getColor());
    }
  }

  @Test
  public void clipboardUsesTwoColumnsAndFourLineCards() {
    final RecyclerView grid = mView.findViewById(R.id.advanced_lt_clipboard_grid);
    Assert.assertEquals(2, ((GridLayoutManager) grid.getLayoutManager()).getSpanCount());
    Assert.assertEquals(3, grid.getAdapter().getItemCount());

    final View item =
        LayoutInflater.from(getApplicationContext())
            .inflate(R.layout.advanced_lt_clipboard_item, grid, false);
    final TextView itemText = item.findViewById(R.id.advanced_lt_clipboard_item_text);
    Assert.assertEquals(4, itemText.getMaxLines());
  }

  @Test
  public void tappingEntryPastesWithoutClosingClipboard() {
    mView.onEntryClicked(1);
    Assert.assertEquals("Antras tekstas", mListener.pastedText);
    Assert.assertFalse(mView.isSelectionMode());
  }

  @Test
  public void bottomFadeReservesScrollSpaceWithoutAccumulatingOffsets() {
    final RecyclerView grid = mView.findViewById(R.id.advanced_lt_clipboard_grid);
    final View fade = mView.findViewById(R.id.advanced_lt_clipboard_bottom_fade);
    int originalPadding = grid.getPaddingBottom();
    int originalHeight = fade.getLayoutParams().height;
    View backspace = mView.findViewById(R.id.advanced_lt_floating_backspace);
    int bottomGap = Math.round(32 * mView.getResources().getDisplayMetrics().density);
    Assert.assertEquals(bottomGap, ((FrameLayout.LayoutParams)backspace.getLayoutParams()).bottomMargin);
    mView.setBottomOffset(36);
    mView.setBottomOffset(36);
    Assert.assertEquals(originalPadding + 36, grid.getPaddingBottom());
    Assert.assertEquals(originalHeight + 36, fade.getLayoutParams().height);
    Assert.assertEquals(bottomGap + 36, ((FrameLayout.LayoutParams)backspace.getLayoutParams()).bottomMargin);
    Assert.assertFalse(fade.isClickable());
    mView.setBottomOffset(0);
    Assert.assertEquals(originalPadding, grid.getPaddingBottom());
  }

  @Test
  public void deleteModeLetsUserChooseAndConfirmSeveralEntries() {
    mView.findViewById(R.id.advanced_lt_clipboard_start_delete).performClick();
    mView.onEntryClicked(0);
    mView.onEntryClicked(2);

    Assert.assertTrue(mView.isSelectionMode());
    Assert.assertEquals(2, mView.getSelectedCount());
    mView.findViewById(R.id.advanced_lt_clipboard_delete_selected).performClick();
    Assert.assertEquals(
        View.VISIBLE,
        mView.findViewById(R.id.advanced_lt_clipboard_delete_confirmation).getVisibility());
    mView.findViewById(R.id.advanced_lt_clipboard_delete_confirm).performClick();

    Assert.assertEquals(Arrays.asList(0, 2), mListener.deletedPositions);
    Assert.assertFalse(mView.isSelectionMode());
  }

  @Test
  public void historyChangeCancelsDeletionOfStaleIndices() {
    mView.findViewById(R.id.advanced_lt_clipboard_start_delete).performClick();
    mView.onEntryClicked(0);
    mView.findViewById(R.id.advanced_lt_clipboard_delete_selected).performClick();
    mView.setEntries(Arrays.asList("Naujas", "Pirmas tekstas", "Antras tekstas"));
    Assert.assertFalse(mView.isSelectionMode());
    Assert.assertEquals(0, mView.getSelectedCount());
    Assert.assertEquals(View.GONE,
        mView.findViewById(R.id.advanced_lt_clipboard_delete_confirmation).getVisibility());
    Assert.assertTrue(mListener.deletedPositions.isEmpty());
  }

  @Test
  public void backClosesConfirmationThenSelectionMode() {
    mView.findViewById(R.id.advanced_lt_clipboard_start_delete).performClick();
    mView.onEntryClicked(0);
    mView.findViewById(R.id.advanced_lt_clipboard_delete_selected).performClick();

    Assert.assertTrue(mView.handleBack());
    Assert.assertTrue(mView.isSelectionMode());
    Assert.assertTrue(mView.handleBack());
    Assert.assertFalse(mView.isSelectionMode());
    Assert.assertFalse(mView.handleBack());
  }

  private static final class FakeListener implements AdvancedLtClipboardView.Listener {
    private String pastedText;
    private List<Integer> deletedPositions = new ArrayList<>();
    @Override
    public void onClose() {}

    @Override
    public void onPaste(@NonNull CharSequence text) {
      pastedText = text.toString();
    }

    @Override
    public void onDelete(@NonNull List<Integer> positions) {
      deletedPositions = new ArrayList<>(positions);
    }

  }
}
