package coredevices.pebble.services

import coredevices.database.AppstoreSource
import coredevices.pebble.Platform
import coredevices.pebble.ui.CommonAppType
import coredevices.pebble.ui.asCommonApp
import io.rebble.libpebblecommon.metadata.WatchType
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class X4ProCatalogTest {
    private val json = Json { ignoreUnknownKeys = true }
    private val source = AppstoreSource(url = "https://store/api", title = "Test")
    private val native = WatchType.X4_PRO.codename

    private fun listing(vararg platforms: String): StoreApplication {
        val entries = platforms.joinToString(",") { name ->
            """{"name":"$name","sdk_version":"3","pebble_process_info_flags":384,"description":"$name","images":{"list":"https://$name/list","screenshot":"https://$name/screen"}}"""
        }
        val data = STOCKS_LISTING.replace(""""type":"plugin"""", """"type":"watchface"""")
            .replace(""""hardware_platforms":[]""", """"hardware_platforms":[$entries]""")
            .replace(""""supported":true,"firmware"""", """"supported":false,"firmware"""")
            .replace(""""flint":{"supported":false""", """"flint":{"supported":${"flint" in platforms}""")
        return json.decodeFromString(StoreApplication.serializer(), data)
    }

    @Test
    fun requestsFlintWhileKeepingTheNativeDeviceIdentity() {
        assertEquals("flint", WatchType.X4_PRO.storeCodename)
        assertEquals("xteinkx4pro", native)
        WatchType.entries.filter { it != WatchType.X4_PRO }.forEach {
            assertEquals(it.codename, it.storeCodename)
        }
    }

    @Test
    fun keepsFlintDownloadAndPreviewForAnExistingStoreListing() {
        val app = listing("flint")
        val view = assertNotNull(app.asCommonApp(WatchType.X4_PRO, Platform.Android, source, emptyList()))
        assertTrue(view.isCompatible)
        assertEquals("https://flint/screen", view.screenshotImageUrl)
        val locker = assertNotNull(app.toLockerEntry(source.url, null))
        assertEquals(listOf("flint"), locker.hardwarePlatforms.map { it.name })
        assertEquals("https://store/stocks.pbw", locker.pbw?.file)
    }

    @Test
    fun keepsExplicitNativeMetadataAndChoosesItsPreviewBeforeFlint() {
        val app = listing("flint", native)
        val view = assertNotNull(app.asCommonApp(WatchType.X4_PRO, Platform.Android, source, emptyList()))
        assertEquals("https://$native/list", view.listImageUrl)
        assertEquals(listOf("https://$native/screen"), (view.commonAppType as CommonAppType.Store).allScreenshotUrls)
        val locker = assertNotNull(app.toLockerEntry(source.url, null))
        assertEquals(setOf(native, "flint"), locker.hardwarePlatforms.map { it.name }.toSet())
        val nativeOnly = listing(native)
        assertTrue(assertNotNull(nativeOnly.asCommonApp(WatchType.X4_PRO, Platform.Android, source, emptyList())).isCompatible)
        assertFalse(assertNotNull(nativeOnly.asCommonApp(WatchType.FLINT, Platform.Android, source, emptyList())).isCompatible)
    }
}
