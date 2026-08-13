package com.ridgeline.android

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.DirectionsRun
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Terrain
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ridgeline.android.theme.toComposeColor
import com.ridgeline.android.theme.toTextStyle
import com.ridgeline.ui.screens.ONBOARDING_STEPS
import com.ridgeline.ui.screens.OnboardingState
import com.ridgeline.ui.theme.Colors
import com.ridgeline.ui.theme.Typography

/**
 * Per-step icon and accent, keyed to ONBOARDING_STEPS' order in
 * OnboardingState.kt. Not part of :ui's OnboardingStep -- that class is
 * Compose-free by design, and this is presentation, not state.
 *
 * Icons are standard Material glyphs (Terrain/PhotoCamera/CloudDownload/
 * DirectionsRun), not the design file's hand-drawn SVG paths -- see
 * DECISIONS.md, "Splash and onboarding screens". Step 1's tint (#B8511C) is
 * a one-off from that step's icon, not one of :ui's shared Colors tokens.
 */
private data class StepVisual(val icon: ImageVector, val tint: Color, val background: Color)

private val STEP_VISUALS = listOf(
    StepVisual(Icons.Outlined.Terrain, Colors.PRIMARY_BLUE.toComposeColor(), Color(0xFFE8F0FE)),
    StepVisual(Icons.Outlined.PhotoCamera, Color(0xFFB8511C), Color(0xFFFDECE3)),
    StepVisual(Icons.Outlined.CloudDownload, Colors.SUCCESS_GREEN.toComposeColor(), Color(0xFFEAF3E5)),
    StepVisual(Icons.Outlined.DirectionsRun, Colors.SUCCESS_GREEN.toComposeColor(), Color(0xFFEAF3E5)),
)

@Composable
fun OnboardingScreen(
    state: OnboardingState,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    onGetStarted: () -> Unit,
) {
    val visual = STEP_VISUALS[state.stepIndex]

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Colors.GROUND.toComposeColor()),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp, end = 20.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            if (!state.isLastStep) {
                TextButton(onClick = onSkip) {
                    Text(text = "Skip", style = Typography.body.toTextStyle(Colors.MUTED.toComposeColor()))
                }
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(visual.background),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = visual.icon,
                    contentDescription = null,
                    tint = visual.tint,
                    modifier = Modifier.size(52.dp),
                )
            }

            Text(
                text = state.step.title,
                style = Typography.onboardingTitle.toTextStyle(Colors.INK.toComposeColor()),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 28.dp),
            )
            Text(
                text = state.step.body,
                style = Typography.body.toTextStyle(Colors.MUTED.toComposeColor()),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .padding(top = 10.dp),
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp, Alignment.CenterHorizontally),
        ) {
            ONBOARDING_STEPS.indices.forEach { index ->
                ProgressDot(active = index == state.stepIndex)
            }
        }

        Box(modifier = Modifier.padding(horizontal = 20.dp, bottom = 44.dp)) {
            Button(
                onClick = if (state.isLastStep) onGetStarted else onNext,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Colors.PRIMARY_BLUE.toComposeColor()),
            ) {
                Text(
                    text = if (state.isLastStep) "Get started" else "Continue",
                    style = Typography.button.toTextStyle(Color.White),
                )
            }
        }
    }
}

@Composable
private fun ProgressDot(active: Boolean) {
    val dotWidth by animateDpAsState(targetValue = if (active) 24.dp else 7.dp, label = "onboardingDotWidth")
    Box(
        modifier = Modifier
            .height(7.dp)
            .width(dotWidth)
            .clip(RoundedCornerShape(4.dp))
            .background(if (active) Colors.PRIMARY_BLUE.toComposeColor() else Colors.DIVIDER.toComposeColor()),
    )
}
