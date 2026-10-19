package com.example

import com.example.core.ads.AdMobManager
import com.example.data.model.ArchiveCreationSettings
import com.example.data.model.ArchiveOutputFormat
import com.example.data.model.CompressionLevelPreset
import com.example.data.model.ProcessedFile
import com.example.ui.components.ArchiveFileIconType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testFormatBytes() {
        assertEquals("0 B", ProcessedFile.formatBytes(0))
        assertEquals("500 B", ProcessedFile.formatBytes(500))
        assertEquals("1.0 KB", ProcessedFile.formatBytes(1024))
        assertEquals("1.5 MB", ProcessedFile.formatBytes((1.5 * 1024 * 1024).toLong()))
    }

    @Test
    fun testArchiveFileIconType() {
        assertEquals(ArchiveFileIconType.ZIP, ArchiveFileIconType.fromExtension("zip"))
        assertEquals(ArchiveFileIconType.ZIP, ArchiveFileIconType.fromExtension("ZIP"))
        assertEquals(ArchiveFileIconType.RAR, ArchiveFileIconType.fromExtension("rar"))
        assertEquals(ArchiveFileIconType.SEVEN_ZIP, ArchiveFileIconType.fromExtension("7z"))
        assertEquals(ArchiveFileIconType.ISO, ArchiveFileIconType.fromExtension("iso"))
        assertEquals(ArchiveFileIconType.ISO, ArchiveFileIconType.fromExtension("ISO"))
        assertNull(ArchiveFileIconType.fromExtension("mp4"))
        assertNull(ArchiveFileIconType.fromExtension("pdf"))
    }

    @Test
    fun testCompressionLevelPresets() {
        assertEquals(0, CompressionLevelPreset.STORE.level)
        assertEquals(2, CompressionLevelPreset.FASTEST.level)
        assertEquals(6, CompressionLevelPreset.NORMAL.level)
        assertEquals(9, CompressionLevelPreset.BEST_COMPRESSION.level)

        val settings = ArchiveCreationSettings(
            format = ArchiveOutputFormat.ZIP,
            archiveName = "Respaldo",
            compressionLevel = CompressionLevelPreset.BEST_COMPRESSION.level,
            levelPreset = CompressionLevelPreset.BEST_COMPRESSION
        )
        assertEquals(ArchiveOutputFormat.ZIP, settings.format)
        assertEquals(9, settings.compressionLevel)
        assertEquals(CompressionLevelPreset.BEST_COMPRESSION, settings.levelPreset)
    }

    @Test
    fun testAdMobAdUnitsConfiguration() {
        assertEquals(2, AdMobManager.BANNER_AD_UNIT_IDS.size)
        assertEquals("ca-app-pub-1495262574338316/2803163788", AdMobManager.BANNER_AD_UNIT_IDS[0])
        assertEquals("ca-app-pub-1495262574338316/2903409205", AdMobManager.BANNER_AD_UNIT_IDS[1])

        assertEquals(6, AdMobManager.INTERSTITIAL_AD_UNIT_IDS.size)
        assertEquals("ca-app-pub-1495262574338316/2452733430", AdMobManager.INTERSTITIAL_AD_UNIT_IDS[0])
        assertEquals("ca-app-pub-1495262574338316/3503687777", AdMobManager.INTERSTITIAL_AD_UNIT_IDS[5])

        val banner1 = AdMobManager.getNextBannerAdUnitId()
        val banner2 = AdMobManager.getNextBannerAdUnitId()
        assertTrue(banner1.startsWith("ca-app-pub-1495262574338316/"))
        assertTrue(banner2.startsWith("ca-app-pub-1495262574338316/"))
    }

    @Test
    fun testAppThemeModes() {
        assertEquals(3, com.example.data.preferences.AppThemeMode.values().size)
        assertEquals("Modo Claro", com.example.data.preferences.AppThemeMode.LIGHT.title)
        assertEquals("Modo Oscuro", com.example.data.preferences.AppThemeMode.DARK.title)
        assertEquals("Automático", com.example.data.preferences.AppThemeMode.SYSTEM.title)
    }
}
