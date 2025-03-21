/*
 * Copyright (c) 2019. JetBrains s.r.o.
 * Use of this source code is governed by the MIT license that can be found in the LICENSE file.
 */

package jetbrains.datalore.plot.builder.scale

import jetbrains.datalore.base.gcommon.collect.ClosedRange
import jetbrains.datalore.plot.base.ContinuousTransform
import jetbrains.datalore.plot.base.Scale

interface ScaleProvider<T> {
    val discreteDomain: Boolean
    val breaks: List<Any>?
    val limits: List<Any?>? // when 'continuous' limits, NULL means undefined upper or lower limit.
    val continuousTransform: ContinuousTransform
    val mapperProvider: MapperProvider<T>

    /**
     * Create scale for discrete input (domain)
     */
    fun createScale(defaultName: String, discreteDomain: Collection<*>): Scale<T>

    /**
     * Create scale for continuous (numeric) input (domain)
     */
    fun createScale(defaultName: String, continuousDomain: ClosedRange<Double>): Scale<T>
}
