/*
 * Copyright (c) 2019. JetBrains s.r.o.
 * Use of this source code is governed by the MIT license that can be found in the LICENSE file.
 */

package jetbrains.datalore.plotDemo.plotConfig

import jetbrains.datalore.plotDemo.model.plotConfig.ScaleSize
import jetbrains.datalore.vis.demoUtils.PlotSpecsDemoWindowJfx

fun main() {
    with(ScaleSize()) {
        PlotSpecsDemoWindowJfx(
            "scale_size ans scale_size_area",
            plotSpecList()
        ).open()
    }
}

