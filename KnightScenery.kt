package com.nexus.gamelibrary

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.lerp
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

// ════════════════════════════════════════════════════════════════
//  Chevalier : décors animés (un par monde)
//  montagnes, volcans, éclairs, pluie, neige, aurores, brume, créatures volantes…
// ════════════════════════════════════════════════════════════════

private const val TAU = 6.2831855f

internal fun hsh(a: Int, b: Int): Float {
    var h = a * 374761393 + b * 668265263
    h = (h xor (h ushr 13)) * 1274126177
    h = h xor (h ushr 16)
    return (h and 0xFFFF) / 65535f
}

internal fun wrapF(v: Float, m: Float): Float = ((v % m) + m) % m

internal fun DrawScope.triPx(c: Color, x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float) {
    val p = Path()
    p.moveTo(x1, y1)
    p.lineTo(x2, y2)
    p.lineTo(x3, y3)
    p.close()
    drawPath(p, c)
}

internal fun DrawScope.polyPx(c: Color, vararg pts: Float) {
    if (pts.size < 6) return
    val p = Path()
    p.moveTo(pts[0], pts[1])
    var i = 2
    while (i + 1 < pts.size) {
        p.lineTo(pts[i], pts[i + 1])
        i += 2
    }
    p.close()
    drawPath(p, c)
}

private fun DrawScope.lineF(c: Color, x1: Float, y1: Float, x2: Float, y2: Float, w: Float) {
    drawLine(c, Offset(x1, y1), Offset(x2, y2), strokeWidth = max(1f, w), cap = StrokeCap.Round)
}

private fun DrawScope.glowPx(c: Color, x: Float, y: Float, r: Float, a: Float) {
    if (r <= 0f) return
    val ctr = Offset(x, y)
    drawCircle(Brush.radialGradient(listOf(c.copy(alpha = a), c.copy(alpha = 0f)), center = ctr, radius = r), r, ctr)
}

// ── Tables par monde (13 mondes)
// styles : 0 collines/arbres · 1 cité gothique · 2 montagnes · 3 grottes · 4 désert · 5 cimetière · 6 cristaux géants
//          7 volcan · 8 ruines · 9 sous-marin · 10 abysses · 11 forêt de pins · 12 marais
private val LAYERS = arrayOf(
    intArrayOf(2, 0, 0),
    intArrayOf(11, 11, 11),
    intArrayOf(3, 3, 3),
    intArrayOf(0, 12, 12),
    intArrayOf(4, 4, 4),
    intArrayOf(2, 2, 11),
    intArrayOf(6, 6, 3),
    intArrayOf(1, 5, 5),
    intArrayOf(7, 7, 2),
    intArrayOf(2, 8, 8),
    intArrayOf(9, 9, 9),
    intArrayOf(2, 1, 1),
    intArrayOf(10, 10, 3)
)
// astre : 0 aucun · 1 soleil · 2 lune · 3 grande lune · 4 lune rouge · 5 éclipse
private val CELESTIAL = intArrayOf(1, 2, 0, 2, 1, 2, 0, 3, 4, 1, 1, 4, 5)
private val STARS = booleanArrayOf(false, true, false, true, false, true, false, true, false, false, false, true, true)
private val CLOUDS = booleanArrayOf(true, false, false, false, false, true, false, true, true, true, false, true, false)
// météo : 1 pluie · 2 neige · 3 braises · 4 tempête de sable · 5 lucioles · 6 bulles · 7 pollen doré · 8 spores bleues · 9 étincelles · 10 feux follets · 11 spores roses
private val WEATHER = intArrayOf(7, 5, 8, 10, 4, 2, 9, 1, 3, 7, 6, 1, 11)
private val LIGHTNING = booleanArrayOf(false, false, false, false, false, false, false, true, true, true, false, true, false)
// créatures volantes : 1 oiseaux · 2 chauves-souris · 3 poissons
private val FLYERS = intArrayOf(1, 0, 2, 0, 0, 0, 0, 2, 0, 0, 3, 2, 0)
// aurore : 1 verte
private val AURORA = intArrayOf(0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0)
private val CEILING = booleanArrayOf(false, true, true, true, false, false, true, true, true, true, true, true, true)
private val WORLD_ACCENT = longArrayOf(
    0xFF7CFF8A, 0xFFB6FF6A, 0xFF6FE3FF, 0xFF9CFF5A, 0xFFFFB84A, 0xFF9FD8FF, 0xFF8C9CFF,
    0xFF7CFFC0, 0xFFFF6A2A, 0xFFFFD36B, 0xFF3ED1C0, 0xFFB06CFF, 0xFFFF3D6E
)

internal fun worldAccent(wi: Int): Color = Color(WORLD_ACCENT[wi.coerceIn(0, WORLD_ACCENT.lastIndex)])

private fun flashColor(wi: Int): Color = when (wi) {
    11 -> Color(0xFFC9A8FF)
    8 -> Color(0xFFFF9A6A)
    else -> Color(0xFFDDE8FF)
}

// ════════════════════════════════════════════════════════════════
//  Éclairs
// ════════════════════════════════════════════════════════════════

/** Intensité de l'éclair (0 = aucun) : un éclair toutes les 5 à 7 s, en double flash. */
internal fun lightningLevel(wi0: Int, frame: Int): Float {
    val wi = wi0.coerceIn(0, 12)
    if (!LIGHTNING[wi]) return 0f
    val period = 300 + (wi % 4) * 55
    val n = frame / period
    val start = (hsh(n, wi + 3) * (period - 40)).toInt()
    val dt = frame % period - start
    if (dt < 0 || dt > 22) return 0f
    return when {
        dt < 5 -> 1f
        dt < 9 -> 0.25f
        dt < 14 -> 0.85f
        else -> (22 - dt) / 8f * 0.5f
    }
}

private fun DrawScope.drawBolt(frame: Int, wi: Int, s: Float, flash: Float, col: Color) {
    if (flash < 0.4f) return
    val period = 300 + (wi % 4) * 55
    val n = frame / period
    var x = (0.15f + 0.7f * hsh(n, wi + 9)) * size.width
    var y = 0f
    val endY = size.height * (0.55f + 0.2f * hsh(n, wi + 11))
    var i = 0
    while (y < endY && i < 40) {
        val nx = x + (hsh(n * 31 + i, wi) - 0.5f) * s * 1.4f
        val ny = y + s * (0.5f + 0.5f * hsh(n * 17 + i, wi + 1))
        lineF(col.copy(alpha = 0.35f * flash), x, y, nx, ny, s * 0.35f)
        lineF(Color.White.copy(alpha = flash), x, y, nx, ny, max(2f, s * 0.08f))
        if (i % 4 == 2) {
            val bx = nx + (hsh(n + i, wi + 2) - 0.5f) * s * 3f
            val by = ny + s * 1.3f
            lineF(col.copy(alpha = 0.3f * flash), nx, ny, bx, by, s * 0.2f)
            lineF(Color.White.copy(alpha = 0.8f * flash), nx, ny, bx, by, max(1.5f, s * 0.05f))
        }
        x = nx
        y = ny
        i++
    }
}

// ════════════════════════════════════════════════════════════════
//  Ciel : étoiles, astres, aurores, nuages, créatures
// ════════════════════════════════════════════════════════════════

private fun DrawScope.drawStars(frame: Int, wi: Int, mul: Float) {
    val unit = (size.height / 400f).coerceAtLeast(0.8f)
    for (i in 0 until 46) {
        val x = hsh(i, wi + 1) * size.width
        val y = hsh(i, wi + 2) * size.height * 0.55f
        val a = (0.25f + 0.55f * (0.5f + 0.5f * sin(frame * 0.03f + i * 1.3f))) * mul
        drawCircle(Color.White.copy(alpha = a), (0.6f + 1.4f * hsh(i, 3)) * unit, Offset(x, y))
    }
}

private fun DrawScope.drawCelestial(kind: Int, wi: Int, frame: Int) {
    if (kind == 0) return
    val hc = Offset(size.width * 0.7f, if (wi == 10) size.height * 0.02f else size.height * 0.24f)
    val base = size.height * 0.07f
    when (kind) {
        1 -> {
            val r = base * (if (wi == 4) 1.9f else if (wi == 10) 1.5f else 1.2f)
            glowPx(Color(0xFFFFE08A), hc.x, hc.y, r * 6f, 0.5f)
            for (i in 0 until 14) {
                val a = frame * 0.004f + i * (TAU / 14f)
                val l0 = r * 1.25f
                val l1 = r * (1.9f + 0.5f * (i % 2))
                lineF(Color(0xFFFFE9A8).copy(alpha = 0.35f), hc.x + cos(a) * l0, hc.y + sin(a) * l0, hc.x + cos(a) * l1, hc.y + sin(a) * l1, r * 0.12f)
            }
            drawCircle(Color(0xFFFFF3C0), r, hc)
            drawCircle(Color.White.copy(alpha = 0.8f), r * 0.7f, hc)
        }
        2, 3 -> {
            val r = base * (if (kind == 3) 1.9f else 1.0f)
            val mc = if (wi == 3) Color(0xFFD8F0B0) else Color(0xFFEFF3FF)
            glowPx(mc, hc.x, hc.y, r * 5f, 0.4f)
            drawCircle(mc, r, hc)
            val cr = Color(0xFF8A90A8).copy(alpha = 0.28f)
            drawCircle(cr, r * 0.22f, Offset(hc.x - r * 0.35f, hc.y - r * 0.2f))
            drawCircle(cr, r * 0.15f, Offset(hc.x + r * 0.3f, hc.y + r * 0.3f))
            drawCircle(cr, r * 0.1f, Offset(hc.x + r * 0.25f, hc.y - r * 0.45f))
            drawCircle(cr, r * 0.12f, Offset(hc.x - r * 0.1f, hc.y + r * 0.5f))
        }
        4 -> {
            val r = base * 1.6f
            glowPx(Color(0xFFFF3A2A), hc.x, hc.y, r * 5f, 0.45f + 0.05f * sin(frame * 0.03f))
            drawCircle(Color(0xFFD02A26), r, hc)
            drawCircle(Color(0xFF8A1418).copy(alpha = 0.5f), r * 0.25f, Offset(hc.x - r * 0.3f, hc.y - r * 0.2f))
            drawCircle(Color(0xFF8A1418).copy(alpha = 0.5f), r * 0.18f, Offset(hc.x + r * 0.35f, hc.y + r * 0.25f))
        }
        else -> {
            val r = base * 1.4f
            glowPx(Color(0xFFB06CFF), hc.x, hc.y, r * 4.5f, 0.3f + 0.1f * sin(frame * 0.04f))
            drawCircle(Color(0xFFE8C8FF).copy(alpha = 0.9f), r * 1.08f, hc)
            drawCircle(Color(0xFF05050F), r, hc)
        }
    }
}

