/*
 * Copyright (c) 2021. JetBrains s.r.o.
 * Use of this source code is governed by the MIT license that can be found in the LICENSE file.
 */

package jetbrains.datalore.plot.config

import jetbrains.datalore.plot.base.Aes
import jetbrains.datalore.plot.base.interact.TooltipLineSpec.DataPoint
import jetbrains.datalore.plot.builder.GeomLayer
import jetbrains.datalore.plot.builder.interact.TooltipSpec.Line
import jetbrains.datalore.plot.config.Option.Layer.GEOM
import jetbrains.datalore.plot.config.Option.Layer.TOOLTIPS
import jetbrains.datalore.plot.config.Option.Layer.TOOLTIP_FORMATS
import jetbrains.datalore.plot.config.Option.Layer.TOOLTIP_LINES
import jetbrains.datalore.plot.config.Option.Layer.TOOLTIP_VARIABLES
import jetbrains.datalore.plot.config.Option.Plot.LAYERS
import jetbrains.datalore.plot.config.Option.PlotBase.DATA
import jetbrains.datalore.plot.config.Option.PlotBase.MAPPING
import jetbrains.datalore.plot.config.Option.TooltipFormat.FIELD
import jetbrains.datalore.plot.config.Option.TooltipFormat.FORMAT
import jetbrains.datalore.plot.config.TestUtil.buildGeomLayer
import jetbrains.datalore.plot.config.TestUtil.buildPointLayer
import jetbrains.datalore.plot.server.config.ServerSideTestUtil
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue


class TooltipConfigTest {

    private val data = mapOf(
        "displ" to listOf(1.6),
        "hwy" to listOf(160.0),
        "cty" to listOf(15.0),
        "year" to listOf(1998),
        "class" to listOf("suv"),
        "origin" to listOf("US"),
        "model name" to listOf("dodge")
    )
    private val mapping = mapOf(
        Aes.X.name to "displ",
        Aes.Y.name to "hwy",
        Aes.COLOR.name to "cty",
        Aes.SHAPE.name to "class"
    )

    @Test
    fun defaultTooltips() {
        val geomLayer = buildPointLayer(data, mapping, tooltips = null)

        val expectedLines = listOf(
            "cty: 15.00"
        )
        val lines = getGeneralTooltipStrings(geomLayer)
        assertTooltipStrings(expectedLines, lines)

        val axisTooltips = getAxisTooltips(geomLayer)
        assertEquals(2, axisTooltips.size, "Wrong number of axis tooltips")
    }

    @Test
    fun hideTooltips() {
        val geomLayer = buildPointLayer(data, mapping, tooltips = "none")

        val lines = getGeneralTooltipStrings(geomLayer)
        assertTooltipStrings(emptyList(), lines)

        val axisTooltips = getAxisTooltips(geomLayer)
        assertEquals(0, axisTooltips.size, "Wrong number of axis tooltips")
    }

    @Test
    fun useNamesToConfigTooltipLines() {
        val myData = data + mapOf(
            "shape" to listOf("shape"),
            "foo^" to listOf("foo"),
            "^bar" to listOf("bar"),
            "foo^bar" to listOf("foobar"),
            "foo@" to listOf("foo with @"),
            "@bar" to listOf("bar with @"),
            "foo@bar" to listOf("foobar with @")
        )
        val tooltipConfig = mapOf(
            TOOLTIP_LINES to listOf(
                "^shape",           // aes
                "@shape",            // variable with name that match to the name of aes
                "\\^shape",         // '^' is part of the string (it's not the name) => string is "^shape"
                "\\@shape",          // '@' is part of the string (it's not the name) => string is "@shape"
                "@{model name}",     // space in the name
                "@foo^",             // with '^' at the end
                "@^bar",            // with '^' at the beginning
                "@foo^bar",         // with '^' at the middle
                "@foo@",             // with '@' at the end
                "@@bar",             // with '@' at the beginning
                "@foo@bar",          // with '@' at the middle
                "@foo^\\^",          // the second '^' is part of the result string
                "(@foo^)",          // with brackets as part of the result string
                "^shape, ^shape"   // result string comma separated
            )
        )
        val geomLayer = buildPointLayer(myData, mapping, tooltips = tooltipConfig)

        val expectedLines = listOf(
            "suv",
            "shape",
            "^shape",
            "@shape",
            "dodge",
            "foo",
            "bar",
            "foobar",
            "foo with @",
            "bar with @",
            "foobar with @",
            "foo^",
            "(foo)",
            "suv, suv"
        )
        val lines = getGeneralTooltipStrings(geomLayer)
        assertTooltipStrings(expectedLines, lines)
    }

