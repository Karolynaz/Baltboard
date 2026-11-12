package lt.klaviatura.advanced;

import com.anysoftkeyboard.AnySoftKeyboardRobolectricTestRunner;
import org.json.*;
import org.junit.*;
import org.junit.runner.RunWith;

@RunWith(AnySoftKeyboardRobolectricTestRunner.class)
public class GeminiClientTest {
  @Test public void translationPromptAllowsNaturalEnglishTranslation() {
    String prompt = GeminiClient.buildSystemPrompt("lt", GeminiClient.DEFAULT_TRANSLATE_PROMPT);
    Assert.assertTrue(prompt.contains("natural, fluent English"));
    Assert.assertTrue(prompt.contains("rather than translating word for word"));
    Assert.assertFalse(prompt.contains("never translate"));
    Assert.assertTrue(prompt.contains("Return JSON only"));
    Assert.assertTrue(GeminiClient.buildSystemPrompt("lt", GeminiClient.DEFAULT_PROMPT)
        .contains("Do not translate"));
  }
  @Test public void apiErrorExplainsFailureWithoutExposingKey() {
    GeminiClient.Result result = GeminiClient.parseApiError(400,
        "{\"error\":{\"status\":\"INVALID_ARGUMENT\",\"message\":\"Invalid key secret-value\"}}", "secret-value");
    Assert.assertFalse(result.isSuccess());
    Assert.assertTrue(result.error.contains("HTTP 400"));
    Assert.assertTrue(result.error.contains("INVALID_ARGUMENT"));
    Assert.assertFalse(result.error.contains("secret-value"));
    Assert.assertFalse(GeminiClient.parseApiError(503, "not JSON", "").isSuccess());
  }
  private String response(String finish,String payload) throws Exception {
    return new JSONObject().put("candidates",new JSONArray().put(new JSONObject()
        .put("finishReason",finish).put("content",new JSONObject().put("parts",new JSONArray()
            .put(new JSONObject().put("text",payload)))))).toString();
  }
  @Test public void correctedTextPreservesNewlines() throws Exception {
    String text = "Hello.\n\nHow are you?";
    Assert.assertEquals(text,GeminiClient.parseResponse("wrong",response("STOP",
        new JSONObject().put("status","corrected").put("text",text).toString())).text);
  }
  @Test public void unchangedAlwaysUsesOriginal() {
    Assert.assertEquals("original",GeminiClient.parseCorrection("original","{\"status\":\"unchanged\",\"text\":\"different\"}").text);
  }
  @Test public void truncatedBlockedAndMalformedResultsCannotBeApplied() throws Exception {
    Assert.assertFalse(GeminiClient.parseResponse("original",response("MAX_TOKENS","{}")).isSuccess());
    Assert.assertFalse(GeminiClient.parseResponse("original","{}").isSuccess());
    Assert.assertFalse(GeminiClient.parseCorrection("original","not JSON").isSuccess());
    Assert.assertFalse(GeminiClient.parseCorrection("original","{\"status\":\"unable\",\"text\":\"reason\"}").isSuccess());
    Assert.assertFalse(GeminiClient.parseCorrection("original","{\"status\":\"corrected\",\"text\":\"\"}").isSuccess());
  }
  @Test public void customPromptKeepsTechnicalResponseContract() {
    String prompt=GeminiClient.buildSystemPrompt("lt","Keep the tone casual.");
    Assert.assertTrue(prompt.contains("Keep the tone casual."));
    Assert.assertTrue(prompt.contains("Return JSON only"));
    Assert.assertTrue(GeminiClient.buildSystemPrompt("en","").contains(GeminiClient.DEFAULT_PROMPT));
  }
  @Test public void otherProviderResponsesParsedAndTruncationRejected() throws Exception {
    String payload="{\"status\":\"corrected\",\"text\":\"Fixed.\"}";
    JSONObject claude=new JSONObject().put("stop_reason","end_turn").put("content",new JSONArray()
        .put(new JSONObject().put("type","text").put("text",payload)));
    Assert.assertEquals("Fixed.",GeminiClient.parseOtherResponse("text",claude.toString(),true).text);
    claude.put("stop_reason","max_tokens");
    Assert.assertFalse(GeminiClient.parseOtherResponse("text",claude.toString(),true).isSuccess());
    JSONObject openai=new JSONObject().put("status","completed").put("output",new JSONArray()
        .put(new JSONObject().put("type","message").put("content",new JSONArray()
            .put(new JSONObject().put("type","output_text").put("text",payload)))));
    Assert.assertEquals("Fixed.",GeminiClient.parseOtherResponse("text",openai.toString(),false).text);
    openai.put("status","incomplete");
    Assert.assertFalse(GeminiClient.parseOtherResponse("text",openai.toString(),false).isSuccess());
  }
}
