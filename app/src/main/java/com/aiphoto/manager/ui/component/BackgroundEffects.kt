package com.aiphoto.manager.ui.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import com.aiphoto.manager.ui.theme.LocalAppColorSet
import com.aiphoto.manager.ui.theme.ThemeGradients
import kotlin.random.Random

private data class ParticleState(
    val xRatio: Float,
    val yRatio: Float,
    val size: Float,
    val speed: Float,
    val alphaMax: Float
)

@Composable
fun AuraParticlesBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val colorSet = LocalAppColorSet.current
    val particles = remember {
        List(15) {
            ParticleState(
                xRatio = Random.nextFloat(),
                yRatio = Random.nextFloat(),
                size = Random.nextFloat() * 15f + 8f, 
                speed = Random.nextFloat() * 0.06f + 0.02f,
                alphaMax = Random.nextFloat() * 0.20f + 0.10f
            )
        }
    }

    val transition = rememberInfiniteTransition(label = "particles")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(25000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "progress"
    )

    val primaryColor = colorSet.colorScheme.primary

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ThemeGradients.backgroundGradient(colorSet))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            particles.forEach { p ->
                val currentYRatio = (p.yRatio - progress * p.speed) % 1.0f
                val adjustedY = if (currentYRatio < 0f) currentYRatio + 1.0f else currentYRatio
                
                val x = p.xRatio * w
                val y = adjustedY * h

                // Horizontal sway
                val sway = kotlin.math.sin((progress * 2 * kotlin.math.PI * p.speed * 4) + p.xRatio * 10).toFloat() * 20f

                drawCircle(
                    color = primaryColor.copy(alpha = p.alphaMax),
                    radius = p.size,
                    center = Offset(x + sway, y)
                )
            }
        }
        content()
    }
}
