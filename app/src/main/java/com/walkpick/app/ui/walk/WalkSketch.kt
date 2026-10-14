package com.walkpick.app.ui.walk

import com.walkpick.app.data.local.FindEntity
import com.walkpick.app.data.local.WalkEntity
import kotlin.math.cos

/** A walk on a card: its numbers, the picture of its route and its photos in the order they were taken. */
data class DiaryWalk(
    val walk: WalkEntity,
    val photos: Int,
    val sketch: List<Pair<Float, Float>>,
    /** The finds of the walk in the order they were taken, for the thumbnails of its card. */
    val finds: List<FindEntity> = emptyList(),
)

/**
 * The route as shares 0..1 of the width and the height of a tile [aspect] times wider than high, with
 * the north up, the whole route inside it and its proportions kept: the picture of a walk on a card.
 */
internal fun routeSketch(places: List<Pair<Double, Double>>, aspect: Float = 1f): List<Pair<Float, Float>> {
    if (places.size < 2) return emptyList()
    // A degree of longitude is shorter than a degree of latitude away from the equator.
    val squeeze = cos(Math.toRadians(places.first().first))
    val xs = places.map { it.second * squeeze }
    val ys = places.map { it.first }
    val (left, bottom) = xs.min() to ys.min()
    val (wide, high) = xs.max() - left to ys.max() - bottom
    // The tile in the units of the route: as high as the route, or higher when the route is too wide.
    val tileHigh = maxOf(high, wide / aspect).takeIf { it > 0 } ?: return emptyList()
    val tileWide = tileHigh * aspect
    val (padX, padY) = (tileWide - wide) / 2 to (tileHigh - high) / 2
    return places.indices.map { index ->
        val x = (xs[index] - left + padX) / tileWide
        val y = 1 - (ys[index] - bottom + padY) / tileHigh
        x.toFloat() to y.toFloat()
    }
}
