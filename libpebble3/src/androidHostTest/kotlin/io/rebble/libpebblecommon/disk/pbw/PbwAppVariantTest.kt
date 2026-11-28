package io.rebble.libpebblecommon.disk.pbw

import io.rebble.libpebblecommon.database.entity.asMetadata
import io.rebble.libpebblecommon.metadata.DeviceProfile
import io.rebble.libpebblecommon.metadata.DeviceProfiles
import io.rebble.libpebblecommon.metadata.WatchType
import kotlinx.io.files.Path
import org.junit.Test
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.Instant

class PbwAppVariantTest {

    /**
     * Legacy multi-platform pbw: declares `targetPlatforms: ["chalk"]` in appinfo.json but also
     * ships a root (aplite) build. Emery is aplite-compatible, so we must fall back to the root
     * build instead of refusing the install (the bug behind MOB-7701). The root application.size
     * is 9104 and the chalk build is 2612, so we can confirm the ROOT build was selected.
     */
    @Test
    fun emeryFallsBackToRootApliteBuildNotDeclaredInTargetPlatforms() {
        val app = PbwApp(loadPbw("variant_root_and_chalk.pbw"))

        val variant = app.bestVariantFor(WatchType.EMERY)

        assertEquals(WatchType.APLITE, variant)
        // APLITE resolves to the root build (no aplite/ subdir), proving we picked the root, not chalk.
        assertEquals(9104, app.getManifest(variant!!)!!.application.size)
    }

    @Test
    fun basaltFallsBackToRootApliteBuild() {
        val app = PbwApp(loadPbw("variant_root_and_chalk.pbw"))
        assertEquals(WatchType.APLITE, app.bestVariantFor(WatchType.BASALT))
    }

    @Test
    fun chalkPicksChalkBuild() {
        val app = PbwApp(loadPbw("variant_root_and_chalk.pbw"))

        val variant = app.bestVariantFor(WatchType.CHALK)

        assertEquals(WatchType.CHALK, variant)
        assertEquals(2612, app.getManifest(variant!!)!!.application.size)
    }

    /**
     * A genuine chalk-only pbw (round Pebble Time Round build only, no root build) must NOT install
     * on emery — there is no compatible square build, so selection returns null.
     */
    @Test
    fun chalkOnlyPbwHasNoVariantForEmery() {
        val app = PbwApp(loadPbw("variant_chalk_only.pbw"))
        assertNull(app.bestVariantFor(WatchType.EMERY))
    }

    @Test
    fun chalkOnlyPbwStillInstallsOnChalk() {
        val app = PbwApp(loadPbw("variant_chalk_only.pbw"))
        assertEquals(WatchType.CHALK, app.bestVariantFor(WatchType.CHALK))
    }

    /**
     * The locker entry drives the BlobDB app metadata, which is what actually gates the install.
     * It must agree with [bestVariantFor]: a root (aplite) build undeclared in targetPlatforms
     * still has to yield emery-compatible metadata, or the app silently never syncs to the watch.
     */
    @Test
    fun lockerEntryIncludesRootApliteBuildNotDeclaredInTargetPlatforms() {
        val app = PbwApp(loadPbw("variant_root_and_chalk.pbw"))

        val entry = app.toLockerEntry(Instant.fromEpochSeconds(0), orderIndex = 0)

        assertEquals(
            setOf(WatchType.APLITE.codename, WatchType.CHALK.codename),
            entry.platforms.map { it.name }.toSet(),
        )
        // Icon 11 is the root build's, 22 the chalk one — proving emery resolved to the root build.
        assertEquals(11u, entry.asMetadata(WatchType.EMERY)?.icon?.get())
    }

    @Test
    fun lockerEntryForChalkOnlyPbwHasNoEmeryMetadata() {
        val app = PbwApp(loadPbw("variant_chalk_only.pbw"))

        val entry = app.toLockerEntry(Instant.fromEpochSeconds(0), orderIndex = 0)

        assertEquals(listOf(WatchType.CHALK.codename), entry.platforms.map { it.name })
        assertNull(entry.asMetadata(WatchType.EMERY))
        assertNotNull(entry.asMetadata(WatchType.CHALK))
    }

