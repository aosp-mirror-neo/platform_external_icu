# ICU Upstream API Update Steps

Here are the steps to perform when exposing a new API from ICU in the Android SDK:

1.  Change `tools/srcgen/src/main/java/com/android/icu4j/srcgen/Icu4jTransform.java`. Particularly, make sure that the APIs are not in the list of `DECLARATIONS_TO_HIDE`

2.  Flag the change:
    1.  Add a new flag in `icu.aconfig` or use an existing one.
    2.  Map the new API to the flag in `tools/srcgen/flagged-api.json`.

3.  Regenerate `android_icu4j/` by running the following script and command: `tools/srcgen/generate_android_icu4j.sh`

4.  Run `m i18n.module.public.api.stubs.source-update-current-api` to update `current.txt`.

5.  Run `m droid`.

6.  Run `atest CtsIcuTestCases / CtsIcu4cTestCases` to test the changes.
    *   If the API isn't covered by existing tests in `CtsIcu4cTestCases`, please add a new test case in `android_icu4j/testing/src/android/icu/extratest/`.

7. If everything goes well and tests pass, commit the changes to the repo:
        ```bash
        git add -A
        git commit -> Add your commit message there
        ```
