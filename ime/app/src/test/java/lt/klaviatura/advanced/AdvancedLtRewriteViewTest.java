package lt.klaviatura.advanced;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import android.view.*;
import android.widget.*;
import com.anysoftkeyboard.AnySoftKeyboardRobolectricTestRunner;
import org.junit.*;
import org.junit.runner.RunWith;
import java.util.*;

@RunWith(AnySoftKeyboardRobolectricTestRunner.class)
public class AdvancedLtRewriteViewTest {
  private List<Button> buttons(View view) {
    List<Button> all=new ArrayList<>();
    if(view instanceof Button) all.add((Button)view);
    if(view instanceof ViewGroup) for(int i=0;i<((ViewGroup)view).getChildCount();i++) all.addAll(buttons(((ViewGroup)view).getChildAt(i)));
    return all;
  }
  @Test public void resultActionsAndCancelAreIndependent() {
    int[] calls=new int[3];
    AdvancedLtRewriteView panel=new AdvancedLtRewriteView(getApplicationContext(),()->calls[0]++);
    panel.result("Corrected text",()->calls[1]++,()->calls[2]++);
    List<Button> actions=buttons(panel);
    for(Button button:actions) { Assert.assertEquals(View.VISIBLE,button.getVisibility()); button.performClick(); }
    Assert.assertArrayEquals(new int[]{1,1,1},calls);
  }
  @Test public void errorOffersRetryWithoutStaleCopyOrReplace() {
    int[] calls=new int[2];
    AdvancedLtRewriteView panel=new AdvancedLtRewriteView(getApplicationContext(),()->calls[0]++);
    panel.result("old",()->Assert.fail(),()->Assert.fail());
    panel.error("Offline",()->calls[1]++);
    List<Button> actions=buttons(panel);
    Assert.assertEquals(View.GONE,actions.get(1).getVisibility());
    Assert.assertEquals(View.GONE,actions.get(2).getVisibility());
    actions.get(0).performClick();
    Assert.assertArrayEquals(new int[]{0,1},calls);
    panel.loading(); actions.get(0).performClick();
    Assert.assertEquals(1,calls[0]);
  }
  @Test public void systemInsetsDoNotAccumulate() {
    AdvancedLtRewriteView panel=new AdvancedLtRewriteView(getApplicationContext(),()->{});
    panel.setBottomOffset(0); int base=panel.getPaddingBottom();
    panel.setBottomOffset(40); panel.setBottomOffset(40);
    Assert.assertEquals(base+40,panel.getPaddingBottom());
  }
}
