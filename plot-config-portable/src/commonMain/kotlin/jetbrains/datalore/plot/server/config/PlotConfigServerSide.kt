/*
 * Copyright (c) 2019. JetBrains s.r.o.
 * Use of this source code is governed by the MIT license that can be found in the LICENSE file.
 */

package jetbrains.datalore.plot.server.config

import jetbrains.datalore.base.logging.PortableLogging
import jetbrains.datalore.plot.base.DataFrame
import jetbrains.datalore.plot.base.DataFrame.Variable
import jetbrains.datalore.plot.base.data.DataFrameUtil
import jetbrains.datalore.plot.base.stat.Stats
import jetbrains.datalore.plot.builder.assemble.PlotFacets
import jetbrains.datalore.plot.builder.data.DataProcessing
import jetbrains.datalore.plot.builder.data.GroupingContext
import jetbrains.datalore.plot.builder.data.OrderOptionUtil.OrderOption
import jetbrains.datalore.plot.builder.tooltip.DataFrameValue
import jetbrains.datalore.plot.config.*
import jetbrains.datalore.plot.config.Option.Meta.DATA_META
import jetbrains.datalore.plot.config.Option.Meta.GeoDataFrame.GDF
import jetbrains.datalore.plot.config.Option.Meta.GeoDataFrame.GEOMETRY
import jetbrains.datalore.plot.server.config.transform.PlotConfigServerSideTransforms.bistroTransform
import jetbrains.datalore.plot.server.config.transform.PlotConfigServerSideTransforms.entryTransform
import jetbrains.datalore.plot.server.config.transform.PlotConfigServerSideTransforms.migrationTransform

open class PlotConfigServerSide(opts: Map<String, Any>) : PlotConfig(opts) {

    override fun createLayerConfig(
        layerOptions: Map<String, Any>,
        sharedData: DataFrame,
        plotMappings: Map<*, *>,
        plotDiscreteAes: Set<*>,
        plotOrderOptions: List<OrderOption>
    ): LayerConfig {

        val geomName = layerOptions[Option.Layer.GEOM] as String
        val geomKind = Option.GeomName.toGeomKind(geomName)
        return LayerConfig(
            layerOptions,
            sharedData,
            plotMappings,
            plotDiscreteAes,
            plotOrderOptions,
            GeomProto(geomKind),
            false
        )
    }

    /**
     * WARN! Side effects - performs modifications deep in specs tree
     */
    private fun updatePlotSpec() {
        val layerIndexWhereSamplingOccurred = HashSet<Int>()
        val dataByTileByLayerAfterStat = dataByTileByLayerAfterStat { layerIndex, message ->
            layerIndexWhereSamplingOccurred.add(layerIndex)
            PlotConfigUtil.addComputationMessage(this, message)
        }

        // merge tiles
        val dataByLayerAfterStat = ArrayList<DataFrame>()
        val layerConfigs = layerConfigs
        for (layerIndex in layerConfigs.indices) {

            val layerSerieByVarName = HashMap<String, Pair<Variable, ArrayList<Any?>>>()
            // merge tiles
            for (tileDataByLayerAfterStat in dataByTileByLayerAfterStat) {
                val tileLayerDataAfterStat = tileDataByLayerAfterStat[layerIndex]
                val variables = tileLayerDataAfterStat.variables()
                if (layerSerieByVarName.isEmpty()) {
                    for (variable in variables) {
                        layerSerieByVarName[variable.name] = Pair(variable, ArrayList(tileLayerDataAfterStat[variable]))
                    }
                } else {
                    for (variable in variables) {
                        layerSerieByVarName[variable.name]!!.second.addAll(tileLayerDataAfterStat[variable])
                    }
                }
            }

            val builder = DataFrame.Builder()
            for (varName in layerSerieByVarName.keys) {
                val variable = layerSerieByVarName[varName]!!.first
                val serie = layerSerieByVarName[varName]!!.second
                builder.put(variable, serie)
            }
            val layerDataAfterStat = builder.build()
            dataByLayerAfterStat.add(layerDataAfterStat)
        }

        run {
            // replace layer data with data after stat
            for ((layerIndex, layerConfig) in layerConfigs.withIndex()) {
                // optimization: only replace layer' data if 'combined' data was changed (because of stat or sampling occurred)
                if (layerConfig.stat !== Stats.IDENTITY || layerIndexWhereSamplingOccurred.contains(layerIndex)) {
                    val layerStatData = dataByLayerAfterStat[layerIndex]
                    layerConfig.replaceOwnData(layerStatData)
                }
            }
        }

        dropUnusedDataBeforeEncoding(layerConfigs)
    }

