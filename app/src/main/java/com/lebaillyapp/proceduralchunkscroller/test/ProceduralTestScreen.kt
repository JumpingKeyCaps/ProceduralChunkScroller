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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lebaillyapp.proceduralchunkscroller.engine.config.GeneratorConfig
import com.lebaillyapp.proceduralchunkscroller.engine.geometry.GeometrySample
import com.lebaillyapp.proceduralchunkscroller.engine.geometry.SpatialGenerator
import kotlin.math.ceil
import kotlin.math.max

private val WallColor = Color(0xFFA9CE72)
private val CenterColor = Color(0xFFD99A52)

/**
 * # Procedural geometry test screen.
 *
 * Provides a lightweight visual laboratory for the procedural geometry engine.
 *
 * Displays three consecutive depth windows generated from the same
 * SpatialGenerator instance and exposes the generator configuration
 * through a sliding settings panel.
 *
 * Geometry generation is performed outside the Canvas drawing phase.
 */
@Composable
fun ProceduralTestScreen(
    modifier: Modifier = Modifier
) {
    var config by remember {
        mutableStateOf(
            GeneratorConfig(
                seed = 123456789L,
                baseWidth = 100.0,
                minWidth = 30.0
            )
        )
    }

    var startDepth by rememberSaveable {
        mutableStateOf(0.0)
    }

    var chunkHeight by rememberSaveable {
        mutableStateOf(250.0)
    }

    var sampleStep by rememberSaveable {
        mutableStateOf(1.0)
    }

    var settingsVisible by rememberSaveable {
        mutableStateOf(false)
    }

    var showCenterline by rememberSaveable {
        mutableStateOf(true)
    }

    var showGrid by rememberSaveable {
        mutableStateOf(true)
    }

    val chunks = remember(config, startDepth, chunkHeight, sampleStep) {
        val generator = SpatialGenerator(config)

        List(3) { index ->
            val chunkStart = startDepth + index * chunkHeight

            generateChunk(
                generator = generator,
                startDepth = chunkStart,
                chunkHeight = chunkHeight,
                step = sampleStep
            )
        }
    }

    val allSamples = remember(chunks) {
        chunks.flatten()
    }

    val widths = remember(allSamples) {
        allSamples.map { it.width }
    }

    val minWidth = widths.minOrNull() ?: 0.0
    val maxWidth = widths.maxOrNull() ?: 0.0
    val averageWidth = widths.average().takeIf { it.isFinite() } ?: 0.0

    val maxDelta = remember(allSamples) {
        allSamples.zipWithNext { first, second ->
            max(
                kotlin.math.abs(second.leftX - first.leftX),
                kotlin.math.abs(second.rightX - first.rightX)
            )
        }.maxOrNull() ?: 0.0
    }

    val deterministic = remember(config, allSamples) {
        val generator = SpatialGenerator(config)

        allSamples.take(50).all { sample ->
            val first = generator.sample(sample.depth)
            val second = generator.sample(sample.depth)

            first == second
        }
    }

    val minimumWidthValid = minWidth >= config.minWidth

    Box(
        modifier = modifier
            .fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Procedural Test Lab",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "SpatialGenerator V1",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = {
                        settingsVisible = true
                    }
                ) {
                    Text("⚙", fontSize = 24.sp)
                }
            }

            Text(
                text = "SEED : ${config.seed}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )

            chunks.forEachIndexed { index, samples ->
                key(config, index) {
                    ChunkCanvasBox(
                        index = index,
                        samples = samples,
                        showCenterline = showCenterline,
                        showGrid = showGrid
                    )
                }
            }

            HorizontalDivider()

            Text(
                text = "GEOMETRY DIAGNOSTICS",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                DiagnosticValue(
                    label = "Samples",
                    value = allSamples.size.toString(),
                    modifier = Modifier.weight(1f)
                )

                DiagnosticValue(
                    label = "Min width",
                    value = "%.1f".format(minWidth),
                    modifier = Modifier.weight(1f)
                )

                DiagnosticValue(
                    label = "Average",
                    value = "%.1f".format(averageWidth),
                    modifier = Modifier.weight(1f)
                )

                DiagnosticValue(
                    label = "Max width",
                    value = "%.1f".format(maxWidth),
                    modifier = Modifier.weight(1f)
                )
            }

            Text(
                text = "Maximum wall variation / sample: %.3f".format(maxDelta),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            HorizontalDivider()

            Text(
                text = "AUTOMATED CHECKS",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            CheckRow(
                label = "Determinism",
                valid = deterministic
            )

            CheckRow(
                label = "Minimum width",
                valid = minimumWidthValid
            )

            CheckRow(
                label = "Finite geometry",
                valid = allSamples.all {
                    it.leftX.isFinite() &&
                            it.rightX.isFinite() &&
                            it.width.isFinite()
                }
            )

            Text(
                text = "Start: $startDepth m | End: ${startDepth + chunkHeight * 3} m | Step: $sampleStep m",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Button(
                onClick = {
                    // Changing the configuration triggers sample regeneration.
                    config = config.copy()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Regenerate")
            }
        }

        if (settingsVisible) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.05f))
                    .clickable {
                        settingsVisible = false
                    }
            )
        }

        AnimatedVisibility(
            visible = settingsVisible,
            enter = slideInHorizontally { it },
            exit = slideOutHorizontally { it },
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.90f)
                    .widthIn(max = 360.dp),
                tonalElevation = 8.dp,
                shadowElevation = 12.dp,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.35f)
            ) {
                SettingsPanel(
                    config = config,
                    startDepth = startDepth,
                    chunkHeight = chunkHeight,
                    sampleStep = sampleStep,
                    showCenterline = showCenterline,
                    showGrid = showGrid,
                    onConfigChange = { config = it },
                    onStartDepthChange = { startDepth = it },
                    onChunkHeightChange = { chunkHeight = it },
                    onSampleStepChange = { sampleStep = it },
                    onShowCenterlineChange = { showCenterline = it },
                    onShowGridChange = { showGrid = it },
                    onClose = {
                        settingsVisible = false
                    }
                )
            }
        }
    }
}

