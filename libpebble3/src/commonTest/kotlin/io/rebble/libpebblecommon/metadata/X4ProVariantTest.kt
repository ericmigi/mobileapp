package io.rebble.libpebblecommon.metadata

import io.rebble.libpebblecommon.connection.bt.ble.pebble.PebbleLeScanRecord.Companion.decodePebbleScanRecord
import io.rebble.libpebblecommon.packets.WatchVersion.WatchVersionResponse
import io.rebble.libpebblecommon.services.watchInfo
import kotlin.test.BeforeTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class X4ProVariantTest {
    private val tdeck = DeviceProfile(1, "LilyGO T-Deck Pro", "tdeck", 15, "tdeckpro")
    private val previous = DeviceProfiles.current
    @BeforeTest fun importProfile() { DeviceProfiles.load(tdeck.addedTo(previous)) }
    @AfterTest fun restoreProfiles() { DeviceProfiles.load(previous) }

    @Test
    fun discoveryRequiresTheBoardRevisionAndMatchingTransport() {
        fun record(model: Byte, suffix: String) = (
            byteArrayOf(0) + "84C7BB7AF090".encodeToByteArray() +
                byteArrayOf(model, 0, 4, 47, 0, 0) + suffix.encodeToByteArray()
            ).decodePebbleScanRecord().extendedInfo?.advertisedBoard
        assertEquals(WatchHardwarePlatform.XTEINK_X4_PRO, record(15, "x4pro"))
        assertEquals(tdeck.hardware, record(15, "tdeck"))
        assertNull(record(14, "tdeck"))
        assertNull(record(15, ""))
        assertNull(record(15, "other"))
        assertNull(record(14, "x4pro"))
    }

    @Test
    fun modelDescriptionUsesTheReportedBoard() {
        assertEquals("Xteink X4 Pro", WatchColor.Unknown.modelDescription(WatchHardwarePlatform.XTEINK_X4_PRO))
        assertNull(WatchColor.Unknown.modelDescription(null))
        assertNull(WatchColor.Unknown.modelDescription(WatchHardwarePlatform.CORE_ASTERIX))
        assertEquals(WatchColor.TimeBlack.uiDescription, WatchColor.TimeBlack.modelDescription(null))
    }

    @Test
    fun boardDistinguishesReadersAndSelectsTheRightBinary() {
        val response = WatchVersionResponse().apply {
            running.versionTag.set("v4.47.0")
            running.hardwarePlatform.set(15u)
            btAddress.set(UByteArray(6))
            capabilities.set(UByteArray(8))
        }
        val targets = listOf("flint", "xteinkx4pro", "tdeckpro")
        for ((boardName, expected) in listOf(
            "asterix" to WatchHardwarePlatform.CORE_ASTERIX,
            "x4pro" to WatchHardwarePlatform.XTEINK_X4_PRO,
            "tdeck" to tdeck.hardware,
        )) {
            response.board.set(boardName)
            val platform = response.watchInfo(WatchColor.Unknown).platform
            assertEquals(expected, platform)
            assertEquals(expected, WatchHardwarePlatform.fromHWRevision(platform.revision))
            assertEquals(expected.watchType, platform.watchType.getBestVariant(targets))
        }
        assertEquals(WatchType.FLINT, WatchType.X4_PRO.getBestVariant(listOf("flint")))
        assertNull(WatchType.FLINT.getBestVariant(listOf("xteinkx4pro", "tdeckpro")))
        assertNull(tdeck.type.getBestVariant(listOf("flint", "xteinkx4pro")))
    }
}