private fun DrawScope.drawAurora(frame: Int) {
    val c1 = Color(0xFF5CFFB0)
    for (b in 0..2) {
        val top = size.height * (0.1f + 0.06f * b)
        val p = Path()
        val step = size.width / 24f
        p.moveTo(0f, top)
        var x = 0f
        while (x <= size.width + step) {
            p.lineTo(x, top + sin(x * 0.012f + frame * 0.012f + b * 1.7f) * size.height * 0.05f)
            x += step
        }
        x = size.width + step
        while (x >= 0f) {
            val wave = sin(x * 0.012f + frame * 0.012f + b * 1.7f) * size.height * 0.05f
            p.lineTo(x, top + wave + size.height * (0.2f + 0.05f * sin(x * 0.02f + frame * 0.02f + b)))
            x -= step
        }
        p.close()
        drawPath(
            p,
            Brush.verticalGradient(
                listOf(Color.Transparent, c1.copy(alpha = 0.32f - 0.06f * b), Color.Transparent),
                startY = top - size.height * 0.05f, endY = top + size.height * 0.3f
            )
        )
    }
}

private fun DrawScope.drawClouds(wi: Int, frame: Int, flash: Float) {
    val cc = when (wi) {
        0 -> Color(0xFFFFFFFF)
        5 -> Color(0xFFDCE8F5)
        7 -> Color(0xFF3A3A52)
        8 -> Color(0xFF4A2A26)
        9 -> Color(0xFF6A6878)
        else -> Color(0xFF2E2442)
    }
    val col = lerp(cc, flashColor(wi), flash * 0.8f)
    val a = if (wi == 0 || wi == 5) 0.75f else 0.82f
    for (i in 0 until 6) {
        val sp = 0.25f + 0.2f * hsh(i, wi + 40)
        val x = wrapF(i * size.width / 4f + frame * sp, size.width * 1.6f) - size.width * 0.3f
        val y = size.height * (0.06f + 0.16f * hsh(i, wi + 41))
        val cw = size.height * (0.35f + 0.25f * hsh(i, wi + 42))
        val ch = cw * 0.25f
        drawOval(col.copy(alpha = a * 0.55f), Offset(x, y + ch * 0.35f), Size(cw, ch * 0.8f))
        drawOval(col.copy(alpha = a), Offset(x + cw * 0.1f, y + ch * 0.1f), Size(cw * 0.5f, ch * 0.9f))
        drawOval(col.copy(alpha = a), Offset(x + cw * 0.35f, y - ch * 0.1f), Size(cw * 0.5f, ch))
        drawOval(col.copy(alpha = a * 0.9f), Offset(x + cw * 0.55f, y + ch * 0.2f), Size(cw * 0.4f, ch * 0.7f))
    }
}

private fun DrawScope.drawFlyers(kind: Int, frame: Int, s: Float) {
    when (kind) {
        1 -> for (i in 0 until 5) {
            val x = wrapF(frame * (0.5f + 0.4f * hsh(i, 61)) + i * 260f, size.width + 200f) - 100f
            val y = size.height * (0.12f + 0.16f * hsh(i, 62)) + sin(frame * 0.03f + i) * s * 0.3f
            val flap = sin(frame * 0.25f + i * 2f) * s * 0.16f
            val sz = s * 0.28f
            val c = INK.copy(alpha = 0.7f)
            lineF(c, x - sz, y - flap, x, y, max(1.5f, s * 0.04f))
            lineF(c, x, y, x + sz, y - flap, max(1.5f, s * 0.04f))
        }
        2 -> for (i in 0 until 4) {
            val dir = if (i % 2 == 0) 1f else -1f
            val x = wrapF(dir * frame * (0.7f + 0.5f * hsh(i, 63)) + i * 330f, size.width + 260f) - 130f
            val y = size.height * (0.1f + 0.3f * hsh(i, 64)) + sin(frame * 0.05f + i * 3f) * s * 0.5f
            val flap = sin(frame * 0.35f + i * 2f) * s * 0.22f
            val sz = s * 0.42f
            val c = INK.copy(alpha = 0.85f)
            polyPx(c, x, y, x - sz * 1.1f, y - flap, x - sz * 0.75f, y + sz * 0.15f, x - sz * 0.45f, y - flap * 0.2f + sz * 0.05f, x - sz * 0.2f, y + sz * 0.2f)
            polyPx(c, x, y, x + sz * 1.1f, y - flap, x + sz * 0.75f, y + sz * 0.15f, x + sz * 0.45f, y - flap * 0.2f + sz * 0.05f, x + sz * 0.2f, y + sz * 0.2f)
            drawOval(c, Offset(x - sz * 0.12f, y - sz * 0.1f), Size(sz * 0.24f, sz * 0.4f))
        }
        3 -> {
            for (i in 0 until 5) {
                val dir = if (i % 2 == 0) 1f else -1f
                val x = wrapF(dir * frame * (0.4f + 0.3f * hsh(i, 65)) + i * 300f, size.width + 200f) - 100f
                val y = size.height * (0.25f + 0.4f * hsh(i, 66)) + sin(frame * 0.04f + i) * s * 0.3f
                val wag = sin(frame * 0.2f + i) * s * 0.07f
                val c = Color(0xFF0A3A4A).copy(alpha = 0.55f)
                drawOval(c, Offset(x - s * 0.3f, y - s * 0.1f), Size(s * 0.6f, s * 0.22f))
                triPx(c, x - dir * s * 0.25f, y, x - dir * s * 0.55f, y - s * 0.15f + wag, x - dir * s * 0.55f, y + s * 0.15f + wag)
            }
            for (i in 0 until 3) {
                val x = size.width * (0.2f + 0.3f * i) + sin(frame * 0.01f + i * 2f) * s
                val y = size.height * (0.3f + 0.15f * hsh(i, 67)) + sin(frame * 0.02f + i) * s * 0.5f
                val pulse = 0.8f + 0.2f * sin(frame * 0.08f + i)
                glowPx(Color(0xFF7CFFF0), x, y, s * 1.3f, 0.25f)
                drawOval(Color(0xFF7CFFF0).copy(alpha = 0.4f), Offset(x - s * 0.3f, y - s * 0.2f * pulse), Size(s * 0.6f, s * 0.4f * pulse))
                for (t in -1..1) {
                    lineF(Color(0xFF7CFFF0).copy(alpha = 0.35f), x + t * s * 0.12f, y + s * 0.1f, x + t * s * 0.12f + sin(frame * 0.06f + t) * s * 0.1f, y + s * 0.6f, s * 0.03f)
                }
            }
        }
    }
}

private fun DrawScope.drawAbyssEyes(frame: Int, s: Float) {
    for (e in 0..1) {
        val ex = size.width * (0.28f + 0.42f * e)
        val ey = size.height * (0.3f + 0.1f * e)
        val er = s * (2.2f - 0.7f * e)
        val open = (0.55f + 0.45f * sin(frame * 0.012f + e * 2.1f)).coerceIn(0.05f, 1f)
        glowPx(Color(0xFFFF3D6E), ex, ey, er * 2.2f, 0.18f)
        drawOval(Color(0xFF1A0A18), Offset(ex - er, ey - er * 0.5f * open), Size(er * 2f, er * open))
        if (open > 0.25f) {
            val ix = ex + sin(frame * 0.01f + e) * er * 0.3f
            drawCircle(Color(0xFFFF3D6E).copy(alpha = 0.85f), er * 0.34f * min(1f, open * 1.5f), Offset(ix, ey))
            drawOval(Color.Black, Offset(ix - er * 0.06f, ey - er * 0.3f * open), Size(er * 0.12f, er * 0.6f * open))
        }
        lineF(Color(0xFF05050F), ex - er, ey, ex + er, ey, s * 0.08f)
    }
}

// ════════════════════════════════════════════════════════════════
//  Structures lointaines
// ════════════════════════════════════════════════════════════════

private fun DrawScope.branch(x: Float, y: Float, ang: Float, len: Float, depth: Int, col: Color, wpx: Float, seed: Int) {
    val nx = x + sin(ang) * len
    val ny = y - cos(ang) * len
    drawLine(col, Offset(x, y), Offset(nx, ny), strokeWidth = max(1f, wpx), cap = StrokeCap.Round)
    if (depth > 0) {
        branch(nx, ny, ang - 0.45f - 0.35f * hsh(seed, depth), len * 0.74f, depth - 1, col, wpx * 0.66f, seed + 1)
        branch(nx, ny, ang + 0.45f + 0.35f * hsh(seed, depth + 7), len * 0.7f, depth - 1, col, wpx * 0.66f, seed + 2)
    }
}

private fun DrawScope.pine(col: Color, tx: Float, ty: Float, th: Float, tiers: Int, snow: Boolean) {
    drawRect(col, Offset(tx - th * 0.03f, ty - th * 0.15f), Size(th * 0.06f, th * 0.17f))
    for (j in 0 until tiers) {
        val top = ty - th + j * (th * 0.6f / max(1, tiers - 1))
        val half = th * 0.16f * (1f + 0.45f * j)
        val tierH = th * 0.42f
        triPx(col, tx, top, tx - half, top + tierH, tx + half, top + tierH)
        if (snow) triPx(Color(0xFFEAF4FF).copy(alpha = 0.55f), tx, top, tx + half * 0.15f, top + tierH * 0.3f, tx - half * 0.55f, top + tierH * 0.4f)
    }
}

