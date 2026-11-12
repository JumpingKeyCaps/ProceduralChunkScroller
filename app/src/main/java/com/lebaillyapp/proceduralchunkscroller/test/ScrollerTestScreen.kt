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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.lebaillyapp.proceduralchunkscroller.engine.config.GeneratorConfig
import com.lebaillyapp.proceduralchunkscroller.engine.geometry.GeometrySample
import com.lebaillyapp.proceduralchunkscroller.engine.geometry.SpatialGenerator
import kotlin.math.ceil
import kotlin.math.max
import kotlin.random.Random

private const val DEFAULT_CHUNK_HEIGHT = 250
private const val DEFAULT_SAMPLE_STEP = 1.0

private const val MIN_DEPTH = 250
private const val MAX_DEPTH = 20_000
private const val DEPTH_STEP = 250

/**
 * Creates a calibrated default configuration featuring progressive depth biomes
 * (Surface -> Twilight Zone -> Abyssal Trenches).
 */
private fun createDefaultConfig(seed: Long): GeneratorConfig {
    return GeneratorConfig(
        seed = seed,
        baseWidth = 140.0,
        minWidth = 20.0,
        biomes = listOf(
            // Biome 1: Surface (0m - 1500m) -> Wide, smooth, gentle waves
            GeneratorConfig.BiomeLayer(
                startDepth = 0.0,
                endDepth = 1500.0,
                baseWidth = 160.0,
                minWidth = 35.0,
                leftWall = GeneratorConfig.SignalConfig(
                    amplitude = 25.0,
                    frequency = 0.002,
                    octaves = 2,
                    persistence = 0.4,
                    lacunarity = 1.5
                ),
                rightWall = GeneratorConfig.SignalConfig(
                    amplitude = 25.0,
                    frequency = 0.002,
                    octaves = 2,
                    persistence = 0.4,
                    lacunarity = 1.5
                )
            ),
            // Biome 2: Twilight Zone (1500m - 5000m) -> Narrowing down, increasing roughness
            GeneratorConfig.BiomeLayer(
                startDepth = 1500.0,
                endDepth = 5000.0,
                baseWidth = 100.0,
                minWidth = 20.0,
                leftWall = GeneratorConfig.SignalConfig(
                    amplitude = 45.0,
                    frequency = 0.005,
                    octaves = 3,
                    persistence = 0.5,
                    lacunarity = 1.8
                ),
                rightWall = GeneratorConfig.SignalConfig(
                    amplitude = 45.0,
                    frequency = 0.005,
                    octaves = 3,
                    persistence = 0.5,
                    lacunarity = 1.8
                )
            ),
            // Biome 3: Abyssal Trenches (5000m - 20000m) -> Tight corridor, high frequency jagged walls
            GeneratorConfig.BiomeLayer(
                startDepth = 5000.0,
                endDepth = 20000.0,
                baseWidth = 70.0,
                minWidth = 14.0,
                leftWall = GeneratorConfig.SignalConfig(
                    amplitude = 65.0,
                    frequency = 0.012,
                    octaves = 4,
                    persistence = 0.65,
                    lacunarity = 2.1
                ),
                rightWall = GeneratorConfig.SignalConfig(
                    amplitude = 65.0,
                    frequency = 0.012,
                    octaves = 4,
                    persistence = 0.65,
                    lacunarity = 2.1
                )
            )
        )
    )
}

/**
 * Displays the procedural shaft over an adjustable absolute depth range.
 */
