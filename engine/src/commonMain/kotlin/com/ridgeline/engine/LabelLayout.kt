package com.ridgeline.engine

/**
 * Lanes are fixed horizontal bands at absolute screen y-positions, not
 * offsets from each peak's own anchor. See DECISIONS.md, "Label lanes are
 * absolute, not per-peak offsets": real summits from one viewpoint cluster
 * into a narrow elevation-angle band, so per-peak offsets let chips in
 * different lanes land within a chip-height of each other. Absolute bands
 * make lane separation exact; the leader line absorbs the varying drop from
 * chip to actual summit position.
 */
data class LabelLayoutConfig(
    val laneCount: Int = 4,
    val topMarginPx: Double,
    val laneSpacingPx: Double,
    val chipWidthPx: Double,
    val minChipGapPx: Double = 4.0,
) {
    init {
        require(laneCount > 0) { "laneCount must be positive" }
    }

    fun laneYPx(laneIndex: Int): Double = topMarginPx + laneIndex * laneSpacingPx
}

/**
 * A peak placed for rendering: either a name chip in a fixed lane (with a
 * leader line back to [anchorXPx]/[anchorYPx], its actual projected
 * position) or a bare dot at the anchor. See DECISIONS.md, "Distant peaks
 * get dots, not names".
 */
data class PlacedLabel(
    val visiblePeak: VisiblePeak,
    val anchorXPx: Double,
    val anchorYPx: Double,
    val isDot: Boolean,
    val laneIndex: Int? = null,
    val chipXPx: Double? = null,
)

/**
 * Assigns [projected] peaks to label chips in fixed lanes, or demotes them to
 * dots when confidence is [Confidence.MARGINAL] or every lane is already
 * occupied at that x. `CLEAR` peaks take priority over `HAZY` ones when lanes
 * run out (DECISIONS.md, "Distant peaks get dots, not names"); within a tier,
 * nearer peaks are placed first.
 */
fun layoutLabels(projected: List<ProjectedPeak>, config: LabelLayoutConfig): List<PlacedLabel> {
    val inFrame = projected.filter { it.inFrame }
    val (chipCandidates, dotOnly) = inFrame.partition { it.visiblePeak.confidence != Confidence.MARGINAL }

    val priorityOrder = chipCandidates.sortedWith(
        compareBy(
            { it.visiblePeak.confidence != Confidence.CLEAR }, // CLEAR (false) sorts before HAZY (true)
            { it.visiblePeak.distanceM },
        )
    )

    val laneOccupancy = Array(config.laneCount) { mutableListOf<ClosedFloatingPointRange<Double>>() }
    val placed = mutableListOf<PlacedLabel>()
    val demotedToDot = mutableListOf<ProjectedPeak>()

    for (candidate in priorityOrder) {
        val halfWidth = config.chipWidthPx / 2.0
        val desiredRange = (candidate.xPx - halfWidth)..(candidate.xPx + halfWidth)

        val lane = (0 until config.laneCount).firstOrNull { laneIndex ->
            laneOccupancy[laneIndex].none { occupied -> overlapsWithGap(desiredRange, occupied, config.minChipGapPx) }
        }

        if (lane != null) {
            laneOccupancy[lane].add(desiredRange)
            placed += PlacedLabel(
                visiblePeak = candidate.visiblePeak,
                anchorXPx = candidate.xPx,
                anchorYPx = candidate.yPx,
                isDot = false,
                laneIndex = lane,
                chipXPx = candidate.xPx,
            )
        } else {
            demotedToDot += candidate
        }
    }

    for (dot in dotOnly + demotedToDot) {
        placed += PlacedLabel(
            visiblePeak = dot.visiblePeak,
            anchorXPx = dot.xPx,
            anchorYPx = dot.yPx,
            isDot = true,
        )
    }

    return placed
}

private fun overlapsWithGap(
    a: ClosedFloatingPointRange<Double>,
    b: ClosedFloatingPointRange<Double>,
    gap: Double,
): Boolean {
    val separated = a.endInclusive + gap <= b.start || b.endInclusive + gap <= a.start
    return !separated
}
