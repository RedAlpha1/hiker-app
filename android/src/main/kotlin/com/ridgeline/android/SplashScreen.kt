package com.ridgeline.android

import androidx.compose.animation.core.EaseOutBack
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ridgeline.android.theme.toComposeColor
import com.ridgeline.android.theme.toTextStyle
import com.ridgeline.ui.theme.Colors
import com.ridgeline.ui.theme.Typography
import kotlinx.coroutines.delay

/**
 * First screen shown on every launch, dark chrome per the design file
 * (Ridgeline_Standalone.html, "1 · Splash") -- the only screen besides the AR
 * viewfinder that isn't the warm parchment ground. Purely a brand beat: no
 * async init actually gates it yet (MapLibre.getInstance() in MainActivity
 * is synchronous), so the delay below is a fixed minimum dwell time, not a
 * wait on real work. If region-tile or peak-catalog preloading becomes
 * genuinely async later, this is where it would gate on that instead.
 */
private const val SPLASH_MIN_DWELL_MS = 1200L

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(SPLASH_MIN_DWELL_MS)
        onFinished()
    }

    var logoVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { logoVisible = true }
    val logoScale by animateFloatAsState(
        targetValue = if (logoVisible) 1f else 0.82f,
        animationSpec = tween(durationMillis = 500, easing = EaseOutBack),
        label = "splashLogoScale",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0D1A2E), Color(0xFF16283F), Color(0xFF1C3350)),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .scale(logoScale)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Colors.PRIMARY_BLUE.toComposeColor()),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Terrain,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(36.dp),
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 18.dp),
            ) {
                Text(
                    text = "Ridgeline",
                    style = Typography.pageTitle.toTextStyle(Color.White),
                )
                Text(
                    text = "Know every peak you see",
                    style = Typography.body.toTextStyle(Color(0xFF9FB3CC)),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}
