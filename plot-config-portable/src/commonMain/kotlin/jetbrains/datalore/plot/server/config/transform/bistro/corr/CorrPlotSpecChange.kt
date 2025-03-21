/*
 * Copyright (c) 2021. JetBrains s.r.o.
 * Use of this source code is governed by the MIT license that can be found in the LICENSE file.
 */

package jetbrains.datalore.plot.server.config.transform.bistro.corr

import jetbrains.datalore.plot.config.*
import jetbrains.datalore.plot.config.Option
import jetbrains.datalore.plot.config.transform.SpecChange
import jetbrains.datalore.plot.config.transform.SpecChangeContext
import jetbrains.datalore.plot.config.transform.SpecSelector
import jetbrains.datalore.plot.server.config.transform.bistro.PlotSpecUtil
import jetbrains.datalore.plot.server.config.transform.bistro.corr.Option.Corr
import jetbrains.letsPlot.bistro.corr.CorrPlot

class CorrPlotSpecChange : SpecChange {
    override fun apply(spec: MutableMap<String, Any>, ctx: SpecChangeContext) {
        val corrPlotSpec = buildCorrPlotSpec(spec)

        // Replace layers/scales
        spec[Option.Plot.LAYERS] = corrPlotSpec.get(Option.Plot.LAYERS) ?: error("Missing layers in corr plot")
        spec[Option.Plot.SCALES] = corrPlotSpec.get(Option.Plot.SCALES) ?: error("Missing scales in corr plot")

        // size, coord - take either user defined value from plotSpec or computed value from corrSpec
        if (Option.Plot.COORD !in spec) {
            corrPlotSpec.get(Option.Plot.COORD)?.let { spec[Option.Plot.COORD] = it }
        }

        if (Option.Plot.SIZE !in spec) {
            corrPlotSpec.get(Option.Plot.SIZE)?.let { spec[Option.Plot.SIZE] = it }
        }

        // Merge theme
        val corrTheme = corrPlotSpec.getMap(Option.Plot.THEME) ?: emptyMap()
        val plotTheme = spec.getMap(Option.Plot.THEME) ?: emptyMap()
        spec[Option.Plot.THEME] = (corrTheme + plotTheme).toMutableMap()

        // Clean-up
        spec.remove(Option.Plot.BISTRO)
    }

    private fun buildCorrPlotSpec(plotSpec: MutableMap<String, Any>): Map<String, Any> {
        val bistroSpec = plotSpec.getMap(Option.Plot.BISTRO) ?: error("'bistro' not found in PlotSpec")

        val corrPlot = CorrPlot(
            data = plotSpec.getMap(Option.PlotBase.DATA) ?: emptyMap<Any, Any>(),
            title = bistroSpec.getString(Corr.TITLE),
            showLegend = bistroSpec.getBool(Corr.SHOW_LEGEND),
            flip = bistroSpec.getBool(Corr.FLIP) ?: true,
            threshold = bistroSpec.getDouble(Corr.THRESHOLD),
            adjustSize = bistroSpec.getDouble(Corr.ADJUST_SIZE)
        )

        bistroSpec.getMap(Corr.TILE_LAYER)?.let {
            corrPlot.tiles(
                type = it.getString(Corr.Layer.TYPE),
                diag = it.getBool(Corr.Layer.DIAG)
            )
        }

        bistroSpec.getMap(Corr.POINT_LAYER)?.let {
            corrPlot.points(
                type = it.getString(Corr.Layer.TYPE),
                diag = it.getBool(Corr.Layer.DIAG)
            )
        }

        bistroSpec.getMap(Corr.LABEL_LAYER)?.let {
            corrPlot.labels(
                type = it.getString(Corr.Layer.TYPE),
                diag = it.getBool(Corr.Layer.DIAG),
                mapSize = it.getBool(Corr.Layer.MAP_SIZE),
                color = it.getString(Corr.Layer.COLOR)
            )
        }

        when (val name = bistroSpec.getString(Corr.PALETTE)) {
            "gradient" -> corrPlot.gradientPalette(
                low = bistroSpec.getString(Corr.GRADIENT_LOW) ?: error("Gradient LOW is not set"),
                mid = bistroSpec.getString(Corr.GRADIENT_MID) ?: error("Gradient MID is not set"),
                high = bistroSpec.getString(Corr.GRADIENT_HIGH) ?: error("Gradient HIGH is not set")
            )
            "BrBG" -> corrPlot.paletteBrBG()
            "PiYG" -> corrPlot.palettePiYG()
            "PRGn" -> corrPlot.palettePRGn()
            "PuOr" -> corrPlot.palettePuOr()
            "RdBu" -> corrPlot.paletteRdBu()
            "RdGy" -> corrPlot.paletteRdGy()
            "RdYlBu" -> corrPlot.paletteRdYlBu()
            "RdYlGn" -> corrPlot.paletteRdYlGn()
            "Spectral" -> corrPlot.paletteSpectral()
            null -> Unit
            else -> throw IllegalArgumentException("Unknown scale: $name")
        }

        val corrPlotOptions = corrPlot.build()
        return PlotSpecUtil.toPlotSpec(corrPlotOptions)
            //.also { println(jetbrains.datalore.base.json.JsonSupport.formatJson(it)) }
    }

    override fun isApplicable(spec: Map<String, Any>): Boolean {
        return spec.getString(Option.Plot.BISTRO, Option.Meta.NAME) == Corr.NAME
    }

    companion object {
        fun specSelector(): SpecSelector {
            return SpecSelector.root()
        }
    }
}
