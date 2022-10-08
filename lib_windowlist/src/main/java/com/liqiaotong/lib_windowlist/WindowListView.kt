package com.liqiaotong.lib_windowlist

import android.animation.Animator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.text.TextUtils
import android.util.AttributeSet
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.widget.RelativeLayout
import android.widget.Scroller
import androidx.core.view.drawToBitmap
import kotlin.math.ceil


class WindowListView : RelativeLayout {

    private var screenRectF: RectF? = null
    private var windowSelectedStrokeWidth: Float = 10f
    private var windowSelectedStrokeColor: Int = Color.BLACK
    private var windowStrokeWidth: Float = 5f
    private var windowStrokeColor: Int = Color.GRAY
    private var windowPadding: Float = 50f
    private var windowCornerRadius: Float = 50f
    private var windowColumnNum: Int = 2
    private var windowWHScale: Float = 0.8f
    private var scrollY: Float = 0f
    private var windows: MutableList<BWindow> = ArrayList()

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, -1)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        setLayerType(View.LAYER_TYPE_HARDWARE, null)
        initResource(context, attrs, defStyleAttr)
        initPaint()
        invalidate()
    }

    private fun initResource(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) {
//        context?.let {
//            var a = context.obtainStyledAttributes(attrs, R.styleable.AnProgressView, defStyleAttr, 0)
//            lineColor = a.getColor(R.styleable.AnProgressView_apv_lineColor, defaultColor)
//            fixedPointColor = a.getColor(R.styleable.AnProgressView_apv_fixedPointColor, defaultColor)
//            pointColor = a.getColor(R.styleable.AnProgressView_apv_pointColor, defaultColor)
//            numberColor = a.getColor(R.styleable.AnProgressView_apv_numberColor, Color.parseColor("#FFFFFF"))
//            numberSize = a.getDimension(R.styleable.AnProgressView_apv_numberSize, 30f)
//            isShowNumber = a.getBoolean(R.styleable.AnProgressView_apv_isShowNumber, false)
//            lineHeight = a.getDimension(R.styleable.AnProgressView_apv_lineHeight, 10f)
//            fixedPointSize = a.getDimension(R.styleable.AnProgressView_apv_fixedPointSize, 45f)
//            pointSize = a.getDimension(R.styleable.AnProgressView_apv_pointSize, 90f)
//            fixedPointCount = a.getInt(R.styleable.AnProgressView_apv_fixedPointCount, 2)?.let { if (it < 2) 2 else it }
//            a.recycle()
//        }

    }

    private var windowPaint: Paint? = null
    private var windowRoundClipPaint: Paint? = null
    private var windowStrokePaint: Paint? = null
    private var windowSelectedStrokePaint: Paint? = null
    private fun initPaint() {
        windowPaint = Paint()
        windowRoundClipPaint = Paint()
        windowRoundClipPaint?.color = Color.parseColor("#FFFFFF")
        windowStrokePaint = Paint()
        windowStrokePaint?.color = windowStrokeColor
        windowStrokePaint?.style = Paint.Style.STROKE
        windowStrokePaint?.strokeWidth = windowStrokeWidth
        windowSelectedStrokePaint = Paint()
        windowSelectedStrokePaint?.color = windowSelectedStrokeColor
        windowSelectedStrokePaint?.style = Paint.Style.STROKE
        windowSelectedStrokePaint?.strokeWidth = windowSelectedStrokeWidth
    }

    private fun getScreenWidth(): Float {
        return screenRectF?.width() ?: width.toFloat()
    }

    private fun getScreenHeight(): Float {
        return screenRectF?.height() ?: height.toFloat()
    }

    private fun getWindowWidth(): Float {
        val widthNoPadding = getScreenWidth() - windowPadding * (windowColumnNum + 1)
        return widthNoPadding / windowColumnNum
    }

    private fun getWindowHeight(): Float {
        return getWindowWidth() / windowWHScale
    }

    private var windowCornerBitmapRadius: Float? = 0f
    private var windowCornerBitmap: Bitmap? = null
    private fun getWindowCornerBitmap(radius: Float, drawWindowRectF: RectF): Bitmap? {
        if (windowCornerBitmap == null || windowCornerBitmapRadius != radius) {
            windowCornerBitmapRadius = radius
            val windowCornerBitmap = Bitmap.createBitmap(drawWindowRectF.width().toInt(), drawWindowRectF.height().toInt(), Bitmap.Config.ARGB_8888)
            val canvas = Canvas(windowCornerBitmap)
            windowRoundClipPaint?.let {
                canvas.drawRoundRect(
                    0f, 0f, ceil(drawWindowRectF.width()), ceil(drawWindowRectF.height()), windowCornerBitmapRadius ?: 0f, windowCornerBitmapRadius ?: 0f,
                    it
                )
            }
            this.windowCornerBitmap = windowCornerBitmap
        }
        return this.windowCornerBitmap
    }

    override fun dispatchDraw(canvas: Canvas?) {
        super.dispatchDraw(canvas)
        canvas?.let { canvas ->
            val windowWidth = getWindowWidth()
            val windowHeight = getWindowHeight()
            for (index in windows.indices) {
                windows[index]?.let { window ->
                    window.bitmap?.let {
                        //window rect
                        val wsc = index % windowColumnNum
                        val windowLeft = windowWidth * wsc + windowPadding * (wsc + 1)
                        val hsc = (index.toFloat() / windowColumnNum.toFloat()).toInt()
                        val windowTop = windowHeight * hsc + windowPadding * (hsc + 1)
                        val windowRight = windowLeft + windowWidth
                        val windowBottom = windowTop + windowHeight
                        val windowRectF = RectF(windowLeft, windowTop + scrollY, windowRight, windowBottom + scrollY)
                        windows[index]?.rectF = windowRectF
                        if (windowRectF.top < getScreenHeight() && windowRectF.bottom > 0 && windowRectF.left < getScreenWidth() && windowRectF.right > 0) {
                            if (!TextUtils.equals(window.id, scaleWindowId)) drawWindow(canvas, window)
                        }
                    }
                }
            }

            canvas.saveLayer(screenRectF, Paint())

            if (!TextUtils.isEmpty(scaleWindowId)) {
                windows.firstOrNull { TextUtils.equals(it.id, scaleWindowId) }?.let {
                    drawWindow(canvas, it)
                }
            }

        }
    }

    private val porterDuffXfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
    private fun drawWindow(canvas: Canvas, window: BWindow) {
        val drawWindowRectF = RectF(window.rectF)

        //点击缩放
        if (window.touchScale != 1f) {
            val scaleOffsetX = (drawWindowRectF.width() * 0.5f).let { it - window.touchScale * it }
            val scaleOffsetY = (drawWindowRectF.height() * 0.5f).let { it - window.touchScale * it }
            drawWindowRectF.run {
                left += scaleOffsetX
                top += scaleOffsetY
                right -= scaleOffsetX
                bottom -= scaleOffsetY
            }
        }

        //窗口打开或关闭缩放
        if (TextUtils.equals(scaleWindowId, window.id)) {
            drawWindowRectF.run {
                left += (0 - left) * windowScaleProgress
                top += (0 - top) * windowScaleProgress
                right += (getScreenWidth() - right) * windowScaleProgress
                bottom += (getScreenHeight() - bottom) * windowScaleProgress
            }
        }

        //圆角设置
        val strokeRadius = if (TextUtils.equals(scaleWindowId, window.id)) {
            windowCornerRadius * (1f - windowScaleProgress)
        } else {
            windowCornerRadius
        }


        //绘制圆角
        if (strokeRadius > 0) {
            getWindowCornerBitmap(strokeRadius, drawWindowRectF)?.let { canvas?.drawBitmap(it, null, drawWindowRectF, windowPaint) }
            windowPaint?.xfermode = porterDuffXfermode
        }

        //绘制窗口
        window?.bitmap?.let {
            var windowScale: Float = if (drawWindowRectF?.width() != null && drawWindowRectF?.height() != null) {
                drawWindowRectF?.width()!! / drawWindowRectF?.height()!!
            } else {
                it.width.toFloat() / it.height.toFloat()
            }
            val bitmapWidth = it.width
            val bitmapHeight = it.width.toFloat() / windowScale
            canvas?.drawBitmap(it, Rect(0, 0, bitmapWidth, bitmapHeight.toInt()), drawWindowRectF, windowPaint)
        }

        windowPaint?.xfermode = null

        //边框透明度
        val strokeAlpha = if (TextUtils.equals(scaleWindowId, window.id)) {
            (255f * (1f - windowScaleProgress)).toInt()
        } else {
            255
        }
        if (strokeAlpha > 0) {
            if (window.isSelected) {
                if (windowSelectedStrokeWidth > 0) {
                    windowSelectedStrokePaint?.let {
                        it.alpha = strokeAlpha
                        canvas?.drawRoundRect(drawWindowRectF, strokeRadius, strokeRadius, it)
                    }
                }
            } else {
                if (windowStrokeWidth > 0) {
                    windowStrokePaint?.let {
                        it.alpha = strokeAlpha
                        canvas?.drawRoundRect(drawWindowRectF, strokeRadius, strokeRadius, it)
                    }
                }
            }
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        screenRectF = RectF(left.toFloat() + paddingLeft, top.toFloat() + paddingTop, right.toFloat() - paddingRight, bottom.toFloat() - paddingBottom)
    }

    fun addWindow(view: View?, isAnimator: Boolean? = false) {
        view?.let { view ->
            addWindow(view.drawToBitmap(Bitmap.Config.ARGB_8888), isAnimator)
        }
    }

    fun addWindow(bitmap: Bitmap?, isAnimator: Boolean? = false) {
        bitmap?.let {
            val window = BWindow().apply { this.bitmap = it }
            windows.add(window)
            //刷新并更新位置
            invalidate()
            if (isAnimator == true) {
                setWindowSelect(window.id)
                //开始缩放动画
                scaleWindowZoomIn = true
                windowScaleProgress = 1f
                scaleAnimator?.cancel()
                invalidate()
                moveToBottom()
                scaleWindow(window, false)
            }
        }
    }

    private var downX: Float = 0f
    private var downY: Float = 0f
    private var moveX: Float = 0f
    private var moveY: Float = 0f
    private var downScrollY: Float = 0f
    private var isMove: Boolean = false
    private var scroller = Scroller(context, null, true)
    private var velocityTracker: VelocityTracker = VelocityTracker.obtain()

    override fun onTouchEvent(event: MotionEvent?): Boolean {

        if (!isEnabled || (scaleAnimator?.isRunning == true && !scaleWindowZoomIn)) return super.onTouchEvent(event)

        velocityTracker.addMovement(event)

        when (event?.action) {
            MotionEvent.ACTION_MOVE -> {
                moveX = (event?.x ?: 0f) - downX
                moveY = (event?.y ?: 0f) - downY
                if (kotlin.math.abs(moveX) > 0 || kotlin.math.abs(moveY) > 0) {
                    scrollY = downScrollY + moveY
                    if (!isMove) {
                        //取消按下效果，并取消点击事件
                        setTouch(event?.x ?: 0f, event?.y ?: 0f, isTouched = false, isClicked = false)
                    }
                    isMove = true
                    postInvalidate()
                }
            }
            MotionEvent.ACTION_DOWN -> {
                isMove = false
                downX = event?.x ?: 0f
                downY = event?.y ?: 0f
                downScrollY = scrollY
                scroller.abortAnimation()
                //按下效果
                setTouch(event?.x ?: 0f, event?.y ?: 0f, isTouched = true, isClicked = false)
            }
            MotionEvent.ACTION_UP -> {
                velocityTracker.computeCurrentVelocity(300, Float.MAX_VALUE)
                //Y轴滑动速率
                val yVelocity = velocityTracker.yVelocity.toInt()
                //计算Y轴最大滑动距离
                var maxScrollY = getMaxScrollY()
                val screenMaxY = getScreenHeight()
                if (maxScrollY > screenMaxY) {
                    maxScrollY -= screenMaxY
                } else {
                    maxScrollY = 0f
                }
                if (scrollY > 0 || scrollY < -maxScrollY) {
                    //顶部或底部阻尼效果
                    val dy = if (scrollY > 0) -scrollY.toInt() else if (scrollY < -maxScrollY.toInt()) -(scrollY.toInt() + maxScrollY.toInt()) else 0
                    scroller.startScroll(0, scrollY.toInt(), 0, dy, 350)
                    postInvalidate()
                } else {
                    //滑动效果
                    scroller.fling(0, scrollY.toInt(), 0, yVelocity, 0, 0, -maxScrollY.toInt(), 0)
                }
                velocityTracker.clear()
                //取消按下效果，并确定是否点击
                setTouch(event?.x ?: 0f, event?.y ?: 0f, false, !isMove)
            }
            MotionEvent.ACTION_CANCEL -> {
                Log.d("message", "action cancel")
            }
        }
        return true
    }

    private val touchAnimators: MutableList<ValueAnimator> = ArrayList()
    private fun setTouch(touchX: Float, touchY: Float, isTouched: Boolean, isClicked: Boolean) {
        windows.firstOrNull { it.rectF?.contains(touchX, touchY) == true }?.let { window ->

            //判断是否点击
            if (!isTouched && isClicked) {
                setWindowSelect(window.id)
                scaleWindow(window, true)
                postInvalidate()
            }

            var scaleAnimator: ValueAnimator?
            val notUseAnimator = touchAnimators.filter { !it.isRunning }
            if (notUseAnimator.isNotEmpty()) {
                scaleAnimator = notUseAnimator[0]
            } else {
                scaleAnimator = ValueAnimator()
                touchAnimators.add(scaleAnimator)
            }
            scaleAnimator.cancel()
            scaleAnimator.removeAllUpdateListeners()

            scaleAnimator.setFloatValues(window.touchScale, if (isTouched) 0.95f else 1f)
            scaleAnimator.addUpdateListener {
                //点击缩放窗口
                window.touchScale = it.animatedValue as Float
                postInvalidate()
            }
            scaleAnimator.duration = 100
            scaleAnimator.start()
        }
    }

    private var scaleWindowId: String? = null
    private var scaleWindowZoomIn: Boolean = false
    private var windowScaleProgress: Float = 0f
    private var scaleAnimator: ValueAnimator? = null
    private fun scaleWindow(window: BWindow, isZoomIn: Boolean) {

        if (scaleAnimator?.isRunning == true || scaleWindowZoomIn == isZoomIn) return

        scaleWindowZoomIn = isZoomIn

        if (scaleWindowZoomIn) {
            if (windowScaleProgress == 1f) windowScaleProgress = 0f
        } else {
            if (windowScaleProgress == 0f) windowScaleProgress = 1f
        }

        scaleWindowId = window.id

        scaleAnimator?.cancel()
        if (scaleAnimator == null) {
            scaleAnimator = ValueAnimator()
            scaleAnimator?.duration = 350
        }
        scaleAnimator?.removeAllUpdateListeners()
        scaleAnimator?.addUpdateListener {
            windowScaleProgress = it.animatedValue as Float
            postInvalidate()
        }
        scaleAnimator?.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator?) {

            }

            override fun onAnimationEnd(animation: Animator?) {
                if (!scaleWindowZoomIn) scaleWindowId = null
            }

            override fun onAnimationCancel(animation: Animator?) {
                if (!scaleWindowZoomIn) scaleWindowId = null
            }

            override fun onAnimationRepeat(animation: Animator?) {

            }
        })
        scaleAnimator?.setFloatValues(windowScaleProgress, if (isZoomIn) 1f else 0f)
        scaleAnimator?.start()
    }

    fun closeWindow() {
        if (!TextUtils.isEmpty(scaleWindowId)) {
            windows.firstOrNull { TextUtils.equals(it.id, scaleWindowId) }?.let {
                scaleWindow(it, false)
            }
        }
    }

    private fun getMaxScrollY(): Float {
        val row = ceil(windows.size.toFloat() / windowColumnNum.toFloat()).toInt()
        return (row * getWindowHeight() + windowPadding * (row + 1))
    }

    private fun moveToBottom() {
        val maxSY = getMaxScrollY() - getScreenHeight()
        if (maxSY > 0) {
            scrollY = -maxSY
        }
        postInvalidate()
    }

    private fun setWindowSelect(id: String?) {
        id?.let {
            windows.forEach { it.isSelected = TextUtils.equals(it.id, id) }
        }
    }

    override fun onDetachedFromWindow() {
        velocityTracker.recycle()
        super.onDetachedFromWindow()
    }

    override fun computeScroll() {
        super.computeScroll()
        if (scroller.computeScrollOffset()) {
            scrollY = scroller.currY.toFloat()
            postInvalidate()
        }
    }

}