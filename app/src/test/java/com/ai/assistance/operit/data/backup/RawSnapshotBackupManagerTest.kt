package com.ai.assistance.operit.data.backup

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RawSnapshotBackupManagerTest {

    @Test
    fun snapshotPackageName_acceptsMinibilePackagePrefix() {
        assertTrue(isSupportedSnapshotPackageName("io.github.black0bag.minibile"))
        assertTrue(isSupportedSnapshotPackageName("io.github.black0bag.minibile.debug"))
        assertTrue(isSupportedSnapshotPackageName("io.github.black0bag.minibile.clone"))
    }

    @Test
    fun snapshotPackageName_rejectsDifferentPackagePrefix() {
        assertFalse(isSupportedSnapshotPackageName("com.ai.assistance.operit"))
        assertFalse(isSupportedSnapshotPackageName("com.example.operit"))
    }
}
