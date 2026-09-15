package com.ai.assistance.operit.core.vibecoding.cloudbuild

import com.ai.assistance.operit.core.vibecoding.domain.BuildBackend
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CloudBuildInspectorTest {

    private val inspector = CloudBuildInspector()

    @Test
    fun `inspect detects android and suggests cloud backend`() {
        val result =
            inspector.inspect(
                listOf(
                    "settings.gradle.kts",
                    "app/build.gradle.kts",
                    "src/main/AndroidManifest.xml",
                ),
            )
        assertTrue(result.ok)
        assertEquals("inspect", result.action)
        assertEquals("android", result.stack?.primary)
        assertEquals(BuildBackend.CLOUD, result.stack?.suggestedBackend)
        assertEquals("android-gradle-apk", result.matchedTemplate?.id)
        assertTrue(result.matchedTemplate!!.signatureRequired)
    }

    @Test
    fun `inspect detects go and suggests local backend`() {
        val result = inspector.inspect(listOf("go.mod", "main.go"))
        assertTrue(result.ok)
        assertEquals("go", result.stack?.primary)
        assertEquals(BuildBackend.LOCAL, result.stack?.suggestedBackend)
        assertEquals("generic-command", result.matchedTemplate?.id)
    }

    @Test
    fun `inspect unknown project falls back with warning`() {
        val result = inspector.inspect(listOf("README.md", "notes.txt"))
        assertFalse(result.ok)
        assertEquals("unknown", result.stack?.primary)
        assertEquals("generic-command", result.matchedTemplate?.id)
        assertTrue(result.warnings.any { it.contains("技术栈未知") })
    }

    @Test
    fun `prepare renders readonly plan preview`() {
        val result = inspector.prepare(listOf("settings.gradle.kts", "app/build.gradle.kts"))
        assertEquals("prepare", result.action)
        assertTrue(result.summary.contains("TEMPORARY private"))
        assertTrue(result.summary.contains("android-gradle-apk"))
        assertTrue(result.summary.contains("TEST_ARTIFACT"))
        assertTrue(result.summary.contains("仅只读，不写远端"))
    }

    @Test
    fun `permission probe reports missing token and scopes without leaking token`() {
        val noToken = inspector.probePermissions(hasToken = false, availableScopes = emptySet())
        assertFalse(noToken.isReady())
        assertTrue(noToken.reason.contains("未配置 GitHub 凭据"))
        assertFalse(noToken.reason.contains("ghp_"))
        assertTrue(noToken.missingScopes.contains("Actions: read"))

        val partial = inspector.probePermissions(hasToken = true, availableScopes = setOf("Actions: read"))
        assertFalse(partial.isReady())
        assertTrue(partial.missingScopes.contains("Contents: read"))

        val ready = inspector.probePermissions(hasToken = true, availableScopes = setOf("Actions: read", "Contents: read"))
        assertTrue(ready.isReady())
        assertTrue(ready.missingScopes.isEmpty())
    }

    @Test
    fun `status renders run summaries`() {
        val runs =
            listOf(
                CloudRunSummary(
                    runId = 1L,
                    workflowName = "CI",
                    headSha = "abcdef123456",
                    status = "completed",
                    conclusion = "success",
                    runUrl = "https://github.com/x/y/actions/runs/1",
                    createdAt = "2026-01-01T00:00:00Z",
                ),
            )
        val result = inspector.status(runs)
        assertTrue(result.ok)
        assertEquals("status", result.action)
        assertTrue(result.summary.contains("#1"))
        assertTrue(result.summary.contains("completed/success"))
        assertEquals(1, result.runs.size)
    }

    @Test
    fun `empty status reports no runs`() {
        val result = inspector.status(emptyList())
        assertTrue(result.ok)
        assertTrue(result.summary.contains("未找到运行记录"))
    }

    @Test
    fun `registry contains all planned templates`() {
        val ids = CloudBuildTemplateRegistry.templates.map { it.id }.toSet()
        assertEquals(
            setOf(
                "android-gradle-apk",
                "flutter-android",
                "node-web",
                "python-package",
                "jvm-gradle",
                "jvm-maven",
                "rust-binary",
                "generic-command",
            ),
            ids,
        )
        assertNotNull(CloudBuildTemplateRegistry.templates.first { it.id == "android-gradle-apk" }.source)
    }
}