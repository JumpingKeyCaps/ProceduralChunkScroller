
package com.lebaillyapp.proceduralchunkscroller.engine.geometry

import com.lebaillyapp.proceduralchunkscroller.engine.config.GeneratorConfig
import com.lebaillyapp.proceduralchunkscroller.engine.noise.FbmNoise

/**
 * # Generates deterministic spatial geometry from a procedural configuration.
 *
 * Combines three independent fBm signals to define the shape of a passage:
 * - Centerline displacement.
 * - Global width variation.
 * - Independent left and right wall asymmetry.
 *
 * Each sample is calculated directly from its absolute depth and the
 * configured seed. No previous samples or internal generation state
 * are required.
 *
 * The generator is independent of rendering dimensions, chunk boundaries,
 * Android and Compose.
 */
class SpatialGenerator(
    private val config: GeneratorConfig
) {

    /**
     * ### Generates a geometry sample at an absolute depth.
     *
     * The centerline signal determines the horizontal displacement
     * of the passage.
     *
     * The width signal expands or narrows the passage around its
     * configured reference width.
     *
     * Two independently seeded asymmetry signals deform the left
     * and right walls separately.
     *
     * The final geometry always respects the configured minimum width.
     *
     * @param depth Absolute spatial depth at which geometry is evaluated.
     * @return Generated geometry containing both wall coordinates.
     *
     * @throws IllegalArgumentException If the depth is not finite or
     * if an intermediate calculation exceeds the supported numeric range.
     */
    fun sample(depth: Double): GeometrySample {
        require(depth.isFinite()) {
            "Depth must be finite."
        }

        val centerOffset = evaluateSignal(
            depth = depth,
            signal = config.centerline,
            seed = config.seed xor CENTERLINE_SEED_OFFSET
        )

        val widthVariation = evaluateSignal(
            depth = depth,
            signal = config.width,
            seed = config.seed xor WIDTH_SEED_OFFSET
        )

        val leftAsymmetry = evaluateSignal(
            depth = depth,
            signal = config.asymmetry,
            seed = config.seed xor LEFT_ASYMMETRY_SEED_OFFSET
        )

        val rightAsymmetry = evaluateSignal(
            depth = depth,
            signal = config.asymmetry,
            seed = config.seed xor RIGHT_ASYMMETRY_SEED_OFFSET
        )

        val centerX = centerOffset

        val requestedWidth = config.baseWidth + widthVariation

        require(requestedWidth.isFinite()) {
            "Generated width exceeds the supported numeric range."
        }

        val baseWidth = requestedWidth.coerceAtLeast(config.minWidth)
        val halfWidth = baseWidth * 0.5

        var leftX = centerX - halfWidth + leftAsymmetry
        var rightX = centerX + halfWidth + rightAsymmetry

        require(leftX.isFinite() && rightX.isFinite()) {
            "Generated wall coordinates exceed the supported numeric range."
        }

        val finalWidth = rightX - leftX

        require(finalWidth.isFinite()) {
            "Generated passage width exceeds the supported numeric range."
        }

        if (finalWidth < config.minWidth) {
            val actualCenter = leftX + finalWidth * 0.5
            val minimumHalfWidth = config.minWidth * 0.5

            leftX = actualCenter - minimumHalfWidth
            rightX = actualCenter + minimumHalfWidth
        }

        return GeometrySample(
            depth = depth,
            leftX = leftX,
            rightX = rightX
        )
    }

    /**
     * ### Evaluates an individual spatial signal.
     *
     * The fBm result is normalized in the range [0.0, 1.0).
     * It is remapped to [-1.0, 1.0) before applying the configured amplitude.
     *
     * This produces variations in both directions around the signal's
     * neutral position.
     *
     * @param depth Absolute spatial depth.
     * @param signal Configuration of the signal to evaluate.
     * @param seed Independent seed assigned to this signal.
     * @return Signed spatial displacement.
     */
    private fun evaluateSignal(
        depth: Double,
        signal: GeneratorConfig.SignalConfig,
        seed: Long
    ): Double {
        val noise = FbmNoise.sample(
            x = depth,
            seed = seed,
            octaves = signal.octaves,
            baseFrequency = signal.frequency,
            persistence = signal.persistence,
            lacunarity = signal.lacunarity
        )

        val centeredNoise = noise * 2.0 - 1.0
        val displacement = centeredNoise * signal.amplitude

        require(displacement.isFinite()) {
            "Generated signal displacement is not finite."
        }

        return displacement
    }

    private companion object {

        /** Independent seed offset for centerline displacement. */
        val CENTERLINE_SEED_OFFSET: Long = 0x1A2B3C4D5E6F7081UL.toLong()

        /** Independent seed offset for global width variation. */
        val WIDTH_SEED_OFFSET: Long = 0x2B3C4D5E6F708192UL.toLong()

        /** Independent seed offset for left wall asymmetry. */
        val LEFT_ASYMMETRY_SEED_OFFSET: Long = 0x3C4D5E6F708192A3UL.toLong()

        /** Independent seed offset for right wall asymmetry. */
        val RIGHT_ASYMMETRY_SEED_OFFSET: Long = 0x4D5E6F708192A3B4UL.toLong()
    }
}
