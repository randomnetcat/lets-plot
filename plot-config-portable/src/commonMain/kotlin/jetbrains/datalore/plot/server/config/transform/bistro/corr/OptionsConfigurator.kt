/*
 * Copyright (c) 2021. JetBrains s.r.o.
 * Use of this source code is governed by the MIT license that can be found in the LICENSE file.
 */

package jetbrains.letsPlot.bistro.corr

import jetbrains.letsPlot.bistro.corr.CorrPlot.LayerParams

internal object OptionsConfigurator {
    /**
     * @return true if at least one layer fills diagonal
     */
    fun configure(
        tiles: LayerParams,
        points: LayerParams,
        labels: LayerParams,
        flipY: Boolean
    ) {
        adjustTypeColorSize(tiles, points, labels)
        flipType(tiles, points, labels, flipY)
        adjustDiag(tiles, points, labels)
    }

    private fun adjustTypeColorSize(
        tiles: LayerParams,
        points: LayerParams,
        labels: LayerParams
    ) {
        val hasTiles = tiles.added
        val hasPoints = points.added
        val hasLabels = labels.added

        var tilesType = tiles.type
        var pointsType = points.type
        var labelsType = labels.type

        if (hasTiles && hasPoints) {
            // avoid showing tiles and points in the same cells
            if (tilesType == null && pointsType == null) {
                tilesType = "lower"
                pointsType = "upper"
            } else if (tilesType == null) {
                if (pointsType == "lower") {
                    tilesType = "upper"
                } else if (pointsType in listOf("upper", "full")) {
                    tilesType = "lower"
                }
            } else if (pointsType == null) {
                pointsType = flip(tilesType)
            }
        }

        if (hasLabels && labelsType == null && labels.color == null) {
            // avoid labels without 'color' showing on top of tiles or points.
            @Suppress("DuplicatedCode")
            if (hasPoints) {
                if (pointsType == null) {
                    labelsType = "lower"
                    pointsType = "upper"
                } else {
                    labelsType = flip(pointsType)
                }
            }
            @Suppress("DuplicatedCode")
            if (hasTiles) {
                if (tilesType == null && labelsType == null) {
                    tilesType = "lower"
                    labelsType = "upper"
                } else if (tilesType == null) {
                    tilesType = flip(labelsType)
                } else {
                    labelsType = flip(tilesType)
                }
            }
        }

        // Set labels color if labels are over points or tiles.
        if (hasLabels && labels.color == null) {
            if (hasTiles &&
                overlap(labelsType ?: "full", tilesType ?: "full")
            ) {
                labels.color = "white"
            }
            if (hasPoints &&
                overlap(labelsType ?: "full", pointsType ?: "full")
            ) {
                labels.color = "white"
            }
        }

        // Map labels size if labels are over points.
        if (hasPoints && hasLabels && labels.mapSize == null) {
            if (overlap(labelsType ?: "full", pointsType ?: "full")
            ) {
                labels.mapSize = true
            }
        }

        // Update all layers parameters.
        if (hasTiles) {
            tiles.type = tilesType ?: "full"
        }
        if (hasPoints) {
            points.type = pointsType ?: "full"
        }
        if (hasLabels) {
            labels.type = labelsType ?: "full"
        }
    }

    /**
     * Set all 'diag' values (if were null)
     */
    private fun adjustDiag(
        tiles: LayerParams,
        points: LayerParams,
        labels: LayerParams
    ): Boolean {
        fun adjust(params: LayerParams): Boolean {
            return if (params.added) {
                params.diag = params.diag ?: (params.type == "full")
                params.diag as Boolean
            } else {
                false
            }
        }

        val tilesDiag = adjust(tiles)
        val pointsDiag = adjust(points)
        val labelsDiag = adjust(labels)
        return tilesDiag ||
                pointsDiag ||
                labelsDiag
    }

    private fun flipType(
        tiles: LayerParams,
        points: LayerParams,
        labels: LayerParams,
        flip: Boolean
    ) {
        if (flip) {
            if (tiles.added) tiles.type = flip(tiles.type)
            if (points.added) points.type = flip(points.type)
            if (labels.added) labels.type = flip(labels.type)
        }
    }

    private fun flip(type: String?): String? {
        if (type == "upper") return "lower"
        else if (type == "lower") return "upper"
        return type
    }

    private fun overlap(type0: String?, type1: String?): Boolean {
        if (type0 == null || type1 == null) return false
        if (type0 == "full" || type1 == "full") return true
        return type0 == type1
    }

    fun getCombinedMatrixType(
        tiles: LayerParams,
        points: LayerParams,
        labels: LayerParams,
    ): String {
        fun combined(type: String?, otherType: String): String {
            return if (type == null || otherType == "full") {
                otherType
            } else if (type == "full") {
                type
            } else if (overlap(type, otherType)) {
                type
            } else {
                "full"
            }
        }


        var type: String? = if (tiles.added) tiles.type!! else null
        if (points.added) {
            type = combined(type, points.type!!)
        }
        if (labels.added) {
            type = combined(type, labels.type!!)
        }
        return type!!
    }

    fun getKeepMatrixDiag(
        tiles: LayerParams,
        points: LayerParams,
        labels: LayerParams
    ): Boolean {
        val tilesDiag = if (tiles.added) tiles.diag!! else false
        val pointsDiag = if (points.added) points.diag!! else false
        val labelsDiag = if (labels.added) labels.diag!! else false
        return tilesDiag || pointsDiag || labelsDiag
    }
}
