/*
 * Copyright (c) 2021. JetBrains s.r.o.
 * Use of this source code is governed by the MIT license that can be found in the LICENSE file.
 */

package jetbrains.datalore.plot.server.config.transform.bistro.corr

object Option {
    object Corr {
        const val NAME = "corr"
        const val TITLE = "title"
        const val SHOW_LEGEND = "show_legend"
        const val FLIP = "flip"
        const val THRESHOLD = "threshold"
        const val ADJUST_SIZE = "adjust_size"
        const val PALETTE = "palette"
        const val GRADIENT_LOW = "low"
        const val GRADIENT_MID = "mid"
        const val GRADIENT_HIGH = "high"

        const val POINT_LAYER = "point_params"
        const val TILE_LAYER = "tile_params"
        const val LABEL_LAYER = "label_params"

        object Layer {
            const val TYPE = "type"
            const val DIAG = "diag"
            const val COLOR = "color"
            const val MAP_SIZE = "map_size"
        }
    }
}