    @Test
    fun changeFormatForTheDefault() {
        val tooltipConfig = mapOf(
            TOOLTIP_FORMATS to listOf(
                mapOf(
                    FIELD to "^color",
                    FORMAT to ".4f"         // number format
                )
            )
        )
        val geomLayer = buildPointLayer(data, mapping, tooltips = tooltipConfig)

        val expectedLines = listOf(
            "cty: 15.0000"
        )
        val lines = getGeneralTooltipStrings(geomLayer)
        assertTooltipStrings(expectedLines, lines)
    }

    @Test
    fun changeFormatForTheAes() {
        val mappingWithColor = mapOf(
            Aes.X.name to "displ",
            Aes.Y.name to "hwy",
            Aes.COLOR.name to "year"
        )

        // default
        val defaultGeomLayer = buildPointLayer(data, mappingWithColor, tooltips = null)
        assertTooltipStrings(
            listOf(
                "year: 1,998.00"
            ),
            getGeneralTooltipStrings(defaultGeomLayer)
        )

        // redefine format for the 'color' aes
        val geomLayerWithAesInTooltip = buildPointLayer(
            data, mappingWithColor, tooltips = mapOf(
                TOOLTIP_FORMATS to listOf(
                    mapOf(
                        FIELD to "^color",
                        FORMAT to "{d}"
                    )
                )
            )
        )
        assertTooltipStrings(
            listOf("year: 1998"),
            getGeneralTooltipStrings(geomLayerWithAesInTooltip)
        )

        // redefine format for the 'year' variable
        val geomLayerWithVarInTooltip = buildPointLayer(
            data, mappingWithColor, tooltips = mapOf(
                TOOLTIP_LINES to listOf("@|@year"),
                TOOLTIP_FORMATS to listOf(
                    mapOf(
                        FIELD to "year",
                        FORMAT to "{d}"
                    )
                )
            )
        )
        assertTooltipStrings(
            listOf("year: 1998"),
            getGeneralTooltipStrings(geomLayerWithVarInTooltip)
        )
    }

    @Test
    fun configLabels() {
        val tooltipConfig = mapOf(
            TOOLTIP_LINES to listOf(
                "@{model name}",             // no label
                "|@{model name}",            // empty label
                "@|@{model name}",           // default = the variable name
                "the model|@{model name}"    // specified
            )
        )
        val geomLayer = buildPointLayer(data, mapping, tooltips = tooltipConfig)

        val expectedLines = listOf(
            Line(null, "dodge"),
            Line("", "dodge"),
            Line("model name", "dodge"),
            Line("the model", "dodge")
        )
        assertTooltipLines(expectedLines, getGeneralTooltipLines(geomLayer))
    }

    @Test
    fun userComplicatedTooltipLines() {
        val tooltipConfig = mapOf(
            TOOLTIP_LINES to listOf(
                "mpg data set info",             // static text
                "^color (mpg)",                 // formatted
                "@{model name} car (@origin)",   // multiple sources in the line
                "x/y|^x x ^y"                  // specify label
            ),
            TOOLTIP_FORMATS to listOf(          //define formats
                mapOf(FIELD to "^color", FORMAT to ".1f"),
                mapOf(FIELD to "^x", FORMAT to ".3f"),
                mapOf(FIELD to "^y", FORMAT to ".1f")
            )
        )
        val geomLayer = buildPointLayer(data, mapping, tooltips = tooltipConfig)

        val expectedLines = listOf(
            "mpg data set info",
            "15.0 (mpg)",
            "dodge car (US)",
            "x/y: 1.600 x 160.0"
        )
        val lines = getGeneralTooltipStrings(geomLayer)
        assertTooltipStrings(expectedLines, lines)
    }

