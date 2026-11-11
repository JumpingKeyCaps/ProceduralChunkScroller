package com.lebaillyapp.proceduralchunkscroller.test

import androidx.compose.foundation.layout.padding



import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.lebaillyapp.proceduralchunkscroller.engine.config.GeneratorConfig
import com.lebaillyapp.proceduralchunkscroller.engine.geometry.GeometrySample
import com.lebaillyapp.proceduralchunkscroller.engine.geometry.SpatialGenerator
import kotlin.math.ceil
import kotlin.random.Random

private const val CHUNK_HEIGHT = 250
private const val SAMPLE_STEP = 1.0

private const val BASE_WIDTH = 95.0
private const val MIN_WIDTH = 16.0

private const val CENTERLINE_AMPLITUDE = 35.6
private const val CENTERLINE_FREQUENCY = 0.0021

private const val WIDTH_AMPLITUDE = 40.9
private const val WIDTH_FREQUENCY = 0.0010

private const val ASYMMETRY_AMPLITUDE = 27.3
private const val ASYMMETRY_FREQUENCY = 0.0040

private const val OCTAVES = 3

private const val CENTERLINE_PERSISTENCE = 0.30
private const val CENTERLINE_LACUNARITY = 1.30

private const val WIDTH_PERSISTENCE = 0.0
private const val WIDTH_LACUNARITY = 1.60

private const val ASYMMETRY_PERSISTENCE = 0.60
private const val ASYMMETRY_LACUNARITY = 1.20

private const val MIN_DEPTH = 250
private const val MAX_DEPTH = 20_000
private const val DEPTH_STEP = 250

/**
 * Displays a continuous procedural shaft using a fixed generator profile.
 *
 * The user can generate a new deterministic seed and adjust the maximum
 * exploration depth. The number of displayed chunks is derived from that
 * depth, while each chunk remains evaluated using absolute depth coordinates.
 */
@Composable
fun ScrollerTestScreen(
    modifier: Modifier = Modifier
) {
    var seed by remember {
        mutableLongStateOf(Random.nextLong())
    }

    var maxDepth by remember {
        mutableIntStateOf(5_000)
    }

    val config = remember(seed) {
        GeneratorConfig(
            seed = seed,
            baseWidth = BASE_WIDTH,
            minWidth = MIN_WIDTH,
            centerline = GeneratorConfig.SignalConfig(
                amplitude = CENTERLINE_AMPLITUDE,
                frequency = CENTERLINE_FREQUENCY,
                octaves = OCTAVES,
                persistence = CENTERLINE_PERSISTENCE,
                lacunarity = CENTERLINE_LACUNARITY
            ),
            width = GeneratorConfig.SignalConfig(
                amplitude = WIDTH_AMPLITUDE,
                frequency = WIDTH_FREQUENCY,
                octaves = OCTAVES,
                persistence = WIDTH_PERSISTENCE,
                lacunarity = WIDTH_LACUNARITY
            ),
            asymmetry = GeneratorConfig.SignalConfig(
                amplitude = ASYMMETRY_AMPLITUDE,
                frequency = ASYMMETRY_FREQUENCY,
                octaves = OCTAVES,
                persistence = ASYMMETRY_PERSISTENCE,
                lacunarity = ASYMMETRY_LACUNARITY
            )
        )
    }

    val generator = remember(config) {
        SpatialGenerator(config)
    }

    val chunkCount = ceil(
        maxDepth.toDouble() / CHUNK_HEIGHT
    ).toInt()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        ScrollerSettings(
            seed = seed,
            maxDepth = maxDepth,
            chunkCount = chunkCount,
            onGenerateSeed = {
                seed = Random.nextLong()
            },
            onMaxDepthChange = {
                maxDepth = it
            }
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            items(
                count = chunkCount,
                key = { index -> index }
            ) { index ->

                val startDepth = index * CHUNK_HEIGHT
                val endDepth = minOf(
                    startDepth + CHUNK_HEIGHT,
                    maxDepth
                )

                val samples = remember(
                    generator,
                    startDepth,
                    endDepth
                ) {
                    buildList {
                        var depth = startDepth.toDouble()

                        while (depth <= endDepth.toDouble()) {
                            add(generator.sample(depth))
                            depth += SAMPLE_STEP
                        }
                    }
                }

                ChunkPreview(
                    startDepth = startDepth,
                    endDepth = endDepth,
                    samples = samples
                )
            }
        }
    }
}

