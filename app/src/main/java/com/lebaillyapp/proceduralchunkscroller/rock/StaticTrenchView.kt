package com.lebaillyapp.proceduralchunkscroller.rock

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize

@Composable
fun StaticTrenchView(
    modifier: Modifier = Modifier
) {
    var canvasSize by remember { mutableStateOf(Size.Zero) }
    val wallEdgeColor = Color(0xFFD8D5C8)
    val voidBackgroundColor = Color(0xFF000000)

    // États des Seekbars pour tuner les paramètres du shader en direct
    var hatchDensity by remember { mutableStateOf(260f) }
    var displacementStrength by remember { mutableStateOf(0.08f) }
    var scaleMultiplier by remember { mutableStateOf(1.0f) }

    // Points de guidage normalisés de la faille
    val normalizedEdgePoints = remember {
        floatArrayOf(
            0.40f, 0.00f,
            0.45f, 0.20f,
            0.48f, 0.45f,
            0.38f, 0.70f,
            0.42f, 1.00f
        )
    }

    val rockBrush = rememberTrenchRockBrush(
        size = canvasSize,
        wallColor = wallEdgeColor,
        voidColor = voidBackgroundColor,
        edgePoints = normalizedEdgePoints,
        hatchDensity = hatchDensity,
        displacementStrength = displacementStrength,
        scaleMultiplier = scaleMultiplier
    )

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .background(voidBackgroundColor)
                .onSizeChanged { canvasSize = it.toSize() }
        ) {
            if (size.width <= 0f || size.height <= 0f) return@Canvas

            if (rockBrush != null) {
                drawRect(brush = rockBrush)
            }
        }

        // Panneau de contrôle des Seekbars en superposition en bas
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color(0x88000000))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text("Densité hachures: ${hatchDensity.toInt()}", color = Color.White)
            Slider(
                value = hatchDensity,
                onValueChange = { hatchDensity = it },
                valueRange = 50f..600f
            )

            Text("Amplitude relief: ${String.format("%.3f", displacementStrength)}", color = Color.White)
            Slider(
                value = displacementStrength,
                onValueChange = { displacementStrength = it },
                valueRange = 0.0f..0.25f
            )

            Text("Échelle blocs: ${String.format("%.2f", scaleMultiplier)}", color = Color.White)
            Slider(
                value = scaleMultiplier,
                onValueChange = { scaleMultiplier = it },
                valueRange = 0.2f..3.0f
            )
        }
    }
}