private fun DrawScope.drawStructure(
    style: Int, wi: Int, k: Int, salt: Int, xp: Float, base: Float, u: Float, seg: Float,
    col: Color, lit: Color, accent: Color, layer: Int, frame: Int
) {
    val h1 = hsh(k, salt)
    val h2 = hsh(k, salt + 101)
    val h3 = hsh(k, salt + 202)
    val wid = seg * u
    val shade = Color.Black.copy(alpha = 0.22f)
    when (style) {
        0 -> {
            val hh = (1.4f + h1 * 2.2f) * u
            drawOval(col, Offset(xp - wid * 0.1f, base - hh), Size(wid * 1.2f, hh * 2f))
            val n = 1 + (h2 * 3f).toInt()
            for (i in 0 until n) {
                val tx = xp + wid * (0.1f + 0.8f * hsh(k, salt + 10 + i))
                val ty = base - hh * 0.6f
                if (wi == 0) {
                    val th = (1.4f + 1.6f * hsh(k, salt + 30 + i)) * u
                    drawRect(col, Offset(tx - 0.07f * u, ty - th * 0.5f), Size(0.14f * u, th * 0.6f))
                    drawCircle(col, th * 0.38f, Offset(tx, ty - th * 0.75f))
                    drawCircle(col, th * 0.3f, Offset(tx - th * 0.3f, ty - th * 0.55f))
                    drawCircle(col, th * 0.3f, Offset(tx + th * 0.3f, ty - th * 0.55f))
                } else {
                    pine(col, tx, ty, (2.2f + 2.6f * hsh(k, salt + 30 + i)) * u, 3, false)
                }
            }
        }
        1 -> {
            val big = wi == 11 || wi == 7
            val kind = if (big) (h1 * 4f).toInt().coerceIn(0, 3) else (h1 * 3f).toInt().coerceIn(0, 2)
            val bx = xp + wid * 0.5f
            when (kind) {
                0 -> {
                    val tw = (1.1f + h2 * 0.9f) * u
                    val th = (3.5f + h3 * 4.5f) * u
                    drawRect(col, Offset(bx - tw / 2f, base - th), Size(tw, th + 2f))
                    triPx(col, bx - tw * 0.62f, base - th, bx, base - th - tw * 2.6f, bx + tw * 0.62f, base - th)
                    val sw = tw * 0.28f
                    for (sd in intArrayOf(-1, 1)) {
                        val sx = bx + sd * tw * 0.62f
                        drawRect(col, Offset(sx - sw / 2f, base - th * 0.6f), Size(sw, th * 0.6f))
                        triPx(col, sx - sw * 0.8f, base - th * 0.6f, sx, base - th * 0.6f - sw * 3f, sx + sw * 0.8f, base - th * 0.6f)
                    }
                    if (layer >= 1) {
                        val a = 0.5f + 0.2f * sin(frame * 0.05f + k)
                        val wy = base - th * 0.75f
                        drawRect(lit.copy(alpha = a), Offset(bx - tw * 0.12f, wy), Size(tw * 0.24f, tw * 0.5f))
                        triPx(lit.copy(alpha = a), bx - tw * 0.12f, wy, bx, wy - tw * 0.2f, bx + tw * 0.12f, wy)
                    }
                }
                1 -> {
                    val bw = 4.6f * u
                    val bh = (2.2f + h2 * 0.8f) * u
                    drawRect(col, Offset(bx - bw / 2f, base - bh), Size(bw, bh + 2f))
                    drawOval(col, Offset(bx - bw * 0.3f, base - bh - bw * 0.22f), Size(bw * 0.6f, bw * 0.44f))
                    val dy = base - bh - bw * 0.2f
                    triPx(col, bx - bw * 0.05f, dy, bx, dy - bw * 0.45f, bx + bw * 0.05f, dy)
                    for (sd in intArrayOf(-1, 1)) {
                        val sx = bx + sd * bw * 0.5f
                        val sw = bw * 0.07f
                        drawRect(col, Offset(sx - sw, base - bh * 1.5f), Size(sw * 2f, bh * 0.5f + 1f))
                        triPx(col, sx - sw * 1.3f, base - bh * 1.5f, sx, base - bh * 1.5f - bw * 0.28f, sx + sw * 1.3f, base - bh * 1.5f)
                    }
                    if (layer >= 1) {
                        val a = 0.5f + 0.2f * sin(frame * 0.04f + k * 2f)
                        drawRect(lit.copy(alpha = a), Offset(bx - bw * 0.05f, base - bh * 0.85f), Size(bw * 0.1f, bh * 0.5f))
                        triPx(lit.copy(alpha = a), bx - bw * 0.05f, base - bh * 0.85f, bx, base - bh * 0.85f - bw * 0.07f, bx + bw * 0.05f, base - bh * 0.85f)
                    }
                }
                2 -> {
                    val beamY = base - (2.4f + h2 * 1.5f) * u
                    drawRect(col, Offset(xp - 1f, beamY), Size(wid + 2f, 0.45f * u))
                    for (i in 0..1) drawRect(col, Offset(xp + wid * (0.1f + 0.7f * i), beamY), Size(0.7f * u, base - beamY + 2f))
                    var bxx = xp
                    while (bxx < xp + wid) {
                        drawRect(col, Offset(bxx, beamY - 0.3f * u), Size(0.1f * u, 0.3f * u))
                        bxx += 0.4f * u
                    }
                    if (h3 > 0.4f) {
                        val sx = xp + wid * 0.1f + 0.35f * u
                        triPx(col, sx - 0.35f * u, beamY, sx, beamY - 1.8f * u, sx + 0.35f * u, beamY)
                    }
                }
                else -> { // grand château
                    val cw = wid * 1.25f
                    val wh = (2.4f + h2 * 0.8f) * u
                    drawRect(col, Offset(bx - cw / 2f, base - wh), Size(cw, wh + 2f))
                    var tx = bx - cw / 2f
                    while (tx < bx + cw / 2f) {
                        drawRect(col, Offset(tx, base - wh - 0.28f * u), Size(0.22f * u, 0.3f * u))
                        tx += 0.44f * u
                    }
                    val th = [0.0f, 0.0f].size.toFloat()
                    for ((ti, off) in floatArrayOf(-0.42f, 0f, 0.42f).withIndex()) {
                        val tcx = bx + cw * off
                        val tw = (0.9f + 0.5f * (1 - abs(off) * 2f)) * u
                        val tH = (wh + (1.6f + 2.4f * hsh(k, salt + 50 + ti)) * u * (if (off == 0f) 1.5f else 1f))
                        drawRect(col, Offset(tcx - tw / 2f, base - tH), Size(tw, tH + 2f))
                        triPx(col, tcx - tw * 0.65f, base - tH, tcx, base - tH - tw * 2.4f, tcx + tw * 0.65f, base - tH)
                        if (layer >= 1) {
                            val a = 0.5f + 0.25f * sin(frame * 0.05f + ti * 2f + k)
                            drawRect(lit.copy(alpha = a), Offset(tcx - tw * 0.1f, base - tH * 0.82f), Size(tw * 0.2f, tw * 0.45f))
                            drawRect(lit.copy(alpha = a * 0.9f), Offset(tcx - tw * 0.1f, base - tH * 0.55f), Size(tw * 0.2f, tw * 0.45f))
                        }
                    }
                    if (th < 0f) return
                    if (layer >= 1) for (i in 0..2) {
                        val a = 0.45f + 0.2f * sin(frame * 0.06f + i * 1.7f + k)
                        drawRect(lit.copy(alpha = a), Offset(bx - cw * 0.3f + i * cw * 0.3f, base - wh * 0.7f), Size(0.2f * u, 0.4f * u))
                    }
                }
            }
        }
        2 -> {
            val hh = (3.2f + h1 * 5f) * u
            val pk = xp + wid * (0.3f + 0.4f * h2)
            val lx0 = xp - wid * 0.35f
            val rx0 = xp + wid * 1.35f
            triPx(col, lx0, base, pk, base - hh, rx0, base)
            val pk2 = pk + wid * (0.35f + 0.2f * h3)
            val hk2 = hh * (0.55f + 0.2f * h3)
            triPx(col, pk2 - wid * 0.6f, base, pk2, base - hk2, pk2 + wid * 0.6f, base)
            polyPx(shade, pk, base - hh, pk + (rx0 - pk) * 0.18f, base - hh * 0.62f, pk + (rx0 - pk) * 0.08f, base - hh * 0.45f, pk + (rx0 - pk) * 0.3f, base - hh * 0.2f, rx0, base, pk, base)
            if (wi == 5 || wi == 0) {
                val cap = Color(0xFFEAF4FF).copy(alpha = if (wi == 5) 0.5f else 0.28f)
                val topY = base - hh
                polyPx(
                    cap, pk, topY,
                    pk + (rx0 - pk) * 0.32f, topY + hh * 0.32f, pk + (rx0 - pk) * 0.2f, topY + hh * 0.24f,
                    pk + (rx0 - pk) * 0.08f, topY + hh * 0.34f, pk - (pk - lx0) * 0.1f, topY + hh * 0.25f,
                    pk - (pk - lx0) * 0.32f, topY + hh * 0.32f
                )
            }
        }
        3 -> {
            val ln = (1.2f + h1 * 2.8f) * u
            val sx = xp + wid * (0.15f + 0.7f * h2)
            triPx(col, sx - 0.6f * u, base, sx, base - ln, sx + 0.6f * u, base)
            val tl = (1.0f + h3 * 2.5f) * u * (0.6f + 0.3f * layer)
            val tx2 = xp + wid * (0.1f + 0.8f * hsh(k, salt + 5))
            triPx(col, tx2 - 0.55f * u, -2f, tx2, tl, tx2 + 0.55f * u, -2f)
            if (hsh(k, salt + 9) < 0.2f) drawRect(col, Offset(tx2 - 0.35f * u, 0f), Size(0.7f * u, base))
            // goutte d'eau qui tombe de la stalactite
            val dropT = ((frame * 0.6f + k * 37f) % 120f) / 120f
            drawCircle(accent.copy(alpha = 0.6f * (1f - dropT)), 0.05f * u, Offset(tx2, tl + dropT * (base - tl) * 0.8f))
            if (wi == 2 || wi == 6) {
                val cs = sx + 1.1f * u
                val a = 0.5f + 0.25f * sin(frame * 0.04f + k)
                triPx(accent.copy(alpha = a), cs - 0.25f * u, base, cs, base - (0.8f + h3) * u * 1.3f, cs + 0.25f * u, base)
                glowPx(accent, cs, base - 0.5f * u, 1.6f * u, 0.22f)
            }
            if (wi == 12 && h3 > 0.5f) {
                if (((frame / 40 + k) % 3) != 0) {
                    val ex = sx + 0.5f * u
                    val ey = base - ln * 0.5f
                    drawCircle(accent.copy(alpha = 0.8f), 0.05f * u, Offset(ex, ey))
                    drawCircle(accent.copy(alpha = 0.8f), 0.05f * u, Offset(ex + 0.25f * u, ey))
                }
            }
        }
        4 -> {
            val hh = (1f + h1 * 1.4f) * u
            drawOval(col, Offset(xp - wid * 0.3f, base - hh), Size(wid * 1.6f, hh * 2f))
            drawArc(Color.White.copy(alpha = 0.08f), 200f, 110f, false, Offset(xp - wid * 0.3f, base - hh), Size(wid * 1.6f, hh * 2f), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 0.08f * u))
            val kind = (h2 * 4f).toInt()
            val bx = xp + wid * 0.5f
            val top = base - hh
            when (kind) {
                0 -> {
                    val ph = (2.6f + h3 * 1.8f) * u
                    val pw = ph * 1.5f
                    triPx(col, bx - pw / 2f, top + hh * 0.3f, bx, top + hh * 0.3f - ph, bx + pw / 2f, top + hh * 0.3f)
                    triPx(shade, bx, top + hh * 0.3f - ph, bx + pw / 2f, top + hh * 0.3f, bx, top + hh * 0.3f)
                    for (i in 1..3) lineF(Color.Black.copy(alpha = 0.18f), bx - pw / 2f + pw * 0.08f * i + i * 0.0f, top + hh * 0.3f - ph * 0.22f * i, bx + pw / 2f - pw * 0.08f * i, top + hh * 0.3f - ph * 0.22f * i, 0.04f * u)
                }
                1 -> {
                    val ph = (2.2f + h3) * u
                    lineF(col, bx, top + hh * 0.2f, bx + 0.3f * u, top - ph * 0.5f, 0.2f * u)
                    lineF(col, bx + 0.3f * u, top - ph * 0.5f, bx + 0.1f * u, top - ph, 0.16f * u)
                    for (i in 0 until 6) {
                        val a = -0.4f + i * 0.55f
                        lineF(col, bx + 0.1f * u, top - ph, bx + 0.1f * u + cos(a) * 1.1f * u, top - ph - sin(a) * 0.35f * u + abs(sin(a)) * 0.5f * u, 0.07f * u)
                    }
                }
                2 -> {
                    val ph = (2.4f + h3 * 1.6f) * u
                    drawRect(col, Offset(bx, top - ph), Size(0.5f * u, ph + 2f))
                    triPx(col, bx - 0.03f * u, top - ph, bx + 0.25f * u, top - ph - 0.7f * u, bx + 0.53f * u, top - ph)
                }
                else -> {
                    drawRect(col, Offset(bx - 0.9f * u, top - 1.6f * u), Size(0.5f * u, 1.7f * u))
                    drawRect(col, Offset(bx + 0.4f * u, top - 1.2f * u), Size(0.5f * u, 1.3f * u))
                    drawRect(col, Offset(bx - 0.9f * u, top - 1.9f * u), Size(1.8f * u, 0.4f * u))
                }
            }
        }
        5 -> {
            val kind = (h1 * 4f).toInt().coerceIn(0, 3)
            val bx = xp + wid * 0.5f
            when (kind) {
                0 -> {
                    val th = (2.4f + h2 * 2f) * u
                    polyPx(col, bx - 0.28f * u, base, bx - 0.12f * u, base - th * 0.5f, bx + 0.12f * u, base - th * 0.5f, bx + 0.28f * u, base)
                    branch(bx, base - th * 0.45f, 0f, th * 0.35f, 4, col, 0.14f * u, k * 13 + salt)
                }
                1 -> for (i in 0..2) {
                    val gx = bx + (i - 1) * 0.9f * u
                    val gh = (0.9f + 0.5f * hsh(k, salt + 20 + i)) * u
                    drawRect(col, Offset(gx - 0.3f * u, base - gh), Size(0.6f * u, gh + 2f))
                    drawOval(col, Offset(gx - 0.3f * u, base - gh - 0.3f * u), Size(0.6f * u, 0.6f * u))
                    if (hsh(k, salt + 23 + i) > 0.6f) drawRect(shade, Offset(gx - 0.05f * u, base - gh * 0.8f), Size(0.1f * u, gh * 0.35f))
                }
                2 -> {
                    val cw = 2.6f * u
                    val ch = 2f * u
                    drawRect(col, Offset(bx - cw / 2f, base - ch), Size(cw, ch + 2f))
                    triPx(col, bx - cw * 0.58f, base - ch, bx, base - ch - 1.3f * u, bx + cw * 0.58f, base - ch)
                    drawOval(Color.Black.copy(alpha = 0.5f), Offset(bx - 0.4f * u, base - 1.25f * u), Size(0.8f * u, 1.3f * u))
                    if (layer >= 1) {
                        val a = 0.45f + 0.2f * sin(frame * 0.05f + k)
                        drawRect(lit.copy(alpha = a), Offset(bx - cw * 0.38f, base - ch * 0.8f), Size(0.2f * u, 0.4f * u))
                        drawRect(lit.copy(alpha = a), Offset(bx + cw * 0.28f, base - ch * 0.8f), Size(0.2f * u, 0.4f * u))
                    }
                }
                else -> {
                    val ch = (2.2f + h2) * u
                    drawRect(col, Offset(bx - 0.12f * u, base - ch), Size(0.24f * u, ch + 2f))
                    drawRect(col, Offset(bx - 0.5f * u, base - ch * 0.72f), Size(1f * u, 0.22f * u))
                    drawRect(col, Offset(bx - 0.6f * u, base - 0.3f * u), Size(1.2f * u, 0.34f * u))
                }
            }
        }
        6 -> {
            val n = 2 + (h2 * 3f).toInt()
            for (i in 0 until n) {
                val cxp = xp + wid * (0.1f + 0.8f * hsh(k, salt + 70 + i))
                val ch = (1.6f + 3.6f * hsh(k, salt + 80 + i)) * u
                val wd = (0.35f + 0.4f * hsh(k, salt + 90 + i)) * u
                val cc = lerp(col, accent, 0.32f)
                polyPx(cc, cxp - wd, base, cxp - wd * 0.9f, base - ch * 0.7f, cxp + wd * 0.1f, base - ch, cxp + wd * 0.9f, base - ch * 0.7f, cxp + wd, base)
                polyPx(Color.White.copy(alpha = 0.14f), cxp - wd, base, cxp - wd * 0.9f, base - ch * 0.7f, cxp + wd * 0.1f, base - ch, cxp + wd * 0.1f, base)
                val a = 0.18f + 0.1f * sin(frame * 0.04f + i + k)
                glowPx(accent, cxp, base - ch * 0.5f, ch * 0.9f, a)
            }
        }
        7 -> {
            val hh = (4f + h1 * 3f) * u
            val cxv = xp + wid * 0.5f
            val half = wid * 0.75f
            val topW = wid * 0.12f
            polyPx(col, cxv - half, base, cxv - topW, base - hh, cxv + topW, base - hh, cxv + half, base)
            polyPx(shade, cxv, base - hh, cxv + topW, base - hh, cxv + half, base, cxv, base)
            val pulse = 0.8f + 0.2f * sin(frame * 0.06f + k)
            glowPx(Color(0xFFFF7A2A), cxv, base - hh, 3.2f * u, 0.5f * pulse)
            drawOval(Color(0xFFFF8A30).copy(alpha = 0.9f), Offset(cxv - topW, base - hh - 0.14f * u), Size(topW * 2f, 0.3f * u))
            for (i in 0..2) {
                var lx = cxv + (i - 1) * topW * 0.8f
                var ly = base - hh
                for (j in 0 until 4) {
                    val nx = lx + (hsh(k * 7 + i, j + salt) - 0.4f) * 0.7f * u + (i - 1) * 0.35f * u
                    val ny = ly + hh * 0.22f
                    lineF(Color(0xFFFF7A2A).copy(alpha = 0.75f * pulse), lx, ly, nx, ny, 0.08f * u)
                    lx = nx
                    ly = ny
                }
            }
            for (i in 0..3) {
                val t = ((frame * 0.4f + i * 50f + k * 17f) % 200f) / 200f
                val sx = cxv + sin(t * 3f + i) * 0.6f * u + t * 0.9f * u
                val sy = base - hh - t * 3.2f * u
                drawCircle(Color(0xFF3A2A28).copy(alpha = 0.32f * (1f - t)), (0.4f + t * 1.1f) * u, Offset(sx, sy))
            }
            if (((frame / 60 + k) % 9) < 2) {
                for (i in 0 until 12) {
                    val a = -1.57f + (hsh(i, k) - 0.5f) * 1.6f
                    val tt = ((frame % 60) / 60f + i * 0.02f).coerceIn(0f, 1f)
                    val sp = (2f + 3f * hsh(i, k + 5)) * u
                    drawCircle(Color(0xFFFFB04A).copy(alpha = 1f - tt), 0.07f * u, Offset(cxv + cos(a) * sp * tt, base - hh + sin(a) * sp * tt + tt * tt * 2f * u))
                }
            }
        }
        8 -> {
            val kind = (h1 * 4f).toInt().coerceIn(0, 3)
            val bx = xp + wid * 0.5f
            when (kind) {
                0 -> for (i in 0..2) {
                    val cxp = bx + (i - 1) * 1.1f * u
                    val ch = (2.4f + 1.8f * hsh(k, salt + 20 + i)) * u
                    val broken = hsh(k, salt + 30 + i) > 0.5f
                    drawRect(col, Offset(cxp - 0.25f * u, base - ch), Size(0.5f * u, ch + 2f))
                    drawRect(col, Offset(cxp - 0.35f * u, base - 0.2f * u), Size(0.7f * u, 0.25f * u))
                    if (broken) polyPx(col, cxp - 0.25f * u, base - ch, cxp - 0.1f * u, base - ch - 0.3f * u, cxp + 0.05f * u, base - ch - 0.1f * u, cxp + 0.25f * u, base - ch - 0.4f * u, cxp + 0.25f * u, base - ch)
                    else drawRect(col, Offset(cxp - 0.38f * u, base - ch - 0.2f * u), Size(0.76f * u, 0.22f * u))
                }
                1 -> {
                    val ah = (3f + h2 * 1.2f) * u
                    drawRect(col, Offset(bx - 1.5f * u, base - ah), Size(0.55f * u, ah + 2f))
                    drawRect(col, Offset(bx + 0.95f * u, base - ah * 0.8f), Size(0.55f * u, ah * 0.8f + 2f))
                    drawArc(col, 180f, 150f, false, Offset(bx - 1.5f * u, base - ah - 1.2f * u), Size(3f * u, 2.4f * u), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 0.5f * u))
                }
                2 -> {
                    val wh = (2f + h2 * 1.4f) * u
                    drawRect(col, Offset(bx - 1.4f * u, base - wh), Size(2.8f * u, wh + 2f))
                    drawOval(Color.Black.copy(alpha = 0.45f), Offset(bx - 0.35f * u, base - wh * 0.7f), Size(0.7f * u, wh * 0.45f))
                    polyPx(col, bx - 1.4f * u, base - wh, bx - 1.0f * u, base - wh - 0.4f * u, bx - 0.6f * u, base - wh - 0.1f * u, bx - 0.2f * u, base - wh - 0.5f * u, bx, base - wh)
                }
                else -> {
                    drawRect(col, Offset(bx - 0.5f * u, base - 0.4f * u), Size(1.1f * u, 0.45f * u))
                    drawRect(col, Offset(bx - 0.25f * u, base - 1.6f * u), Size(0.5f * u, 1.2f * u))
                    drawCircle(col, 0.22f * u, Offset(bx, base - 1.8f * u))
                    drawRect(col, Offset(bx - 0.6f * u, base - 1.5f * u), Size(0.35f * u, 0.14f * u))
                }
            }
            if (layer >= 1) {
                val bob = sin(frame * 0.03f + k) * 0.2f * u
                drawOval(col, Offset(xp + wid * 0.1f, base - 4.2f * u + bob), Size(0.7f * u, 0.4f * u))
                drawOval(col, Offset(xp + wid * 0.8f, base - 3.6f * u - bob), Size(0.5f * u, 0.3f * u))
            }
        }
        9 -> {
            val kind = (h1 * 4f).toInt().coerceIn(0, 3)
            val bx = xp + wid * 0.5f
            when (kind) {
                0 -> for (i in 0..2) {
                    val kx = bx + (i - 1) * 0.7f * u
                    for (j in 0 until 8) {
                        val a = sin(frame * 0.03f + j * 0.5f + k + i) * j * 0.06f * u
                        val b2 = sin(frame * 0.03f + (j + 1) * 0.5f + k + i) * (j + 1) * 0.06f * u
                        lineF(col, kx + a, base - j * 0.5f * u, kx + b2, base - (j + 1) * 0.5f * u, (0.16f - j * 0.015f) * u)
                    }
                }
                1 -> {
                    branch(bx - 0.5f * u, base, 0.15f, 1.0f * u, 3, lerp(col, accent, 0.35f), 0.16f * u, k + salt)
                    branch(bx + 0.5f * u, base, -0.1f, 1.3f * u, 3, lerp(col, accent, 0.25f), 0.18f * u, k + salt + 9)
                }
                2 -> {
                    val tw = (1.0f + h2 * 0.8f) * u
                    val th = (3.2f + h3 * 3f) * u
                    drawRect(col, Offset(bx - tw / 2f, base - th), Size(tw, th + 2f))
                    triPx(col, bx - tw * 0.62f, base - th, bx, base - th - tw * 2.4f, bx + tw * 0.62f, base - th)
                    val a = 0.4f + 0.2f * sin(frame * 0.05f + k)
                    drawRect(lit.copy(alpha = a), Offset(bx - tw * 0.1f, base - th * 0.7f), Size(tw * 0.2f, tw * 0.4f))
                }
                else -> {
                    val hl = 3.6f * u
                    polyPx(col, bx - hl / 2f, base, bx - hl * 0.45f, base - 1.1f * u, bx + hl * 0.4f, base - 1.5f * u, bx + hl / 2f, base - 0.3f * u)
                    drawRect(col, Offset(bx - 0.06f * u, base - 3.4f * u), Size(0.12f * u, 2.4f * u))
                    polyPx(col, bx + 0.08f * u, base - 3.2f * u, bx + 1.2f * u, base - 2.5f * u, bx + 0.08f * u, base - 1.5f * u)
                }
            }
        }
        10 -> {
            val n = 1 + (h2 * 2f).toInt()
            for (i in 0 until n) {
                val tx = xp + wid * (0.15f + 0.7f * hsh(k, salt + 40 + i))
                val tl = (3.5f + 3f * hsh(k, salt + 50 + i)) * u
                val segs = 14
                var px0 = tx
                var py0 = base
                for (j in 1..segs) {
                    val t = j.toFloat() / segs
                    val nx = tx + sin(frame * 0.02f + j * 0.4f + k + i) * t * 0.9f * u + t * t * 0.8f * u * (if (i % 2 == 0) 1f else -1f)
                    val ny = base - t * tl
                    lineF(col, px0, py0, nx, ny, (0.4f - t * 0.36f) * u)
                    if (j % 3 == 0) drawCircle(accent.copy(alpha = 0.35f), 0.05f * u, Offset(nx, ny))
                    px0 = nx
                    py0 = ny
                }
                glowPx(accent, px0, py0, 0.8f * u, 0.4f)
            }
        }
        else -> {
            val n = 2 + (h2 * 2f).toInt()
            for (i in 0 until n) {
                val tx = xp + wid * (0.1f + 0.8f * hsh(k, salt + 30 + i))
                pine(col, tx, base, (3f + 3.5f * hsh(k, salt + 40 + i)) * u, 5, wi == 5)
            }
        }
    }
    if (style == 12) {
        // (non utilisé) – marais géré ci-dessous
    }
}

