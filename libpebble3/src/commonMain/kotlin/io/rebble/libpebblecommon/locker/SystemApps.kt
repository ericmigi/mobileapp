package io.rebble.libpebblecommon.locker

import io.rebble.libpebblecommon.SystemAppIDs
import io.rebble.libpebblecommon.metadata.WatchType
import kotlin.uuid.Uuid

public enum class SystemApps(
    public val uuid: Uuid,
    public val displayName: String,
    public val type: AppType,
    private val supportsAplite: Boolean,
    public val defaultOrder: Int,
) {
    // Apps
    Settings(SystemAppIDs.SETTINGS_APP_UUID, "Settings", AppType.Watchapp, true, defaultOrder = -10),
    Music(SystemAppIDs.MUSIC_APP_UUID, "Music", AppType.Watchapp, true, defaultOrder = -9),
    Notifications(SystemAppIDs.NOTIFICATIONS_APP_UUID, "Notifications", AppType.Watchapp, true, defaultOrder = -8),
    Alarms(SystemAppIDs.ALARMS_APP_UUID, "Alarms", AppType.Watchapp, true, defaultOrder = -7),
    Weather(SystemAppIDs.WEATHER_APP_UUID, "Weather", AppType.Watchapp, false, defaultOrder = -6),
    Health(SystemAppIDs.HEALTH_APP_UUID, "Health", AppType.Watchapp, false, defaultOrder = -5),
    Workout(SystemAppIDs.WORKOUT_APP_UUID, "Workout", AppType.Watchapp, false, defaultOrder = -4),
    Watchfaces(SystemAppIDs.WATCHFACES_APP_UUID, "Watchfaces", AppType.Watchapp, true, defaultOrder = -3),
    Timeline(SystemAppIDs.TIMELINE_MENU_ENTRY_UUID, "Timeline", AppType.Watchapp, true, defaultOrder = 100),
    // Faces
    Tictoc(SystemAppIDs.TICTOC_APP_UUID, "Tictoc", AppType.Watchface, true, defaultOrder = -2),
    Kickstart(SystemAppIDs.KICKSTART_APP_UUID, "Kickstart", AppType.Watchface, false, defaultOrder = -1),
    ;

    public val compatiblePlatforms: List<WatchType>
        get() = if (supportsAplite) WatchType.entries else WatchType.entries.filter { it != WatchType.APLITE }
}
