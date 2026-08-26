package com.avinal.memos.api

import com.avinal.memos.db.dao.LinkPreviewDao
import com.avinal.memos.db.entity.LinkPreviewEntity
import com.avinal.memos.domain.LinkPreview
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType

class LinkPreviewFetcher(
    private val client: HttpClient,
    private val dao: LinkPreviewDao? = null,
    private val ttlMs: Long = TTL_7_DAYS,
) {

    private val cache = LinkedHashMap<String, LinkPreview?>(100, 0.75f, true)

    suspend fun fetch(url: String): LinkPreview? {
        cache[url]?.let { return it }
        if (cache.containsKey(url)) return null

        val now = currentTimeMs()
        val minTimestamp = now - ttlMs

        dao?.let { d ->
            try {
                val cached = d.getIfFresh(url, minTimestamp)
                if (cached != null) {
                    val preview = cached.toDomain()
                    addToMemoryCache(url, preview)
                    return preview
                }
            } catch (_: Exception) {}
        }

        val preview = try {
            val response = client.get(url) {
                header("User-Agent", "Mozilla/5.0 (compatible; NikkiBot/1.0)")
            }
            val contentType = response.contentType()
            if (contentType == null || !contentType.match(ContentType.Text.Html)) {
                null
            } else {
                val html = response.bodyAsText(fallbackCharset = Charsets.UTF_8)
                parseOpenGraph(url, html)
            }
        } catch (_: Exception) {
            null
        }

        addToMemoryCache(url, preview)

        if (preview != null) {
            dao?.let { d ->
                try {
                    d.upsert(preview.toEntity(now))
                } catch (_: Exception) {}
            }
        }

        return preview
    }

    private fun addToMemoryCache(url: String, preview: LinkPreview?) {
        synchronized(cache) {
            if (cache.size >= MAX_CACHE) {
                val first = cache.keys.first()
                cache.remove(first)
            }
            cache[url] = preview
        }
    }

    companion object {
        private const val MAX_CACHE = 100
        const val TTL_7_DAYS = 7L * 24 * 60 * 60 * 1000

        private fun currentTimeMs(): Long = kotlin.time.Clock.System.now().toEpochMilliseconds()

        private fun LinkPreviewEntity.toDomain(): LinkPreview = LinkPreview(
            url = url, title = title, description = description,
            imageUrl = imageUrl, siteName = siteName,
        )

        private fun LinkPreview.toEntity(cachedAt: Long): LinkPreviewEntity = LinkPreviewEntity(
            url = url, title = title, description = description,
            imageUrl = imageUrl, siteName = siteName, cachedAt = cachedAt,
        )

        private val ogTagRegex = Regex(
            """<meta\s+[^>]*property\s*=\s*["']og:(\w+)["'][^>]*content\s*=\s*["']([^"']*?)["'][^>]*/?>""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
        )
        private val ogTagReversedRegex = Regex(
            """<meta\s+[^>]*content\s*=\s*["']([^"']*?)["'][^>]*property\s*=\s*["']og:(\w+)["'][^>]*/?>""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
        )
        private val titleRegex = Regex("""<title[^>]*>([^<]*)</title>""", RegexOption.IGNORE_CASE)

        fun parseOpenGraph(url: String, html: String): LinkPreview? {
            val og = mutableMapOf<String, String>()
            ogTagRegex.findAll(html).forEach { og[it.groupValues[1]] = it.groupValues[2] }
            ogTagReversedRegex.findAll(html).forEach { og.putIfAbsent(it.groupValues[2], it.groupValues[1]) }

            val title = og["title"] ?: titleRegex.find(html)?.groupValues?.get(1)?.trim()
            if (title.isNullOrBlank()) return null

            return LinkPreview(
                url = url,
                title = title.take(200),
                description = og["description"]?.take(300),
                imageUrl = og["image"],
                siteName = og["site_name"] ?: extractDomain(url),
            )
        }

        private fun extractDomain(url: String): String? {
            val start = url.indexOf("://")
            if (start < 0) return null
            val afterProtocol = url.substring(start + 3)
            val end = afterProtocol.indexOf('/')
            val host = if (end > 0) afterProtocol.substring(0, end) else afterProtocol
            return host.removePrefix("www.")
        }
    }
}
