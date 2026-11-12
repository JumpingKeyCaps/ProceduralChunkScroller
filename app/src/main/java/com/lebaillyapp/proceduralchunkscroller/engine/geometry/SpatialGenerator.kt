package com.lebaillyapp.proceduralchunkscroller.engine.geometry

import com.lebaillyapp.proceduralchunkscroller.engine.config.GeneratorConfig
import com.lebaillyapp.proceduralchunkscroller.engine.noise.FbmNoise

/**
 * # Generates deterministic spatial geometry with independent walls.
 *
 * Evaluates the left and right boundaries separately using dedicated fBm signals,
 * removing the need for a central reference axis and preventing coordinate explosion.
 */
class SpatialGenerator(
    private val config: GeneratorConfig
) {

    /**
     * Generates a geometry sample at an absolute depth.
     *
     * @param depth Absolute spatial depth at which geometry is evaluated.
     * @return Generated geometry containing both independent wall coordinates.
     */
    fun sample(depth: Double): GeometrySample {
        require(depth.isFinite()) {
            "Depth must be finite."
        }

        // 1. Evaluate independent left and right wall signals
        val leftRaw = evaluateWallSignal(
            depth = depth,
            signal = config.leftWall, // ou config.asymmetry réadapté
            seed = config.seed xor LEFT_WALL_SEED_OFFSET,
            basePosition = -config.baseWidth * 0.5
        )

        val rightRaw = evaluateWallSignal(
            depth = depth,
            signal = config.rightWall,
            seed = config.seed xor RIGHT_WALL_SEED_OFFSET,
            basePosition = config.baseWidth * 0.5
        )

        var leftX = leftRaw
        var rightX = rightRaw

        require(leftX.isFinite() && rightX.isFinite()) {
            "Generated wall coordinates exceed the supported numeric range."
        }

        // 2. Enforce strict minimum width constraint ($W_{min}$)
        val currentWidth = rightX - leftX
        require(currentWidth.isFinite()) {
            "Generated passage width exceeds the supported numeric range."
        }

        if (currentWidth < config.minWidth) {
            val deficit = config.minWidth - currentWidth
            val center = (leftX + rightX) * 0.5
            val halfMin = config.minWidth * 0.5

            // Élargissement symétrique pour respecter W_min sans tout décaler
            leftX = center - halfMin
            rightX = center + halfMin
        }

        return GeometrySample(
            depth = depth,
            leftX = leftX,
            rightX = rightX
        )
    }

    private fun evaluateWallSignal(
        depth: Double,
        signal: GeneratorConfig.SignalConfig,
        seed: Long,
        basePosition: Double
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
        val displacement = basePosition + (centeredNoise * signal.amplitude)

        require(displacement.isFinite()) {
            "Generated wall displacement is not finite."
        }

        return displacement
    }

    private companion object {
        val LEFT_WALL_SEED_OFFSET: Long = 0x1A2B3C4D5E6F7081UL.toLong()
        val RIGHT_WALL_SEED_OFFSET: Long = 0x2B3C4D5E6F708192UL.toLong()
    }
}