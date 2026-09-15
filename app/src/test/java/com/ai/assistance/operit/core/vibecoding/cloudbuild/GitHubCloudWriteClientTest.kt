package com.ai.assistance.operit.core.vibecoding.cloudbuild

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GitHubCloudWriteClientTest {

    @Test
    fun `repo name validation accepts valid names`() {
        assertTrue(GitHubCloudWriteClient.isValidRepoName("vc-build-demo"))
        assertTrue(GitHubCloudWriteClient.isValidRepoName("my-repo-1"))
        assertTrue(GitHubCloudWriteClient.isValidRepoName("a"))
    }

    @Test
    fun `repo name validation rejects invalid names`() {
        assertFalse(GitHubCloudWriteClient.isValidRepoName(""))
        assertFalse(GitHubCloudWriteClient.isValidRepoName("MyRepo"))
        assertFalse(GitHubCloudWriteClient.isValidRepoName("repo_with_underscore"))
        assertFalse(GitHubCloudWriteClient.isValidRepoName("-leading-dash"))
        assertFalse(GitHubCloudWriteClient.isValidRepoName("a".repeat(101)))
    }

    @Test
    fun `upload path validation accepts safe relative paths`() {
        assertTrue(GitHubCloudWriteClient.isValidUploadPath("README.md"))
        assertTrue(GitHubCloudWriteClient.isValidUploadPath("src/main.js"))
        assertTrue(GitHubCloudWriteClient.isValidUploadPath(".github/workflows/ci.yml"))
    }

    @Test
    fun `upload path validation rejects traversal and absolute paths`() {
        assertFalse(GitHubCloudWriteClient.isValidUploadPath(""))
        assertFalse(GitHubCloudWriteClient.isValidUploadPath("/etc/passwd"))
        assertFalse(GitHubCloudWriteClient.isValidUploadPath("../secret"))
        assertFalse(GitHubCloudWriteClient.isValidUploadPath("a\\b"))
    }

    @Test
    fun `release verification completeness`() {
        val complete =
            ReleaseVerification(
                tag = "v1.0.0",
                exists = true,
                assetNames = listOf("app.apk"),
                digestVerified = true,
                signatureRequired = true,
                signatureVerified = true,
            )
        assertTrue(complete.isComplete())

        val missingDigest = complete.copy(digestVerified = false)
        assertFalse(missingDigest.isComplete())

        val missingSignature = complete.copy(signatureVerified = false)
        assertFalse(missingSignature.isComplete())

        val notExists = complete.copy(exists = false, assetNames = emptyList())
        assertFalse(notExists.isComplete())
    }

    @Test
    fun `secret name validation accepts valid names`() {
        assertTrue(GitHubCloudWriteClient.isValidSecretName("MY_SECRET"))
        assertTrue(GitHubCloudWriteClient.isValidSecretName("release_token"))
        assertTrue(GitHubCloudWriteClient.isValidSecretName("API_KEY_2026"))
    }

    @Test
    fun `secret name validation rejects invalid names`() {
        assertFalse(GitHubCloudWriteClient.isValidSecretName(""))
        assertFalse(GitHubCloudWriteClient.isValidSecretName("GITHUB_TOKEN"))
        assertFalse(GitHubCloudWriteClient.isValidSecretName("secret-with-dash"))
        assertFalse(GitHubCloudWriteClient.isValidSecretName("secret with space"))
    }
}