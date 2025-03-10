/*
 * Copyright (c) 2020. JetBrains s.r.o.
 * Use of this source code is governed by the MIT license that can be found in the LICENSE file.
 */

package jetbrains.datalore.plot.base.geom

import jetbrains.datalore.base.geometry.DoubleRectangle
import jetbrains.datalore.plot.base.*
import jetbrains.datalore.plot.base.geom.legend.GenericLegendKeyElementFactory
import jetbrains.datalore.plot.base.interact.GeomTargetCollector
import jetbrains.datalore.plot.base.render.LegendKeyElementFactory
import jetbrains.datalore.plot.base.render.SvgRoot
import jetbrains.datalore.plot.base.render.svg.LinePath
import jetbrains.datalore.vis.svg.SvgGElement
import jetbrains.datalore.vis.svg.slim.SvgSlimElements
import jetbrains.datalore.vis.svg.slim.SvgSlimGroup
import jetbrains.datalore.vis.svg.slim.SvgSlimObject

abstract class GeomBase : Geom {

    override val legendKeyElementFactory: LegendKeyElementFactory
        get() = GenericLegendKeyElementFactory()

    override fun build(
        root: SvgRoot,
        aesthetics: Aesthetics,
        pos: PositionAdjustment,
        coord: CoordinateSystem,
        ctx: GeomContext
    ) {
        buildIntern(root, aesthetics, pos, coord, ctx)
    }

    protected fun getGeomTargetCollector(ctx: GeomContext): GeomTargetCollector {
        return ctx.targetCollector
    }

    protected abstract fun buildIntern(
        root: SvgRoot,
        aesthetics: Aesthetics,
        pos: PositionAdjustment,
        coord: CoordinateSystem,
        ctx: GeomContext
    )

    companion object {
        fun wrap(slimGroup: SvgSlimGroup): SvgGElement {
            val g = SvgGElement()
            g.isPrebuiltSubtree = true
            g.children().add(slimGroup.asDummySvgNode())
            return g
        }

        fun wrap(o: SvgSlimObject): SvgGElement {
            val slimGroup = SvgSlimElements.g(1)
            o.appendTo(slimGroup)
            return wrap(slimGroup)
        }

        fun overallAesBounds(ctx: GeomContext): DoubleRectangle {
            return when {
                ctx.flipped -> ctx.getAesBounds().flip()
                else -> ctx.getAesBounds()
            }
        }

        fun layerAesBounds(aesthetics: Aesthetics): DoubleRectangle {
            // ToDo: flip?
            return DoubleRectangle(
                aesthetics.range(Aes.X)!!,
                aesthetics.range(Aes.Y)!!
            )
        }

        fun appendNodes(paths: List<LinePath>, root: SvgRoot) {
            for (path in paths) {
                root.add(path.rootGroup)
            }
        }
    }
}