private fun DrawScope.drawSwamp(k: Int, salt: Int, xp: Float, base: Float, u: Float, seg: Float, col: Color, accent: Color, frame: Int) {
    val h1 = hsh(k, salt)
    val h2 = hsh(k, salt + 101)
    val wid = seg * u
    val bx = xp + wid * 0.5f
    val th = (2.4f + h1 * 2.2f) * u
    polyPx(col, bx - 0.4f * u, base, bx - 0.14f * u, base - th * 0.55f, bx + 0.14f * u, base - th * 0.55f, bx + 0.4f * u, base)
    branch(bx, base - th * 0.5f, 0f, th * 0.34f, 4, col, 0.16f * u, k * 11 + salt)
    for (i in 0 until 5) {
        val mx = bx + (hsh(k, salt + 60 + i) - 0.5f) * 1.6f * u
        val my = base - th * (0.5f + 0.35f * hsh(k, salt + 70 + i))
        val ml = (0.4f + 0.7f * hsh(k, salt + 80 + i)) * u
        lineF(accent.copy(alpha = 0.28f), mx, my, mx + sin(frame * 0.03f + i + k) * 0.08f * u, my + ml, 0.05f * u)
    }
    for (i in 0 until 6) {
        val rx = xp + wid * (0.05f + 0.9f * hsh(k, salt + 90 + i))
        val rh = (0.9f + 0.8f * hsh(k, salt + 95 + i)) * u
        val sw = sin(frame * 0.03f + i + k) * 0.06f * u
        lineF(col, rx, base, rx + sw, base - rh, 0.05f * u)
        if (h2 > 0.5f && i % 2 == 0) drawOval(col, Offset(rx + sw - 0.05f * u, base - rh - 0.2f * u), Size(0.1f * u, 0.28f * u))
    }
}