/**
 * # Generates one consecutive depth window.
 *
 * The final sample is clamped to the chunk end so that adjacent
 * chunks share the exact same boundary depth.
 */
private fun generateChunk(
    generator: SpatialGenerator,
    startDepth: Double,
    chunkHeight: Double,
    step: Double
): List<GeometrySample> {

    val endDepth = startDepth + chunkHeight
    val count = ceil(chunkHeight / step)
        .toInt()
        .coerceIn(1, 5000)

    return (0..count).map { index ->
        val depth = minOf(
            startDepth + index * step,
            endDepth
        )

        generator.sample(depth)
    }
}

/**
 * # Displays one generated depth window.
 *
 * Draws both walls and optional reference markers.
 * The horizontal mapping is shared by all three windows
 * through the same configuration.
 */
@Composable
private fun ChunkCanvasBox(
    index: Int,
    samples: List<GeometrySample>,
    showCenterline: Boolean,
    showGrid: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "CHUNK #${index + 1}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = samples.firstOrNull()?.depth?.let {
                        "%.0f m".format(it)
                    } ?: "-",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 4.dp)
            ) {
                if (samples.isEmpty()) return@Canvas

                val maxExtent = samples.maxOf {
                    max(
                        kotlin.math.abs(it.leftX),
                        kotlin.math.abs(it.rightX)
                    )
                }.coerceAtLeast(1.0)

                val scale = (size.width * 0.45f) / maxExtent.toFloat()

                val firstDepth = samples.first().depth
                val lastDepth = samples.last().depth
                val depthRange = (lastDepth - firstDepth).coerceAtLeast(1.0)

                fun mapX(value: Double): Float {
                    return size.width * 0.5f + value.toFloat() * scale
                }

                fun mapY(depth: Double): Float {
                    return ((depth - firstDepth) / depthRange).toFloat() * size.height
                }

                if (showGrid) {
                    for (i in 1..3) {
                        val y = size.height * i / 4f

                        drawLine(
                            color = Color.Gray.copy(alpha = 0.25f),
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    drawLine(
                        color = Color.Gray.copy(alpha = 0.25f),
                        start = Offset(size.width / 2f, 0f),
                        end = Offset(size.width / 2f, size.height),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                val leftPath = Path()
                val rightPath = Path()
                val centerPath = Path()

                samples.forEachIndexed { sampleIndex, sample ->
                    val y = mapY(sample.depth)

                    val left = Offset(mapX(sample.leftX), y)
                    val right = Offset(mapX(sample.rightX), y)
                    val center = Offset(mapX(sample.centerX), y)

                    if (sampleIndex == 0) {
                        leftPath.moveTo(left.x, left.y)
                        rightPath.moveTo(right.x, right.y)
                        centerPath.moveTo(center.x, center.y)
                    } else {
                        leftPath.lineTo(left.x, left.y)
                        rightPath.lineTo(right.x, right.y)
                        centerPath.lineTo(center.x, center.y)
                    }
                }

                drawPath(
                    path = leftPath,
                    color = WallColor,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = 2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                )

                drawPath(
                    path = rightPath,
                    color = WallColor,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = 2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                )

                if (showCenterline) {
                    drawPath(
                        path = centerPath,
                        color = CenterColor,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = 1.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    )
                }
            }
        }
    }
}

/**
 * # Displays the configuration controls.
 *
 * Numeric values are applied immediately when their sliders change.
 * Seed changes are applied explicitly to avoid regenerating geometry
 * while the user is typing.
 */
@Composable
private fun SettingsPanel(
    config: GeneratorConfig,
    startDepth: Double,
    chunkHeight: Double,
    sampleStep: Double,
    showCenterline: Boolean,
    showGrid: Boolean,
    onConfigChange: (GeneratorConfig) -> Unit,
    onStartDepthChange: (Double) -> Unit,
    onChunkHeightChange: (Double) -> Unit,
    onSampleStepChange: (Double) -> Unit,
    onShowCenterlineChange: (Boolean) -> Unit,
    onShowGridChange: (Boolean) -> Unit,
    onClose: () -> Unit
) {
    var seedInput by rememberSaveable(config.seed) {
        mutableStateOf(config.seed.toString())
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "SETTINGS",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            TextButton(onClick = onClose) {
                Text("Close")
            }
        }

        HorizontalDivider()

        Text("GENERATION", fontWeight = FontWeight.Bold)

        OutlinedTextField(
            value = seedInput,
            onValueChange = { seedInput = it },
            label = { Text("Seed (Long)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                seedInput.toLongOrNull()?.let { seed ->
                    onConfigChange(config.copy(seed = seed))
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = seedInput.toLongOrNull() != null
        ) {
            Text("Apply seed")
        }

        DoubleSetting(
            label = "Start depth",
            value = startDepth,
            min = 0.0,
            max = 100_000.0,
            onValueChange = onStartDepthChange,
            useSlider = false
        )

        SliderSetting(
            label = "Chunk height",
            value = chunkHeight.toFloat(),
            range = 50f..1000f,
            steps = 18,
            format = { "%.0f m".format(it) },
            onValueChange = { onChunkHeightChange(it.toDouble()) }
        )

        SliderSetting(
            label = "Sample step",
            value = sampleStep.toFloat(),
            range = 0.25f..10f,
            steps = 38,
            format = { "%.2f m".format(it) },
            onValueChange = { onSampleStepChange(it.toDouble()) }
        )

        HorizontalDivider()

        Text("GEOMETRY", fontWeight = FontWeight.Bold)

        SliderSetting(
            label = "Base width",
            value = config.baseWidth.toFloat(),
            range = 30f..300f,
            steps = 53,
            format = { "%.1f".format(it) },
            onValueChange = {
                val newBaseWidth = it.toDouble()
                val newMinWidth = config.minWidth.coerceAtMost(newBaseWidth)

                onConfigChange(
                    config.copy(
                        baseWidth = newBaseWidth,
                        minWidth = newMinWidth
                    )
                )
            }
        )

        SliderSetting(
            label = "Minimum width",
            value = config.minWidth.toFloat(),
            range = 1f..config.baseWidth.toFloat(),
            steps = 49,
            format = { "%.1f".format(it) },
            onValueChange = {
                onConfigChange(
                    config.copy(minWidth = it.toDouble())
                )
            }
        )

        HorizontalDivider()

        Text("SIGNALS", fontWeight = FontWeight.Bold)

        SignalSettings(
            title = "Centerline",
            signal = config.centerline,
            onChange = {
                onConfigChange(config.copy(centerline = it))
            }
        )

        SignalSettings(
            title = "Width",
            signal = config.width,
            onChange = {
                onConfigChange(config.copy(width = it))
            }
        )

        SignalSettings(
            title = "Asymmetry",
            signal = config.asymmetry,
            onChange = {
                onConfigChange(config.copy(asymmetry = it))
            }
        )

        HorizontalDivider()

        Text("DISPLAY", fontWeight = FontWeight.Bold)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Centerline marker")
            Switch(
                checked = showCenterline,
                onCheckedChange = onShowCenterlineChange
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Grid")
            Switch(
                checked = showGrid,
                onCheckedChange = onShowGridChange
            )
        }

        Spacer(Modifier.height(20.dp))
    }
}

/**
 * # Displays the controls of an individual fBm signal.
 */
@Composable
private fun SignalSettings(
    title: String,
    signal: GeneratorConfig.SignalConfig,
    onChange: (GeneratorConfig.SignalConfig) -> Unit
) {
    var expanded by rememberSaveable(title) {
        mutableStateOf(true)
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold
            )

            Text(if (expanded) "−" else "+")
        }

        if (expanded) {
            SliderSetting(
                label = "Amplitude",
                value = signal.amplitude.toFloat(),
                range = 0f..100f,
                format = { "%.1f".format(it) },
                onValueChange = {
                    onChange(signal.copy(amplitude = it.toDouble()))
                }
            )

            SliderSetting(
                label = "Frequency",
                value = signal.frequency.toFloat(),
                range = 0.001f..0.05f,
                format = { "%.4f".format(it) },
                onValueChange = {
                    onChange(signal.copy(frequency = it.toDouble()))
                }
            )

            SliderSetting(
                label = "Octaves",
                value = signal.octaves.toFloat(),
                range = 1f..10f,
                steps = 8,
                format = { it.toInt().toString() },
                onValueChange = {
                    onChange(signal.copy(octaves = it.toInt()))
                }
            )

            SliderSetting(
                label = "Persistence",
                value = signal.persistence.toFloat(),
                range = 0f..2f,
                steps = 39,
                format = { "%.2f".format(it) },
                onValueChange = {
                    onChange(signal.copy(persistence = it.toDouble()))
                }
            )

            SliderSetting(
                label = "Lacunarity",
                value = signal.lacunarity.toFloat(),
                range = 0.1f..5f,
                steps = 48,
                format = { "%.2f".format(it) },
                onValueChange = {
                    onChange(signal.copy(lacunarity = it.toDouble()))
                }
            )
        }
    }
}

