
package com.lebaillyapp.proceduralchunkscroller.test

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.lebaillyapp.proceduralchunkscroller.engine.config.GeneratorConfig
import com.lebaillyapp.proceduralchunkscroller.engine.geometry.GeometrySample
import com.lebaillyapp.proceduralchunkscroller.engine.geometry.SpatialGenerator
import kotlin.math.ceil
import kotlin.random.Random

private const val DEFAULT_CHUNK_HEIGHT = 250
private const val DEFAULT_SAMPLE_STEP = 1.0

private const val DEFAULT_BASE_WIDTH = 95.0
private const val DEFAULT_MIN_WIDTH = 16.0

private const val DEFAULT_CENTERLINE_AMPLITUDE = 35.6
private const val DEFAULT_CENTERLINE_FREQUENCY = 0.0021

private const val DEFAULT_WIDTH_AMPLITUDE = 40.9
private const val DEFAULT_WIDTH_FREQUENCY = 0.0010

private const val DEFAULT_ASYMMETRY_AMPLITUDE = 27.3
private const val DEFAULT_ASYMMETRY_FREQUENCY = 0.0040

private const val DEFAULT_OCTAVES = 3

private const val DEFAULT_CENTERLINE_PERSISTENCE = 0.30
private const val DEFAULT_CENTERLINE_LACUNARITY = 1.30

private const val DEFAULT_WIDTH_PERSISTENCE = 0.0
private const val DEFAULT_WIDTH_LACUNARITY = 1.60

private const val DEFAULT_ASYMMETRY_PERSISTENCE = 0.60
private const val DEFAULT_ASYMMETRY_LACUNARITY = 1.20

private const val MIN_DEPTH = 250
private const val MAX_DEPTH = 20_000
private const val DEPTH_STEP = 250

/**
 * Creates the calibrated default geometry configuration.
 */
private fun createDefaultConfig(seed: Long): GeneratorConfig {
    return GeneratorConfig(
        seed = seed,
        baseWidth = DEFAULT_BASE_WIDTH,
        minWidth = DEFAULT_MIN_WIDTH,
        centerline = GeneratorConfig.SignalConfig(
            amplitude = DEFAULT_CENTERLINE_AMPLITUDE,
            frequency = DEFAULT_CENTERLINE_FREQUENCY,
            octaves = DEFAULT_OCTAVES,
            persistence = DEFAULT_CENTERLINE_PERSISTENCE,
            lacunarity = DEFAULT_CENTERLINE_LACUNARITY
        ),
        width = GeneratorConfig.SignalConfig(
            amplitude = DEFAULT_WIDTH_AMPLITUDE,
            frequency = DEFAULT_WIDTH_FREQUENCY,
            octaves = DEFAULT_OCTAVES,
            persistence = DEFAULT_WIDTH_PERSISTENCE,
            lacunarity = DEFAULT_WIDTH_LACUNARITY
        ),
        asymmetry = GeneratorConfig.SignalConfig(
            amplitude = DEFAULT_ASYMMETRY_AMPLITUDE,
            frequency = DEFAULT_ASYMMETRY_FREQUENCY,
            octaves = DEFAULT_OCTAVES,
            persistence = DEFAULT_ASYMMETRY_PERSISTENCE,
            lacunarity = DEFAULT_ASYMMETRY_LACUNARITY
        )
    )
}

/**
 * Displays the procedural shaft over an adjustable absolute depth range.
 *
 * The generator profile can be modified in real time through a lateral
 * settings panel. The number of chunks is derived from the maximum depth.
 * Each chunk is sampled using absolute depth coordinates.
 */