// ════════════════════════════════════════════════════════════════
//  Fond complet
// ════════════════════════════════════════════════════════════════

internal fun DrawScope.drawWorldBackdrop(wi0: Int, w: KWorld, cx: Float, cy: Float, s: Float, viewW: Float, frame: Int) {
    val wi = wi0.coerceIn(0, 12)
    val flash = lightningLevel(wi, frame)
    val fcol = flashColor(wi)
    val sky1 = lerp(Color(w.sky1), fcol, flash * 0.5f)
    val sky2b = Color(w.sky2)
    val sky2 = lerp(sky2b, fcol, flash * 0.65f)
    drawRect(Brush.verticalGradient(listOf(sky1, sky2)), Offset.Zero, size)
    val halo = lerp(sky2b, Color.White, 0.6f)

    if (STARS[wi]) drawStars(frame, wi, 1f - flash * 0.7f)
    drawCelestial(CELESTIAL[wi], wi, frame)
    if (AURORA[wi] > 0) drawAurora(frame)
    if (CLOUDS[wi]) drawClouds(wi, frame, flash)
    drawFlyers(FLYERS[wi], frame, s)
    if (wi == 12) drawAbyssEyes(frame, s)

    // faisceaux de lumière
    val shaftA = if (wi == 4 || wi == 8) 0f else if (wi == 10) 0.2f else 0.12f
    if (shaftA > 0f) {
        val count = if (wi == 10) 5 else 3
        for (i in 0 until count) {
            val x0 = size.width * (0.12f + 0.8f * i / count) - cx * s * 0.05f
            val sway = sin(frame * 0.008f + i * 2f) * s * 0.4f
            val p = Path()
            p.moveTo(x0, 0f)
            p.lineTo(x0 + s * 1.4f, 0f)
            p.lineTo(x0 + s * 3.6f + sway, size.height * 0.85f)
            p.lineTo(x0 + s * 1.6f + sway, size.height * 0.85f)
            p.close()
            drawPath(p, Brush.verticalGradient(listOf(halo.copy(alpha = shaftA), Color.Transparent), startY = 0f, endY = size.height * 0.85f))
        }
    }

    val lit = lerp(halo, Color(0xFFFFE9B0), 0.55f)
    val accent = worldAccent(wi)
    val fog = lerp(sky2b, Color.White, 0.28f)
    val ls = LAYERS[wi]
    for (layer in 0..2) {
        val style = ls[layer]
        val factor = 0.10f + 0.12f * layer
        val off = cx * factor
        val seg = 7f - layer
        val first = floor(off / seg).toInt() - 1
        val col = lerp(sky2b, Color.Black, 0.38f + 0.2f * layer)
        val base = (11.5f - cy * (0.6f + 0.2f * layer) - (2 - layer) * 0.7f) * s
        for (kk in first..first + (viewW / seg).toInt() + 3) {
            val xp = (kk * seg - off) * s
            if (style == 12) drawSwamp(kk, wi * 7 + layer, xp, base, s, seg, col, accent, frame)
            else drawStructure(style, wi, kk, wi * 7 + layer, xp, base, s, seg, col, lit, accent, layer, frame)
        }
        val fh = 5f * s
        drawRect(
            Brush.verticalGradient(listOf(Color.Transparent, fog.copy(alpha = 0.30f + 0.05f * layer)), startY = base - fh, endY = base),
            Offset(0f, base - fh), Size(size.width, fh)
        )
        if (base < size.height) drawRect(col, Offset(0f, base), Size(size.width, size.height - base))
        if (layer == 0) drawBolt(frame, wi, s, flash, fcol)
    }
}