    private fun dropUnusedDataBeforeEncoding(layerConfigs: List<LayerConfig>) {
        // Clean-up shared data (aka plot data)
        val variablesToKeepByLayerConfig: Map<LayerConfig, Set<String>> =
            layerConfigs.associateWith { variablesToKeep(facets, it) }

        val plotData = sharedData
        val plotVars = DataFrameUtil.variables(plotData)
        val plotVarsToKeep = HashSet<String>()
        for (plotVar in plotVars.keys) {
            var canDropPlotVar = true
            for ((layerConfig, layerVarsToKeep) in variablesToKeepByLayerConfig) {
                val layerData = layerConfig.ownData!!
                if (DataFrameUtil.variables(layerData).containsKey(plotVar)) {
                    // This variable not needed for this layer
                    // because there is same variable in the plot's data.
                    continue
                }
                if (layerVarsToKeep.contains(plotVar)) {
                    // Have to keep this variable.
                    canDropPlotVar = false
                    break
                }
            }

            if (!canDropPlotVar) {
                plotVarsToKeep.add(plotVar)
            }
        }

        if (plotVarsToKeep.size < plotVars.size) {
            val plotDataCleaned = DataFrameUtil.removeAllExcept(plotData, plotVarsToKeep)
            replaceSharedData(plotDataCleaned)
        }

        // Clean-up data in layers.
        for ((layerConfig, layerVarsToKeep) in variablesToKeepByLayerConfig) {
            val layerData = layerConfig.ownData!!
            val layerDataCleaned = DataFrameUtil.removeAllExcept(layerData, layerVarsToKeep)
            layerConfig.replaceOwnData(layerDataCleaned)
        }
    }


    private fun dataByTileByLayerAfterStat(layerIndexAndSamplingMessage: (Int, String) -> Unit): List<List<DataFrame>> {

        // transform layers data before stat
        val dataByLayer = ArrayList<DataFrame>()
        for (layerConfig in layerConfigs) {
            var layerData = layerConfig.combinedData
            layerData = DataProcessing.transformOriginals(layerData, layerConfig.varBindings, scaleMap)
            dataByLayer.add(layerData)
        }

        // slice data to tiles
        val facets = facets
        val inputDataByTileByLayer = PlotConfigUtil.toLayersDataByTile(dataByLayer, facets)

        // apply stat to each layer in each tile separately
        val result = ArrayList<MutableList<DataFrame>>()
        while (result.size < inputDataByTileByLayer.size) {
            result.add(ArrayList())
        }

        for ((layerIndex, layerConfig) in layerConfigs.withIndex()) {

            val statCtx = ConfiguredStatContext(dataByLayer, scaleMap)
            for (tileIndex in inputDataByTileByLayer.indices) {
                val tileLayerInputData = inputDataByTileByLayer[tileIndex][layerIndex]
                val varBindings = layerConfig.varBindings
                val groupingContext = GroupingContext(
                    myData = tileLayerInputData,
                    bindings = varBindings,
                    groupingVarName = layerConfig.explicitGroupingVarName,
                    pathIdVarName = null, // only on client side
                    myExpectMultiple = true
                )

                val groupingContextAfterStat: GroupingContext
                val stat = layerConfig.stat
                var tileLayerDataAfterStat: DataFrame
                if (stat === Stats.IDENTITY) {
                    // Do not apply stat
                    tileLayerDataAfterStat = tileLayerInputData
                    groupingContextAfterStat = groupingContext
                } else {
                    // Need to keep variables without bindings (used in tooltips and for ordering)
                    val varsWithoutBinding = layerConfig.run {
                        tooltips.valueSources
                            .filterIsInstance<DataFrameValue>()
                            .map(DataFrameValue::getVariableName) +
                                orderOptions.mapNotNull(OrderOption::byVariable)
                    }

                    val tileLayerDataAndGroupingContextAfterStat = DataProcessing.buildStatData(
                        tileLayerInputData,
                        stat,
                        varBindings,
                        scaleMap,
                        groupingContext,
                        facets,
                        statCtx,
                        varsWithoutBinding,
                        layerConfig.orderOptions,
                        layerConfig.aggregateOperation
                    ) { message ->
                        layerIndexAndSamplingMessage(
                            layerIndex,
                            createStatMessage(message, layerConfig)
                        )
                    }

                    tileLayerDataAfterStat = tileLayerDataAndGroupingContextAfterStat.data
                    groupingContextAfterStat = tileLayerDataAndGroupingContextAfterStat.groupingContext
                }

                // Apply sampling to layer tile data if necessary
                tileLayerDataAfterStat =
                    PlotSampling.apply(
                        tileLayerDataAfterStat, // layerConfig,
                        layerConfig.samplings!!,
                        groupingContextAfterStat.groupMapper
                    ) { message ->
                        layerIndexAndSamplingMessage(
                            layerIndex,
                            createSamplingMessage(message, layerConfig)
                        )
                    }
                result[tileIndex].add(tileLayerDataAfterStat)
            }

        }

        return result
    }

