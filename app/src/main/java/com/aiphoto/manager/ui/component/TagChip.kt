package com.aiphoto.manager.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TagChip(
    text: String,
    color: String,
    isSelected: Boolean = false,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    onRemove: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val tagColor = try {
        Color(android.graphics.Color.parseColor(color))
    } catch (_: Exception) {
        MaterialTheme.colorScheme.primaryContainer
    }

    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) tagColor else tagColor.copy(alpha = 0.15f),
        animationSpec = tween(200)
    )
    val textColor by animateColorAsState(
        targetValue = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
        animationSpec = tween(200)
    )

    Box(
        modifier = modifier
            .widthIn(max = 320.dp)
            .clip(RoundedCornerShape(50))
            .background(backgroundColor)
            .then(
                if (!isSelected) Modifier.border(1.dp, tagColor.copy(alpha = 0.5f), RoundedCornerShape(50))
                else Modifier
            )
            .then(
                when {
                    onLongClick != null && onClick != null -> Modifier.combinedClickable(
                        onClick = onClick,
                        onLongClick = onLongClick
                    )
                    onClick != null -> Modifier.clickable { onClick() }
                    onLongClick != null -> Modifier.combinedClickable(
                        onClick = {},
                        onLongClick = onLongClick
                    )
                    else -> Modifier
                }
            )
            .padding(horizontal = if (onRemove != null) 6.dp else 12.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                color = textColor,
                maxLines = 1,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .widthIn(min = 36.dp)
                    .then(
                        if (text.length > 20) Modifier.basicMarquee()
                        else Modifier
                    )
            )
            if (onRemove != null) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove",
                    tint = textColor,
                    modifier = Modifier
                        .size(16.dp)
                        .clip(RoundedCornerShape(50))
                        .clickable { onRemove() }
                        .padding(2.dp)
                )
            }
        }
    }
}
