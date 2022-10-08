package com.liqiaotong.lib_windowlist

import android.animation.ValueAnimator
import android.graphics.Bitmap
import android.graphics.RectF
import android.view.animation.DecelerateInterpolator

class BWindow {

    companion object {
        private var id: Int = 0
    }

    var rectF: RectF? = null
    var bitmap: Bitmap? = null
    var id: String? = null
    var url: String? = null
    var title: String? = null
    var isSelected: Boolean = false
    var touchScale: Float = 1f

    init {
        BWindow.id = BWindow.id + 1
        id = "${System.currentTimeMillis()}${BWindow.id}"
    }

}