/*
 * Copyright (c) 2020. JetBrains s.r.o.
 * Use of this source code is governed by the MIT license that can be found in the LICENSE file.
 */

package jetbrains.datalore.plot

object FeatureSwitch {
    const val USE_THEME2 = false

    const val PLOT_VIEW_TOOLBOX = false

    const val PLOT_DEBUG_DRAWING = false
    const val LEGEND_DEBUG_DRAWING = false
    private const val PRINT_DEBUG_LOGS = false

    fun isDebugLogEnabled(): Boolean {
        return PRINT_DEBUG_LOGS
    }
}