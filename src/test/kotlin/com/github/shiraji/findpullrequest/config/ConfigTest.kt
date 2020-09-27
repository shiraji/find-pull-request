package com.github.shiraji.findpullrequest.config

import com.intellij.ide.util.PropertiesComponent
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ConfigTest {
    private val config: PropertiesComponent = mockk()

    @Test
    fun `Should get disabled`() {
        every { config.getBoolean("com.github.shiraji.findpullrequest.model.FindPullRequestConfig.DISABLE", false) } returns true
        val result = config.isDisable()
        verify { config.getBoolean("com.github.shiraji.findpullrequest.model.FindPullRequestConfig.DISABLE", false) }
        assertTrue(result)
    }

    @Test
    fun `Should get debug mode`() {
        every { config.getBoolean("com.github.shiraji.findpullrequest.model.FindPullRequestConfig.DEBUG_MODE", false) } returns true
        val result = config.isDebugMode()
        verify { config.getBoolean("com.github.shiraji.findpullrequest.model.FindPullRequestConfig.DEBUG_MODE", false) }
        assertTrue(result)
    }

    @Test
    fun `Should get jump to file`() {
        every { config.getBoolean("com.github.shiraji.findpullrequest.model.FindPullRequestConfig.JUMP_TO_FILE", true) } returns true
        val result = config.isJumpToFile()
        verify { config.getBoolean("com.github.shiraji.findpullrequest.model.FindPullRequestConfig.JUMP_TO_FILE", true) }
        assertTrue(result)
    }

    @Test
    fun `Should get protocol`() {
        every { config.getValue("com.github.shiraji.findpullrequest.model.FindPullRequestConfig.PROTOCOL", "https://") } returns "http://"
        val result = config.getProtocol()
        verify { config.getValue("com.github.shiraji.findpullrequest.model.FindPullRequestConfig.PROTOCOL", "https://") }
        assertEquals(result, "http://")
    }

    @Test
    fun `Should get isPopupAfterCopy`() {
        every { config.getBoolean("com.github.shiraji.findpullrequest.model.FindPullRequestConfig.POPUP_AFTER_COPY", false) } returns true
        val result = config.isPopupAfterCopy()
        verify { config.getBoolean("com.github.shiraji.findpullrequest.model.FindPullRequestConfig.POPUP_AFTER_COPY", false) }
        assertTrue(result)
    }

    @Test
    fun `Should get hosting`() {
        every { config.getValue("com.github.shiraji.findpullrequest.model.FindPullRequestConfig.HOSTING", "") } returns "github.com"
        val result = config.getHosting()
        verify { config.getValue("com.github.shiraji.findpullrequest.model.FindPullRequestConfig.HOSTING", "") }
        assertEquals(result, "github.com")
    }
}