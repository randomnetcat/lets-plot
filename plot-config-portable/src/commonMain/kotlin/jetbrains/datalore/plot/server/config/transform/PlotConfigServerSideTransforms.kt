/*
 * Copyright (c) 2019. JetBrains s.r.o.
 * Use of this source code is governed by the MIT license that can be found in the LICENSE file.
 */

package jetbrains.datalore.plot.server.config.transform

import jetbrains.datalore.plot.config.Option.Layer
import jetbrains.datalore.plot.config.Option.Plot
import jetbrains.datalore.plot.config.Option.PlotBase
import jetbrains.datalore.plot.config.transform.PlotSpecTransform
import jetbrains.datalore.plot.config.transform.SpecSelector
import jetbrains.datalore.plot.config.transform.migration.MoveGeomPropertiesToLayerMigration
import jetbrains.datalore.plot.server.config.transform.bistro.corr.CorrPlotSpecChange

object PlotConfigServerSideTransforms {
    fun migrationTransform(): PlotSpecTransform {
        // ToDo: remove after all input is updated (demo, test, sci ide)
        return PlotSpecTransform.builderForRawSpec()
            .change(
                MoveGeomPropertiesToLayerMigration.specSelector(false),
                MoveGeomPropertiesToLayerMigration()
            )
            .build()
    }

    fun entryTransform(): PlotSpecTransform {
        return PlotSpecTransform.builderForRawSpec()
            .change(
                SpecSelector.of(PlotBase.DATA),
                NumericDataVectorSpecChange()
            )
            .change(
                SpecSelector.of(Plot.LAYERS, PlotBase.DATA),
                NumericDataVectorSpecChange()
            )
            .change(
                SpecSelector.of(Plot.LAYERS, Layer.GEOM, PlotBase.DATA),
                NumericDataVectorSpecChange()
            )
            .change(
                ReplaceDataVectorsInAesMappingChange.specSelector(),
                ReplaceDataVectorsInAesMappingChange()
            )
            .build()
    }

    fun bistroTransform(): PlotSpecTransform {
        return PlotSpecTransform.builderForRawSpec()
            .change(
                CorrPlotSpecChange.specSelector(),
                CorrPlotSpecChange()
            )
            .build()
    }

}
