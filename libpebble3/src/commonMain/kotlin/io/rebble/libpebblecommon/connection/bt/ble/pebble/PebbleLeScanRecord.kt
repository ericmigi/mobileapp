package io.rebble.libpebblecommon.connection.bt.ble.pebble

import io.rebble.libpebblecommon.metadata.WatchHardwarePlatform

public data class PebbleLeScanRecord internal constructor(
    val payloadType: Byte,
    val serialNumber: String,
    val extendedInfo: ExtendedPebbleScanRecord?,
) {
    public companion object {
        private const val SERIAL_END = 13
        private const val EXTENDED_END = SERIAL_END + 6

        internal fun ByteArray.decodePebbleScanRecord(): PebbleLeScanRecord {
            require(size >= SERIAL_END) { "Truncated Pebble scan record" }
            return PebbleLeScanRecord(
                payloadType = this[0],
                serialNumber = decodeToString(1, SERIAL_END),
                extendedInfo = if (size >= EXTENDED_END) decodeExtendedInfo() else null,
            )
        }

        private fun ByteArray.decodeExtendedInfo(): ExtendedPebbleScanRecord {
            val hardware = this[SERIAL_END].toUByte().toInt()
            val flags = this[SERIAL_END + 5].toInt()
            return ExtendedPebbleScanRecord(
                hardwarePlatform = hardware,
                color = this[SERIAL_END + 1],
                major = this[SERIAL_END + 2].toUByte().toInt(),
                minor = this[SERIAL_END + 3].toUByte().toInt(),
                patch = this[SERIAL_END + 4].toUByte().toInt(),
                runningPrf = flags and 1 != 0,
                firstUse = flags and 2 != 0,
                advertisedBoard = WatchHardwarePlatform.fromBoardMarker(
                    hardware.toUByte(), decodeToString(EXTENDED_END, size),
                ),
            )
        }
    }
}

public data class ExtendedPebbleScanRecord internal constructor(
    val hardwarePlatform: Int,
    val color: Byte,
    val major: Int,
    val minor: Int,
    val patch: Int,
    val runningPrf: Boolean,
    val firstUse: Boolean,
    val advertisedBoard: WatchHardwarePlatform? = null,
)
