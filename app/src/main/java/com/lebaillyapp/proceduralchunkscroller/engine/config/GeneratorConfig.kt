
package com.lebaillyapp.proceduralchunkscroller.engine.config

/**
 * # Defines the parameters used by the procedural geometry generator.
 *
 * Provides an explicit configuration for the three independent spatial signals:
 * - Centerline displacement.
 * - Global width variation.
 * - Left/right wall asymmetry.
 *
 * Each signal has its own fBm parameters, allowing independent control over
 * its amplitude, spatial frequency and fractal characteristics.
 *
 * The configuration is immutable and contains no generation state.
 *
 * All spatial values use the same abstract coordinate unit.
 */
data class GeneratorConfig(

    /**
     * Global seed identifying the generated geometry.
     */
    val seed: Long,

    /**
     * Reference width of the generated passage.
     *
     * This is the width around which the width signal produces variations.
     */
    val baseWidth: Double = 100.0,

    /**
     * Minimum permitted width of the generated passage.
     *
     * The geometry generator must enforce this constraint when calculating
     * the final distance between the two walls.
     */
    val minWidth: Double = 30.0,

    /**
     * ### Centerline displacement signal.
     *
     * Controls the horizontal displacement of the passage's central axis.
     */
    val centerline: SignalConfig = SignalConfig(
        amplitude = 20.0,
        frequency = 0.01,
        octaves = 3,
        persistence = 0.5,
        lacunarity = 2.0
    ),

    /**
     * ### Global width variation signal.
     *
     * Controls how the passage expands and narrows around its reference width.
     */
    val width: SignalConfig = SignalConfig(
        amplitude = 15.0,
        frequency = 0.005,
        octaves = 3,
        persistence = 0.5,
        lacunarity = 2.0
    ),

    /**
     * ### Left/right wall asymmetry signal.
     *
     * Controls the independent lateral deformation of each wall.
     */
    val asymmetry: SignalConfig = SignalConfig(
        amplitude = 8.0,
        frequency = 0.015,
        octaves = 3,
        persistence = 0.5,
        lacunarity = 2.0
    )

) {

    init {
        require(baseWidth.isFinite() && baseWidth > 0.0) {
            "Base width must be finite and strictly positive."
        }

        require(minWidth.isFinite() && minWidth > 0.0) {
            "Minimum width must be finite and strictly positive."
        }

        require(minWidth <= baseWidth) {
            "Minimum width cannot exceed base width."
        }
    }

    /**
     * # Defines the parameters of an individual spatial signal.
     *
     * The amplitude controls the magnitude of the signal.
     *
     * The frequency and fBm parameters control its spatial variation.
     *
     * This configuration can be independently assigned to any of the
     * generator's spatial signals.
     */
    data class SignalConfig(

        /**
         * Maximum displacement magnitude produced by this signal.
         */
        val amplitude: Double,

        /**
         * Initial spatial sampling frequency.
         */
        val frequency: Double,

        /**
         * Number of fBm octaves.
         */
        val octaves: Int = 3,

        /**
         * Amplitude multiplier applied between successive octaves.
         */
        val persistence: Double = 0.5,

        /**
         * Frequency multiplier applied between successive octaves.
         */
        val lacunarity: Double = 2.0

    ) {

        init {
            require(amplitude.isFinite() && amplitude >= 0.0) {
                "Signal amplitude must be finite and non-negative."
            }

            require(frequency.isFinite() && frequency > 0.0) {
                "Signal frequency must be finite and strictly positive."
            }

            require(octaves >= 1) {
                "Signal octaves must be at least 1."
            }

            require(persistence.isFinite() && persistence >= 0.0) {
                "Signal persistence must be finite and non-negative."
            }

            require(lacunarity.isFinite() && lacunarity > 0.0) {
                "Signal lacunarity must be finite and strictly positive."
            }
        }
    }
}
