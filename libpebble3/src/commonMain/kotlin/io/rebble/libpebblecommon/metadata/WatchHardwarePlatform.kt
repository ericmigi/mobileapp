package io.rebble.libpebblecommon.metadata

import co.touchlab.kermit.Logger
import io.rebble.libpebblecommon.LibPebbleConfig
import io.rebble.libpebblecommon.LibPebbleConfigFlow
import io.rebble.libpebblecommon.di.LibPebbleCoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

public data class WatchHardwarePlatform internal constructor(
    public val protocolNumber: UByte,
    private val _watchType: WatchType,
    public val revision: String,
) {
    public val watchType: WatchType
        get() = when (this) {
            UNKNOWN -> unknownWatchTypePlatform
            else -> _watchType
        }

    public companion object {
        public val UNKNOWN: WatchHardwarePlatform = WatchHardwarePlatform(0u, WatchType.BASALT, "unknown")
        public val PEBBLE_ONE_EV_1: WatchHardwarePlatform = WatchHardwarePlatform(1u, WatchType.APLITE, "ev1")
        public val PEBBLE_ONE_EV_2: WatchHardwarePlatform = WatchHardwarePlatform(2u, WatchType.APLITE, "ev2")
        public val PEBBLE_ONE_EV_2_3: WatchHardwarePlatform = WatchHardwarePlatform(3u, WatchType.APLITE, "ev2_3")
        public val PEBBLE_ONE_EV_2_4: WatchHardwarePlatform = WatchHardwarePlatform(4u, WatchType.APLITE, "ev2_4")
        public val PEBBLE_ONE_POINT_FIVE: WatchHardwarePlatform = WatchHardwarePlatform(5u, WatchType.APLITE, "v1_5")
        public val PEBBLE_TWO_POINT_ZERO: WatchHardwarePlatform = WatchHardwarePlatform(6u, WatchType.APLITE, "v2_0")
        public val PEBBLE_SNOWY_EVT_2: WatchHardwarePlatform = WatchHardwarePlatform(7u, WatchType.BASALT, "snowy_evt2")
        public val PEBBLE_SNOWY_DVT: WatchHardwarePlatform = WatchHardwarePlatform(8u, WatchType.BASALT, "snowy_dvt")
        public val PEBBLE_BOBBY_SMILES: WatchHardwarePlatform = WatchHardwarePlatform(10u, WatchType.BASALT, "snowy_s3")
        public val PEBBLE_ONE_BIGBOARD_2: WatchHardwarePlatform = WatchHardwarePlatform(254u, WatchType.APLITE, "bb2")
        public val PEBBLE_ONE_BIGBOARD: WatchHardwarePlatform = WatchHardwarePlatform(255u, WatchType.APLITE, "bigboard")
        public val PEBBLE_SNOWY_BIGBOARD: WatchHardwarePlatform = WatchHardwarePlatform(253u, WatchType.BASALT, "snowy_bb2")
        public val PEBBLE_SNOWY_BIGBOARD_2: WatchHardwarePlatform = WatchHardwarePlatform(252u, WatchType.BASALT, "unk")
        public val PEBBLE_SPALDING_EVT: WatchHardwarePlatform = WatchHardwarePlatform(9u, WatchType.CHALK, "spalding_evt")
        public val PEBBLE_SPALDING_PVT: WatchHardwarePlatform = WatchHardwarePlatform(11u, WatchType.CHALK, "spalding")
        public val PEBBLE_SPALDING_BIGBOARD: WatchHardwarePlatform = WatchHardwarePlatform(251u, WatchType.CHALK, "spalding_bb2")
        public val PEBBLE_SILK_EVT: WatchHardwarePlatform = WatchHardwarePlatform(12u, WatchType.DIORITE, "silk_evt")
        public val PEBBLE_SILK: WatchHardwarePlatform = WatchHardwarePlatform(14u, WatchType.DIORITE, "silk")
        public val CORE_ASTERIX: WatchHardwarePlatform = WatchHardwarePlatform(15u, WatchType.FLINT, "asterix")
        public val XTEINK_X4_PRO: WatchHardwarePlatform = WatchHardwarePlatform(15u, WatchType.X4_PRO, "x4pro")
        public val CORE_OBELIX_EVT: WatchHardwarePlatform = WatchHardwarePlatform(16u, WatchType.EMERY, "obelix_evt")
        public val CORE_OBELIX_DVT: WatchHardwarePlatform = WatchHardwarePlatform(17u, WatchType.EMERY, "obelix_dvt")
        public val CORE_OBELIX_PVT: WatchHardwarePlatform = WatchHardwarePlatform(18u, WatchType.EMERY, "obelix_pvt")
        public val CORE_GETAFIX_EVT: WatchHardwarePlatform = WatchHardwarePlatform(19u, WatchType.GABBRO, "getafix_evt")
        public val CORE_GETAFIX_DVT: WatchHardwarePlatform = WatchHardwarePlatform(20u, WatchType.GABBRO, "getafix_dvt")
        public val CORE_GETAFIX_DVT2: WatchHardwarePlatform = WatchHardwarePlatform(21u, WatchType.GABBRO, "getafix_dvt2")
        public val PEBBLE_SILK_BIGBOARD: WatchHardwarePlatform = WatchHardwarePlatform(250u, WatchType.DIORITE, "silk_bb")
        public val PEBBLE_SILK_BIGBOARD_2_PLUS: WatchHardwarePlatform = WatchHardwarePlatform(248u, WatchType.DIORITE, "silk_bb2")
        public val PEBBLE_ROBERT_EVT: WatchHardwarePlatform = WatchHardwarePlatform(13u, WatchType.EMERY, "robert_evt")
        public val PEBBLE_ROBERT_BIGBOARD: WatchHardwarePlatform = WatchHardwarePlatform(249u, WatchType.EMERY, "robert_bb")
        public val PEBBLE_ROBERT_BIGBOARD_2: WatchHardwarePlatform = WatchHardwarePlatform(247u, WatchType.EMERY, "robert_bb2")
        public val CORE_OBELIX_BIGBOARD: WatchHardwarePlatform = WatchHardwarePlatform(244u, WatchType.EMERY, "obelix_bb")
        public val CORE_OBELIX_BIGBOARD_2: WatchHardwarePlatform = WatchHardwarePlatform(243u, WatchType.EMERY, "obelix_bb2")
        public val CORE_QEMU_FLINT: WatchHardwarePlatform = WatchHardwarePlatform(246u, WatchType.FLINT, "qemu_flint")
        public val CORE_QEMU_EMERY: WatchHardwarePlatform = WatchHardwarePlatform(245u, WatchType.EMERY, "qemu_emery")
        public val CORE_QEMU_GABBRO: WatchHardwarePlatform = WatchHardwarePlatform(242u, WatchType.GABBRO, "qemu_gabbro")

        internal val standard: List<WatchHardwarePlatform> = listOf(
            UNKNOWN, PEBBLE_ONE_EV_1, PEBBLE_ONE_EV_2, PEBBLE_ONE_EV_2_3,
            PEBBLE_ONE_EV_2_4, PEBBLE_ONE_POINT_FIVE, PEBBLE_TWO_POINT_ZERO, PEBBLE_SNOWY_EVT_2,
            PEBBLE_SNOWY_DVT, PEBBLE_BOBBY_SMILES, PEBBLE_ONE_BIGBOARD_2, PEBBLE_ONE_BIGBOARD,
            PEBBLE_SNOWY_BIGBOARD, PEBBLE_SNOWY_BIGBOARD_2, PEBBLE_SPALDING_EVT, PEBBLE_SPALDING_PVT,
            PEBBLE_SPALDING_BIGBOARD, PEBBLE_SILK_EVT, PEBBLE_SILK, CORE_ASTERIX,
            XTEINK_X4_PRO, CORE_OBELIX_EVT, CORE_OBELIX_DVT, CORE_OBELIX_PVT,
            CORE_GETAFIX_EVT, CORE_GETAFIX_DVT, CORE_GETAFIX_DVT2, PEBBLE_SILK_BIGBOARD,
            PEBBLE_SILK_BIGBOARD_2_PLUS, PEBBLE_ROBERT_EVT, PEBBLE_ROBERT_BIGBOARD, PEBBLE_ROBERT_BIGBOARD_2,
            CORE_OBELIX_BIGBOARD, CORE_OBELIX_BIGBOARD_2, CORE_QEMU_FLINT, CORE_QEMU_EMERY,
            CORE_QEMU_GABBRO,
        )
        public val entries: List<WatchHardwarePlatform> get() = standard + DeviceProfiles.current.map { it.hardware }

        private var unknownWatchTypePlatform: WatchType = WatchType.EMERY

        public fun fromProtocolNumber(number: UByte): WatchHardwarePlatform {
            return standard.firstOrNull { it.protocolNumber == number } ?: UNKNOWN.also {
                Logger.d { "unknown hardware revision: $number" }
            }
        }

        internal fun fromBoardMarker(number: UByte, marker: String): WatchHardwarePlatform? =
            entries.firstOrNull {
                it.protocolNumber == number && it.revision == marker &&
                    (it == XTEINK_X4_PRO || it.watchType.profile != null)
            }

        internal fun fromHWRevision(revision: String?): WatchHardwarePlatform {
            if (revision == "unk") return UNKNOWN
            return entries.firstOrNull() { it.revision == revision } ?: UNKNOWN.also {
                Logger.d { "unknown hardware revision: $revision" }
            }
        }

        internal fun init(
            libPebbleCoroutineScope: LibPebbleCoroutineScope,
            libPebbleConfigFlow: StateFlow<LibPebbleConfig>,
        ) {
            libPebbleCoroutineScope.launch {
                libPebbleConfigFlow.collect {
                    unknownWatchTypePlatform = it.watchConfig.unknownWatchTypePlatform
                }
            }
        }
    }
}

@Serializer(WatchHardwarePlatform::class)
internal class WatchHardwarePlatformSerializer {
    override val descriptor: SerialDescriptor
        get() = PrimitiveSerialDescriptor("WatchHardwarePlatform", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): WatchHardwarePlatform {
        val revision = decoder.decodeString()
        return WatchHardwarePlatform.fromHWRevision(revision) ?: throw SerializationException("Unknown hardware revision $revision")
    }

    override fun serialize(encoder: Encoder, value: WatchHardwarePlatform) {
        val revision = value.revision
        encoder.encodeString(revision)
    }

}