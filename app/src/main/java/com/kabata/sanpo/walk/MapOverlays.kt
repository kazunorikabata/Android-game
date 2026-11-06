package com.kabata.sanpo.walk

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Point
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Overlay

/** まだ歩いていない場所をおおう「霧」。歩いたマスだけ穴をあける */
class FogOverlay(fogColor: Int) : Overlay() {
    var cells: Set<Long> = emptySet()

    private val paint = Paint().apply {
        color = fogColor
        style = Paint.Style.FILL
    }
    private val path = Path()
    private val gp = GeoPoint(0.0, 0.0)
    private val a = Point()
    private val b = Point()

    override fun draw(canvas: Canvas, mapView: MapView, shadow: Boolean) {
        if (shadow) return
        val proj = mapView.projection
        val box = proj.boundingBox
        val w = mapView.width.toFloat()
        val h = mapView.height.toFloat()
        val step = WalkTracker.CELL

        path.reset()
        path.fillType = Path.FillType.EVEN_ODD
        path.addRect(-w, -h, 2 * w, 2 * h, Path.Direction.CW)
        for (key in cells) {
            val south = WalkTracker.cellY(key) * step
            val west = WalkTracker.cellX(key) * step
            if (south > box.latNorth || south + step < box.latSouth ||
                west > box.lonEast || west + step < box.lonWest
            ) continue
            gp.setCoords(south + step, west)
            proj.toPixels(gp, a)
            gp.setCoords(south, west + step)
            proj.toPixels(gp, b)
            path.addRect(a.x.toFloat(), a.y.toFloat(), b.x.toFloat(), b.y.toFloat(), Path.Direction.CW)
        }
        canvas.drawPath(path, paint)
    }
}

/** 今日歩いたルート */
class RouteOverlay(lineColor: Int, widthPx: Float) : Overlay() {
    var segments: List<List<GeoPoint>> = emptyList()

    private val paint = Paint().apply {
        color = lineColor
        style = Paint.Style.STROKE
        strokeWidth = widthPx
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        isAntiAlias = true
    }
    private val path = Path()
    private val pt = Point()

    override fun draw(canvas: Canvas, mapView: MapView, shadow: Boolean) {
        if (shadow) return
        val proj = mapView.projection
        path.reset()
        for (seg in segments) {
            seg.forEachIndexed { i, p ->
                proj.toPixels(p, pt)
                if (i == 0) path.moveTo(pt.x.toFloat(), pt.y.toFloat())
                else path.lineTo(pt.x.toFloat(), pt.y.toFloat())
            }
        }
        canvas.drawPath(path, paint)
    }
}
