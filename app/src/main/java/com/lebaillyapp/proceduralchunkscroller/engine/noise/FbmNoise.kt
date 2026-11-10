
package com.lebaillyapp.proceduralchunkscroller.engine.noise

import kotlin.math.pow

/**
 * # Fractional Brownian Motion (fBm) generator.
 *
 * Combines multiple octaves of a base noise function at varying frequencies
 * and amplitudes to produce natural-looking multi-scale fractal noise.
 *
 * Each octave contributes a progressively finer or coarser variation,
 * depending on the configured lacunarity and persistence.
 *
 * The generator is stateless and deterministic as long as the supplied
 * noise provider is deterministic.
 */
object FbmNoise {

    /**
     * ### Evaluates Fractional Brownian Motion at a continuous spatial position.
     *
     * Each octave samples the supplied noise provider using:
     *
     * - An independently offset seed.
     * - A progressively scaled frequency.
     * - A progressively scaled amplitude.
     *
     * The accumulated result is normalized by the sum of octave amplitudes.
     *
     * When the noise provider returns values in [0.0, 1.0), the normalized
     * result also remains in that range.
     *
     * @param x Continuous spatial coordinate.
     * @param seed Base generation seed.
     * @param octaves Number of noise layers, must be at least 1.
     * @param baseFrequency Initial spatial sampling frequency, strictly positive and finite.
     * @param persistence Amplitude multiplier per octave, finite and non-negative.
     * @param lacunarity Frequency multiplier per octave, strictly positive and finite.
     * @param noiseProvider Function defining the base noise source to accumulate.
     * @return Normalized fractal noise value.
     *
     * @throws IllegalArgumentException If an input parameter is invalid or
     * if an intermediate calculation exceeds the supported numeric range.
     */
    fun sample(
        x: Double,
        seed: Long,
        octaves: Int = 3,
        baseFrequency: Double = 1.0,
        persistence: Double = 0.5,
        lacunarity: Double = 2.0,
        noiseProvider: (
            spatialCoord: Double,
            currentSeed: Long,
            currentFrequency: Double
        ) -> Double = { spatialCoord, currentSeed, currentFrequency ->
            ValueNoise1D.sample(spatialCoord, currentSeed, currentFrequency)
        }
    ): Double {
        require(x.isFinite()) {
            "Position must be finite."
        }

        require(octaves >= 1) {
            "Octaves must be at least 1."
        }

        require(baseFrequency.isFinite() && baseFrequency > 0.0) {
            "Base frequency must be finite and strictly positive."
        }

        require(persistence.isFinite() && persistence >= 0.0) {
            "Persistence must be finite and non-negative."
        }

        require(lacunarity.isFinite() && lacunarity > 0.0) {
            "Lacunarity must be finite and strictly positive."
        }

        var total = 0.0
        var frequency = baseFrequency
        var amplitude = 1.0
        var normalizationFactor = 0.0

        for (i in 0 until octaves) {
            // Offset the seed to decorrelate successive noise layers.
            val octaveSeed = seed + i.toLong() * 1013L

            val noiseValue = noiseProvider(x, octaveSeed, frequency)

            require(noiseValue.isFinite()) {
                "Noise provider returned a non-finite value at octave $i."
            }

            total += noiseValue * amplitude
            normalizationFactor += amplitude

            require(total.isFinite() && normalizationFactor.isFinite()) {
                "Accumulated noise exceeds the supported numeric range."
            }

            // No need to calculate unused parameters after the last octave.
            if (i < octaves - 1) {
                amplitude *= persistence
                frequency *= lacunarity

                require(frequency.isFinite() && frequency > 0.0) {
                    "Octave frequency exceeds the supported numeric range."
                }

                require(amplitude.isFinite()) {
                    "Octave amplitude exceeds the supported numeric range."
                }
            }
        }

        return total / normalizationFactor
    }
}
