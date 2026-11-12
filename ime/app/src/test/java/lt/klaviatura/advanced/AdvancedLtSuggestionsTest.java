package lt.klaviatura.advanced;

import java.util.AbstractList;
import java.util.Arrays;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

public class AdvancedLtSuggestionsTest {
  @Test public void limitedListDoesNotRequireSubList() {
    List<CharSequence> source = new AbstractList<CharSequence>() {
      public int size() { return 5; }
      public CharSequence get(int index) { return "word" + index; }
      public List<CharSequence> subList(int from, int to) { throw new UnsupportedOperationException(); }
    };
    Assert.assertEquals(Arrays.asList("word0", "word1", "word2"), AdvancedLtSuggestions.firstThree(source));
    Assert.assertTrue(AdvancedLtSuggestions.firstThree(java.util.Collections.emptyList()).isEmpty());
  }
}
