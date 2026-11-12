package com.lebaillyapp.proceduralchunkscroller.engine.config

/**
 * # Defines the parameters used by the procedural geometry generator.
 *
 * Provides an explicit configuration for independent spatial signals
 * driving the left and right walls, with support for depth-based biomes.
 */
data class GeneratorConfig(

    /**
     * Global seed identifying the generated geometry.
     */
    val seed: Long,

    /**
     * Default reference width of the generated passage (used as fallback).
     */
    val baseWidth: Double = 100.0,

    /**
     * Default minimum permitted width of the generated passage.
     */
    val minWidth: Double = 30.0,

    /**
     * Default left wall spatial signal configuration.
     */
    val leftWall: SignalConfig = SignalConfig(
        amplitude = 25.0,
        frequency = 0.01,
        octaves = 3,
        persistence = 0.5,
        lacunarity = 2.0
    ),

    /**
     * Default right wall spatial signal configuration.
     */
    val rightWall: SignalConfig = SignalConfig(
        amplitude = 25.0,
        frequency = 0.01,
        octaves = 3,
        persistence = 0.5,
        lacunarity = 2.0
    ),

    /**
     * Ordered list of depth-based biomes governing structural shifts.
     */
    val biomes: List<BiomeLayer> = emptyList()

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

        // Validate biomes chronological order and non-overlapping bounds if present
        if (biomes.isNotEmpty()) {
            for (i in 0 until biomes.size - 1) {
                val current = biomes[i]
                val next = biomes[i + 1]
                require(current.endDepth <= next.startDepth) {
                    "Biomes must be continuous and ordered by depth without overlapping."
                }
            }
        }
    }

    /**
     * # Defines a localized biome layer over a specific depth interval.
     */
    data class BiomeLayer(
        val startDepth: Double,
        val endDepth: Double,
        val baseWidth: Double,
        val minWidth: Double,
        val leftWall: SignalConfig,
        val rightWall: SignalConfig
    ) {
        init {
            require(startDepth.isFinite() && startDepth >= 0.0) {
                "Biome start depth must be finite and non-negative."
            }
            require(endDepth.isFinite() && endDepth > startDepth) {
                "Biome end depth must be finite and strictly greater than start depth."
            }
            require(baseWidth.isFinite() && baseWidth > 0.0) {
                "Biome base width must be finite and strictly positive."
            }
            require(minWidth.isFinite() && minWidth > 0.0) {
                "Biome minimum width must be finite and strictly positive."
            }
            require(minWidth <= baseWidth) {
                "Biome minimum width cannot exceed base width."
            }
        }
    }

    /**
     * # Defines the parameters of an individual spatial signal.
     */
    data class SignalConfig(
        val amplitude: Double,
        val frequency: Double,
        val octaves: Int = 3,
        val persistence: Double = 0.5,
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