@Composable
fun ScrollerTestScreen(
    modifier: Modifier = Modifier
) {
    // Initialisation indépendante de la seed et de la config pour préserver les réglages lors du changement de seed
    var seed by remember {
        mutableLongStateOf(Random.nextLong())
    }

    var config by remember {
        mutableStateOf(createDefaultConfig(seed))
    }

    var maxDepth by remember {
        mutableIntStateOf(20_000)
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
                        text = "Procedural Scroller (Biomes)",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text(
                        text = "$maxDepth m · $chunkCount chunks · Seed ${config.seed}",
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
                seed = config.seed,
                config = config,
                maxDepth = maxDepth,
                chunkHeight = chunkHeight,
                sampleStep = sampleStep,
                onGenerateSeed = {
                    val newSeed = Random.nextLong()
                    seed = newSeed
                    // On met à jour uniquement la seed dans la config existante pour conserver tous les réglages actuels
                    config = config.copy(seed = newSeed)
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
 * Calculates a shared horizontal viewport extent based on maximum biome widths and amplitudes.
 */
private fun calculateViewportHalfExtent(
    config: GeneratorConfig
): Double {
    val maxBiomeWidth = config.biomes.maxOfOrNull { it.baseWidth } ?: config.baseWidth
    val maxAmplitude = config.biomes.maxOfOrNull { max(it.leftWall.amplitude, it.rightWall.amplitude) } ?: 50.0
    return ((maxBiomeWidth * 0.5) + maxAmplitude).coerceAtLeast(1.0)
}

/**
 * Displays the lateral settings panel with full live parameter controls for each biome.
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
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.42f),
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
                    val snapped = (it / DEPTH_STEP).toInt() * DEPTH_STEP
                    onMaxDepthChange(snapped.coerceIn(MIN_DEPTH, MAX_DEPTH))
                }
            )

            SliderSetting(
                label = "Chunk height",
                value = chunkHeight.toFloat(),
                valueText = "$chunkHeight m",
                valueRange = 50f..1_000f,
                steps = 18,
                onValueChange = {
                    onChunkHeightChange((it / 50f).toInt() * 50)
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
                text = "Biome Layers Configuration",
                style = MaterialTheme.typography.titleMedium
            )

            config.biomes.forEachIndexed { index, biome ->
                val biomeName = when (index) {
                    0 -> "Biome 1 · Surface (0 - 1500m)"
                    1 -> "Biome 2 · Twilight (1500 - 5000m)"
                    else -> "Biome 3 · Abyssal (5000m+)"
                }

                Text(
                    text = biomeName,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )

                SliderSetting(
                    label = "Base Width",
                    value = biome.baseWidth.toFloat(),
                    valueText = "%.1f".format(biome.baseWidth),
                    valueRange = 30f..1000f,
                    onValueChange = { newWidth ->
                        val w = newWidth.toDouble()
                        val updatedBiomes = config.biomes.toMutableList()
                        updatedBiomes[index] = biome.copy(
                            baseWidth = w,
                            minWidth = biome.minWidth.coerceAtMost(w)
                        )
                        onConfigChange(config.copy(biomes = updatedBiomes))
                    }
                )

                SliderSetting(
                    label = "Min Width",
                    value = biome.minWidth.toFloat(),
                    valueText = "%.1f".format(biome.minWidth),
                    valueRange = 1f..biome.baseWidth.toFloat(),
                    onValueChange = { newMin ->
                        val updatedBiomes = config.biomes.toMutableList()
                        updatedBiomes[index] = biome.copy(minWidth = newMin.toDouble())
                        onConfigChange(config.copy(biomes = updatedBiomes))
                    }
                )

                SignalSettings(
                    signal = biome.leftWall,
                    frequencyRange = 0.0001f..0.03f,
                    onChange = { updatedSignal ->
                        val updatedBiomes = config.biomes.toMutableList()
                        updatedBiomes[index] = biome.copy(
                            leftWall = updatedSignal,
                            rightWall = updatedSignal
                        )
                        onConfigChange(config.copy(biomes = updatedBiomes))
                    }
                )

                DividerLine()
            }
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
            valueRange = 0f..200f,
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
            valueRange = 1f..8f,
            steps = 6,
            onValueChange = {
                onChange(signal.copy(octaves = it.toInt()))
            }
        )

        SliderSetting(
            label = "Persistence",
            value = signal.persistence.toFloat(),
            valueText = "%.2f".format(signal.persistence),
            valueRange = 0f..1.5f,
            steps = 29,
            onValueChange = {
                onChange(signal.copy(persistence = it.toDouble()))
            }
        )

        SliderSetting(
            label = "Lacunarity",
            value = signal.lacunarity.toFloat(),
            valueText = "%.2f".format(signal.lacunarity),
            valueRange = 0.5f..4f,
            steps = 34,
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
 * Renders one chunk combining organic dithering, spaced diagonal hatching, and topological sonar echoes on edges.
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

            // 1. Fond de roche sombre global
            drawRect(color = Color.Black.copy(alpha = 0.4f))

            // 2. STYLE A : Tramage organique (staggered dots) dans la roche
            val dotColor = wallColor.copy(alpha = 0.14f)
            val dotSpacingY = 14f
            val dotSpacingX = 16f
            var scanY = startDepth.toFloat()
            var rowIndex = 0

            while (scanY < endDepth) {
                val sampleAtY = samples.minByOrNull { kotlin.math.abs(it.depth - scanY.toDouble()) }
                if (sampleAtY != null) {
                    val xLeft = mapX(sampleAtY.leftX)
                    val xRight = mapX(sampleAtY.rightX)
                    val screenY = mapY(scanY.toDouble())

                    // Décalage d'une ligne sur deux pour casser l'effet "pointillés alignés"
                    val rowOffset = if (rowIndex % 2 == 0) 0f else dotSpacingX * 0.5f

                    // Roche gauche
                    var scanX = 10f + rowOffset
                    while (scanX < xLeft) {
                        drawCircle(color = dotColor, radius = 1.1f, center = Offset(scanX, screenY))
                        scanX += dotSpacingX
                    }

                    // Roche droite
                    scanX = xRight + 10f + rowOffset
                    while (scanX < size.width) {
                        drawCircle(color = dotColor, radius = 1.1f, center = Offset(scanX, screenY))
                        scanX += dotSpacingX
                    }
                }
                scanY += dotSpacingY
                rowIndex++
            }

            // 3. STYLE B : Hachurages diagonaux espacés dans la roche (micro-traits obliques)
            val hatchColor = wallColor.copy(alpha = 0.16f)
            val gridStepY = 30f // Espacement vertical plus large (moins de traits)
            val gridStepX = 35f
            var hy = startDepth.toFloat()
            var hRow = 0

            while (hy < endDepth) {
                val sample = samples.minByOrNull { kotlin.math.abs(it.depth - hy.toDouble()) }
                if (sample != null) {
                    val xLeft = mapX(sample.leftX)
                    val xRight = mapX(sample.rightX)
                    val screenY = mapY(hy.toDouble())
                    val xShift = if (hRow % 2 == 0) 0f else gridStepX * 0.5f

                    // Hachures roche gauche
                    var hx = 10f + xShift
                    while (hx < xLeft) {
                        drawLine(
                            color = hatchColor,
                            start = Offset(hx - 5f, screenY + 5f),
                            end = Offset(hx + 5f, screenY - 5f),
                            strokeWidth = 1f
                        )
                        hx += gridStepX
                    }

                    // Hachures roche droite
                    hx = xRight + 10f + xShift
                    while (hx < size.width) {
                        drawLine(
                            color = hatchColor,
                            start = Offset(hx - 5f, screenY + 5f),
                            end = Offset(hx + 5f, screenY - 5f),
                            strokeWidth = 1f
                        )
                        hx += gridStepX
                    }
                }
                hy += gridStepY
                hRow++
            }

            // 4. Nettoyage du centre (la fosse est rendue vide/sombre)
            val trenchPath = Path().apply {
                val leftPts = samples.map { Offset(mapX(it.leftX), mapY(it.depth)) }
                val rightPts = samples.map { Offset(mapX(it.rightX), mapY(it.depth)) }
                if (leftPts.isNotEmpty()) {
                    moveTo(leftPts.first().x, leftPts.first().y)
                    leftPts.forEach { lineTo(it.x, it.y) }
                    rightPts.asReversed().forEach { lineTo(it.x, it.y) }
                    close()
                }
            }
            drawPath(path = trenchPath, color = backgroundColor)

            // 5. STYLE C : Effet Topographique / Sonar (Échos multiples sur les arêtes)
            for (i in 3 downTo 1) {
                val offset = i * 3.5f
                val echoAlpha = 0.07f * i
                val echoColor = wallColor.copy(alpha = echoAlpha)

                val echoLeft = Path()
                val echoRight = Path()

                samples.forEachIndexed { idx, s ->
                    val lx = mapX(s.leftX) - offset
                    val rx = mapX(s.rightX) + offset
                    val y = mapY(s.depth)
                    if (idx == 0) {
                        echoLeft.moveTo(lx, y)
                        echoRight.moveTo(rx, y)
                    } else {
                        echoLeft.lineTo(lx, y)
                        echoRight.lineTo(rx, y)
                    }
                }

                drawPath(path = echoLeft, color = echoColor, style = Stroke(width = 1.dp.toPx()))
                drawPath(path = echoRight, color = echoColor, style = Stroke(width = 1.dp.toPx()))
            }

            // 6. Tracé principal net des arêtes (Edges)
            val mainLeftPath = Path()
            val mainRightPath = Path()
            samples.forEachIndexed { idx, s ->
                val lx = mapX(s.leftX)
                val rx = mapX(s.rightX)
                val y = mapY(s.depth)
                if (idx == 0) {
                    mainLeftPath.moveTo(lx, y)
                    mainRightPath.moveTo(rx, y)
                } else {
                    mainLeftPath.lineTo(lx, y)
                    mainRightPath.lineTo(rx, y)
                }
            }

            drawPath(path = mainLeftPath, color = wallColor, style = Stroke(width = 2.dp.toPx()))
            drawPath(path = mainRightPath, color = wallColor, style = Stroke(width = 2.dp.toPx()))
        }

        Surface(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(6.dp),
            color = Color.Black.copy(alpha = 0.55f),
            shape = RoundedCornerShape(4.dp)
        ) {
            Text(
                text = "CHUNK ${index + 1} · $startDepth-$endDepth m",
                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                color = Color.White,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}