
package com.lebaillyapp.proceduralchunkscroller.engine.geometry

/**
 * # Represents a generated geometry sample at an absolute depth.
 *
 * Stores the horizontal coordinates of the left and right walls
 * at a specific position along the generated passage.
 *
 * The sample is independent of rendering dimensions and does not
 * depend on chunk boundaries or sampling order.
 *
 * The center position and passage width are derived directly
 * from the wall coordinates to avoid storing redundant values.
 *
 * All coordinates use the same abstract spatial unit.
 */
data class GeometrySample(

    /**
     * Absolute depth coordinate of this geometry sample.
     */
    val depth: Double,

    /**
     * Horizontal coordinate of the left wall.
     */
    val leftX: Double,

    /**
     * Horizontal coordinate of the right wall.
     */
    val rightX: Double

) {

    init {
        require(depth.isFinite()) {
            "Depth must be finite."
        }

        require(leftX.isFinite()) {
            "Left wall coordinate must be finite."
        }

        require(rightX.isFinite()) {
            "Right wall coordinate must be finite."
        }

        require(leftX < rightX) {
            "Left wall coordinate must be smaller than right wall coordinate."
        }
    }

    /**
     * ### Returns the horizontal position of the passage center.
     *
     * Calculated as the midpoint between both wall coordinates.
     */
    val centerX: Double
        get() = leftX + (rightX - leftX) * 0.5

    /**
     * ### Returns the horizontal distance between both walls.
     */
    val width: Double
        get() = rightX - leftX
}