/**
 * # Displays a slider with its current numerical value.
 *
 * @param label Parameter name.
 * @param value Current slider value.
 * @param range Supported value range.
 * @param steps Number of discrete intermediate steps.
 * @param format Converts the current value into display text.
 * @param onValueChange Called whenever the slider value changes.
 */
@Composable
private fun SliderSetting(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    format: (Float) -> String,
    onValueChange: (Float) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = format(value),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Slider(
            value = value.coerceIn(range),
            onValueChange = onValueChange,
            valueRange = range,
            steps = steps
        )
    }
}

/**
 * # Numeric floating-point setting.
 *
 * Used for values requiring direct numerical input rather than
 * continuous slider manipulation.
 */
@Composable
private fun DoubleSetting(
    label: String,
    value: Double,
    min: Double = Double.NEGATIVE_INFINITY,
    max: Double = Double.POSITIVE_INFINITY,
    onValueChange: (Double) -> Unit,
    useSlider: Boolean = false
) {
    var text by remember(value) {
        mutableStateOf(value.toString())
    }

    OutlinedTextField(
        value = text,
        onValueChange = { input ->
            text = input

            input.toDoubleOrNull()
                ?.takeIf {
                    it.isFinite() && it >= min && it <= max
                }
                ?.let(onValueChange)
        },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Decimal
        ),
        modifier = Modifier.fillMaxWidth()
    )
}

/**
 * # Integer setting with bounded values.
 */
@Composable
private fun IntSetting(
    label: String,
    value: Int,
    min: Int,
    max: Int,
    onValueChange: (Int) -> Unit
) {
    var text by remember(value) {
        mutableStateOf(value.toString())
    }

    OutlinedTextField(
        value = text,
        onValueChange = { input ->
            text = input

            input.toIntOrNull()
                ?.takeIf { it in min..max }
                ?.let(onValueChange)
        },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number
        ),
        modifier = Modifier.fillMaxWidth()
    )
}

/**
 * # Displays a numerical diagnostic value.
 */
@Composable
private fun DiagnosticValue(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * # Displays the result of an automated geometry check.
 */
@Composable
private fun CheckRow(
    label: String,
    valid: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label)

        Text(
            text = if (valid) "PASS" else "FAIL",
            color = if (valid) {
                Color(0xFF58B878)
            } else {
                MaterialTheme.colorScheme.error
            },
            fontWeight = FontWeight.Bold
        )
    }
}