// ════════════════════════════════════════════════════════════════
//  Météo et atmosphère au premier plan
// ════════════════════════════════════════════════════════════════

private fun DrawScope.drawParticles(kind: Int, cx: Float, s: Float, viewW: Float, viewH: Float, frame: Int) {
    val span = viewW + 10f
    when (kind) {
        1 -> for (i in 0 until 90) {
            val par = 0.8f + 0.3f * hsh(i, 5)
            val x = wrapF(hsh(i, 1) * span - cx * par, span) - 5f
            val y = wrapF(hsh(i, 3) * viewH + frame * (0.28f + 0.2f * hsh(i, 2)), viewH)
            lineF(Color(0xFFBFD0FF).copy(alpha = 0.32f), x * s, y * s, (x - 0.12f) * s, (y + 0.55f) * s, max(1f, 0.03f * s))
        }
        2 -> for (i in 0 until 70) {
            val x = wrapF(hsh(i, 1) * span - cx * 0.7f + sin(frame * 0.02f + i) * 0.5f, span) - 5f
            val y = wrapF(hsh(i, 3) * viewH + frame * (0.02f + 0.03f * hsh(i, 2)), viewH)
            drawCircle(Color.White.copy(alpha = 0.8f), (0.03f + 0.04f * hsh(i, 4)) * s, Offset(x * s, y * s))
        }
        3 -> for (i in 0 until 50) {
            val x = wrapF(hsh(i, 1) * span - cx * 0.6f + sin(frame * 0.03f + i) * 0.4f, span) - 5f
            val y = viewH - wrapF(hsh(i, 3) * viewH + frame * (0.012f + 0.02f * hsh(i, 2)), viewH)
            val a = 0.5f + 0.4f * sin(frame * 0.15f + i)
            glowPx(Color(0xFFFF7A2A), x * s, y * s, 0.28f * s, 0.35f * a)
            drawCircle(Color(0xFFFFC060).copy(alpha = a), (0.02f + 0.025f * hsh(i, 4)) * s, Offset(x * s, y * s))
        }
        4 -> {
            drawRect(Color(0xFFD8A860).copy(alpha = 0.08f), Offset.Zero, size)
            for (i in 0 until 40) {
                val x = wrapF(hsh(i, 1) * span + frame * (0.25f + 0.3f * hsh(i, 2)), span) - 5f
                val y = hsh(i, 3) * viewH
                val len = 1.2f + 1.4f * hsh(i, 4)
                lineF(Color(0xFFEBC27A).copy(alpha = 0.2f), x * s, y * s, (x + len) * s, (y + 0.05f) * s, max(1f, 0.03f * s))
            }
        }
        5 -> for (i in 0 until 22) {
            val x = wrapF(hsh(i, 1) * span - cx * 0.6f + sin(frame * 0.01f * (1f + hsh(i, 6)) + i) * 1.2f, span) - 5f
            val y = hsh(i, 3) * viewH * 0.8f + viewH * 0.2f + cos(frame * 0.012f + i * 1.3f) * 0.8f
            val a = 0.5f + 0.5f * sin(frame * 0.05f + i * 2f)
            glowPx(Color(0xFFD8FF6A), x * s, y * s, 0.3f * s, 0.5f * a)
            drawCircle(Color(0xFFF4FFB0).copy(alpha = 0.5f + 0.5f * a), 0.035f * s, Offset(x * s, y * s))
        }
        6 -> for (i in 0 until 30) {
            val x = wrapF(hsh(i, 1) * span - cx * 0.6f + sin(frame * 0.03f + i) * 0.3f, span) - 5f
            val y = viewH - wrapF(hsh(i, 3) * viewH + frame * (0.01f + 0.02f * hsh(i, 2)), viewH)
            val r = (0.04f + 0.08f * hsh(i, 4)) * s
            drawCircle(Color(0xFFBFF8FF).copy(alpha = 0.5f), r, Offset(x * s, y * s), style = androidx.compose.ui.graphics.drawscope.Stroke(width = max(1f, r * 0.2f)))
            drawCircle(Color.White.copy(alpha = 0.6f), r * 0.2f, Offset(x * s - r * 0.35f, y * s - r * 0.35f))
        }
        7 -> for (i in 0 until 30) {
            val x = wrapF(hsh(i, 1) * span - cx * 0.5f + frame * 0.01f * (1f + hsh(i, 2)), span) - 5f
            val y = wrapF(hsh(i, 3) * viewH + sin(frame * 0.02f + i) * 0.4f + frame * 0.004f, viewH)
            val a = 0.25f + 0.35f * (0.5f + 0.5f * sin(frame * 0.03f + i * 1.7f))
            drawCircle(Color(0xFFFFE9A0).copy(alpha = a), (0.02f + 0.03f * hsh(i, 4)) * s, Offset(x * s, y * s))
        }
        8 -> for (i in 0 until 30) {
            val x = wrapF(hsh(i, 1) * span - cx * 0.5f + sin(frame * 0.02f + i) * 0.4f, span) - 5f
            val y = viewH - wrapF(hsh(i, 3) * viewH + frame * (0.006f + 0.01f * hsh(i, 2)), viewH)
            val a = 0.3f + 0.4f * (0.5f + 0.5f * sin(frame * 0.04f + i))
            glowPx(Color(0xFF6FE3FF), x * s, y * s, 0.2f * s, 0.3f * a)
            drawCircle(Color(0xFFBFF4FF).copy(alpha = a), 0.03f * s, Offset(x * s, y * s))
        }
        9 -> for (i in 0 until 26) {
            val x = wrapF(hsh(i, 1) * span - cx * (0.3f + 0.4f * hsh(i, 5)), span) - 5f
            val y = hsh(i, 3) * viewH
            val a = max(0f, sin(frame * 0.05f + i * 1.9f))
            val r = (0.05f + 0.07f * hsh(i, 4)) * s * a
            lineF(Color(0xFFDDE8FF).copy(alpha = a), x * s - r, y * s, x * s + r, y * s, 1.5f)
            lineF(Color(0xFFDDE8FF).copy(alpha = a), x * s, y * s - r, x * s, y * s + r, 1.5f)
        }
        10 -> for (i in 0 until 8) {
            val bx = wrapF(hsh(i, 1) * span - cx * 0.6f + sin(frame * 0.012f + i * 2f) * 1.5f, span) - 5f
            val by = viewH * 0.5f + hsh(i, 3) * viewH * 0.4f + sin(frame * 0.03f + i) * 0.4f
            glowPx(Color(0xFF8CFF6A), bx * s, by * s, 0.7f * s, 0.4f)
            drawCircle(Color(0xFFD8FFB0), 0.09f * s, Offset(bx * s, by * s))
            for (t in 1..3) {
                val tx = bx - t * 0.18f
                val ty = by + sin(frame * 0.03f + i - t * 0.4f) * 0.05f * t
                drawCircle(Color(0xFF8CFF6A).copy(alpha = 0.3f - t * 0.07f), (0.07f - t * 0.012f) * s, Offset(tx * s, ty * s))
            }
        }
        11 -> for (i in 0 until 30) {
            val x = wrapF(hsh(i, 1) * span - cx * 0.5f + sin(frame * 0.015f + i) * 0.6f, span) - 5f
            val y = viewH - wrapF(hsh(i, 3) * viewH + frame * (0.005f + 0.008f * hsh(i, 2)), viewH)
            val a = 0.25f + 0.45f * (0.5f + 0.5f * sin(frame * 0.035f + i * 1.3f))
            glowPx(Color(0xFFFF3D6E), x * s, y * s, 0.3f * s, 0.35f * a)
            drawCircle(Color(0xFFFFA0C0).copy(alpha = a), (0.02f + 0.03f * hsh(i, 4)) * s, Offset(x * s, y * s))
        }
    }
}

internal fun DrawScope.drawWorldWeather(wi0: Int, w: KWorld, cx: Float, s: Float, viewW: Float, viewH: Float, frame: Int) {
    val wi = wi0.coerceIn(0, 12)
    val fog = lerp(Color(w.sky2), Color.White, 0.3f)
    for (i in 0..2) {
        val period = size.width * 1.7f
        val x = ((frame * 0.25f * (i + 1) + i * 420f) % period) - size.width * 0.5f
        drawOval(fog.copy(alpha = 0.06f), Offset(x, size.height * (0.5f + 0.12f * i)), Size(size.width * 0.75f, s * 1.7f))
    }
    drawParticles(WEATHER[wi], cx, s, viewW, viewH, frame)

    if (CEILING[wi]) {
        val period = viewW + 6f
        for (k in 0..9) {
            var xx = k * 3.3f - cx * 1.2f + hsh(k, wi) * 1.5f
            xx = wrapF(xx, period) - 2f
            val len = 0.7f + 1.8f * hsh(k, wi + 9)
            val ww = 0.45f + 0.7f * hsh(k, wi + 4)
            triPx(INK, (xx - ww / 2f) * s, -2f, xx * s, len * s, (xx + ww / 2f) * s, -2f)
        }
    }

    val fl = lightningLevel(wi, frame)
    if (fl > 0f) drawRect(flashColor(wi).copy(alpha = 0.2f * fl), Offset.Zero, size)
}

// ════════════════════════════════════════════════════════════════
//  Objets de décor posés sur le sol (3 par monde)
// ════════════════════════════════════════════════════════════════

