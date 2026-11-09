# ProceduralChunkScroller

> **Procedural Infinite Chunk Rendering in Jetpack Compose via `LazyColumn` & AGSL Shaders.**

`ProceduralChunkScroller` is an experimental technical demonstration for Android. 

It showcases how to hijack Jetpack Compose's native `LazyColumn` recycling system to build a performant, infinite procedural viewport driven by deterministic seed calculations and low-level AGSL (Android Graphics Shading Language) post-processing.

---

## Visual Concept & Architecture

The project renders an abyssal, trench-like environment in a strict **1-bit / 4-tone dithered visual style** (inspired by retro terminals and classic dithered prints). Instead of instantiating heavy bitmap textures or traditional 3D scene graphs, the pipeline uses math and raw canvas operations wrapped inside recycled Compose layout nodes.

```
+-------------------------------------------------------+
|  Layer 4: Fixed UI Overlay & Cockpit Controls         |
+-------------------------------------------------------+
|  Layer 3: Global AGSL Post-Processing                 |
|           (4-Tone Palette, Bayer Matrix Dithering,    |
|            Scanlines, Curvature)                      |
+-------------------------------------------------------+
|  Layer 2: LazyColumn Chunk Container                  |
|           (Procedural vector paths, dynamic events,   |
|            death checkpoints)                         |
+-------------------------------------------------------+
|  Layer 1: Parallax Background Layer                   |
+-------------------------------------------------------+
```

---

## Key Technical Concepts

### 1. Deterministic Seed-Based Engine
*   **Zero Storage Overhead:** The environment geometry is never saved as coordinate lists or raster image files.
*   **Pure Mathematical Functions:** Wall topologies are generated dynamically on demand using a deterministic 1D Noise / Harmonic function:
    $$\text{WallPosition}(Y) = f(Y \cdot \text{frequency}, \text{Seed})$$
*   **Seamless Stitching:** A given chunk index $N$ always computes its boundary coordinates matching precisely with chunk $N-1$ and chunk $N+1$, ensuring 0-latency lazy rendering without visual seams.

### 2. Single Chunk Canvas Rendering
*   Each item inside the `LazyColumn` is a standalone layout node (`ProceduralChunk`).
*   Using Jetpack Compose's native `Canvas`, the block traces vector paths representing left/right wall boundaries based on depth parameters.
*   **Local Hatching Brush:** Rather than filling walls with flat solid colors, walls are drawn using an AGSL-powered `ShaderBrush` that applies procedural cross-hatching and dot-dithering directly on the GPU.

### 3. Scalable `LazyColumn` Chunk Virtualization
*   By leveraging `LazyColumn`, the engine only calculates and renders chunks currently visible on screen (plus a minimal prefetch window).
*   As the viewport scrolls vertically, layout nodes are continuously recycled by the Compose runtime, maintaining a constant memory footprint ($\approx 0\text{ MB}$ memory churn for terrain data).

### 4. AGSL Post-Processing Pipeline
*   The entire viewport container is wrapped with an AGSL `RuntimeShader` using `Modifier.graphicsLayer`.
*   **4-Tone Quantization:** Color outputs are constrained to a predefined 4-color palette (Black, Dark Gray, Light Gray, Off-White) with selective accent channels.
*   **Bayer Dithering:** Converts smooth gradients into sharp pixel-grid dither patterns in real time.
*   **CRT / Scanline Aesthetics:** Adds subtle phosphor horizontal lines and corner vignetting for a terminal display aesthetic.

---

## Engine Pipeline Roadmap

- [x] **Phase 1: Architecture & Specifications**
- [ ] **Phase 2: Deterministic Seed Generator** (`NoiseEngine.kt`)
- [ ] **Phase 3: Unit Chunk Canvas & AGSL Local Brush** (`ChunkBlock.kt`)
- [ ] **Phase 4: Infinite `LazyColumn` Recycling** (`AbyssalViewport.kt`)
- [ ] **Phase 5: Global AGSL Post-Processing Layer** (`DitherShader.agsl`)
- [ ] **Phase 6: Dynamic Data & Event Marker Injection**

---

## Tech Stack

*   **Language:** Kotlin
*   **UI Framework:** Jetpack Compose (BOM 2024+)
*   **Shading Language:** AGSL (Android Graphics Shading Language — Android 13 / API 33+)
*   **Graphics Engine:** Native Android `Canvas` & `Path` APIs

---

## License

MIT License. Feel free to use, experiment with, or adapt this architecture for your own procedural Compose experiments.
