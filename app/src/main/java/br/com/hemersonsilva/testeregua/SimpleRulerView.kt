/*
 * Copyright (C) 2016 (@seek 951882080@qq.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package br.com.hemersonsilva.testeregua

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.text.TextPaint
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.SoundEffectConstants
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.OverScroller
import androidx.core.view.GestureDetectorCompat
import androidx.core.view.ViewCompat
import java.text.DecimalFormat
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class SimpleRulerView(context: Context?, attrs: AttributeSet? = null) : View(context, attrs), GestureDetector.OnGestureListener {
    /**
     * the ruler line paint
     */
    private lateinit var rulerPaint: Paint

    /**
     * the buttom text paint
     */
    private lateinit var textPaint: TextPaint

    /**
     * current selected index
     */
    private var mSelectedIndex = -1

    /**
     * the selected color
     */
    var highlightColor = Color.RED

    /**
     * the text color
     */
    var markTextColor = Color.WHITE

    /**
     * the ruler line color
     */
    var markColor = Color.WHITE

    /**
     * view height
     */
    private var mWidth = 0

    /**
     * if you don't want the default text, you can custom them
     *
     * @param mTextList
     */
    /**
     * texts for show
     */
    private var textList: List<String> = arrayListOf()

    /**
     * scroll helper
     */
    private lateinit var mScroller: OverScroller

    /**
     * the max distance be allowed to scroll
     */
    private var mMaxOverScrollDistance = 0f

    /**
     * just for help to calculate
     */
    private var mContentHeight = 0f

    /**
     * fling or not
     */
    private var mFling = false

    /**
     * the gesture detector help us to handle
     */
    private lateinit var mGestureDetectorCompat: GestureDetectorCompat

    /**
     * the max and min value this view could draw
     */
    private var mMaxValue = 0f
    private var mMinValue = 0f

    /**
     * the difference value between two adjacent value
     */
    private var mIntervalValue = 1f

    /**
     * the difference distance of two adjacent value
     */
    var intervalDis = 0f

    /**
     * the total of ruler line
     */
    private var mRulerCount = 0

    /**
     * text size
     */
    private var mTextSize = 0f

    /**
     * ruler line width
     */
    private var mRulerLineWidth = 0f

    private var scaleLineSmall = 0
    private var scaleLineMedium = 0
    private var scaleLineLarge = 0

    /**
     * half width
     */
    private var mViewScopeSize = 0
    lateinit var onValueChangeListener: OnValueChangeListener

    interface OnValueChangeListener {
        /**
         * when the current selected index changed will call this method
         *
         * @param view     the SimplerulerView
         * @param position the selected index
         * @param value    represent the selected value
         */
        fun onChange(view: SimpleRulerView?, position: Int, value: Float)
    }

    /**
     * set default
     *
     * @param attrs
     */
    @SuppressLint("Recycle", "CustomViewStyleable")
    private fun init(attrs: AttributeSet?) {
        val dm = resources.displayMetrics
        intervalDis = dm.density * 5
        mRulerLineWidth = dm.density * 1
        mTextSize = dm.scaledDensity * 14
        val typedArray = if (attrs == null) null else context
                .obtainStyledAttributes(attrs, R.styleable.simpleRulerView)
        if (typedArray != null) {
            highlightColor = typedArray
                    .getColor(R.styleable.simpleRulerView_highlightColor,
                            highlightColor)
            markTextColor = typedArray.getColor(
                    R.styleable.simpleRulerView_textColor, markTextColor)
            markColor = typedArray.getColor(R.styleable.simpleRulerView_rulerColor,
                    markColor)
            mIntervalValue = typedArray
                    .getFloat(R.styleable.simpleRulerView_intervalValue,
                            mIntervalValue)
            mMaxValue = typedArray
                    .getFloat(R.styleable.simpleRulerView_maxValue,
                            mMaxValue)
            mMinValue = typedArray
                    .getFloat(R.styleable.simpleRulerView_minValue,
                            mMinValue)
            mTextSize = typedArray.getDimension(
                    R.styleable.simpleRulerView_textSize,
                    mTextSize)
            mRulerLineWidth = typedArray.getDimension(
                    R.styleable.simpleRulerView_rulerLineWidth, mRulerLineWidth)
            intervalDis = typedArray.getDimension(R.styleable.simpleRulerView_intervalDistance, intervalDis)
            retainLength = typedArray.getInteger(R.styleable.simpleRulerView_retainLength, 0)
        }

        calculateTotal()

        rulerPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        rulerPaint.strokeWidth = mRulerLineWidth
        textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.textSize = mTextSize
        mGestureDetectorCompat = GestureDetectorCompat(context, this)
        mScroller = OverScroller(context, DecelerateInterpolator())

        scaleLineSmall = resources.getDimension(R.dimen.scale_line_small).toInt()
        scaleLineMedium = resources.getDimension(R.dimen.scale_line_medium).toInt()
        scaleLineLarge = resources.getDimension(R.dimen.scale_line_large).toInt()
        //textStartPoint = resources.getDimension(R.dimen.text_start_point).toInt()

        setSelectedIndex(0)
    }

    /**
     * we mesure by ourselves
     */
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(measureWidth(widthMeasureSpec),
                measureHeight(heightMeasureSpec))
    }

    private fun measureWidth(widthMeasureSpec: Int): Int {
        val measureMode = MeasureSpec.getMode(widthMeasureSpec)
        val measureSize = MeasureSpec.getSize(widthMeasureSpec)
        var result = suggestedMinimumWidth
        when (measureMode) {
            MeasureSpec.AT_MOST, MeasureSpec.EXACTLY -> result = measureSize
            else -> {
            }
        }
        return result
    }

    private fun measureHeight(heightMeasure: Int): Int {
        val measureMode = MeasureSpec.getMode(heightMeasure)
        val measureSize = MeasureSpec.getSize(heightMeasure)
        var result = mTextSize.toInt() * 5
        when (measureMode) {
            MeasureSpec.EXACTLY -> result = Math.max(result, measureSize)
            MeasureSpec.AT_MOST -> result = Math.min(result, measureSize)
            else -> {
            }
        }
        return result
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w != oldw || h != oldh) {
            mWidth = w
            mMaxOverScrollDistance = h / 2f
            mContentHeight = ((mMaxValue - mMinValue) / mIntervalValue
                    * intervalDis)
            mViewScopeSize = ceil((mMaxOverScrollDistance
                    / intervalDis).toDouble()).toInt()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        /**
         * here is the start         the selected            the end
         * /                        /                       /
         * ||||||||||||||||||||||||||||||||||||||||||||||||||
         * |    |    |    |    |    |    |    |    |   |    |
         * 0         1        2
         * \____mViewScopeSize_____/
         *
         */

        var start = mSelectedIndex - mViewScopeSize
        var end = mSelectedIndex + mViewScopeSize
        start = max(start, -mViewScopeSize * 2)
        end = min(end, mRulerCount + mViewScopeSize * 2)

        if (mSelectedIndex.toFloat() == mMaxValue) {
            end += mViewScopeSize
        } else if (mSelectedIndex.toFloat() == mMinValue) {
            start -= mViewScopeSize
        }
        var x = start * intervalDis
        val markHeight = mWidth - mTextSize
        for (i in start until end) {
            // draw line
            val remainderBy2 = i % 2
            val remainderBy5 = i % 5
            rulerPaint.color = markColor

            if (i < 0 || i > 200) {
                rulerPaint.color = resources.getColor(android.R.color.transparent)
            }


           val textStartPoint = context.resources.getDimension(R.dimen.text_start_point)
           val text2StartPoint = context.resources.getDimension(R.dimen.text2_start_point)
           val lineStartPoint = context.resources.getDimension(R.dimen.line_start_point)
           val line2StartPoint = context.resources.getDimension(R.dimen.line2_start_point)


            val size: Int = scaleLineLarge
            val size2: Int = if (i % 10 == 0) scaleLineLarge else if (i % 5 == 0) scaleLineMedium else scaleLineSmall

            canvas.drawLine(lineStartPoint, x,lineStartPoint + size, x, rulerPaint)

            canvas.drawLine(line2StartPoint, x,line2StartPoint + size2, x, rulerPaint)




            //canvas.drawLine(0F, x,start + size, x, rulerPaint)

//            // if(mRulerCount)
//            if (remainderBy2 == 0 && remainderBy5 == 0) {
//                canvas.drawLine(80f, x, 130f, x, rulerPaint)
//            } else if (remainderBy2 != 0 && remainderBy5 == 0) {
//                canvas.drawLine(80f, x, 150 * 3 / 4.toFloat(), x, rulerPaint)
//            } else {
//                canvas.drawLine(80f, x, 100f, x, rulerPaint)
//            }


            if (mRulerCount > 0 && i >= 0 && i < mRulerCount) {
                textPaint.color = markTextColor

                if (i % 5 == 0) {
                    var text: String?
                    text = if (textList.isNotEmpty()) {
                        val index = i / 2
                        if (index < textList.size) {
                            textList[index]
                        } else {
                            ""
                        }
                    } else {
                        format(i * mIntervalValue + mMinValue)
                    }
                    canvas.drawText(text, 0, text.length, textStartPoint , x, textPaint)
                }
            }

//			// draw text
            if (mRulerCount > 0 && i >= 0 && i < mRulerCount) {
                textPaint.color = markTextColor

                if (i % 10 == 0) {
                    var text: String?
                    text = if (textList.isNotEmpty()) {
                        val index = i / 10
                        if (index < textList.size) {
                            textList[index]
                        } else {
                            ""
                        }
                    } else {
                        format(i * mIntervalValue + mMinValue)
                    }
                    canvas.drawText(text, 0, text.length, text2StartPoint , x, textPaint)
                }
            }
            x += intervalDis
        }
    }

    /**
     * remain the text length
     */
    private var retainLength = 0
    fun getRetainLength(): Int {
        return retainLength
    }

    /**
     * set the remain length that can be good look
     *
     * @param retainLength
     */
    fun setRetainLength(retainLength: Int) {
        require(!(retainLength < 1 || retainLength > 3)) {
            ("retainLength beyond expected,only support in [0,3],but now you set "
                    + retainLength)
        }
        this.retainLength = retainLength
        invalidate()
    }

    /**
     * format the text
     *
     * @param fvalue
     * @return
     */
    private fun format(fvalue: Float): String {
        return when (retainLength) {
            0 -> DecimalFormat("##0").format(fvalue.toDouble())
            1 -> DecimalFormat("##0.0").format(fvalue.toDouble())
            2 -> DecimalFormat("##0.00").format(fvalue.toDouble())
            3 -> DecimalFormat("##0.000").format(fvalue.toDouble())
            else -> DecimalFormat("##0.0").format(fvalue.toDouble())
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        var resolve = mGestureDetectorCompat.onTouchEvent(event)
        if (!mFling && MotionEvent.ACTION_UP == event.action) {
            adjustPosition()
            resolve = true
        }
        return resolve || super.onTouchEvent(event)
    }

    /**
     * we hope that after every scroll the selected should be legal
     */
    private fun adjustPosition() {
        val scrollY = scrollY
        val dx = (mSelectedIndex * intervalDis - scrollY
                - mMaxOverScrollDistance)
        mScroller.startScroll(0, scrollY, 0, dx.toInt())
        postInvalidate()
    }

    /**
     * clamp selected index in bounds.
     *
     * @param selectedIndex
     * @return
     */
    private fun clampSelectedIndex(selectedIndex: Int): Int {
        var index = selectedIndex
        if (index < 0) {
            index = 0
        } else if (index > mRulerCount) {
            index = mRulerCount - 1
        }
        return index
    }

    /**
     * refresh current selected index
     *
     * @param offsetX
     */
    private fun refreshSelected(offsetX: Int = scrollY) {
        val offset = (offsetX + mMaxOverScrollDistance).toInt()
        var tempIndex = (offset / intervalDis).roundToInt()
        tempIndex = clampSelectedIndex(tempIndex)
        if (mSelectedIndex == tempIndex) {
            return
        }
        mSelectedIndex = tempIndex
        // dispatch the selected index
        onValueChangeListener.onChange(
                this,
                mSelectedIndex, format(mSelectedIndex * mIntervalValue
                + mMinValue).toFloat())
    }

    override fun onDown(e: MotionEvent): Boolean {
        if (!mScroller.isFinished) {
            mScroller.forceFinished(false)
        }
        mFling = false
        if (null != parent) {
            parent.requestDisallowInterceptTouchEvent(true)
        }
        return true
    }

    override fun onShowPress(e: MotionEvent) {}

    /**
     * allowed to tab up to select
     */
    override fun onSingleTapUp(e: MotionEvent): Boolean {
        playSoundEffect(SoundEffectConstants.CLICK)
        refreshSelected((scrollY + e.y - mMaxOverScrollDistance).toInt())
        adjustPosition()
        return true
    }

    override fun computeScroll() {
        super.computeScroll()
        if (mScroller.computeScrollOffset()) {
            // if the content disappear from sight ,we should be interrupt
            val scrollY = scrollY.toFloat()
            if (scrollY < -mMaxOverScrollDistance
                    || scrollY > mContentHeight - mMaxOverScrollDistance) {
                mScroller.abortAnimation()
            }
            scrollTo(mScroller.currX, mScroller.currY)
            refreshSelected()
            invalidate()
        } else {
            if (mFling) {
                mFling = false
                adjustPosition()
            }
        }
    }

    override fun onScroll(e1: MotionEvent, e2: MotionEvent, distanceX: Float,
                          distanceY: Float): Boolean {
        var mOffsety = distanceY
        val scrollY = scrollY.toFloat()
        if (scrollY < -mMaxOverScrollDistance) {
            mOffsety = distanceY / 4f
        } else if (scrollY > mContentHeight - mMaxOverScrollDistance) {
            mOffsety = distanceY / 4f
        }
        scrollBy(0, mOffsety.toInt())
        refreshSelected()
        return true
    }

    override fun onLongPress(e: MotionEvent) {}
    override fun onFling(e1: MotionEvent, e2: MotionEvent, velocityX: Float,
                         velocityY: Float): Boolean {
        val scrollY = scrollY.toFloat()
        // if current position is the boundary, we do not fling
        if (scrollY < -mMaxOverScrollDistance
                || scrollY > mContentHeight - mMaxOverScrollDistance) return false
        mFling = true
        fling((-velocityY).toInt())
        return true
    }

    private fun fling(velocityY: Int) {
        mScroller.fling(0, scrollY, 0, velocityY / 3,
                0,
                0, (-mMaxOverScrollDistance).toInt(), (mContentHeight - mMaxOverScrollDistance).toInt(),
                0, (mMaxOverScrollDistance / 4).toInt())
        ViewCompat.postInvalidateOnAnimation(this)
    }

    fun getmSelectedIndex(): Int {
        return mSelectedIndex
    }

    /**
     * set the the selectedIndex be the current selected
     *
     * @param selectedIndex
     */
    fun setSelectedIndex(selectedIndex: Int) {
        mSelectedIndex = selectedIndex
        post {
            scrollTo(0,
                    (mSelectedIndex * intervalDis - mMaxOverScrollDistance).toInt())
            invalidate()
            refreshSelected()
        }
    }

    /**
     * see [.setSelectedIndex]
     *
     * @param selectedValue
     */
    fun setSelectedValue(selectedValue: Float) {
        require(!(selectedValue < mMinValue || selectedValue > mMaxValue)) {
            ("expected selectedValue in ["
                    + mMinValue + "," + mMaxValue
                    + "],but the selectedValue is " + selectedValue)
        }
        setSelectedIndex(((selectedValue - mMinValue) / mIntervalValue).toInt())
    }

    /**
     * set the max value to the ruler
     *
     * @param mMaxValue
     */
    var maxValue: Float
        get() = mMaxValue
        set(mMaxValue) {
            this.mMaxValue = mMaxValue
            calculateTotal()
            invalidate()
        }

    /**
     * calculate the ruler-line's amount by the maximum and the minimum value
     */
    private fun calculateTotal() {
        mRulerCount = ((mMaxValue - mMinValue) / mIntervalValue).toInt() + 1
    }

    /**
     * set the min value to the ruler
     *
     * @param mMinValue
     */
    var minValue: Float
        get() = mMinValue
        set(mMinValue) {
            this.mMinValue = mMinValue
            calculateTotal()
            invalidate()
        }

    var intervalValue: Float
        get() = mIntervalValue
        set(mIntervalValue) {
            this.mIntervalValue = mIntervalValue
            calculateTotal()
            invalidate()
        }

    /**
     * Constructor that is called when inflating SimpleRulerView from XML
     *
     * @param context
     * @param attrs
     */
    /**
     * simple constructor to use when creating a SimpleRulerView from code
     *
     * @param context
     */
    init {
        init(attrs)
    }
}