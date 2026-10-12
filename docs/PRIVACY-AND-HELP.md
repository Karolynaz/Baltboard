# Privacy & troubleshooting

[Back to Baltboard](../README.md)

## Before enabling a keyboard

Android warns that a keyboard can access what you type. This is expected for an input method, but it is still important: install only a keyboard you trust. Avoid sharing screenshots containing private messages, API keys or clipboard entries.

## AI features

AI rewriting and translation are opt-in. Tapping an AI action sends the captured text to the provider selected in settings. This may be the selected passage or the text available from the current editor. Do not use it for confidential material unless you are comfortable with that provider's data handling.

Your API key is stored encrypted on the device using Android Keystore. It is not bundled in the public source or APK. Removing the key in Baltboard removes the locally saved key; it does not revoke it with the provider. Revoke exposed keys through the provider's own console.

The result is shown for review, not silently applied. The keyboard checks for changed text before replacement, but you should always review an AI result for meaning and accuracy.

## Clipboard

The keyboard keeps a local text history when clipboard synchronization is enabled. Android clips marked sensitive are excluded by the reviewed 2.0.35 code. This depends on the copying app supplying that flag; do not assume every secret will be recognized automatically. Delete saved entries you do not want to retain.

## Common questions

### Do I need Lithuanian for AnySoftKeyboard?

No. Lithuanian and English layouts and dictionaries are bundled in this Baltboard build.

### Why are settings in English while I type Lithuanian?

Interface language follows Android's system language. The LT/EN typing layout is a separate setting. Lithuanian system language uses Lithuanian labels; other system languages fall back to English.

### Why does AI say it cannot correct the text?

Check the saved API key, provider, model ID, network connection and provider quota. The model must be available to your API account; creating a key alone does not select a model. Use the API field's **⋯ → Test connection**. Errors are shown without automatically replacing your text.

### Can I use a ChatGPT, Claude or Grok subscription as an API key?

No. Enter a key issued for the selected provider's API. Model access and billing are controlled by that provider. Baltboard does not provide shared API credits.

### The APK update says the package conflicts.

Android requires matching package identity and signing certificates for an update. Do not uninstall your existing keyboard immediately: that would remove its local data. First check that you downloaded the correct Baltboard build and report the exact Android message.

### Is this on Google Play?

This repository currently distributes a personal test APK through GitHub Releases. It is not a production Play Store release.

### How do I report editor-specific bugs?

Include the other app's name/version, phone model, Android version, Baltboard version and exact steps. Examples: pasting, moving the cursor, accepting a suggestion, or holding backspace. Share a harmless reproduction sentence rather than private text.
