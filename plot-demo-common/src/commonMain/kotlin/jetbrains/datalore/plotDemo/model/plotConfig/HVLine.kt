/*
 * Copyright (c) 2020. JetBrains s.r.o.
 * Use of this source code is governed by the MIT license that can be found in the LICENSE file.
 */

package jetbrains.datalore.plotDemo.model.plotConfig

import jetbrains.datalore.plot.parsePlotSpec
import jetbrains.datalore.plotDemo.data.Iris

class HVLine {
    fun plotSpecList(): List<MutableMap<String, Any>> {
        return listOf(
            hline(),
            vline(),
            hlineAlone(),
            vlineAlone(),
            vhLinesByConst()
        )
    }

    private fun hline(): MutableMap<String, Any> {
        val spec = "{" +
                "   'kind': 'plot'," +
                "   'ggtitle': {'text': 'HLine'}," +
                "   'layers': [" +
                "               { " +
                "                   'geom': 'point', " +
                "                   'mapping': {" +
                "                                 'x': 'sepal length (cm)'," +
                "                                 'y': 'sepal width (cm)'," +
                "                                 'color': 'target'" +
                "                              }" +
                "               }," +
                "               { " +
                "                   'geom': { " +
                "                              'name' : 'hline'," +
                "                              'data': { 'hl': [3.0] }" +
                "                           }," +
                "                   'mapping': {'yintercept': 'hl'}, " +
                "                   'color': 'red'" +
                "               }" +
                "           ]" +
                "}"

        val plotSpec = HashMap(parsePlotSpec(spec))
        plotSpec["data"] = Iris.df
        return plotSpec
    }

    private fun vline(): MutableMap<String, Any> {
        val spec = "{" +
                "   'kind': 'plot'," +
                "   'ggtitle': {'text': 'VLine'}," +
                "   'layers': [" +
                "               { " +
                "                   'geom': 'point', " +
                "                   'mapping': {" +
                "                                 'x': 'sepal length (cm)'," +
                "                                 'y': 'sepal width (cm)'," +
                "                                 'color': 'target'" +
                "                              }" +
                "               }," +
                "               { " +
                "                   'geom': { " +
                "                              'name' : 'vline'," +
                "                              'data': { 'vl': [5.0, 7.0] }" +
                "                           }," +
                "                   'mapping': {'xintercept': 'vl'}, " +
                "                   'color': 'red'" +
                "               }" +
                "           ]" +
                "}"

        val plotSpec = HashMap(parsePlotSpec(spec))
        plotSpec["data"] = Iris.df
        return plotSpec
    }

    private fun hlineAlone(): MutableMap<String, Any> {
        val spec = "{" +
                "   'kind': 'plot'," +
                "   'ggtitle': {'text': 'HLine alone'}," +
                "   'layers': [" +
                "               { " +
                "                   'geom': { " +
                "                              'name' : 'hline'," +
                "                              'data': { 'hl': [5.0, 7.0] }" +
                "                           }," +
                "                   'mapping': {'yintercept': 'hl'}, " +
                "                   'color': 'red'" +
                "               }" +
                "           ]" +
                "}"

        return HashMap(parsePlotSpec(spec))
    }

    private fun vlineAlone(): MutableMap<String, Any> {
        val spec = "{" +
                "   'kind': 'plot'," +
                "   'ggtitle': {'text': 'VLine alone'}," +
                "   'layers': [" +
                "               { " +
                "                   'geom': { " +
                "                              'name' : 'vline'," +
                "                              'data': { 'vl': [5.0, 7.0] }" +
                "                           }," +
                "                   'mapping': {'xintercept': 'vl'}, " +
                "                   'color': 'red'" +
                "               }" +
                "           ]" +
                "}"

        return HashMap(parsePlotSpec(spec))
    }

    private fun vhLinesByConst(): MutableMap<String, Any> {
        val spec = """
            {
                'kind': 'plot',
                'ggtitle': {'text': 'VLine, HLine by const'},
                'layers': [
                            {
                                'geom': 'vline',
                                'xintercept': 5,
                                'color': 'red'
                            },
                            {
                                'geom': 'hline',
                                'yintercept': 5,
                                'color': 'blue'
                            }
                          ]
            }
        """.trimIndent()

        return HashMap(parsePlotSpec(spec))
    }
}