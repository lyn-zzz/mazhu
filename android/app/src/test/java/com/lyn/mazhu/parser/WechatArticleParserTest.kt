package com.lyn.mazhu.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WechatArticleParserTest {
    private val parser = WechatArticleParser()

    @Test
    fun parsesWechatMetadataAndEncodedContent() {
        val html = """
            <html>
              <head>
                <meta property="og:title" content="测试文章" />
                <meta property="og:image" content="https://example.com/cover.jpg" />
              </head>
              <body>
                <a id="js_name">测试公众号</a>
                <script>
                  var data = {
                    create_time: '1783318431',
                    content_noencode: '\x3cp\x3e第一段正文，用来验证微信公众号文章内容能够被正确解码和提取。\x3c/p\x3e\x3cp\x3e第二段正文，继续补充足够长度，确保解析器不会把正常文章误判为访问校验页面。\x3c/p\x3e\x3cp\x3e第三段正文，文章中的主要信息最终会作为纯文本保存在本地数据库中。\x3c/p\x3e'
                  };
                </script>
              </body>
            </html>
        """.trimIndent()

        val article = parser.parseHtml(html)

        assertEquals("测试文章", article.title)
        assertEquals("测试公众号", article.accountName)
        assertEquals("https://example.com/cover.jpg", article.coverUrl)
        assertTrue(article.contentText.contains("第一段正文"))
        assertTrue(article.contentText.contains("第二段正文"))
    }
}