@Composable
fun ScrollerTestScreen(
    modifier: Modifier = Modifier
) {
    var seed by remember {
        mutableLongStateOf(Random.nextLong())
    }

    var config by remember(seed) {
        mutableStateOf(createDefaultConfig(seed))
    }

    var maxDepth by remember {
        mutableIntStateOf(5_000)
    }

    var chunkHeight by remember {
        mutableIntStateOf(DEFAULT_CHUNK_HEIGHT)
    }

    var sampleStep by remember {
        mutableStateOf(DEFAULT_SAMPLE_STEP)
    }

    var settingsVisible by remember {
        mutableStateOf(false)
    }

    val generator = remember(config) {
        SpatialGenerator(config)
    }

    val chunkCount = ceil(
        maxDepth.toDouble() / chunkHeight
    ).toInt()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Procedural Scroller",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text(
                        text = "$maxDepth m · $chunkCount chunks · Seed $seed",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = {
                        settingsVisible = !settingsVisible
                    }
                ) {
                    Text(
                        text = if (settingsVisible) "Close" else "Settings"
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                items(
                    count = chunkCount,
                    key = { index -> index }
                ) { index ->

                    val startDepth = index * chunkHeight
                    val endDepth = minOf(
                        startDepth + chunkHeight,
                        maxDepth
                    )

                    val samples = remember(
                        generator,
                        startDepth,
                        endDepth,
                        sampleStep
                    ) {
                        generateChunkSamples(
                            generator = generator,
                            startDepth = startDepth,
                            endDepth = endDepth,
                            sampleStep = sampleStep
                        )
                    }

                    ChunkPreview(
                        index = index,
                        startDepth = startDepth,
                        endDepth = endDepth,
                        samples = samples,
                        viewportHalfExtent = calculateViewportHalfExtent(config)
                    )
                }
            }
        }

        if (settingsVisible) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.25f))
                    .clickable {
                        settingsVisible = false
                    }
            )
        }

        AnimatedVisibility(
            visible = settingsVisible,
            modifier = Modifier.align(Alignment.CenterEnd),
            enter = slideInHorizontally { it },
            exit = slideOutHorizontally { it }
        ) {
            SettingsPanel(
                seed = seed,
                config = config,
                maxDepth = maxDepth,
                chunkHeight = chunkHeight,
                sampleStep = sampleStep,
                onGenerateSeed = {
                    seed = Random.nextLong()
                    config = config.copy(seed = seed)
                },
                onMaxDepthChange = {
                    maxDepth = it
                },
                onChunkHeightChange = {
                    chunkHeight = it
                },
                onSampleStepChange = {
                    sampleStep = it
                },
                onConfigChange = {
                    config = it
                },
                onClose = {
                    settingsVisible = false
                }
            )
        }
    }
}

/**
 * Generates all samples belonging to one chunk.
 *
 * Both chunk boundaries are included. The final boundary is explicitly
 * sampled when the sample step does not divide the chunk height exactly.
 */
private fun generateChunkSamples(
    generator: SpatialGenerator,
    startDepth: Int,
    endDepth: Int,
    sampleStep: Double
): List<GeometrySample> {
    return buildList {
        var depth = startDepth.toDouble()

        while (depth < endDepth.toDouble()) {
            add(generator.sample(depth))
            depth += sampleStep
        }

        if (isEmpty() || last().depth != endDepth.toDouble()) {
            add(generator.sample(endDepth.toDouble()))
        }
    }
}

/**
 * Calculates a shared horizontal viewport extent for all chunks.
 */
private fun calculateViewportHalfExtent(
    config: GeneratorConfig
): Double {
    return (
            config.baseWidth +
                    config.centerline.amplitude +
                    config.width.amplitude +
                    config.asymmetry.amplitude
            ).coerceAtLeast(1.0)
}

/**
 * Displays the lateral settings panel.
 *
 * All configuration changes are immediately propagated to the generator.
 */
@Composable
private fun SettingsPanel(
    seed: Long,
    config: GeneratorConfig,
    maxDepth: Int,
    chunkHeight: Int,
    sampleStep: Double,
    onGenerateSeed: () -> Unit,
    onMaxDepthChange: (Int) -> Unit,
    onChunkHeightChange: (Int) -> Unit,
    onSampleStepChange: (Double) -> Unit,
    onConfigChange: (GeneratorConfig) -> Unit,
    onClose: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxHeight()
            .width(320.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        shadowElevation = 12.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.titleLarge
                )

                Button(onClick = onClose) {
                    Text("Close")
                }
            }

            Text(
                text = "Generation",
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = "Seed: $seed",
                style = MaterialTheme.typography.bodySmall
            )

            Button(
                onClick = onGenerateSeed,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Generate new seed")
            }

            SliderSetting(
                label = "Maximum depth",
                value = maxDepth.toFloat(),
                valueText = "$maxDepth m",
                valueRange = MIN_DEPTH.toFloat()..MAX_DEPTH.toFloat(),
                steps = ((MAX_DEPTH - MIN_DEPTH) / DEPTH_STEP) - 1,
                onValueChange = {
                    val snapped = (
                            it / DEPTH_STEP
                            ).toInt() * DEPTH_STEP

                    onMaxDepthChange(
                        snapped.coerceIn(MIN_DEPTH, MAX_DEPTH)
                    )
                }
            )

            SliderSetting(
                label = "Chunk height",
                value = chunkHeight.toFloat(),
                valueText = "$chunkHeight m",
                valueRange = 50f..1_000f,
                steps = 18,
                onValueChange = {
                    onChunkHeightChange(
                        (it / 50f).toInt() * 50
                    )
                }
            )

            SliderSetting(
                label = "Sample step",
                value = sampleStep.toFloat(),
                valueText = "%.2f m".format(sampleStep),
                valueRange = 0.25f..10f,
                steps = 38,
                onValueChange = {
                    onSampleStepChange(it.toDouble())
                }
            )

            DividerLine()

            Text(
                text = "Geometry",
                style = MaterialTheme.typography.titleMedium
            )

            SliderSetting(
                label = "Base width",
                value = config.baseWidth.toFloat(),
                valueText = "%.1f".format(config.baseWidth),
                valueRange = 30f..300f,
                onValueChange = {
                    val newWidth = it.toDouble()

                    onConfigChange(
                        config.copy(
                            baseWidth = newWidth,
                            minWidth = config.minWidth.coerceAtMost(newWidth)
                        )
                    )
                }
            )

            SliderSetting(
                label = "Minimum width",
                value = config.minWidth.toFloat(),
                valueText = "%.1f".format(config.minWidth),
                valueRange = 1f..config.baseWidth.toFloat(),
                onValueChange = {
                    onConfigChange(
                        config.copy(minWidth = it.toDouble())
                    )
                }
            )

            DividerLine()

            Text(
                text = "Centerline",
                style = MaterialTheme.typography.titleMedium
            )

            SignalSettings(
                signal = config.centerline,
                frequencyRange = 0.0001f..0.02f,
                onChange = {
                    onConfigChange(config.copy(centerline = it))
                }
            )

            DividerLine()

            Text(
                text = "Width",
                style = MaterialTheme.typography.titleMedium
            )

            SignalSettings(
                signal = config.width,
                frequencyRange = 0.0001f..0.02f,
                onChange = {
                    onConfigChange(config.copy(width = it))
                }
            )

            DividerLine()

            Text(
                text = "Asymmetry",
                style = MaterialTheme.typography.titleMedium
            )

            SignalSettings(
                signal = config.asymmetry,
                frequencyRange = 0.0001f..0.02f,
                onChange = {
                    onConfigChange(config.copy(asymmetry = it))
                }
            )
        }
    }
}

