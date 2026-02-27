package com.example.funplayer

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.widget.FrameLayout

/**
 * 分屏时：左右两半各绘制一遍完整画面，等比例缩放 (0.5,0.5) 保持画面比例，
 * 左右在正中间无缝衔接，上下留黑边以保持比例。控制由上层透明 overlay 统一提供。
 */
class VrSplitContainerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    /** 为 true 时启用左右分屏；为 false 时与普通 FrameLayout 一致。 */
    var splitMode: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                invalidate()
            }
        }

    override fun onSizeChanged(w: Int, h: Int, oldW: Int, oldH: Int) {
        super.onSizeChanged(w, h, oldW, oldH)
        if (splitMode && (w != oldW || h != oldH) && w > 0 && h > 0) {
            requestLayout()
            post { invalidate() }
        }
    }

    override fun dispatchDraw(canvas: Canvas) {
        if (!splitMode || childCount == 0) {
            super.dispatchDraw(canvas)
            return
        }
        val child = getChildAt(0)
        val w = width
        val h = height
        if (w <= 0 || h <= 0) return
        val halfW = w / 2f
        val halfH = h / 2f
        val vertOffset = halfH / 2f // 垂直居中：缩放后高度 h/2，居中需下移 h/4
        // 等比例缩放保持画面比例。右半屏裁剪向左重叠 2px，消除横屏下取整导致的中间黑边
        val overlap = 2f
        // 左半屏
        canvas.save()
        canvas.clipRect(0f, 0f, halfW + overlap, h.toFloat())
        canvas.translate(0f, vertOffset)
        canvas.scale(0.5f, 0.5f)
        drawChild(canvas, child, drawingTime)
        canvas.restore()
        // 右半屏（左边界略重叠，避免中间黑缝）
        canvas.save()
        canvas.clipRect(halfW - overlap, 0f, w.toFloat(), h.toFloat())
        canvas.translate(halfW - overlap, vertOffset)
        canvas.scale(0.5f, 0.5f)
        drawChild(canvas, child, drawingTime)
        canvas.restore()
    }
}