    // Outliers
    private val boxPlotData = mapOf(
        "x" to List(6) { 'A' },
        "y" to listOf(4.2, 11.5, 7.3, 5.8, 6.4, 10.0)
    )

    @Test
    fun defaultOutlierTooltips() {
        val geomLayer = buildGeomLayer(
            geom = "boxplot",
            data = boxPlotData,
            mapping = mapOf(
                Aes.X.name to "x",
                Aes.Y.name to "y"
            ),
            tooltips = null
        )
        val expectedLines = mapOf(
            Aes.YMAX to "11.50",
            Aes.UPPER to "8.65",
            Aes.MIDDLE to "6.85",
            Aes.LOWER to "6.10",
            Aes.YMIN to "4.20"
        )
        val lines = getOutlierLines(geomLayer)

        assertEquals(expectedLines.size, lines.size, "Wrong number of outlier tooltips")
        for (aes in lines.keys) {
            assertEquals(expectedLines[aes], lines[aes], "Wrong line for ${aes.name} in the outliers")
        }
    }

    @Test
    fun configOutlierTooltips() {
        val tooltipConfig = mapOf(
            TOOLTIP_LINES to listOf(
                "lower/upper|^lower, ^upper"
            ),
            TOOLTIP_FORMATS to listOf(
                mapOf(
                    FIELD to "^Y",
                    FORMAT to ".1f"
                ),       // all positionals
                mapOf(FIELD to "^middle", FORMAT to ".3f"),    // number format
                mapOf(FIELD to "^ymax", FORMAT to "{.1f}")     // line pattern
            )
        )

        val geomLayer = buildGeomLayer(
            geom = "boxplot",
            data = boxPlotData,
            mapping = mapOf(
                Aes.X.name to "x",
                Aes.Y.name to "y"
            ),
            tooltips = tooltipConfig
        )

        // upper, lower will be in the general tooltip and will be removed from outliers
        // outliers - without label
        val expectedLines = mapOf(
            Aes.YMAX to "11.5",
            Aes.MIDDLE to "6.850",
            Aes.YMIN to "4.2"
        )
        val generalExpectedLine = "lower/upper: 6.1, 8.7"

        val outlierLines = getOutlierLines(geomLayer)
        assertEquals(expectedLines.size, outlierLines.size, "Wrong number of outlier tooltips")
        for (aes in outlierLines.keys) {
            assertEquals(expectedLines[aes], outlierLines[aes], "Wrong line for ${aes.name} in the outliers")
        }

        val generalLines = getGeneralTooltipStrings(geomLayer)
        assertTooltipStrings(listOf(generalExpectedLine), generalLines)
    }

    @Test
    fun `numeric format for non-numeric value will be ignored`() {
        val tooltipConfig = mapOf(
            TOOLTIP_LINES to listOf(
                "class is @class"
            ),
            TOOLTIP_FORMATS to listOf(
                mapOf(
                    FIELD to "class",
                    FORMAT to ".2f"
                )
            )
        )
        val geomLayer = buildPointLayer(data, mapping, tooltips = tooltipConfig)
        val expectedLines = listOf(
            "class is suv"
        )
        assertTooltipStrings(expectedLines, getGeneralTooltipStrings(geomLayer))
    }

    @Test
    fun `geom_text by default should not show tooltip`() {
        val geomTextLayer = buildGeomLayer(
            geom = "text",
            data = mapOf("label" to listOf("my text")),
            mapping = mapOf(Aes.LABEL.name to "label"),
            tooltips = null
        )
        assertTooltipStrings(
            expected = emptyList(),
            actual = getGeneralTooltipStrings(geomTextLayer)
        )
    }

