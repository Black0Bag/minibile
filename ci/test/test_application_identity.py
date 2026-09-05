from __future__ import annotations

import unittest
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
RELEASE_APPLICATION_ID = "io.github.black0bag.minibile"
DEBUG_APPLICATION_ID = f"{RELEASE_APPLICATION_ID}.debug"
SOURCE_NAMESPACE = "com.ai.assistance.operit"


def text(path: str) -> str:
    return (REPO_ROOT / path).read_text(encoding="utf-8")


class ApplicationIdentityContractTest(unittest.TestCase):
    def test_gradle_separates_install_identity_from_source_namespace(self) -> None:
        gradle = text("app/build.gradle.kts")

        self.assertIn(f'namespace = "{SOURCE_NAMESPACE}"', gradle)
        self.assertIn(f'applicationId = "{RELEASE_APPLICATION_ID}"', gradle)
        self.assertIn('applicationIdSuffix = ".debug"', gradle)
        self.assertNotIn('applicationId = "com.ai.assistance.operit"', gradle)

    def test_shortcuts_target_new_install_ids_and_existing_classes(self) -> None:
        release_shortcuts = text("app/src/main/res/xml/shortcuts.xml")
        debug_shortcuts = text("app/src/debug/res/xml/shortcuts.xml")

        self.assertIn(f'android:targetPackage="{RELEASE_APPLICATION_ID}"', release_shortcuts)
        self.assertIn(f'android:targetPackage="{DEBUG_APPLICATION_ID}"', debug_shortcuts)
        self.assertIn(
            f'android:targetClass="{SOURCE_NAMESPACE}.ui.main.MainActivity"',
            release_shortcuts,
        )
        self.assertNotIn('android:targetPackage="com.ai.assistance.operit', release_shortcuts)
        self.assertNotIn('android:targetPackage="com.ai.assistance.operit', debug_shortcuts)

        for action in (
            "OPEN_VOICE_FLOATING_WINDOW",
            "OPEN_SETTINGS_SHORTCUT",
            "OPEN_DATA_RECOVERY",
        ):
            expected = f'android:action="{RELEASE_APPLICATION_ID}.action.{action}"'
            self.assertIn(expected, release_shortcuts)
            self.assertIn(expected, debug_shortcuts)
        self.assertNotIn(
            'android:action="com.ai.assistance.operit.action.',
            release_shortcuts,
        )
        self.assertNotIn(
            'android:action="com.ai.assistance.operit.action.',
            debug_shortcuts,
        )

    def test_app_owned_service_actions_use_new_install_identity(self) -> None:
        for path in (
            "app/src/main/java/com/ai/assistance/operit/api/chat/AIForegroundService.kt",
            "app/src/main/java/com/ai/assistance/operit/services/FloatingChatService.kt",
        ):
            with self.subTest(path=path):
                value = text(path)
                self.assertIn(f'"{RELEASE_APPLICATION_ID}.action.', value)
                self.assertNotIn('"com.ai.assistance.operit.action.', value)

    def test_operit_editor_source_and_generated_assets_use_new_identity(self) -> None:
        source = text("examples/operit_editor.ts")
        generated = text("examples/operit_editor.js")
        bundled = text("app/src/main/assets/packages/operit_editor.js")
        expected_component = (
            f"{RELEASE_APPLICATION_ID}/"
            f"{SOURCE_NAMESPACE}.core.tools.javascript.ScriptExecutionReceiver"
        )

        self.assertIn(
            f"/sdcard/Android/data/{RELEASE_APPLICATION_ID}/files/packages",
            source,
        )
        self.assertIn(
            f'const SANDBOX_SCRIPT_EXECUTION_ACTION = "{RELEASE_APPLICATION_ID}.EXECUTE_JS"',
            source,
        )
        self.assertIn(expected_component, source)
        for value in (source, generated, bundled):
            self.assertNotIn(
                "/sdcard/Android/data/com.ai.assistance.operit/",
                value,
            )
            self.assertNotIn("com.ai.assistance.operit.DEBUG_INSTALL_TOOLPKG", value)
            self.assertNotIn("com.ai.assistance.operit.EXECUTE_JS", value)
        self.assertEqual(generated, bundled)

    def test_debug_protocol_uses_new_action_and_full_source_class(self) -> None:
        manifest = text("app/src/main/AndroidManifest.xml")
        shell_script = text("tools/adb/execute_js.sh")
        expected_action = f"{RELEASE_APPLICATION_ID}.EXECUTE_JS"
        expected_component = (
            '${APP_PACKAGE}/'
            f"{SOURCE_NAMESPACE}.core.tools.javascript.ScriptExecutionReceiver"
        )

        self.assertIn(f'android:name="{expected_action}"', manifest)
        self.assertIn(f'EXECUTE_JS_ACTION="{expected_action}"', shell_script)
        self.assertIn(expected_component, shell_script)
        self.assertNotIn("com.ai.assistance.operit.EXECUTE_JS", manifest)

    def test_python_debug_tools_only_accept_new_install_ids(self) -> None:
        for path in (
            "tools/example_packages/sync_example_packages.py",
            "tools/toolpkg/debug_toolpkg.py",
        ):
            with self.subTest(path=path):
                value = text(path)
                self.assertIn(f'DEBUG_APP_PACKAGE = "{DEBUG_APPLICATION_ID}"', value)
                self.assertIn(f'RELEASE_APP_PACKAGE = "{RELEASE_APPLICATION_ID}"', value)
                self.assertNotIn('DEBUG_APP_PACKAGE = "com.ai.assistance.operit.debug"', value)
                self.assertNotIn('RELEASE_APP_PACKAGE = "com.ai.assistance.operit"', value)

    def test_instrumentation_targets_debug_install_identity(self) -> None:
        kotlin_test = text(
            "app/src/androidTest/java/com/ai/assistance/operit/ExampleInstrumentedTest.kt"
        )
        bridge_tests = (
            "app/src/androidTest/js/com/ai/assistance/operit/core/tools/javascript/bridge_contract/basic_syntax.js",
            "app/src/androidTest/js/com/ai/assistance/operit/core/tools/javascript/bridge_contract/java_to_js.js",
            "app/src/androidTest/js/com/ai/assistance/operit/core/tools/javascript/bridge_edges/bridge_edges.js",
        )

        self.assertIn(f'assertEquals("{DEBUG_APPLICATION_ID}", appContext.packageName)', kotlin_test)
        for path in bridge_tests:
            with self.subTest(path=path):
                self.assertIn(f"'{DEBUG_APPLICATION_ID}'", text(path))


if __name__ == "__main__":
    unittest.main()