/**
 * Displays the seed and controls the maximum generated depth.
 *
 * The chunk count is derived from the selected depth and is not independently
 * configurable.
 */
@Composable
private fun ScrollerSettings(
    seed: Long,
    maxDepth: Int,
    chunkCount: Int,
    onGenerateSeed: () -> Unit,
    onMaxDepthChange: (Int) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column {
            androidx.compose.foundation.layout.Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Seed",
                        style = MaterialTheme.typography.labelMedium
                    )

                    Text(
                        text = seed.toString(),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Button(
                    onClick = onGenerateSeed
                ) {
                    Text("Generate seed")
                }
            }

            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                androidx.compose.foundation.layout.Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Maximum depth",
                        style = MaterialTheme.typography.labelMedium
                    )

                    Text(
                        text = "$maxDepth m",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Slider(
                    value = maxDepth.toFloat(),
                    onValueChange = { value ->
                        val snappedDepth = (
                                value / DEPTH_STEP
                                ).toInt() * DEPTH_STEP

                        onMaxDepthChange(
                            snappedDepth.coerceIn(
                                MIN_DEPTH,
                                MAX_DEPTH
                            )
                        )
                    },
                    valueRange = MIN_DEPTH.toFloat()..MAX_DEPTH.toFloat(),
                    steps = (
                            (MAX_DEPTH - MIN_DEPTH) / DEPTH_STEP
                            ) - 1
                )

                androidx.compose.foundation.layout.Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "$MIN_DEPTH m",
                        style = MaterialTheme.typography.labelSmall
                    )

                    Text(
                        text = "$MAX_DEPTH m",
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                Text(
                    text = "$chunkCount chunks · ${CHUNK_HEIGHT} m per chunk · ${SAMPLE_STEP} m sample step",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Renders one depth segment of the procedural shaft.
 *
 * No padding, spacing, rounded corners or headers are applied.
 * The canvas occupies the complete width and exact depth height of its chunk.
 */
@Composable
private fun ChunkPreview(
    startDepth: Int,
    endDepth: Int,
    samples: List<GeometrySample>
) {
    val wallColor = MaterialTheme.colorScheme.onSurface
    val fillColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    val backgroundColor = MaterialTheme.colorScheme.surfaceVariant

    val viewportHalfExtent =
        BASE_WIDTH +
                CENTERLINE_AMPLITUDE +
                WIDTH_AMPLITUDE +
                ASYMMETRY_AMPLITUDE

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height((endDepth - startDepth).dp)
            .background(backgroundColor)
    ) {
        if (samples.size < 2) return@Canvas

        fun mapX(x: Double): Float {
            return (
                    size.width / 2.0 +
                            (x / viewportHalfExtent) * (size.width / 2.0)
                    ).toFloat()
        }

        fun mapY(depth: Double): Float {
            val normalizedDepth =
                (depth - startDepth) /
                        (endDepth - startDepth).toDouble()

            return (normalizedDepth * size.height).toFloat()
        }

        val leftPath = Path()
        val rightPath = Path()
        val fillPath = Path()

        samples.forEachIndexed { sampleIndex, sample ->
            val left = Offset(
                x = mapX(sample.leftX),
                y = mapY(sample.depth)
            )

            val right = Offset(
                x = mapX(sample.rightX),
                y = mapY(sample.depth)
            )

            if (sampleIndex == 0) {
                leftPath.moveTo(left.x, left.y)
                rightPath.moveTo(right.x, right.y)
                fillPath.moveTo(left.x, left.y)
            } else {
                leftPath.lineTo(left.x, left.y)
                rightPath.lineTo(right.x, right.y)
                fillPath.lineTo(left.x, left.y)
            }
        }

        samples.asReversed().forEach { sample ->
            fillPath.lineTo(
                x = mapX(sample.rightX),
                y = mapY(sample.depth)
            )
        }

        fillPath.close()

        drawPath(
            path = fillPath,
            color = fillColor
        )

        drawPath(
            path = leftPath,
            color = wallColor,
            style = Stroke(width = 2.dp.toPx())
        )

        drawPath(
            path = rightPath,
            color = wallColor,
            style = Stroke(width = 2.dp.toPx())
        )
    }
}