    @Test
    fun `geom_text should support tooltips configuration`() {
        val geomTextLayer = buildGeomLayer(
            geom = "text",
            data = mapOf("label" to listOf("my text")),
            mapping = mapOf(Aes.LABEL.name to "label"),
            tooltips = mapOf(
                TOOLTIP_LINES to listOf("^label")
            )
        )
        assertTooltipStrings(
            expected = listOf("my text"),
            actual = getGeneralTooltipStrings(geomTextLayer)
        )
    }

    @Test
    fun `geom_text by default should not show axis tooltips`() {
        val geomTextLayer = buildGeomLayer(
            geom = "text",
            data = mapOf(
                "x" to listOf(1.0),
                "y" to listOf(1.0),
                "label" to listOf("my text")
            ),
            mapping = mapOf(
                Aes.X.name to "x",
                Aes.Y.name to "y",
                Aes.LABEL.name to "label"
            ),
            tooltips = null
        )
        assertTooltipStrings(
            expected = emptyList(),
            actual = getGeneralTooltipStrings(geomTextLayer)
        )
        val axisTooltips = getAxisTooltips(geomTextLayer)
        assertEquals(0, axisTooltips.size, "Wrong number of axis tooltips")
    }

    @Test
    fun `geom_text should add axis tooltips if tooltip lines are defined`() {
        val geomTextLayer = buildGeomLayer(
            geom = "text",
            data = mapOf(
                "x" to listOf(1.0),
                "y" to listOf(1.0),
                "label" to listOf("my text")
            ),
            mapping = mapOf(
                Aes.X.name to "x",
                Aes.Y.name to "y",
                Aes.LABEL.name to "label"
            ),
            tooltips = mapOf(
                TOOLTIP_LINES to listOf("^label")
            )
        )
        assertTooltipStrings(
            expected = listOf("my text"),
            actual = getGeneralTooltipStrings(geomTextLayer)
        )
        val axisTooltips = getAxisTooltips(geomTextLayer)
        assertEquals(2, axisTooltips.size, "Wrong number of axis tooltips")
    }

    @Test
    fun `wrong tooltip format (no arguments)`() {
        assertFailTooltipSpec(
            tooltipConfig = mapOf(TOOLTIP_FORMATS to listOf(emptyMap<String, String>())),
            expectedMessage = "Invalid 'format' arguments: 'field' and 'format' are expected"
        )
    }

    @Test
    fun `wrong tooltip format (list instead of map)`() {
        assertFailTooltipSpec(
            tooltipConfig = mapOf(
                TOOLTIP_FORMATS to listOf(
                    listOf(
                        FIELD to "^color",
                        FORMAT to ".2f"
                    )
                )
            ),
            expectedMessage = "Wrong tooltip 'format' arguments"
        )
    }

    @Test
    fun `wrong tooltip format (wrong number of arguments)`() {
        assertFailTooltipSpec(
            tooltipConfig = mapOf(
                TOOLTIP_FORMATS to listOf(
                    mapOf(
                        FIELD to "^color",
                        FORMAT to "{.2f} {.2f}"
                    )
                )
            ),
            expectedMessage = "Wrong number of arguments in pattern '{.2f} {.2f}' to format 'color'. Expected 1 argument instead of 2"
        )
    }

    private fun assertFailTooltipSpec(
        tooltipConfig: Any?,
        expectedMessage: String
    ) {
        val plotOpts = mutableMapOf(
            DATA to data,
            MAPPING to mapping,
            LAYERS to listOf(
                mapOf(
                    GEOM to "point",
                    TOOLTIPS to tooltipConfig
                )
            )
        )
        val plotSpec = ServerSideTestUtil.serverTransformWithoutEncoding(plotOpts.toMutableMap())

        assertTrue(PlotConfig.isFailure(plotSpec))
        assertEquals(expectedMessage, PlotConfig.getErrorMessage(plotSpec))
    }

