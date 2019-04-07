package com.github.shiraji

import org.junit.Assert.assertEquals
import org.junit.jupiter.api.Test

class ExtensionsKtTest {
    @Test
    fun `Should generate md5`() {
        val text = "aaa"
        assertEquals("47bce5c74f589f4867dbd57e9ca9f808", text.toMd5())
    }

    @Test
    fun toSHA1() {
        val text = "aaa"
        assertEquals("7e240de74fb1ed08fa08d38063f6a6a91462a815", text.toSHA1())
    }

    @Test
    fun subtract() {
        val text = "aaabbbccc"
        assertEquals("aaaccc", text.subtract("bbb"))
    }

}