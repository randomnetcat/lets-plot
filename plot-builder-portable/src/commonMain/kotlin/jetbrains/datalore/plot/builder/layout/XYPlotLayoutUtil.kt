/*
 * Copyright (c) 2020. JetBrains s.r.o.
 * Use of this source code is governed by the MIT license that can be found in the LICENSE file.
 */

package jetbrains.datalore.plot.builder.layout

import jetbrains.datalore.base.geometry.DoubleRectangle
import jetbrains.datalore.base.geometry.DoubleVector
import jetbrains.datalore.plot.builder.guide.Orientation

internal object XYPlotLayoutUtil {
    const val GEOM_MARGIN = 0.0          // min space around geom area
    private const val CLIP_EXTEND = 5.0
    val GEOM_MIN_SIZE = DoubleVector(50.0, 50.0)

    fun geomBounds(xAxisThickness: Double, yAxisThickness: Double, plotSize: DoubleVector): DoubleRectangle {
        val marginLeftTop = DoubleVector(yAxisThickness, GEOM_MARGIN)
        val marginRightBottom = DoubleVector(GEOM_MARGIN, xAxisThickness)
        var geomSize = plotSize
            .subtract(marginLeftTop)
            .subtract(marginRightBottom)

        if (geomSize.x < GEOM_MIN_SIZE.x) {
            geomSize = DoubleVector(GEOM_MIN_SIZE.x, geomSize.y)
        }
        if (geomSize.y < GEOM_MIN_SIZE.y) {
            geomSize = DoubleVector(geomSize.x, GEOM_MIN_SIZE.y)
        }
        return DoubleRectangle(marginLeftTop, geomSize)
    }

    fun clipBounds(geomBounds: DoubleRectangle): DoubleRectangle {
        return DoubleRectangle(
            geomBounds.origin.subtract(
                DoubleVector(
                    CLIP_EXTEND,
                    CLIP_EXTEND
                )
            ),
            DoubleVector(
                geomBounds.dimension.x + 2 * CLIP_EXTEND,
                geomBounds.dimension.y + 2 * CLIP_EXTEND
            )
        )
    }

    fun maxTickLabelsBounds(
        axisOrientation: Orientation,
        stretch: Double,
        geomBounds: DoubleRectangle,
        plotSize: DoubleVector
    ): DoubleRectangle {
        val geomPaddung = 10.0          // min space around geom area (labels should not touch geom area).

        val maxGeomBounds = DoubleRectangle(
            geomPaddung, geomPaddung,
            plotSize.x - 2 * geomPaddung,
            plotSize.y - 2 * geomPaddung
        )
        when (axisOrientation) {
            Orientation.TOP,
            Orientation.BOTTOM -> {
                val leftSpace = geomBounds.left - maxGeomBounds.left + stretch
                val rightSpace = maxGeomBounds.right - geomBounds.right + stretch

                val height = 1E42   // just very large number
                val top = when (axisOrientation) {
                    Orientation.TOP -> -height
                    else -> 0.0
                }

                val left = -leftSpace
                val width = leftSpace + rightSpace + geomBounds.width
                return DoubleRectangle(left, top, width, height)
            }

            else -> throw IllegalArgumentException("Orientation not supported: $axisOrientation")
        }
    }
}