internal fun KCam.worldProp(wi0: Int, kind: Int, x: Float, gy: Float, frame: Int, seed: Int) {
    val wi = wi0.coerceIn(0, 12)
    val ink = Color(0xFF0B0B16)
    val acc = worldAccent(wi)
    val sw = sin(frame * 0.03f + seed) * 0.05f
    val bone = Color(0xFFD8D2BC)
    when (wi) {
        0 -> when (kind) {
            0 -> for (i in -1..1) {
                val fx = x + i * 0.2f
                val fh = 0.3f + 0.15f * hsh(seed, i + 3)
                line(fx, gy, fx + sw, gy - fh, Color(0xFF2E7D32), 0.04f)
                val pc = when (i) { -1 -> Color(0xFFFF6B8A); 0 -> Color(0xFFFFE066); else -> Color(0xFFFFFFFF) }
                circ(fx + sw, gy - fh, 0.08f, pc)
                circ(fx + sw, gy - fh, 0.035f, Color(0xFFFFB300))
            }
            1 -> {
                circ(x - 0.2f, gy - 0.2f, 0.26f, ink)
                circ(x + 0.15f, gy - 0.22f, 0.3f, ink)
                circ(x - 0.2f, gy - 0.2f, 0.22f, Color(0xFF2C7A38))
                circ(x + 0.15f, gy - 0.22f, 0.26f, Color(0xFF348A42))
                circ(x + 0.1f, gy - 0.32f, 0.08f, Color(0xFF5CC06A))
            }
            else -> {
                oval(x - 0.28f, gy - 0.28f, 0.56f, 0.34f, ink)
                oval(x - 0.24f, gy - 0.25f, 0.48f, 0.28f, Color(0xFF7A7F8A))
                oval(x - 0.16f, gy - 0.22f, 0.2f, 0.1f, Color(0xFFA0A6B0))
            }
        }
        1 -> when (kind) {
            0 -> {
                val a = 0.7f + 0.3f * sin(frame * 0.05f + seed)
                glow(x, gy - 0.25f, 1.3f, acc, 0.3f * a)
                rect(x - 0.04f, gy - 0.3f, 0.08f, 0.3f, Color(0xFFD8D0C0))
                oval(x - 0.22f, gy - 0.5f, 0.44f, 0.26f, acc)
                circ(x - 0.08f, gy - 0.4f, 0.04f, Color.White.copy(alpha = 0.8f))
                circ(x + 0.09f, gy - 0.36f, 0.03f, Color.White.copy(alpha = 0.8f))
            }
            1 -> {
                for (i in -1..1) {
                    val fx = x + i * 0.22f
                    val fh = 0.28f + 0.3f * hsh(seed, i + 6)
                    rect(fx - 0.03f, gy - fh, 0.06f, fh, Color(0xFFC8C0B0))
                    oval(fx - 0.12f, gy - fh - 0.1f, 0.24f, 0.16f, Color(0xFF6A3A8A))
                }
            }
            else -> {
                line(x, gy, x - 0.3f, gy - 0.25f, ink, 0.08f)
                line(x - 0.3f, gy - 0.25f, x - 0.5f, gy - 0.05f, ink, 0.05f)
                line(x, gy, x + 0.25f, gy - 0.35f, ink, 0.08f)
                line(x + 0.25f, gy - 0.35f, x + 0.45f, gy - 0.1f, ink, 0.05f)
            }
        }
        2 -> when (kind) {
            0 -> {
                glow(x, gy - 0.5f, 2.6f, Color(0xFFFFA040), 0.3f)
                rect(x - 0.05f, gy - 0.7f, 0.1f, 0.7f, ink)
                poly(ink, x - 0.22f, gy - 0.7f, x + 0.22f, gy - 0.7f, x + 0.14f, gy - 0.5f, x - 0.14f, gy - 0.5f)
                val fl = 0.8f + 0.2f * sin(frame * 0.4f + seed)
                tri(x - 0.14f, gy - 0.7f, x + sw, gy - 0.7f - 0.35f * fl, x + 0.14f, gy - 0.7f, Color(0xFFFF8A20))
                tri(x - 0.07f, gy - 0.7f, x + sw, gy - 0.7f - 0.2f * fl, x + 0.07f, gy - 0.7f, Color(0xFFFFE066))
            }
            1 -> crystalCluster(x, gy, acc, frame, seed)
            else -> for (i in -1..1) {
                val hgt = 0.3f + 0.4f * hsh(seed, i + 12)
                tri(x + i * 0.2f - 0.12f, gy, x + i * 0.2f, gy - hgt, x + i * 0.2f + 0.12f, gy, Color(0xFF2A2638))
            }
        }
        3 -> when (kind) {
            0 -> for (i in -2..2) {
                val rx = x + i * 0.14f
                val rh = 0.5f + 0.3f * hsh(seed, i + 20)
                line(rx, gy, rx + sw + i * 0.03f, gy - rh, Color(0xFF2E3A1E), 0.035f)
                if (i % 2 == 0) oval(rx + sw - 0.04f, gy - rh - 0.12f, 0.09f, 0.22f, Color(0xFF4A3524))
            }
            1 -> {
                oval(x - 0.22f, gy - 0.22f, 0.44f, 0.3f, ink)
                oval(x - 0.18f, gy - 0.2f, 0.36f, 0.24f, bone)
                circ(x - 0.07f, gy - 0.1f, 0.05f, ink)
                circ(x + 0.07f, gy - 0.1f, 0.05f, ink)
            }
            else -> {
                oval(x - 0.3f, gy - 0.1f, 0.6f, 0.14f, Color(0xFF3A4A20))
                val b = ((frame + seed * 13) % 90) / 90f
                circ(x - 0.1f + b * 0.1f, gy - 0.1f - b * 0.35f, 0.05f * (1f - b * 0.5f), acc.copy(alpha = 1f - b))
            }
        }
        4 -> when (kind) {
            0 -> {
                rect(x - 0.1f, gy - 0.85f, 0.2f, 0.85f, ink)
                rect(x - 0.07f, gy - 0.82f, 0.14f, 0.8f, Color(0xFF3E7A3A))
                rect(x - 0.38f, gy - 0.6f, 0.3f, 0.12f, ink)
                rect(x - 0.38f, gy - 0.85f, 0.12f, 0.37f, ink)
                rect(x - 0.35f, gy - 0.82f, 0.08f, 0.32f, Color(0xFF3E7A3A))
                rect(x + 0.08f, gy - 0.5f, 0.3f, 0.12f, ink)
                rect(x + 0.26f, gy - 0.72f, 0.12f, 0.3f, ink)
                rect(x + 0.28f, gy - 0.7f, 0.08f, 0.26f, Color(0xFF3E7A3A))
            }
            1 -> for (i in 0..3) {
                val a = -0.5f + i * 0.3f
                line(x, gy - 0.05f, x + cos(a) * 0.4f, gy - 0.05f - sin(a + 1.2f) * 0.3f, bone, 0.05f)
            }
            else -> {
                oval(x - 0.22f, gy - 0.25f, 0.44f, 0.32f, ink)
                oval(x - 0.18f, gy - 0.22f, 0.36f, 0.26f, bone)
                circ(x - 0.07f, gy - 0.12f, 0.05f, ink)
                circ(x + 0.07f, gy - 0.12f, 0.05f, ink)
            }
        }
        5 -> when (kind) {
            0 -> {
                oval(x - 0.4f, gy - 0.3f, 0.8f, 0.4f, Color(0xFFEAF4FF))
                oval(x - 0.28f, gy - 0.4f, 0.5f, 0.3f, Color.White)
                oval(x - 0.35f, gy - 0.05f, 0.7f, 0.1f, Color(0xFFB0C8E0))
            }
            1 -> for (i in -1..1) {
                val hgt = 0.3f + 0.35f * hsh(seed, i + 30)
                tri(x + i * 0.16f - 0.07f, gy, x + i * 0.16f, gy - hgt, x + i * 0.16f + 0.07f, gy, Color(0xFFBFE4FF))
                tri(x + i * 0.16f - 0.02f, gy, x + i * 0.16f, gy - hgt + 0.05f, x + i * 0.16f + 0.04f, gy, Color.White.copy(alpha = 0.7f))
            }
            else -> {
                line(x, gy, x, gy - 0.7f, ink, 0.06f)
                for (j in 0..2) tri(x - 0.2f + j * 0.02f, gy - 0.2f - j * 0.18f, x, gy - 0.42f - j * 0.18f, x + 0.2f - j * 0.02f, gy - 0.2f - j * 0.18f, ink)
            }
        }
        6 -> crystalCluster(x, gy, acc, frame, seed)
        7 -> when (kind) {
            0 -> {
                rect(x - 0.22f, gy - 0.62f, 0.44f, 0.62f, ink)
                oval(x - 0.22f, gy - 0.78f, 0.44f, 0.34f, ink)
                rect(x - 0.18f, gy - 0.6f, 0.36f, 0.58f, Color(0xFF6A6E7C))
                oval(x - 0.18f, gy - 0.74f, 0.36f, 0.28f, Color(0xFF6A6E7C))
                rect(x - 0.03f, gy - 0.5f, 0.06f, 0.26f, Color(0xFF3A3E4A))
                rect(x - 0.1f, gy - 0.42f, 0.2f, 0.06f, Color(0xFF3A3E4A))
            }
            1 -> {
                rect(x - 0.05f, gy - 0.8f, 0.1f, 0.8f, ink)
                rect(x - 0.22f, gy - 0.6f, 0.44f, 0.1f, ink)
                rect(x - 0.03f, gy - 0.78f, 0.06f, 0.76f, Color(0xFF6A6E7C))
                rect(x - 0.2f, gy - 0.58f, 0.4f, 0.06f, Color(0xFF6A6E7C))
            }
            else -> {
                rect(x - 0.04f, gy - 0.25f, 0.08f, 0.25f, Color(0xFFE8E0C8))
                val fl = 0.8f + 0.2f * sin(frame * 0.3f + seed)
                glow(x, gy - 0.35f, 1.1f, Color(0xFFFFC060), 0.3f * fl)
                tri(x - 0.05f, gy - 0.25f, x + sw, gy - 0.25f - 0.14f * fl, x + 0.05f, gy - 0.25f, Color(0xFFFFB030))
            }
        }
        8 -> when (kind) {
            0 -> {
                val pulse = 0.7f + 0.3f * sin(frame * 0.08f + seed)
                glow(x, gy - 0.2f, 1.6f, Color(0xFFFF6A20), 0.3f * pulse)
                oval(x - 0.35f, gy - 0.35f, 0.7f, 0.45f, ink)
                oval(x - 0.3f, gy - 0.32f, 0.6f, 0.38f, Color(0xFF3A2220))
                line(x - 0.2f, gy - 0.15f, x, gy - 0.25f, Color(0xFFFF7A2A), 0.04f)
                line(x, gy - 0.25f, x + 0.2f, gy - 0.12f, Color(0xFFFF7A2A), 0.04f)
            }
            1 -> {
                for (i in -1..1) {
                    val hgt = 0.3f + 0.45f * hsh(seed, i + 40)
                    tri(x + i * 0.2f - 0.13f, gy, x + i * 0.2f, gy - hgt, x + i * 0.2f + 0.13f, gy, Color(0xFF2A1A1A))
                }
            }
            else -> {
                oval(x - 0.2f, gy - 0.1f, 0.4f, 0.12f, Color(0xFFFF6A20))
                for (i in 0..2) {
                    val b = (((frame + seed * 19 + i * 20) % 60) / 60f)
                    circ(x + sin(b * 6f + i) * 0.15f, gy - 0.1f - b * 0.9f, 0.04f * (1f - b), Color(0xFFFFB04A).copy(alpha = 1f - b))
                }
            }
        }
        9 -> when (kind) {
            0 -> {
                rect(x - 0.28f, gy - 0.7f, 0.56f, 0.7f, ink)
                rect(x - 0.24f, gy - 0.66f, 0.48f, 0.64f, Color(0xFF8A8676))
                poly(Color(0xFF8A8676), x - 0.24f, gy - 0.7f, x - 0.1f, gy - 0.85f, x + 0.05f, gy - 0.72f, x + 0.24f, gy - 0.9f, x + 0.24f, gy - 0.7f)
                line(x - 0.1f, gy - 0.6f, x - 0.1f, gy - 0.1f, Color(0xFF6A6656), 0.03f)
                line(x + 0.1f, gy - 0.6f, x + 0.1f, gy - 0.1f, Color(0xFF6A6656), 0.03f)
            }
            1 -> {
                oval(x - 0.3f, gy - 0.18f, 0.6f, 0.22f, Color(0xFF6A6656))
                oval(x - 0.1f, gy - 0.3f, 0.3f, 0.2f, Color(0xFF8A8676))
                oval(x - 0.4f, gy - 0.1f, 0.25f, 0.14f, Color(0xFF7A7666))
            }
            else -> {
                rect(x - 0.2f, gy - 0.25f, 0.4f, 0.25f, ink)
                rect(x - 0.17f, gy - 0.22f, 0.34f, 0.22f, Color(0xFF8A8676))
                circ(x, gy - 0.42f, 0.17f, ink)
                circ(x, gy - 0.42f, 0.14f, Color(0xFF9A9686))
                rect(x - 0.06f, gy - 0.3f, 0.12f, 0.08f, Color(0xFF9A9686))
            }
        }
        10 -> when (kind) {
            0 -> {
                branch(x, gy, Color(0xFFFF7A9A), 0.5f)
                branch(x + 0.22f, gy, Color(0xFFFFB06A), 0.36f)
            }
            1 -> for (i in -1..1) {
                val sx = x + i * 0.14f
                var px0 = sx
                var py0 = gy
                for (j in 1..5) {
                    val nx = sx + sin(frame * 0.04f + j * 0.7f + i + seed) * j * 0.03f
                    val ny = gy - j * 0.14f
                    line(px0, py0, nx, ny, Color(0xFF2E8A4A), 0.05f)
                    px0 = nx
                    py0 = ny
                }
            }
            else -> {
                oval(x - 0.22f, gy - 0.2f, 0.44f, 0.24f, ink)
                oval(x - 0.19f, gy - 0.18f, 0.38f, 0.2f, Color(0xFFE8B8C8))
                for (i in 0..3) line(x - 0.15f + i * 0.1f, gy - 0.16f, x - 0.12f + i * 0.08f, gy - 0.04f, Color(0xFFC890A0), 0.02f)
            }
        }
        11 -> when (kind) {
            0 -> {
                glow(x, gy - 0.7f, 3f, Color(0xFFFFA040), 0.34f)
                rect(x - 0.05f, gy - 0.9f, 0.1f, 0.9f, ink)
                rect(x - 0.14f, gy - 0.12f, 0.28f, 0.12f, ink)
                poly(ink, x - 0.25f, gy - 0.95f, x + 0.25f, gy - 0.95f, x + 0.16f, gy - 0.72f, x - 0.16f, gy - 0.72f)
                val fl = 0.8f + 0.2f * sin(frame * 0.4f + seed)
                tri(x - 0.17f, gy - 0.95f, x + sw, gy - 0.95f - 0.45f * fl, x + 0.17f, gy - 0.95f, Color(0xFFFF8A20))
                tri(x - 0.08f, gy - 0.95f, x + sw, gy - 0.95f - 0.26f * fl, x + 0.08f, gy - 0.95f, Color(0xFFFFE066))
            }
            1 -> {
                rect(x - 0.04f, gy - 1.5f, 0.08f, 1.5f, ink)
                val flap = sin(frame * 0.08f + seed) * 0.06f
                poly(Color(0xFF6A1E3A), x + 0.04f, gy - 1.45f, x + 0.55f + flap, gy - 1.4f, x + 0.5f + flap, gy - 1.0f, x + 0.3f, gy - 1.1f, x + 0.04f, gy - 0.95f)
                circ(x + 0.28f, gy - 1.25f, 0.06f, Color(0xFFE6B84A))
            }
            else -> {
                oval(x - 0.2f, gy - 0.1f, 0.4f, 0.12f, ink)
                rect(x - 0.17f, gy - 0.7f, 0.34f, 0.65f, ink)
                rect(x - 0.13f, gy - 0.66f, 0.26f, 0.58f, Color(0xFF5A566A))
                tri(x - 0.2f, gy - 0.7f, x - 0.5f, gy - 0.95f, x - 0.1f, gy - 0.5f, Color(0xFF3A364A))
                tri(x + 0.2f, gy - 0.7f, x + 0.5f, gy - 0.95f, x + 0.1f, gy - 0.5f, Color(0xFF3A364A))
                circ(x, gy - 0.82f, 0.13f, Color(0xFF5A566A))
                circ(x - 0.04f, gy - 0.83f, 0.025f, acc)
                circ(x + 0.04f, gy - 0.83f, 0.025f, acc)
            }
        }
        else -> when (kind) {
            0 -> {
                var px0 = x
                var py0 = gy
                for (j in 1..7) {
                    val nx = x + sin(frame * 0.03f + j * 0.5f + seed) * j * 0.04f
                    val ny = gy - j * 0.12f
                    line(px0, py0, nx, ny, ink, 0.1f - j * 0.01f)
                    px0 = nx
                    py0 = ny
                }
                glow(px0, py0, 0.6f, acc, 0.5f)
                circ(px0, py0, 0.05f, acc)
            }
            1 -> {
                line(x, gy, x + sw, gy - 0.7f, ink, 0.05f)
                val open = 0.5f + 0.5f * sin(frame * 0.03f + seed)
                glow(x + sw, gy - 0.8f, 1.0f, acc, 0.3f)
                oval(x + sw - 0.17f, gy - 0.8f - 0.1f * open, 0.34f, 0.2f * open + 0.03f, Color(0xFF1A0A18))
                circ(x + sw, gy - 0.8f, 0.06f * min(1f, open * 1.5f), acc)
            }
            else -> {
                val pulse = 0.7f + 0.3f * sin(frame * 0.07f + seed)
                glow(x, gy - 0.2f, 1.2f, acc, 0.3f * pulse)
                oval(x - 0.2f, gy - 0.3f, 0.4f, 0.34f, ink)
                oval(x - 0.16f, gy - 0.27f, 0.32f, 0.28f, acc.copy(alpha = 0.7f))
                circ(x, gy - 0.15f, 0.06f, Color.White.copy(alpha = 0.7f))
            }
        }
    }
}