    @Test
    fun `tooltip with strings similar to number format`() {
        val tooltipConfig = mapOf(
            TOOLTIP_LINES to listOf(
                "^x",   // aes x
                "x",    // static text "x"
                "\$x",  // static text "$x"
                ".1f"   // static text ".1f"
            )
        )
        val geomLayer = buildPointLayer(data, mapping, tooltips = tooltipConfig)
        val expectedLines = listOf(
            "1.60",
            "x",
            "\$x",
            ".1f"
        )
        val lines = getGeneralTooltipStrings(geomLayer)
        assertTooltipStrings(expectedLines, lines)
    }

    @Test
    fun `use the 'group' in tooltips`() {
        val geomLayer = buildPointLayer(
            data = mapOf(
                "x" to listOf(1),
                "y" to listOf(1),
                "id" to listOf("a")
            ),
            mapping = mapOf(
                Aes.X.name to "x",
                Aes.Y.name to "y",
                "group" to "id"
            ),
            tooltips = mapOf(
                TOOLTIP_LINES to listOf("^group")
            )
        )
        assertTooltipStrings(
            expected = listOf("a"),
            actual = getGeneralTooltipStrings(geomLayer)
        )
    }

    @Test
    fun `specified tooltip format for variable should be applied to the aes`() {
        val tooltipConfig = mapOf(
            TOOLTIP_FORMATS to listOf(
                mapOf(
                    FIELD to "cty",
                    FORMAT to "{.1f} (mpg)"
                )
            )
        )
        val geomLayer = buildPointLayer(data, mapping, tooltips = tooltipConfig)

        val expectedLines = listOf(
            "cty: 15.0 (mpg)"
        )
        val lines = getGeneralTooltipStrings(geomLayer)
        assertTooltipStrings(expectedLines, lines)
    }

    // Variable list tests

    @Test
    fun `variable list to place in a tooltip`() {
        val tooltipConfig = mapOf(
            TOOLTIP_VARIABLES to listOf(
                "model name",
                "class",
                "origin"
            )
        )
        val geomLayer = buildPointLayer(data, mapping, tooltips = tooltipConfig)
        val expectedLines = listOf(
            "model name: dodge",
            "class: suv",
            "origin: US"
        )
        val lines = getGeneralTooltipStrings(geomLayer)
        assertTooltipStrings(expectedLines, lines)
    }

    @Test
    fun `tooltip lines should be formed by variable list and line functions`() {
        val tooltipConfig = mapOf(
            TOOLTIP_VARIABLES to listOf(
                "model name",
                "class"
            ),
            TOOLTIP_LINES to listOf(
                "@|@origin"
            )
        )
        val geomLayer = buildPointLayer(data, mapping, tooltips = tooltipConfig)
        val expectedLines = listOf(
            "model name: dodge",
            "class: suv",
            "origin: US"
        )
        val lines = getGeneralTooltipStrings(geomLayer)
        assertTooltipStrings(expectedLines, lines)
    }

    @Test
    fun `variable from the list without specified format() should use aes formatting - default or specified`() {
        run {
            // use default aes formatting

            val tooltipConfig = mapOf(
                TOOLTIP_VARIABLES to listOf(
                    "cty"
                )
            )
            val geomLayer = buildPointLayer(data, mapping, tooltips = tooltipConfig)
            val expectedLines = listOf(
                "cty: 15.00"
            )
            val lines = getGeneralTooltipStrings(geomLayer)
            assertTooltipStrings(expectedLines, lines)
        }
        run {
            // use specified aes formatting

            val tooltipConfig = mapOf(
                TOOLTIP_VARIABLES to listOf(
                    "cty"
                ),
                TOOLTIP_FORMATS to listOf(
                    mapOf(
                        FIELD to "^color",
                        FORMAT to ".4f"
                    )
                )
            )
            val geomLayer = buildPointLayer(data, mapping, tooltips = tooltipConfig)
            val expectedLines = listOf(
                "cty: 15.0000"
            )
            val lines = getGeneralTooltipStrings(geomLayer)
            assertTooltipStrings(expectedLines, lines)
        }
    }