/**
 * Displays all adjustable parameters of one FBM signal.
 */
@Composable
private fun SignalSettings(
    signal: GeneratorConfig.SignalConfig,
    frequencyRange: ClosedFloatingPointRange<Float>,
    onChange: (GeneratorConfig.SignalConfig) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SliderSetting(
            label = "Amplitude",
            value = signal.amplitude.toFloat(),
            valueText = "%.1f".format(signal.amplitude),
            valueRange = 0f..100f,
            onValueChange = {
                onChange(signal.copy(amplitude = it.toDouble()))
            }
        )

        SliderSetting(
            label = "Frequency",
            value = signal.frequency.toFloat(),
            valueText = "%.4f".format(signal.frequency),
            valueRange = frequencyRange,
            onValueChange = {
                onChange(signal.copy(frequency = it.toDouble()))
            }
        )

        SliderSetting(
            label = "Octaves",
            value = signal.octaves.toFloat(),
            valueText = signal.octaves.toString(),
            valueRange = 1f..10f,
            steps = 8,
            onValueChange = {
                onChange(signal.copy(octaves = it.toInt()))
            }
        )

        SliderSetting(
            label = "Persistence",
            value = signal.persistence.toFloat(),
            valueText = "%.2f".format(signal.persistence),
            valueRange = 0f..2f,
            steps = 39,
            onValueChange = {
                onChange(signal.copy(persistence = it.toDouble()))
            }
        )

        SliderSetting(
            label = "Lacunarity",
            value = signal.lacunarity.toFloat(),
            valueText = "%.2f".format(signal.lacunarity),
            valueRange = 0.1f..5f,
            steps = 48,
            onValueChange = {
                onChange(signal.copy(lacunarity = it.toDouble()))
            }
        )
    }
}

/**
 * Displays a labeled slider with its current value.
 */
@Composable
private fun SliderSetting(
    label: String,
    value: Float,
    valueText: String,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    onValueChange: (Float) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = valueText,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Slider(
            value = value.coerceIn(valueRange),
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps
        )
    }
}

/**
 * Displays a subtle divider between settings groups.
 */
@Composable
private fun DividerLine() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(
                MaterialTheme.colorScheme.outlineVariant
            )
    )
}

/**
 * Renders one chunk without adding any spacing or padding.
 *
 * The chunk identifier is drawn as an overlay and does not affect the
 * geometry viewport or the connection between adjacent chunks.
 */
@Composable
private fun ChunkPreview(
    index: Int,
    startDepth: Int,
    endDepth: Int,
    samples: List<GeometrySample>,
    viewportHalfExtent: Double
) {
    val wallColor = MaterialTheme.colorScheme.onSurface
    val fillColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    val backgroundColor = MaterialTheme.colorScheme.surfaceVariant

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height((endDepth - startDepth).dp)
            .background(backgroundColor)
    ) {
        Canvas(
            modifier = Modifier.fillMaxSize()
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

        Surface(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(6.dp),
            color = Color.Black.copy(alpha = 0.55f),
            shape = RoundedCornerShape(4.dp)
        ) {
            Text(
                text = "CHUNK ${index + 1}  ·  $startDepth - $endDepth m",
                modifier = Modifier.padding(
                    horizontal = 7.dp,
                    vertical = 3.dp
                ),
                color = Color.White,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}
