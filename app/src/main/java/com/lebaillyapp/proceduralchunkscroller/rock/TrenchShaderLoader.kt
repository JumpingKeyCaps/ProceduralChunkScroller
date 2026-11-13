package com.lebaillyapp.proceduralchunkscroller.rock

import android.graphics.RuntimeShader
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.platform.LocalContext
import com.lebaillyapp.proceduralchunkscroller.R

@Composable
fun rememberTrenchRockBrush(
    size: Size,
    wallColor: Color,
    voidColor: Color,
    edgePoints: FloatArray,
    hatchDensity: Float,
    displacementStrength: Float,
    scaleMultiplier: Float,
    angularity: Float,
    strataStretch: Float,
    tectonicWarp: Float
): ShaderBrush? {
    if (size.width <= 0f || size.height <= 0f) return null

    val context = LocalContext.current
    val shaderSrc = remember(context) {
        context.resources.openRawResource(R.raw.trench_rock)
            .bufferedReader()
            .use { it.readText() }
    }

    val shader = remember(shaderSrc) { RuntimeShader(shaderSrc) }

    return remember(
        size,
        wallColor,
        voidColor,
        edgePoints,
        hatchDensity,
        displacementStrength,
        scaleMultiplier,
        angularity,
        strataStretch,
        tectonicWarp,
        shader
    ) {
        shader.setFloatUniform("iResolution", size.width, size.height)
        shader.setFloatUniform("iWallColor", wallColor.red, wallColor.green, wallColor.blue, wallColor.alpha)
        shader.setFloatUniform("iVoidColor", voidColor.red, voidColor.green, voidColor.blue, voidColor.alpha)
        shader.setFloatUniform("iEdgePoints", edgePoints)
        shader.setFloatUniform("iHatchDensity", hatchDensity)
        shader.setFloatUniform("iDisplacementStrength", displacementStrength)
        shader.setFloatUniform("iScaleMultiplier", scaleMultiplier)
        shader.setFloatUniform("iAngularity", angularity)
        shader.setFloatUniform("iStrataStretch", strataStretch)
        shader.setFloatUniform("iTectonicWarp", tectonicWarp)
        ShaderBrush(shader)
    }
}