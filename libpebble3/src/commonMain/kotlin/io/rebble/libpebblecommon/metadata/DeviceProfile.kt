package io.rebble.libpebblecommon.metadata

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Board identifiers from the version packet/advertisement and the PBW directory. */
@Serializable
public data class DeviceProfile(
    val schemaVersion: Int,
    val displayName: String,
    val board: String,
    val protocolNumber: Int,
    val nativePlatform: String,
    val compatiblePlatforms: List<String> = emptyList(),
    val storePlatform: String? = null,
) {
    internal val type: WatchType get() = WatchType(nativePlatform)
    internal val hardware: WatchHardwarePlatform get() = WatchHardwarePlatform(protocolNumber.toUByte(), type, board)

    public companion object {
        public fun parse(json: String): DeviceProfile {
            require(json.length <= 16384) { "Profile is too large" }
            return Json.decodeFromString<DeviceProfile>(json).also { it.validate() }
        }
    }

    internal fun validate() {
        require(schemaVersion == 1) { "Unsupported device profile version" }
        require(displayName.isNotBlank() && displayName.length <= 64 && displayName == displayName.trim() &&
            displayName.none { it.isISOControl() }) { "Invalid display name" }
        require(board.matches(Regex("[a-z0-9][a-z0-9_]{0,7}"))) { "Invalid firmware board marker" }
        require(protocolNumber in 1..255) { "Invalid hardware protocol number" }
        require(nativePlatform.matches(Regex("[a-z][a-z0-9_]{0,31}"))) { "Invalid PBW platform name" }
        require(WatchHardwarePlatform.standard.none { it.revision == board }) { "Board marker is reserved" }
        require(WatchType.standard.none { it.codename == nativePlatform }) { "Native platform is reserved" }
        val stock = WatchType.standard.filter { it != WatchType.X4_PRO }.map { it.codename }
        require(compatiblePlatforms.distinct() == compatiblePlatforms && compatiblePlatforms.all { it in stock }) {
            "Compatibility list must contain distinct stock platforms"
        }
        require(storePlatform == null || storePlatform in compatiblePlatforms) { "Store platform must be compatible" }
    }

    public fun addedTo(profiles: List<DeviceProfile>): List<DeviceProfile> {
        validate()
        val existing = profiles.firstOrNull { it.board == board }
        require(existing == null || existing.nativePlatform == nativePlatform && existing.protocolNumber == protocolNumber) {
            "Board marker already belongs to a different device"
        }
        require(profiles.none { it.board != board && it.nativePlatform == nativePlatform }) {
            "Native platform already belongs to a different device"
        }
        require(existing != null || profiles.size < 64) { "Too many imported device profiles" }
        return profiles.filterNot { it.board == board } + this
    }
}

internal object DeviceProfiles {
    private val profiles = MutableStateFlow(emptyList<DeviceProfile>())
    val current: List<DeviceProfile> get() = profiles.value
    fun load(values: List<DeviceProfile>) { profiles.value = values.toList() }
}
