/*
 * Copyright (c) 2020. JetBrains s.r.o.
 * Use of this source code is governed by the MIT license that can be found in the LICENSE file.
 */

package jetbrains.livemap.searching

import jetbrains.datalore.base.typedGeometry.*
import jetbrains.datalore.base.values.Color
import jetbrains.livemap.Client
import jetbrains.livemap.chart.ChartElementComponent
import jetbrains.livemap.core.ecs.EcsEntity
import jetbrains.livemap.fragment.RegionFragmentsComponent
import jetbrains.livemap.geometry.ScreenGeometryComponent
import jetbrains.livemap.mapengine.placement.ScreenLoopComponent

class PolygonLocatorHelper : LocatorHelper {

    override fun getColor(target: EcsEntity): Color? {
        return target.get<ChartElementComponent>().fillColor
    }

    override fun isCoordinateInTarget(coord: Vec<Client>, target: EcsEntity): Boolean {
        if (target.contains<RegionFragmentsComponent>()) {
            target.get<RegionFragmentsComponent>().fragments.forEach { fragment ->
                if (isCoordinateOnEntity(coord, fragment)) {
                    return true
                }
            }

            return false
        } else {
            return isCoordinateOnEntity(coord, target)
        }
    }

    private fun isCoordinateOnEntity(coord: Vec<Client>, target: EcsEntity): Boolean {
        if (!target.contains(LOCATABLE_COMPONENTS)) {
            return false
        }

        target.get<ScreenLoopComponent>().origins.forEach { origin ->
            if (isCoordinateInPolygon(coord - origin, target.get<ScreenGeometryComponent>().geometry)) {
                return true
            }
        }

        return false
    }

    private fun isCoordinateInPolygon(coord: Vec<Client>, multiPolygon: MultiPolygon<Client>): Boolean {
        for (polygon in multiPolygon) {
            if (!polygon.limit().contains(coord)) {
                continue
            }
            var count = 0
            for (ring in polygon) {
                if (LocatorUtil.ringContainsCoordinate(ring, coord)) {
                    ++count
                }
            }
            if (count % 2 == 1) {
                return true
            }
        }
        return false
    }

    companion object {
        val LOCATABLE_COMPONENTS = listOf(ScreenLoopComponent::class, ScreenGeometryComponent::class)
    }
}