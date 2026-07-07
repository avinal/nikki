package com.avinal.memos.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.avinal.memos.api.LinkPreviewFetcher
import com.avinal.memos.domain.LinkPreview
import com.avinal.memos.ui.theme.LocalAccentColor

@Composable
fun LinkPreviewCard(
    url: String,
    fetcher: LinkPreviewFetcher?,
    modifier: Modifier = Modifier,
) {
    if (fetcher == null) return
    var preview by remember(url) { mutableStateOf<LinkPreview?>(null) }
    var attempted by remember(url) { mutableStateOf(false) }

    LaunchedEffect(url) {
        preview = fetcher.fetch(url)
        attempted = true
    }

    val p = preview ?: return
    if (!attempted) return

    val uriHandler = LocalUriHandler.current
    val subtleColor = MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .clickable { uriHandler.openUri(url) }
            .padding(10.dp),
    ) {
        p.imageUrl?.let { imageUrl ->
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                modifier = Modifier.size(56.dp).clip(RoundedCornerShape(4.dp)),
                contentScale = ContentScale.Crop,
            )
            Spacer(Modifier.width(10.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            p.siteName?.let {
                Text(it, fontSize = 11.sp, color = subtleColor, maxLines = 1)
                Spacer(Modifier.height(2.dp))
            }
            Text(
                p.title ?: "",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            p.description?.let {
                Spacer(Modifier.height(2.dp))
                Text(it, fontSize = 12.sp, color = subtleColor, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}
