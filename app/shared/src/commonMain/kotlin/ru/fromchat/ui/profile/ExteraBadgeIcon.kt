package ru.fromchat.ui.profile

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

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

        ExteraBadgeType.Clown -> ClownBadgeIcon(modifier = modifier.size(size))
    }
}

@Composable
private fun ClownBadgeIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val faceColor = Color(0xFFFFFBF0)
        val redColor = Color(0xFFE53935)
        val darkColor = Color(0xFF3E2723)

        drawCircle(
            color = faceColor,
            radius = size.minDimension * 0.40f,
            center = center,
        )

        val hat = Path().apply {
            moveTo(center.x - size.width * 0.30f, center.y - size.height * 0.12f)
            lineTo(center.x + size.width * 0.30f, center.y - size.height * 0.12f)
            lineTo(center.x, center.y - size.height * 0.52f)
            close()
        }
        drawPath(hat, color = redColor)

        drawCircle(
            color = darkColor,
            radius = size.minDimension * 0.05f,
            center = Offset(center.x - size.width * 0.13f, center.y - size.height * 0.05f),
        )
        drawCircle(
            color = darkColor,
            radius = size.minDimension * 0.05f,
            center = Offset(center.x + size.width * 0.13f, center.y - size.height * 0.05f),
        )
        drawCircle(
            color = redColor,
            radius = size.minDimension * 0.10f,
            center = Offset(center.x, center.y + size.height * 0.10f),
        )
        drawArc(
            color = redColor,
            startAngle = 20f,
            sweepAngle = 140f,
            useCenter = false,
            topLeft = Offset(center.x - size.width * 0.13f, center.y + size.height * 0.14f),
            size = Size(size.width * 0.26f, size.height * 0.15f),
            style = Stroke(width = size.minDimension * 0.05f, cap = StrokeCap.Round),
        )
    }
}