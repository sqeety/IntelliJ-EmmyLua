source_id: external-codex-2437e73aa4a9a488
updated_at: 2026-04-08T16:01:55.7077035+08:00
cwd: <workspace>

Fixed IntelliJ intention preview side-effect crash for CreateFieldFromParameterIntention by overriding generatePreview() to return IntentionPreviewInfo.EMPTY. Main compile verified with .\gradlew.bat compileKotlin; targeted test run blocked by pre-existing test dependency/platform test base resolution issue.
