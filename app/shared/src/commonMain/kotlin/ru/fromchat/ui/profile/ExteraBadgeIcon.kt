package ru.fromchat.ui.profile

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ExteraBadgeIcon(
    type: ExteraBadgeType,
    modifier: Modifier = Modifier,
    size: Dp = 20.dp,
) {
    when (type) {
        ExteraBadgeType.Developer -> Icon(
            imageVector = Icons.Filled.Code,
            contentDescription = null,
            modifier = modifier.size(size),
            tint = Color(0xFF00ACC1),
        )

        ExteraBadgeType.Supporter -> Icon(
            imageVector = Icons.Filled.Person,
            contentDescription = null,
            modifier = modifier.size(size),
            tint = Color(0xFF8E24AA),
        )

        ExteraBadgeType.Verified -> Icon(
            imageVector = Icons.Filled.Verified,
            contentDescription = null,
            modifier = modifier.size(size),
            tint = Color(0xFF00C853),
        )

        ExteraBadgeType.Clown -> ClownEmojiBadge(
            modifier = modifier,
            size = size,
        )
    }
}

@Composable
private fun ClownEmojiBadge(
    modifier: Modifier = Modifier,
    size: Dp = 20.dp,
) {
    val textMeasurer = rememberTextMeasurer()
    val emojiLayout = remember(textMeasurer) {
        textMeasurer.measure(
            text = AnnotatedString("🤡"),
            style = TextStyle(
                color = Color.Unspecified,
                fontSize = 100.sp,
            ),
        )
    }
    Canvas(modifier = modifier.size(size)) {
        val badgePx = size.toPx()
        val scale = minOf(
            badgePx / emojiLayout.size.width,
            badgePx / emojiLayout.size.height,
        )
        val scaledWidth = emojiLayout.size.width * scale
        val scaledHeight = emojiLayout.size.height * scale
        val topLeft = Offset(
            x = (badgePx - scaledWidth) / 2f,
            y = (badgePx - scaledHeight) / 2f,
        )
        withTransform({
            scale(scaleX = scale, scaleY = scale, pivot = topLeft)
        }) {
            drawText(textLayoutResult = emojiLayout)
        }
    }
}