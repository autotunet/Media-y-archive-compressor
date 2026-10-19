package com.example.core.iso

import java.io.File
import java.io.InputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

data class IsoEntry(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val sectorOffset: Long,
    val size: Long,
    val dateString: String = ""
)

class Iso9660Reader(private val isoFile: File) {

    private val SECTOR_SIZE = 2048

    fun listEntries(): List<IsoEntry> {
        val entries = mutableListOf<IsoEntry>()
        RandomAccessFile(isoFile, "r").use { raf ->
            val rootRecord = findRootDirectoryRecord(raf) ?: return emptyList()
            parseDirectory(raf, rootRecord.extentSector, rootRecord.dataLength, "", entries)
        }
        return entries
    }

    fun extractEntry(entry: IsoEntry, targetFile: File, onProgress: ((bytesRead: Long, total: Long) -> Unit)? = null) {
        if (entry.isDirectory) {
            targetFile.mkdirs()
            return
        }

        targetFile.parentFile?.mkdirs()
        RandomAccessFile(isoFile, "r").use { raf ->
            raf.seek(entry.sectorOffset * SECTOR_SIZE)
            targetFile.outputStream().use { fos ->
                val buffer = ByteArray(64 * 1024)
                var remaining = entry.size
                var totalRead = 0L

                while (remaining > 0) {
                    val toRead = remaining.coerceAtMost(buffer.size.toLong()).toInt()
                    val read = raf.read(buffer, 0, toRead)
                    if (read <= 0) break
                    fos.write(buffer, 0, read)
                    remaining -= read
                    totalRead += read
                    onProgress?.invoke(totalRead, entry.size)
                }
            }
        }
    }

    fun getInputStreamForEntry(entry: IsoEntry): InputStream? {
        if (entry.isDirectory) return null
        val raf = RandomAccessFile(isoFile, "r")
        raf.seek(entry.sectorOffset * SECTOR_SIZE)
        return object : InputStream() {
            private var remaining = entry.size

            override fun read(): Int {
                if (remaining <= 0) return -1
                val b = raf.read()
                if (b >= 0) remaining--
                return b
            }

            override fun read(b: ByteArray, off: Int, len: Int): Int {
                if (remaining <= 0) return -1
                val toRead = remaining.coerceAtMost(len.toLong()).toInt()
                val read = raf.read(b, off, toRead)
                if (read > 0) remaining -= read
                return read
            }

            override fun close() {
                raf.close()
            }
        }
    }

    private data class DirectoryRecordHeader(
        val extentSector: Long,
        val dataLength: Long,
        val isJoliet: Boolean = false
    )

    private fun findRootDirectoryRecord(raf: RandomAccessFile): DirectoryRecordHeader? {
        val buffer = ByteArray(SECTOR_SIZE)
        var sector = 16 // System area is sectors 0..15, Volume descriptors start at sector 16
        var primaryRoot: DirectoryRecordHeader? = null
        var jolietRoot: DirectoryRecordHeader? = null

        while (sector < 32) {
            raf.seek(sector.toLong() * SECTOR_SIZE)
            val read = raf.read(buffer)
            if (read < SECTOR_SIZE) break

            val type = buffer[0].toInt() and 0xFF
            val magic = String(buffer, 1, 5, StandardCharsets.ISO_8859_1)
            if (magic != "CD001") break

            if (type == 1) { // Primary Volume Descriptor (PVD)
                val rootDirRecord = parseRootRecordFromPvd(buffer, false)
                if (rootDirRecord != null) primaryRoot = rootDirRecord
            } else if (type == 2) { // Supplementary Volume Descriptor (SVD - Joliet)
                // Escape sequences for Joliet: UCS-2 Level 1 (%/@), Level 2 (%/C), Level 3 (%/E)
                val rootDirRecord = parseRootRecordFromPvd(buffer, true)
                if (rootDirRecord != null) jolietRoot = rootDirRecord
            } else if (type == 255) { // Volume Descriptor Set Terminator
                break
            }
            sector++
        }

        // Prefer Joliet (supports long unicode filenames) if available, otherwise Primary
        return jolietRoot ?: primaryRoot
    }

