package io.rebble.libpebblecommon.metadata

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = WatchTypeSerializer::class)
public data class WatchType internal constructor(public val codename: String) {
    public val profile: DeviceProfile? get() = DeviceProfiles.current.firstOrNull { it.nativePlatform == codename }
    public val name: String get() = if (this == X4_PRO) "X4_PRO" else codename.uppercase()
    override fun toString(): String = name

    public fun getCompatibleAppVariants(): List<WatchType> {
        return when (this) {
            APLITE -> listOf(APLITE)
            BASALT -> listOf(BASALT, APLITE)
            CHALK -> listOf(CHALK)
            DIORITE -> listOf(DIORITE, APLITE)
            EMERY -> listOf(
                EMERY,
                BASALT,
                DIORITE,
                APLITE
            )
            X4_PRO -> listOf(X4_PRO, FLINT, DIORITE, APLITE)
            FLINT -> listOf(FLINT, DIORITE, APLITE)
            GABBRO -> listOf(GABBRO, CHALK)
            else -> listOf(this) + profile?.compatiblePlatforms.orEmpty().mapNotNull { fromCodename(it) }
        }
    }

    /**
     * Get the most compatible variant for this WatchType
     * @param availableAppVariants List of variants, from [io.rebble.libpebblecommon.metadata.pbw.appinfo.PbwAppInfo.targetPlatforms]
     */
    public fun getBestVariant(availableAppVariants: List<String>): WatchType? {
        val compatibleVariants = getCompatibleAppVariants()

        return compatibleVariants.firstOrNull() { variant ->
            availableAppVariants.contains(variant.codename)
        }
    }

    public companion object {
        public val APLITE: WatchType = WatchType("aplite")
        public val BASALT: WatchType = WatchType("basalt")
        public val CHALK: WatchType = WatchType("chalk")
        public val DIORITE: WatchType = WatchType("diorite")
        public val EMERY: WatchType = WatchType("emery")
        public val FLINT: WatchType = WatchType("flint")
        public val X4_PRO: WatchType = WatchType("xteinkx4pro")
        public val GABBRO: WatchType = WatchType("gabbro")
        internal val standard: List<WatchType> = listOf(APLITE, BASALT, CHALK, DIORITE, EMERY, FLINT, X4_PRO, GABBRO)
        public val entries: List<WatchType> get() = standard + DeviceProfiles.current.map { it.type }

        public fun fromCodename(codename: String): WatchType? {
            return entries.firstOrNull { it.codename == codename }
        }
    }
}

internal fun WatchType.isColor(): Boolean = when (this) {
    WatchType.BASALT, WatchType.CHALK, WatchType.EMERY, WatchType.GABBRO -> true
    else -> false
}

internal fun WatchType.supportsBtClassic(): Boolean = when (this) {
    WatchType.APLITE, WatchType.BASALT, WatchType.CHALK -> true
    else -> false
}
internal object WatchTypeSerializer : KSerializer<WatchType> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("WatchType", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: WatchType) = encoder.encodeString(value.name)
    override fun deserialize(decoder: Decoder): WatchType {
        val name = decoder.decodeString()
        return WatchType.standard.firstOrNull { it.name == name } ?: WatchType(name.lowercase())
    }
}