    @Test
    fun `variable from the 'line' without specified format() should use aes formatting - default or specified`() {
        run {
            // use default aes formatting
            val tooltipConfig = mapOf(
                TOOLTIP_LINES to listOf(
                    "@cty"
                )
            )
            val geomLayer = buildPointLayer(data, mapping, tooltips = tooltipConfig)
            val expectedLines = listOf(
                "15.00"
            )
            val lines = getGeneralTooltipStrings(geomLayer)
            assertTooltipStrings(expectedLines, lines)
        }
        run {
            // use specified aes formatting
            val tooltipConfig = mapOf(
                TOOLTIP_LINES to listOf(
                    "@cty"
                ),
                TOOLTIP_FORMATS to listOf(
                    mapOf(
                        FIELD to "^color",
                        FORMAT to ".4f"
                    )
                )
            )
            val geomLayer = buildPointLayer(data, mapping, tooltips = tooltipConfig)
            val expectedLines = listOf(
                "15.0000"
            )
            val lines = getGeneralTooltipStrings(geomLayer)
            assertTooltipStrings(expectedLines, lines)
        }
    }

    @Test
    fun `variable should always choose own formatting if it's specified`() {
        run {
            // variable list
            val tooltipConfig = mapOf(
                TOOLTIP_VARIABLES to listOf(
                    "cty"
                ),
                TOOLTIP_FORMATS to listOf(
                    mapOf(
                        FIELD to "cty",
                        FORMAT to "{.1f} (var format)"
                    ),
                    mapOf(
                        FIELD to "^color",
                        FORMAT to "{.1f} (aes format)"
                    )
                )
            )
            val geomLayer = buildPointLayer(data, mapping, tooltips = tooltipConfig)
            val expectedLines = listOf(
                "cty: 15.0 (var format)"
            )
            val lines = getGeneralTooltipStrings(geomLayer)
            assertTooltipStrings(expectedLines, lines)
        }
        run {
            // in line function
            val tooltipConfig = mapOf(
                TOOLTIP_LINES to listOf(
                    "@|@cty"
                ),
                TOOLTIP_FORMATS to listOf(
                    mapOf(
                        FIELD to "cty",
                        FORMAT to "{.1f} (var format)"
                    ),
                    mapOf(
                        FIELD to "^color",
                        FORMAT to "{.1f} (aes format)"
                    )
                )
            )
            val geomLayer = buildPointLayer(data, mapping, tooltips = tooltipConfig)
            val expectedLines = listOf(
                "cty: 15.0 (var format)"
            )
            val lines = getGeneralTooltipStrings(geomLayer)
            assertTooltipStrings(expectedLines, lines)
        }
    }

