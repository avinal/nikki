package com.avinal.memos.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.avinal.memos.domain.Attachment

@Composable
fun AttachmentGrid(
    attachments: List<Attachment>,
    serverUrl: String,
    modifier: Modifier = Modifier,
) {
    val images = attachments.filter { it.isImage }
    if (images.isEmpty()) return

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        images.forEach { attachment ->
            val imageUrl = buildImageUrl(attachment, serverUrl)
            AsyncImage(
                model = imageUrl,
                contentDescription = attachment.filename,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp)),
                contentScale = ContentScale.FillWidth,
            )
        }
    }
}

private fun buildImageUrl(attachment: Attachment, serverUrl: String): String {
    if (attachment.externalLink.isNotEmpty()) return attachment.externalLink
    val base = serverUrl.trimEnd('/')
    return "$base/file/${attachment.name}/${attachment.filename}"
}