private fun KCam.crystalCluster(x: Float, gy: Float, acc: Color, frame: Int, seed: Int) {
    val a = 0.7f + 0.25f * sin(frame * 0.05f + seed)
    glow(x, gy - 0.3f, 1.5f, acc, 0.28f)
    val cc = lerp(acc, Color.White, 0.25f).copy(alpha = a)
    for (i in -1..1) {
        val hgt = 0.35f + 0.35f * hsh(seed, i + 5)
        tri(x + i * 0.18f - 0.11f, gy, x + i * 0.18f, gy - hgt, x + i * 0.18f + 0.11f, gy, INK)
        tri(x + i * 0.18f - 0.07f, gy, x + i * 0.18f, gy - hgt + 0.06f, x + i * 0.18f + 0.07f, gy, cc)
        tri(x + i * 0.18f, gy - hgt + 0.06f, x + i * 0.18f + 0.07f, gy, x + i * 0.18f, gy, Color.White.copy(alpha = 0.25f))
    }
}

private fun KCam.branch(x: Float, gy: Float, col: Color, h: Float) {
    var px0 = x
    var py0 = gy
    val tipsX = ArrayList<Float>()
    for (j in 1..4) {
        val nx = x + sin(j * 0.6f) * 0.04f
        val ny = gy - j * h / 4f
        line(px0, py0, nx, ny, col, 0.09f - j * 0.012f)
        px0 = nx
        py0 = ny
        if (j >= 2) {
            line(nx, ny, nx + 0.16f, ny - 0.1f, col, 0.05f)
            line(nx, ny, nx - 0.16f, ny - 0.1f, col, 0.05f)
            tipsX.add(nx)
        }
    }
    circ(px0, py0, 0.05f, Color.White.copy(alpha = 0.6f))
}

/** Scène plein écran utilisée par les écrans d'histoire (décor du monde + sol + chevalier). */
internal fun DrawScope.drawStoryScene(wi0: Int, frame: Int, sk: KSkin) {
    val wi = wi0.coerceIn(0, KN.worlds.lastIndex)
    val w = KN.worlds[wi]
    val s = size.height / 9.5f
    val viewW = size.width / s
    val cx = 20f + frame * 0.012f
    val cy = 3.0f
    drawWorldBackdrop(wi, w, cx, cy, s, viewW, frame)
    val gy = (11.5f - cy) * s
    drawRect(lerp(Color(w.ground), Color.Black, 0.45f), Offset(0f, gy), Size(size.width, max(0f, size.height - gy)))
    drawRect(Color(w.grass), Offset(0f, gy), Size(size.width, 0.16f * s))
    val c = KCam(this, s, 0f, cy)
    val fx = viewW * 0.22f
    c.oval(fx - 0.1f, 11.5f - 0.06f, 0.9f, 0.14f, Color.Black.copy(alpha = 0.4f))
    c.inked { knightFigure(sk, fx, 11.5f - 1.4f, 1f, frame, 0f, false, -1f, 1.5f) }
    c.knightFigure(sk, fx, 11.5f - 1.4f, 1f, frame, 0f, false, -1f, 1.5f)
    drawWorldWeather(wi, w, cx, s, viewW, size.height / s, frame)
    drawRect(Color.Black.copy(alpha = 0.28f), Offset.Zero, size)
    val diag = sqrt(size.width * size.width + size.height * size.height)
    drawRect(
        Brush.radialGradient(
            0f to Color.Transparent, 0.5f to Color.Transparent, 1f to Color(0xFF02020A).copy(alpha = 0.75f),
            center = Offset(size.width / 2f, size.height / 2f), radius = diag * 0.55f
        ),
        Offset.Zero, size
    )
}