    /**
     * appinfo.json with no targetPlatforms defaults to aplite, but chalk has no aplite fallback,
     * so trusting that default made every such app "incompatible" on round watches (MOB-11410).
     */
    @Test
    fun lockerEntryUsesShippedBuildsWhenTargetPlatformsIsAbsent() {
        val app = PbwApp(loadPbw("variant_no_target_platforms.pbw"))

        assertEquals(listOf(WatchType.APLITE.codename), app.info.targetPlatforms)

        val entry = app.toLockerEntry(Instant.fromEpochSeconds(0), orderIndex = 0)

        // Icon 32 is the chalk build's, 31 the aplite one.
        assertEquals(32u, entry.asMetadata(WatchType.CHALK)?.icon?.get())
    }

    @Test
    fun getTextFileReadsARootEntryAndIsNullWhenAbsent() {
        val app = PbwApp(loadPbw("variant_root_and_chalk.pbw"))
        assertNotNull(app.getTextFile("appinfo.json"))
        assertNull(app.getTextFile("no-such-config.html"))
    }

    @Test
    fun proPrefersShippedNativeBuildAndFallsBackToFlint() {
        val nativeOnly = appWith("xteinkx4pro")
        assertNull(nativeOnly.bestVariantFor(WatchType.FLINT))
        assertEquals(WatchType.X4_PRO, nativeOnly.bestVariantFor(WatchType.X4_PRO))
        assertEquals(WatchType.FLINT, appWith("flint").bestVariantFor(WatchType.X4_PRO))
        val mixed = appWith("flint", "xteinkx4pro")
        assertEquals(WatchType.X4_PRO, mixed.bestVariantFor(WatchType.X4_PRO))
        assertEquals(WatchType.FLINT, mixed.bestVariantFor(WatchType.FLINT))
    }

    @Test
    fun twoImportedBoardsSelectTheirOwnBinariesAndLockerEntries() {
        val first = DeviceProfile(1, "Reader A", "alpha", 15, "reader_a")
        val second = DeviceProfile(1, "Reader B", "beta", 15, "reader_b", listOf("flint"), "flint")
        val previous = DeviceProfiles.current
        try {
            DeviceProfiles.load(second.addedTo(first.addedTo(previous)))
            val archive = appWith("reader_b", "flint", "reader_a")
            assertEquals(first.type, archive.bestVariantFor(first.type))
            assertEquals(second.type, archive.bestVariantFor(second.type))
            assertEquals(WatchType.FLINT, appWith("flint").bestVariantFor(second.type))
            assertNull(appWith("flint", "reader_b").bestVariantFor(first.type))
            val locker = archive.toLockerEntry(Instant.fromEpochSeconds(0), 0)
            assertEquals(setOf("reader_a", "reader_b", "flint"), locker.platforms.map { it.name }.toSet())
        } finally {
            DeviceProfiles.load(previous)
        }
    }

    private fun appWith(vararg platforms: String): PbwApp {
        val fixture = File(loadPbw("variant_chalk_only.pbw").toString())

        val archive = File.createTempFile("pro-variants", ".pbw").apply { deleteOnExit() }
        ZipFile(fixture).use { source ->
            ZipOutputStream(archive.outputStream()).use { target ->
                for (entry in source.entries()) {
                    val names = if (entry.name.startsWith("chalk/")) {
                        platforms.map { "$it/${entry.name.substringAfter('/')}" }
                    } else listOf(entry.name)
                    for (name in names) {
                        target.putNextEntry(ZipEntry(name))
                        source.getInputStream(entry).use { it.copyTo(target) }
                        target.closeEntry()
                    }
                }
            }
        }
        return PbwApp(Path(archive.absolutePath))
    }

    private fun loadPbw(resourceName: String): Path {
        val inputStream = javaClass.classLoader.getResourceAsStream(resourceName)
            ?: throw AssertionError("Resource not found: $resourceName")
        val tempFile = File.createTempFile("test_file", ".pbw")
        tempFile.deleteOnExit()
        FileOutputStream(tempFile).use { output -> inputStream.use { it.copyTo(output) } }
        return Path(tempFile.absolutePath)
    }
}
