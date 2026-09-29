package com.avinal.memos.api

import com.avinal.memos.db.dao.LinkPreviewDao
import com.avinal.memos.db.entity.LinkPreviewEntity
import com.avinal.memos.domain.LinkPreview

class LinkPreviewFetcher(
    private val apiClient: MemosApiClient,
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

        val preview = when (val result = apiClient.getLinkMetadata(url)) {
            is ApiResult.Success -> {
                val dto = result.data
                if (dto.title.isBlank()) null
                else LinkPreview(
                    url = dto.url.ifEmpty { url },
                    title = dto.title,
                    description = dto.description.ifEmpty { null },
                    imageUrl = dto.image.ifEmpty { null },
                    siteName = extractDomain(url),
                )
            }
            else -> null
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
