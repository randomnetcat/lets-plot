/*
 * Copyright (c) 2021. JetBrains s.r.o.
 * Use of this source code is governed by the MIT license that can be found in the LICENSE file.
 */

package jetbrains.datalore.plot.builder

import jetbrains.datalore.base.geometry.DoubleRectangle
import jetbrains.datalore.plot.base.interact.GeomTargetCollector
import jetbrains.datalore.plot.base.render.svg.SvgComponent

internal class BogusFrameOfReference : TileFrameOfReference {

    override fun drawFoR(parent: SvgComponent) {
        throw IllegalStateException("Bogus frame of reference is not supposed to be used.")
    }

    override fun buildGeomComponent(layer: GeomLayer, targetCollector: GeomTargetCollector): SvgComponent {
        throw IllegalStateException("Bogus frame of reference is not supposed to be used.")
    }

    override fun applyClientLimits(clientBounds: DoubleRectangle): DoubleRectangle {
        throw IllegalStateException("Bogus frame of reference is not supposed to be used.")
    }
}