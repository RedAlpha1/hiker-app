package com.ridgeline.ui.screens

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OnboardingStateTest {

    @Test
    fun startsAtStepZero() {
        val state = OnboardingState()
        assertEquals(0, state.stepIndex)
        assertEquals(ONBOARDING_STEPS[0], state.step)
        assertFalse(state.isLastStep)
    }

    @Test
    fun nextAdvancesAndClampsAtTheLastStep() {
        var state = OnboardingState()
        repeat(ONBOARDING_STEPS.size + 2) { state = state.next() }
        assertEquals(ONBOARDING_STEPS.lastIndex, state.stepIndex)
        assertTrue(state.isLastStep)
    }

    @Test
    fun skipJumpsStraightToTheLastStep() {
        val state = OnboardingState().skip()
        assertTrue(state.isLastStep)
    }

    @Test
    fun goToClampsOutOfRangeIndices() {
        assertEquals(0, OnboardingState().goTo(-5).stepIndex)
        assertEquals(ONBOARDING_STEPS.lastIndex, OnboardingState().goTo(99).stepIndex)
        assertEquals(2, OnboardingState().goTo(2).stepIndex)
    }
}