    private fun parseRootRecordFromPvd(buffer: ByteArray, isJoliet: Boolean): DirectoryRecordHeader? {
        val rootOffset = 156 // Offset in PVD where 34-byte root directory record resides
        if (rootOffset + 34 > buffer.size) return null

        val extentSector = readUint32LE(buffer, rootOffset + 2)
        val dataLength = readUint32LE(buffer, rootOffset + 10)
        return DirectoryRecordHeader(extentSector, dataLength, isJoliet)
    }

    private fun parseDirectory(
        raf: RandomAccessFile,
        sector: Long,
        length: Long,
        currentPath: String,
        outList: MutableList<IsoEntry>
    ) {
        val buffer = ByteArray(SECTOR_SIZE)
        var sectorsRemaining = ((length + SECTOR_SIZE - 1) / SECTOR_SIZE).toInt()
        var currentSector = sector

        while (sectorsRemaining > 0) {
            raf.seek(currentSector * SECTOR_SIZE)
            val read = raf.read(buffer)
            if (read < SECTOR_SIZE) break

            var offset = 0
            while (offset < SECTOR_SIZE) {
                val recordLen = buffer[offset].toInt() and 0xFF
                if (recordLen == 0) {
                    // Padding at the end of the sector, skip to next sector
                    break
                }
                if (offset + recordLen > SECTOR_SIZE) break

                val extentLoc = readUint32LE(buffer, offset + 2)
                val dataLen = readUint32LE(buffer, offset + 10)
                val flags = buffer[offset + 25].toInt() and 0xFF
                val isDir = (flags and 0x02) != 0
                val fileIdLen = buffer[offset + 32].toInt() and 0xFF

                if (offset + 33 + fileIdLen <= offset + recordLen) {
                    val rawName = buffer.copyOfRange(offset + 33, offset + 33 + fileIdLen)
                    val name = parseFileName(rawName, flags)

                    // Skip '.' (0x00) and '..' (0x01) records
                    if (name.isNotEmpty() && name != "." && name != "..") {
                        val fullPath = if (currentPath.isEmpty()) name else "$currentPath/$name"
                        val entry = IsoEntry(
                            name = name,
                            path = fullPath,
                            isDirectory = isDir,
                            sectorOffset = extentLoc,
                            size = if (isDir) 0L else dataLen
                        )
                        outList.add(entry)

                        if (isDir && extentLoc != currentSector) {
                            // Recursively parse subdirectory
                            parseDirectory(raf, extentLoc, dataLen, fullPath, outList)
                        }
                    }
                }

                offset += recordLen
            }

            currentSector++
            sectorsRemaining--
        }
    }

    private fun parseFileName(raw: ByteArray, flags: Int): String {
        if (raw.isEmpty()) return ""
        if (raw.size == 1 && raw[0].toInt() == 0) return "."
        if (raw.size == 1 && raw[0].toInt() == 1) return ".."

        // Check if Joliet UCS-2 Big Endian
        var isUcs2 = false
        if (raw.size >= 2 && raw.size % 2 == 0) {
            // In Joliet ASCII characters typically have 0x00 in byte 0
            if (raw[0].toInt() == 0 && raw[1].toInt() != 0) {
                isUcs2 = true
            }
        }

        var name = if (isUcs2) {
            String(raw, StandardCharsets.UTF_16BE)
        } else {
            String(raw, StandardCharsets.ISO_8859_1)
        }

        // Clean up ISO 9660 version suffix ';1' or trailing dot
        val semiIdx = name.indexOf(';')
        if (semiIdx != -1) {
            name = name.substring(0, semiIdx)
        }
        if (name.endsWith(".")) {
            name = name.substring(0, name.length - 1)
        }
        return name.trim()
    }

    private fun readUint32LE(buffer: ByteArray, offset: Int): Long {
        val b0 = buffer[offset].toLong() and 0xFF
        val b1 = buffer[offset + 1].toLong() and 0xFF
        val b2 = buffer[offset + 2].toLong() and 0xFF
        val b3 = buffer[offset + 3].toLong() and 0xFF
        return b0 or (b1 shl 8) or (b2 shl 16) or (b3 shl 24)
    }
}
