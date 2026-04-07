package com.uzuns.uzunsiptv

import org.junit.Assert.assertEquals
import org.junit.Test

class ChannelHotkeyManagerTest {

    @Test
    fun `firstAvailableNumberFromJson uses assigned shortcut keys only`() {
        val rawJson = """{"1":101,"2":1,"10":999}"""

        assertEquals("3", ChannelHotkeyManager.firstAvailableNumberFromJson(rawJson))
    }
}
