package com.ridgeline.ui.screens

data class OnboardingStep(val title: String, val body: String)

/** Copy transcribed verbatim from the design file's four onboarding cards. */
val ONBOARDING_STEPS: List<OnboardingStep> = listOf(
    OnboardingStep(
        title = "Record every trail",
        body = "Distance, elevation gain, pace and your route — tracked automatically, even with zero signal.",
    ),
    OnboardingStep(
        title = "Name what you see",
        body = "Point your camera at the skyline. Every peak in frame gets labeled, live, with elevation and distance.",
    ),
    OnboardingStep(
        title = "Built for offline",
        body = "Download map and terrain data before you go. Everything works with zero signal, all trip.",
    ),
    OnboardingStep(
        title = "Runs too",
        body = "Not just hikes — track daily runs with pace, duration and calories, no peak ID needed.",
    ),
)

data class OnboardingState(val stepIndex: Int = 0) {
    init {
        require(stepIndex in ONBOARDING_STEPS.indices) { "stepIndex out of range: $stepIndex" }
    }

    val step: OnboardingStep get() = ONBOARDING_STEPS[stepIndex]
    val isLastStep: Boolean get() = stepIndex == ONBOARDING_STEPS.lastIndex

    fun next(): OnboardingState = copy(stepIndex = (stepIndex + 1).coerceAtMost(ONBOARDING_STEPS.lastIndex))
    fun skip(): OnboardingState = copy(stepIndex = ONBOARDING_STEPS.lastIndex)
    fun goTo(index: Int): OnboardingState = copy(stepIndex = index.coerceIn(ONBOARDING_STEPS.indices))
}
