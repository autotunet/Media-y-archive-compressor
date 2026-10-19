package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.core.archive.ArchiveManager
import com.example.data.model.ArchiveCreationSettings
import com.example.data.model.ArchiveOutputFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("CompresorPro", appName)
    }

    @Test
    fun `test zip archive creation and inspection`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val testFile1 = File(context.cacheDir, "test1.txt").apply { writeText("Hello CompresorPro 1") }
        val testFile2 = File(context.cacheDir, "test2.txt").apply { writeText("Hello CompresorPro 2") }
        val zipOut = File(context.cacheDir, "test_archive.zip")

        val archiveManager = ArchiveManager()
        archiveManager.createZipArchive(listOf(testFile1, testFile2), zipOut, 6) { _, _, _ -> }

        assertTrue(zipOut.exists())
        assertTrue(zipOut.length() > 0)

        val entries = archiveManager.inspectArchive(zipOut)
        assertEquals(2, entries.size)
        assertTrue(entries.any { it.name == "test1.txt" })
        assertTrue(entries.any { it.name == "test2.txt" })

        val extractDir = File(context.cacheDir, "extracted_test")
        val extractedFiles = archiveManager.extractArchive(zipOut, extractDir) { _, _, _ -> }
        assertEquals(2, extractedFiles.size)
    }

    @Test
    fun `test operation progress model`() {
        val running = com.example.ui.viewmodel.OperationProgress.Running(0.75f, "Comprimiendo video...")
        assertEquals(0.75f, running.progress, 0.001f)
        assertEquals("Comprimiendo video...", running.message)
    }
}
