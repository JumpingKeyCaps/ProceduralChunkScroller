# ProceduralChunkScroller

> **Procedural Infinite Chunk Rendering in Jetpack Compose via `LazyColumn` & AGSL Shaders.**

`ProceduralChunkScroller` is an experimental technical demonstration for Android. 

It showcases how to abuse Jetpack Compose's native `LazyColumn` recycling system to render a performant, 100% deterministic, procedurally-generated 1-bit dithered abyss in real-time using mathematical noise functions and low-level AGSL shaders.

## Architecture & Pipeline Layers

Instead of heavy bitmap assets or complex 3D engine overhead, the rendering pipeline relies on mathematical sampling and native `Canvas` path drawing, wrapped in recycled Compose layout nodes and post-processed via AGSL.

```
+-------------------------------------------------------+
|  Layer 4: Fixed UI Overlay & Cockpit Controls         |
+-------------------------------------------------------+
|  Layer 3: Global AGSL Post-Processing Shader          |
|           (4-Tone Palette, Bayer Matrix Dithering,    |
|            Scanlines, Vignetting)                     |
+-------------------------------------------------------+
|  Layer 2: LazyColumn Chunk Container                  |
|           (Procedural vector paths, dynamic event)    |
+-------------------------------------------------------+
|  Layer 1: Parallax Background Layer                   |
+-------------------------------------------------------+
```

---

## Deep Dive: The Procedural Engine

### 1. Deterministic Multi-Octave Noise (fBm)

The environment geometry is derived from pure, state-less mathematical functions. No coordinate lists or terrain layouts are saved in memory or disk.

* **Deterministic Hash:** A pure pseudo-random hashing function without mutable state:
  $$\text{hash}(\text{seed}, \text{index}) \rightarrow [0.0, 1.0]$$
  
* **1D Value Noise:** Smoothly interpolates between integer control points using cosine or cubic Hermite curves.
  
* **Fractional Brownian Motion (fBm):** Combines multiple octaves of noise at varying amplitudes and frequencies:

  $$N(y) = \sum_{i=0}^{n-1} A_i \cdot N_i(f_i \cdot y)$$



### 2. Geological Model & Boundary Computation

The abyss topology is calculated per absolute depth coordinate ($y$) using independent noise streams:

* **Centerline Drift:**

   $C(y) = 0.5 + N_c(y)$ (determines the macro horizontal curvature of the trench).

  
* **Trench Width:**

  $W(y) = W_{\text{base}} + N_w(y)$ (clamped within safe thresholds to prevent path closures).

  
* **Asymmetric Walls:**
  
  $$L(y) = C(y) - \frac{W(y)}{2} + N_l(y)$$
  $$R(y) = C(y) + \frac{W(y)}{2} + N_r(y)$$


### 3. Sampling, Normalization & Viewport Independence

* **Resolution-Independent Core:** The core engine outputs normalized coordinates ($[0.0, 1.0]$).
* **Viewport Adapter:** Converts normalized values into pixel coordinates dynamically based on screen width/height without altering the underlying geometry across different device aspect ratios ($720\times1600$, $1080\times2420$, $1440\times3200$).
* **Sampling Step:** Each block samples absolute depth coordinates at a fixed step (e.g., $5\text{dp}$ interval) to build smooth closed `Path` objects.


### 4. Layout Node Virtualization (`LazyColumn`)

* As the user scrolls, `LazyColumn` recycles node containers (`FossilBlock`).
* Each item fetches its absolute start depth ($y_{\text{start}} = \text{blockIndex} \times \text{blockHeight}$) and queries `AbyssGenerator` on the fly.
* **Memory Footprint:** $\approx 0\text{ MB}$ memory churn for terrain data regardless of trench depth (even down to $-100,000\text{m}$).


### 5. Local Brush & Global AGSL Post-Processing

* **Local Wall Shader:** Fills vector paths with a custom AGSL `ShaderBrush` generating cross-hatching and dot dithering on the GPU.
* **Global Post-Processing:** Appended to the root container via `Modifier.graphicsLayer` to perform 4-tone palette quantization, Bayer matrix dithering, CRT scanlines, and subtle lens curvature.

---

## Execution Roadmap & Validation Checklist

### Phase 1: Core Engine & Deterministic Noise
- [ ] Implement `DeterministicHash(seed, index)`
- [ ] Build 1D interpolated `ValueNoise`
- [ ] Implement Multi-Octave Noise (`fBm`) with 3 octaves
- [ ] Validate seed consistency across independent runs

### Phase 2: Geological Model (`AbyssGenerator`)
- [ ] Calculate centerline $C(y)$, width $W(y)$, and asymmetric offsets $L(y), R(y)$
- [ ] Ensure non-intersecting walls and continuous smooth curves

### Phase 3: Unit Block Canvas (`FossilBlock`)
- [ ] Build single Compose `Canvas` block sampling depth at regular intervals
- [ ] Construct left and right wall `Path`s with transparent central channel

### Phase 4: Viewport Adapter & Normalization
- [ ] Map normalized coordinates to screen pixel width
- [ ] Test geometry equality across varied screen aspect ratios

### Phase 5: Virtualized Scrolling Assembly
- [ ] Integrate blocks into `LazyColumn` with persistent item keys
- [ ] Validate smooth fling, zero seam artifacts, and seamless recycling

### Phase 6: Shaders & Visual Direction
- [ ] Solid monochrome wall fills
- [ ] Local AGSL hatching `ShaderBrush`
- [ ] Global AGSL 4-Tone quantization & Bayer dithering
- [ ] CRT scanline effect & vignetting

---

## Quality & Determinism Checklist

- [ ] Same Seed + Same Depth = Identical Coordinates (100% deterministic)
- [ ] Unique Seeds = Unique Topologies
- [ ] No visible periodicity or repetitive patterns
- [ ] Zero wall intersections / zero unpassable bottlenecks
- [ ] Perfect seamless alignment at segment boundaries
- [ ] Viewport resolution invariant geometry
- [ ] Direct $O(1)$ jump access to any depth depth without precalculating prior chunks
- [ ] 60 FPS / 120 FPS smooth scrolling with zero GC allocations in layout loop

---

## Tech Stack

* **Language:** Kotlin
* **UI Framework:** Jetpack Compose (BOM 2024+)
* **Graphics Language:** AGSL (Android Graphics Shading Language — API 33+)
* **Vector Engine:** Native Android `Canvas`, `Path`, & `Paint`

## License

MIT License. Free to use, adapt, or build upon.
