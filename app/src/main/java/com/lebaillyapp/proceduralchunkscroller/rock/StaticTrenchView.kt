package com.lebaillyapp.proceduralchunkscroller.rock

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
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

    // --- Les leviers morphologiques & gravure ---
    var hatchDensity by remember { mutableStateOf(260f) }          // Finesse des hachures
    var displacementStrength by remember { mutableStateOf(0.08f) } // Saillie du bord
    var scaleMultiplier by remember { mutableStateOf(1.0f) }      // Échelle globale
    var angularity by remember { mutableStateOf(0.0f) }           // 0 = Organique/Rond -> 1 = Anguleux/Cristallin
    var strataStretch by remember { mutableStateOf(1.0f) }        // Étirement des strates (Plaque plate vs colonne)
    var tectonicWarp by remember { mutableStateOf(0.0f) }         // Torsion / Plissement des blocs

    var showSettings by remember { mutableStateOf(true) }

    val normalizedEdgePoints = remember {
        floatArrayOf(
            0.30f, 0.00f,
            0.35f, 0.20f,
            0.38f, 0.45f,
            0.28f, 0.70f,
            0.32f, 1.00f
        )
    }

    val rockBrush = rememberTrenchRockBrush(
        size = canvasSize,
        wallColor = wallEdgeColor,
        voidColor = voidBackgroundColor,
        edgePoints = normalizedEdgePoints,
        hatchDensity = hatchDensity,
        displacementStrength = displacementStrength,
        scaleMultiplier = scaleMultiplier,
        angularity = angularity,
        strataStretch = strataStretch,
        tectonicWarp = tectonicWarp
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

        IconButton(
            onClick = { showSettings = !showSettings },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .background(Color(0x99000000), shape = CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Paramètres Shader",
                tint = Color.White
            )
        }

        AnimatedVisibility(
            visible = showSettings,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xEE111318))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text("1. Angularité (Rond vs Taillé): ${String.format("%.2f", angularity)}", color = Color.White, style = MaterialTheme.typography.bodySmall)
                Slider(value = angularity, onValueChange = { angularity = it }, valueRange = 0.0f..1.0f)

                Text("2. Strates (Vertical vs Écrasé): ${String.format("%.2f", strataStretch)}", color = Color.White, style = MaterialTheme.typography.bodySmall)
                Slider(value = strataStretch, onValueChange = { strataStretch = it }, valueRange = 0.3f..3.0f)

                Text("3. Torsion tectonique (Warp): ${String.format("%.2f", tectonicWarp)}", color = Color.White, style = MaterialTheme.typography.bodySmall)
                Slider(value = tectonicWarp, onValueChange = { tectonicWarp = it }, valueRange = 0.0f..1.5f)

                Text("4. Échelle des blocs: ${String.format("%.2f", scaleMultiplier)}", color = Color.White, style = MaterialTheme.typography.bodySmall)
                Slider(value = scaleMultiplier, onValueChange = { scaleMultiplier = it }, valueRange = 0.3f..2.5f)

                Text("5. Amplitude bord: ${String.format("%.3f", displacementStrength)}", color = Color.White, style = MaterialTheme.typography.bodySmall)
                Slider(value = displacementStrength, onValueChange = { displacementStrength = it }, valueRange = 0.0f..0.25f)

                Text("6. Densité hachures: ${hatchDensity.toInt()}", color = Color.White, style = MaterialTheme.typography.bodySmall)
                Slider(value = hatchDensity, onValueChange = { hatchDensity = it }, valueRange = 80f..500f)
            }
        }
    }
}