    @Test
    fun `variable mapped to two aes will choose aes formatting - specified or first`() {
        val curMapping = mapOf(
            Aes.X.name to "displ",
            Aes.Y.name to "hwy",
            Aes.COLOR.name to "cty",
            Aes.SHAPE.name to "cty"
        )

        run {
            val tooltipConfig = mapOf(
                TOOLTIP_LINES to listOf(
                    "@|@cty"
                )
            )
            val geomLayer = buildPointLayer(data, curMapping, tooltips = tooltipConfig)
            val expectedLines = listOf(
                "cty: 15.00"
            )
            val lines = getGeneralTooltipStrings(geomLayer)
            assertTooltipStrings(expectedLines, lines)
        }
        run {
            // use color formatting
            val tooltipConfig = mapOf(
                TOOLTIP_LINES to listOf(
                    "@|@cty"
                ),
                TOOLTIP_FORMATS to listOf(
                    mapOf(
                        FIELD to "^color",
                        FORMAT to "{.1f} (color format)"
                    )
                )
            )
            val geomLayer = buildPointLayer(data, curMapping, tooltips = tooltipConfig)
            val expectedLines = listOf(
                "cty: 15.0 (color format)"
            )
            val lines = getGeneralTooltipStrings(geomLayer)
            assertTooltipStrings(expectedLines, lines)
        }
        run {
            // use shape formatting
            val tooltipConfig = mapOf(
                TOOLTIP_LINES to listOf(
                    "@|@cty"
                ),
                TOOLTIP_FORMATS to listOf(
                    mapOf(
                        FIELD to "^shape",
                        FORMAT to "{.1f} (shape format)"
                    )
                )
            )
            val geomLayer = buildPointLayer(data, curMapping, tooltips = tooltipConfig)
            val expectedLines = listOf(
                "cty: 15.0 (shape format)"
            )
            val lines = getGeneralTooltipStrings(geomLayer)
            assertTooltipStrings(expectedLines, lines)
        }
        run {
            // should choose the first alphabetically
            val tooltipConfig = mapOf(
                TOOLTIP_LINES to listOf(
                    "@|@cty"
                ),
                TOOLTIP_FORMATS to listOf(
                    mapOf(
                        FIELD to "^color",
                        FORMAT to "{.1f} (color format)"
                    ),
                    mapOf(
                        FIELD to "^shape",
                        FORMAT to "{.1f} (shape format)"
                    )
                )
            )
            val geomLayer = buildPointLayer(data, curMapping, tooltips = tooltipConfig)
            val expectedLines = listOf(
                "cty: 15.0 (color format)"
            )
            val lines = getGeneralTooltipStrings(geomLayer)
            assertTooltipStrings(expectedLines, lines)
        }
    }

    @Test
    fun `format() should understand DateTime format`() {
        val geomLayer = buildPointLayer(
            data = mapOf(
                "x" to listOf(1609459200000),
                "y" to listOf(0.0)
            ),
            mapping = mapOf(
                Aes.X.name to "x",
                Aes.Y.name to "y",
            ),
            tooltips = mapOf(
                TOOLTIP_LINES to listOf("^x"),
                TOOLTIP_FORMATS to listOf(
                    mapOf(
                        FIELD to "^x",
                        FORMAT to "%d.%m.%y"
                    )
                )
            )
        )
        assertTooltipStrings(
            expected = listOf("01.01.21"),
            actual = getGeneralTooltipStrings(geomLayer)
        )
    }

    companion object {
        private fun getGeneralTooltipStrings(geomLayer: GeomLayer): List<String> {
            return getGeneralTooltipLines(geomLayer).map(Line::toString)
        }

        private fun getGeneralTooltipLines(geomLayer: GeomLayer): List<Line> {
            val dataPoints = geomLayer.contextualMapping.getDataPoints(index = 0)
            return dataPoints.filterNot(DataPoint::isOutlier).map { Line(it.label, it.value) }
        }

        private fun getAxisTooltips(geomLayer: GeomLayer): List<DataPoint> {
            val dataPoints = geomLayer.contextualMapping.getDataPoints(index = 0)
            return dataPoints.filter(DataPoint::isAxis)
        }

        private fun getOutlierLines(geomLayer: GeomLayer): Map<Aes<*>, String> {
            val dataPoints = geomLayer.contextualMapping.getDataPoints(index = 0)
            return dataPoints.filter { it.isOutlier && !it.isAxis }.associateBy({ it.aes!! }, { it.value })
        }

        private fun assertTooltipStrings(expected: List<String>, actual: List<String>) {
            assertEquals(expected.size, actual.size, "Wrong number of lines in the general tooltip")
            for (index in expected.indices) {
                assertEquals(expected[index], actual[index], "Wrong line #$index in the general tooltip")
            }
        }

        private fun assertTooltipLines(expectedLines: List<Line>, actualLines: List<Line>) {
            assertEquals(expectedLines.size, actualLines.size, "Wrong number of lines in the general tooltip")
            for (index in expectedLines.indices) {
                assertEquals(
                    expectedLines[index].label,
                    actualLines[index].label,
                    "Wrong label in line #$index in the general tooltip"
                )
                assertEquals(
                    expectedLines[index].value,
                    actualLines[index].value,
                    "Wrong value in line #$index in the general tooltip"
                )
            }
        }
    }
}