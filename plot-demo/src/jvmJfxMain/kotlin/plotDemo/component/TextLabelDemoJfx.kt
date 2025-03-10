/*
 * Copyright (c) 2019. JetBrains s.r.o.
 * Use of this source code is governed by the MIT license that can be found in the LICENSE file.
 */

package jetbrains.datalore.plotDemo.component

import jetbrains.datalore.plotDemo.model.component.TextLabelDemo
import jetbrains.datalore.vis.demoUtils.SvgViewerDemoWindowJfx

fun main() {
    with(TextLabelDemo()) {
        SvgViewerDemoWindowJfx(
            "Text label anchor and rotation",
            createSvgRoots(listOf(createModel())),
            stylesheets = listOf("/text-label-demo.css")
        ).open()
    }
}

