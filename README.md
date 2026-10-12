# Baltboard

Customized Lithuanian and English Android keyboard based on [AnySoftKeyboard](https://github.com/AnySoftKeyboard/AnySoftKeyboard), licensed under Apache-2.0.

## Downloads

Open [Releases](https://github.com/Karolynaz/Baltboard/releases) and download `Baltboard-v2.0.35.apk` to an Android 6.0+ phone. Allow installation from your browser when Android asks, install the APK, open Baltboard, and follow its keyboard enablement guide.

This is a **test release**, not a Google Play production release. It is non-debuggable but signed with the local Android debug certificate. AI rewrite and translation are optional and require your own API key.

## Source snapshot

The complete reviewed source is provided as `Baltboard-v2.0.35-source.zip` in the release assets. The repository's main branch is not yet the full application source; Git push currently lacks write authorization. Use the named source asset rather than GitHub's automatically generated source archives.

The source archive excludes build outputs, local SDK paths, API keys, signing keys, and upstream deployment workflows. It includes build instructions, credits and regression tests.

## Review verification

125 selected Android unit tests passed, one was skipped. The personal APK was assembled and its signature verified. Fixes include stale AI text replacement, retaining AI preview on replacement failure, sensitive clipboard exclusion, hidden-IME clipboard polling, Android 6 compatibility, and settings lifecycle/key-state handling.
