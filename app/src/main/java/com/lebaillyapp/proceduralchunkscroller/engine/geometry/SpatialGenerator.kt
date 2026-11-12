package com.lebaillyapp.proceduralchunkscroller.engine.geometry

import com.lebaillyapp.proceduralchunkscroller.engine.config.GeneratorConfig
import com.lebaillyapp.proceduralchunkscroller.engine.noise.FbmNoise

/**
 * # Generates deterministic spatial geometry with independent walls and depth-based biomes.
 *
 * Evaluates boundaries using output-blended biomes to completely eliminate
 * frequency or octave discontinuity artifacts across biome borders.
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

        val biomes = config.biomes

        // Si aucun biome n'est configuré, comportement standard global
        val resolution = if (biomes.isEmpty()) {
            val left = evaluateWallSignal(depth, config.leftWall, config.seed xor LEFT_WALL_SEED_OFFSET, -config.baseWidth * 0.5)
            val right = evaluateWallSignal(depth, config.rightWall, config.seed xor RIGHT_WALL_SEED_OFFSET, config.baseWidth * 0.5)
            SampleResolution(left, right, config.minWidth)
        } else {
            resolveSampleWithBiomes(depth, biomes)
        }

        var leftX = resolution.leftX
        var rightX = resolution.rightX
        val minWidth = resolution.minWidth



        require(leftX.isFinite() && rightX.isFinite()) {
            "Generated wall coordinates exceed the supported numeric range."
        }

        // 3. Enforce strict minimum width constraint ($W_{min}$)
        val currentWidth = rightX - leftX
        require(currentWidth.isFinite()) {
            "Generated passage width exceeds the supported numeric range."
        }

        if (currentWidth < minWidth) {
            val center = (leftX + rightX) * 0.5
            val halfMin = minWidth * 0.5

            leftX = center - halfMin
            rightX = center + halfMin
        }

        return GeometrySample(
            depth = depth,
            leftX = leftX,
            rightX = rightX
        )
    }

    /**
     * Résout l'échantillon en gérant soit le biome actif, soit le blending de sortie
     * entre deux biomes si on se trouve dans une zone de transition centrée.
     */
    private fun resolveSampleWithBiomes(depth: Double, biomes: List<GeneratorConfig.BiomeLayer>): SampleResolution {
        val first = biomes.first()
        if (depth <= first.startDepth) {
            val l = evaluateWallSignal(depth, first.leftWall, config.seed xor LEFT_WALL_SEED_OFFSET, -first.baseWidth * 0.5)
            val r = evaluateWallSignal(depth, first.rightWall, config.seed xor RIGHT_WALL_SEED_OFFSET, first.baseWidth * 0.5)
            return SampleResolution(l, r, first.minWidth)
        }

        val last = biomes.last()
        if (depth >= last.endDepth) {
            val l = evaluateWallSignal(depth, last.leftWall, config.seed xor LEFT_WALL_SEED_OFFSET, -last.baseWidth * 0.5)
            val r = evaluateWallSignal(depth, last.rightWall, config.seed xor RIGHT_WALL_SEED_OFFSET, last.baseWidth * 0.5)
            return SampleResolution(l, r, last.minWidth)
        }

        for (i in biomes.indices) {
            val biome = biomes[i]

            // Vérification de la zone de transition avec le biome suivant
            if (i < biomes.size - 1) {
                val nextBiome = biomes[i + 1]
                val boundary = biome.endDepth
                val blendRadius = 75.0 // Zone de transition de 150m centrée sur la frontière

                if (depth in (boundary - blendRadius)..(boundary + blendRadius)) {
                    val linearT = (depth - (boundary - blendRadius)) / (2.0 * blendRadius)
                    val smoothT = linearT * linearT * (3.0 - 2.0 * linearT)

                    // Output blending : on calcule les deux murs indépendamment et on interpole le résultat final
                    return blendBiomes(depth, biome, nextBiome, smoothT)
                }
            }

            if (depth in biome.startDepth..biome.endDepth) {
                val l = evaluateWallSignal(depth, biome.leftWall, config.seed xor LEFT_WALL_SEED_OFFSET, -biome.baseWidth * 0.5)
                val r = evaluateWallSignal(depth, biome.rightWall, config.seed xor RIGHT_WALL_SEED_OFFSET, biome.baseWidth * 0.5)
                return SampleResolution(l, r, biome.minWidth)
            }
        }

        // Fallback global par sécurité
        val l = evaluateWallSignal(depth, config.leftWall, config.seed xor LEFT_WALL_SEED_OFFSET, -config.baseWidth * 0.5)
        val r = evaluateWallSignal(depth, config.rightWall, config.seed xor RIGHT_WALL_SEED_OFFSET, config.baseWidth * 0.5)
        return SampleResolution(l, r, config.minWidth)
    }

    /**
     * Effectue un fondu enchaîné sur les positions finales des murs entre deux biomes adjacents.
     */
    private fun blendBiomes(
        depth: Double,
        b1: GeneratorConfig.BiomeLayer,
        b2: GeneratorConfig.BiomeLayer,
        t: Double
    ): SampleResolution {
        // Mur gauche Biome 1 vs Biome 2
        val left1 = evaluateWallSignal(depth, b1.leftWall, config.seed xor LEFT_WALL_SEED_OFFSET, -b1.baseWidth * 0.5)
        val left2 = evaluateWallSignal(depth, b2.leftWall, config.seed xor LEFT_WALL_SEED_OFFSET, -b2.baseWidth * 0.5)
        val blendedLeft = lerp(left1, left2, t)

        // Mur droit Biome 1 vs Biome 2
        val right1 = evaluateWallSignal(depth, b1.rightWall, config.seed xor RIGHT_WALL_SEED_OFFSET, b1.baseWidth * 0.5)
        val right2 = evaluateWallSignal(depth, b2.rightWall, config.seed xor RIGHT_WALL_SEED_OFFSET, b2.baseWidth * 0.5)
        val blendedRight = lerp(right1, right2, t)

        val blendedMinWidth = lerp(b1.minWidth, b2.minWidth, t)

        return SampleResolution(blendedLeft, blendedRight, blendedMinWidth)
    }

    private data class SampleResolution(
        val leftX: Double,
        val rightX: Double,
        val minWidth: Double
    )

    private fun lerp(start: Double, end: Double, fraction: Double): Double {
        return start + (end - start) * fraction
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