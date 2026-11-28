package io.rebble.libpebblecommon.metadata

import com.russhwolf.settings.MapSettings
import io.rebble.libpebblecommon.LibPebbleConfig
import io.rebble.libpebblecommon.LibPebbleConfigHolder
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class DeviceProfileTest {
    private val json = """{"schemaVersion":1,"displayName":"Reader","board":"reader","protocolNumber":15,"nativePlatform":"my_reader"}"""

    @Test
    fun persistsDistinctProfilesAndResolvesExactBoardAfterRestart() {
        val old = DeviceProfiles.current
        try {
            val settings = MapSettings()
            val holder = LibPebbleConfigHolder(LibPebbleConfig(), settings, Json)
            val first = DeviceProfile.parse(json)
            val second = first.copy(displayName = "Second", board = "second", nativePlatform = "reader2")
            val saved = second.addedTo(first.addedTo(emptyList()))
            val config = holder.config.value
            holder.update(config.copy(watchConfig = config.watchConfig.copy(deviceProfiles = saved)))
            DeviceProfiles.load(emptyList())
            val restored = LibPebbleConfigHolder(LibPebbleConfig(), settings, Json)
            assertEquals(saved, restored.config.value.watchConfig.deviceProfiles)
            assertEquals(first.hardware, WatchHardwarePlatform.fromBoardMarker(15u, "reader"))
            assertEquals(second.hardware, WatchHardwarePlatform.fromHWRevision("second"))
            assertNull(WatchHardwarePlatform.fromBoardMarker(14u, "reader"))
            assertNull(WatchHardwarePlatform.fromBoardMarker(15u, "read"))
            assertEquals(WatchHardwarePlatform.CORE_ASTERIX, WatchHardwarePlatform.fromProtocolNumber(15u))
        } finally {
            DeviceProfiles.load(old)
        }
    }

    @Test
    fun rejectsCollisionsInvalidPathsAndUnknownVersions() {
        val profile = DeviceProfile.parse(json)
        val existing = listOf(profile)
        assertEquals(existing, profile.addedTo(existing))
        for (invalid in listOf(
            profile.copy(schemaVersion = 2), profile.copy(board = "asterix"), profile.copy(nativePlatform = "flint"),
            profile.copy(board = "../foo"), profile.copy(nativePlatform = "../bad"), profile.copy(protocolNumber = 0),
            profile.copy(nativePlatform = "other"), profile.copy(board = "other"),
            profile.copy(compatiblePlatforms = listOf("other")), profile.copy(storePlatform = "flint"),
        )) {
            assertFailsWith<IllegalArgumentException> { invalid.addedTo(existing) }
        }
        assertFailsWith<IllegalArgumentException> { DeviceProfile.parse("{}") }
    }
}
