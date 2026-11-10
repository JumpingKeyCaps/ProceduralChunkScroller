package com.lebaillyapp.proceduralchunkscroller.engine.noise

import com.lebaillyapp.proceduralchunkscroller.engine.random.DeterministicSource
import kotlin.math.floor

/**
 * # Provides 1D interpolated Value Noise based on a deterministic source.
 *
 * Evaluates smooth, continuous curves across continuous coordinates by
 * interpolating between deterministic values sampled at discrete grid points.
 *
 * The resulting noise is independent of sampling order and can be evaluated
 * at any spatial position without maintaining internal state.
 */
object ValueNoise1D {

    /**
     * ### Samples 1D value noise at a given continuous spatial position.
     *
     * The position is scaled by the supplied frequency to determine the
     * discrete grid cell containing the requested coordinate.
     *
     * The two surrounding grid values are then interpolated using a quintic
     * smoothing curve.
     *
     * A frequency of 1.0 corresponds to one grid interval per spatial unit.
     *
     * Lower frequencies produce broader variations, while higher frequencies
     * produce more frequent variations.
     *
     * @param x Continuous spatial coordinate, such as absolute depth.
     * @param seed Generation seed.
     * @param frequency Spatial sampling frequency, strictly positive and finite.
     * @return A deterministic interpolated noise value in the range [0.0, 1.0).
     *
     * @throws IllegalArgumentException If x or frequency is not finite,
     * or if frequency is not strictly positive.
     * @throws IllegalArgumentException If the scaled coordinate exceeds the
     * representable Long index range.
     */
    fun sample(
        x: Double,
        seed: Long,
        frequency: Double = 1.0
    ): Double {
        require(x.isFinite()) {
            "Position must be finite."
        }

        require(frequency.isFinite() && frequency > 0.0) {
            "Frequency must be finite and strictly positive."
        }

        val position = x * frequency

        require(position.isFinite()) {
            "Scaled position must be finite."
        }

        val cell = floor(position)

        require(cell >= Long.MIN_VALUE.toDouble() && cell < Long.MAX_VALUE.toDouble()) {
            "Scaled position exceeds the supported grid index range."
        }

        val i0 = cell.toLong()
        val i1 = i0 + 1L
        val fraction = position - cell

        val v0 = DeterministicSource.sample01(seed, i0)
        val v1 = DeterministicSource.sample01(seed, i1)

        val t = smootherstep(fraction)

        return v0 + t * (v1 - v0)
    }

    /**
     * ### Applies quintic interpolation to a normalized position.
     *
     * The curve is defined by:
     *
     * **6t^5 - 15t^4 + 10t^3**
     *
     * Its first and second derivatives are zero at both endpoints,
     * providing smooth transitions between adjacent grid cells.
     *
     * @param t Interpolation factor in the range [0.0, 1.0).
     * @return Smoothed interpolation factor.
     */
    private fun smootherstep(t: Double): Double {
        return t * t * t * (t * (t * 6.0 - 15.0) + 10.0)
    }
}