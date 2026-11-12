package lt.klaviatura.advanced;

import java.util.ArrayList;
import java.util.List;

public final class AdvancedLtSuggestions {
  private AdvancedLtSuggestions() {}
  public static List<CharSequence> firstThree(List<? extends CharSequence> source) {
    int count = Math.min(3, source.size());
    List<CharSequence> result = new ArrayList<>(count);
    // Some ASK suggestion lists deliberately do not implement subList or listIterator.
    for (int i = 0; i < count; i++) result.add(source.get(i));
    return result;
  }
}
