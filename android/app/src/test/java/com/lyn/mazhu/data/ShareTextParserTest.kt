package com.lyn.mazhu.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ShareTextParserTest {
    @Test
    fun extractsWechatUrlFromSharedText() {
        val sharedText = """
            一篇值得收藏的文章
            https://mp.weixin.qq.com/s/example
        """.trimIndent()

        assertEquals(
            "https://mp.weixin.qq.com/s/example",
            ShareTextParser.extractUrl(sharedText),
        )
    }

    @Test
    fun extractsTitleBeforeUrl() {
        val url = "https://mp.weixin.qq.com/s/example"
        assertEquals(
            "一篇值得收藏的文章",
            ShareTextParser.extractTitle("一篇值得收藏的文章\n$url", url),
        )
    }

    @Test
    fun returnsNullWhenNoUrlExists() {
        assertNull(ShareTextParser.extractUrl("这里只是一段普通文字"))
    }
}