    private fun getStatName(layerConfig: LayerConfig): String {
        var stat: String = layerConfig.stat::class.simpleName!!
        stat = stat.replace("Stat", " stat")
        stat = stat.replace("([a-z])([A-Z]+)".toRegex(), "$1_$2").lowercase()

        return stat
    }

    private fun createSamplingMessage(samplingExpression: String, layerConfig: LayerConfig): String {
        val geomKind = layerConfig.geomProto.geomKind.name.lowercase()
        val stat = getStatName(layerConfig)

        return "$samplingExpression was applied to [$geomKind/$stat] layer"
    }

    private fun createStatMessage(statInfo: String, layerConfig: LayerConfig): String {
        val geomKind = layerConfig.geomProto.geomKind.name.lowercase()
        val stat = getStatName(layerConfig)

        return "$statInfo in [$geomKind/$stat] layer"
    }

    companion object {
        private val LOG = PortableLogging.logger(PlotConfigServerSide::class)

        private fun variablesToKeep(facets: PlotFacets, layerConfig: LayerConfig): Set<String> {
            val stat = layerConfig.stat
            // keep all original vars
            // keep default-mapped stat vars only if not overwritten by actual mapping
            val defStatMapping = Stats.defaultMapping(stat)
            val bindings = layerConfig.varBindings
            val varsToKeep = HashSet(defStatMapping.values)  // initially add all def stat mapping
            for (binding in bindings) {
                val aes = binding.aes
                if (stat.hasDefaultMapping(aes)) {
                    varsToKeep.remove(stat.getDefaultMapping(aes))
                }
                varsToKeep.add(binding.variable)
            }

            // drop var if aes is not rendered by geom
            val renderedAes = HashSet(layerConfig.geomProto.renders())
            val renderedVars = HashSet<Variable>()
            val notRenderedVars = HashSet<Variable>()
            for (binding in bindings) {
                val aes = binding.aes
                if (renderedAes.contains(aes)) {
                    renderedVars.add(binding.variable)
                } else {
                    notRenderedVars.add(binding.variable)
                }
            }
            varsToKeep.removeAll(notRenderedVars)
            varsToKeep.addAll(renderedVars)

            return HashSet<String>() +
                    varsToKeep.map(Variable::name) +
                    Stats.GROUP.name +
                    listOfNotNull(layerConfig.mergedOptions.getString(DATA_META, GDF, GEOMETRY)) +
                    (layerConfig.getMapJoin()?.first?.map { it as String } ?: emptyList()) +
                    facets.variables +
                    listOfNotNull(layerConfig.explicitGroupingVarName) +
                    layerConfig.tooltips.valueSources
                        .filterIsInstance<DataFrameValue>()
                        .map(DataFrameValue::getVariableName) +
                    layerConfig.orderOptions.mapNotNull(OrderOption::byVariable)
        }

        fun processTransform(plotSpecRaw: MutableMap<String, Any>): MutableMap<String, Any> {
            return try {
                if (isGGBunchSpec(plotSpecRaw)) {
                    processTransformInBunch(plotSpecRaw)
                } else {
                    processTransformIntern(plotSpecRaw)
                }
            } catch (e: RuntimeException) {
                val failureInfo = FailureHandler.failureInfo(e)
                if (failureInfo.isInternalError) {
                    LOG.error(e) { failureInfo.message }
                }
                HashMap(failure(failureInfo.message))
            }
        }

        private fun processTransformInBunch(bunchSpecRaw: MutableMap<String, Any>): MutableMap<String, Any> {
            if (!bunchSpecRaw.containsKey(Option.GGBunch.ITEMS)) {
                bunchSpecRaw[Option.GGBunch.ITEMS] = emptyList<Any>()
                return bunchSpecRaw
            }

            // List of items
            val itemsRaw: Any = bunchSpecRaw.get(Option.GGBunch.ITEMS)!!
            if (itemsRaw !is List<*>) {
                throw IllegalArgumentException("GGBunch: list of features expected but was: ${itemsRaw::class.simpleName}")
            }

            val items = ArrayList<MutableMap<String, Any>>()
            for (rawItem in itemsRaw) {
                if (rawItem !is Map<*, *>) {
                    throw IllegalArgumentException("GGBunch item: Map of attributes expected but was: ${rawItem!!::class.simpleName}")
                }

                @Suppress("UNCHECKED_CAST")
                val item = HashMap<String, Any>(rawItem as Map<String, Any>)
                // Item feature spec (Map)
                if (!item.containsKey(Option.GGBunch.Item.FEATURE_SPEC)) {
                    throw IllegalArgumentException("GGBunch item: absent required attribute: ${Option.GGBunch.Item.FEATURE_SPEC}")
                }

                val featureSpecRaw = item[Option.GGBunch.Item.FEATURE_SPEC]!!
                if (featureSpecRaw !is Map<*, *>) {
                    throw IllegalArgumentException("GGBunch item '${Option.GGBunch.Item.FEATURE_SPEC}' : Map of attributes expected but was: ${featureSpecRaw::class.simpleName}")
                }

                // Plot spec
                @Suppress("UNCHECKED_CAST")
                val featureSpec = HashMap<String, Any>(featureSpecRaw as Map<String, Any>)
                val kind = featureSpec[Option.Meta.KIND]
                if (Option.Meta.Kind.PLOT != kind) {
                    throw IllegalArgumentException("GGBunch item feature kind not suppotred: $kind")
                }

                val plotSpec = processTransformIntern(featureSpec)
                item[Option.GGBunch.Item.FEATURE_SPEC] = plotSpec
                items.add(item)
            }

            bunchSpecRaw[Option.GGBunch.ITEMS] = items
            return bunchSpecRaw
        }

        private fun processTransformIntern(plotSpecRaw: MutableMap<String, Any>): MutableMap<String, Any> {
            // testing of error handling
//            throwTestingException(plotSpecRaw)

            var plotSpec = migrationTransform().apply(plotSpecRaw)
            plotSpec = bistroTransform().apply(plotSpec)
            plotSpec = entryTransform().apply(plotSpec)
            PlotConfigServerSide(plotSpec).updatePlotSpec()
            return plotSpec
        }

        @Suppress("unused")
        private fun throwTestingException(plotSpec: Map<String, Any>) {
            if (plotSpec.containsKey(Option.Plot.TITLE)) {
                @Suppress("UNCHECKED_CAST")
                val title = (plotSpec[Option.Plot.TITLE] as Map<String, Any>)[Option.Plot.TITLE_TEXT]!!
                if ("Throw testing exception" == title) {
//                    throw RuntimeException()
//                    throw RuntimeException("My sudden crush")
                    throw IllegalArgumentException("User configuration error")
//                    throw IllegalStateException("User configuration error")
//                    throw IllegalStateException()   // Huh?
                }
            }
        }
    }
}
