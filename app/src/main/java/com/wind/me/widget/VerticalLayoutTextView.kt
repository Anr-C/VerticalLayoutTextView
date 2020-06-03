package com.wind.me.widget

import android.content.Context
import android.content.res.Resources
import android.graphics.*
import android.graphics.Paint.Align
import android.text.TextUtils
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import kotlin.math.ceil

class VerticalLayoutTextView : View {
    private var mEnglishCharPadding = 0
    private var mNormalCharPadding = 0

    private var mAlwaysTransChars: CharArray
    private var mText = ""
    private var mCustomMaxHeight = 0
    private var mFontBaselinePadding = 0f
    private var mFontHeight = 0
    private var mHeight = 0
    private var mLastCanShowCharIndex = 0
    private var mLeftLinePaint: Paint? = null
    private var mLineSpace = 0
    private var mLineWidth = 0
    private var mMaxHeight = 0
    private var mMaxLines = 0
    private var mNeedEllipsizeEnd = false
    private var mNeedLeftLine = false
    private var mNeedSepLine = false
    private var mNormalCharPaddingTop = 0
    private var mOnRealLineChangeListener: OnRealLineChangeListener? = null
    private var mPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var mSepPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var mTextSize = 0
    private var mTextColor = 0
    private var mTransAfterEngChars: CharArray
    private var mChineseChars: CharArray
    private var mWidth = 0
    private var fontFamily = 0
    private var fontFamilyString: String? = null

    init {
        mPaint.textAlign = Align.CENTER
        mEnglishCharPadding = 0
        mSepPaint.strokeWidth = 3.0f
        mSepPaint.style = Paint.Style.STROKE
        mAlwaysTransChars = charArrayOf('℃', '\"', '\"', '[', ']', '(', ')', '\'', '“', '”', '［', '］', '（', '）', '《', '》')
        mChineseChars = charArrayOf(',', '.', '!', '\"', '\"', '[', ']', '(', ')', ':', '\'', '\\', '/', '·', '，', '。', '！', '“', '”', '［', '］', '（', '）', '：', '、', '／')
        mTransAfterEngChars = mChineseChars
    }

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet? = null) : super(context, attrs, 0) {
        val a = context.obtainStyledAttributes(attrs,
                R.styleable.VerticalLayoutTextView)
        mTextColor = a.getColor(R.styleable.VerticalLayoutTextView_textColor, DEFAULT_COLOR)
        mTextSize = a.getDimensionPixelSize(R.styleable.VerticalLayoutTextView_textSize,
                DEFAULT_TEXT_SIZE)
        mNeedSepLine = a.getBoolean(R.styleable.VerticalLayoutTextView_needSepLine, false)
        mText = a.getString(R.styleable.VerticalLayoutTextView_text) ?: ""
        mNeedEllipsizeEnd = a.getBoolean(R.styleable.VerticalLayoutTextView_needEllipsizeEnd, false)
        mCustomMaxHeight = a.getDimensionPixelSize(R.styleable.VerticalLayoutTextView_customMaxHeight, DEFAULT_CUSTOM_MAX_HEIGHT)
        mMaxLines = a.getInt(R.styleable.VerticalLayoutTextView_maxLines, DEFAULT_MAX_LINES)
        mNormalCharPaddingTop = a.getDimensionPixelSize(R.styleable.VerticalLayoutTextView_normalCharPaddingTop,
                DEFAULT_NORMAL_CHAR_PADDINGTOP)
        mNeedLeftLine = a.getBoolean(R.styleable.VerticalLayoutTextView_needLeftLine, false)
        mNormalCharPadding = a.getDimensionPixelSize(R.styleable.VerticalLayoutTextView_normalCharPadding, DEFAULT_NORMAL_CHAR_PADDING)
        mLineSpace = a.getDimensionPixelSize(R.styleable.VerticalLayoutTextView_lineSpace, DEFAULT_LINE_SPACE)
        fontFamily = a.getResourceId(R.styleable.VerticalLayoutTextView_fontFamilyRefer, -1)
        fontFamilyString = a.getString(R.styleable.VerticalLayoutTextView_fontFamilyString)
        a.recycle()
        setTextColor(mTextColor)
        setTextSize(mTextSize)
        setNeedLeftLine(mNeedLeftLine)
        setCusTypeface()
    }

    fun setOnRealLineChangeListener(onRealLineChangeListener: OnRealLineChangeListener?) {
        mOnRealLineChangeListener = onRealLineChangeListener
    }

    fun setNeedEllipsizeEnd(need: Boolean) {
        mNeedEllipsizeEnd = need
    }

    fun setText(contentText: String) {
        mText = contentText
        requestLayout()
        invalidate()
    }

    private fun setCusTypeface() {
        if (fontFamily != -1) {
            setTypeface(ResourcesCompat.getFont(context, fontFamily))
        } else {
            setTypeface(Typeface.create(fontFamilyString, Typeface.NORMAL))
        }
    }

    fun setCustomMaxHeight(maxHeight: Int) {
        mCustomMaxHeight = dip2px(maxHeight.toFloat())
    }

    fun setMaxLines(maxLines: Int) {
        mMaxLines = maxLines
    }

    fun setNormalCharPaddingTop(normalCharPaddingTop: Int) {
        mNormalCharPaddingTop = dip2px(normalCharPaddingTop.toFloat())
    }

    fun setNeedLeftLine(needLeftLine: Boolean) {
        mNeedLeftLine = needLeftLine
        if (mNeedLeftLine) {
            mLeftLinePaint = Paint(Paint.ANTI_ALIAS_FLAG)
            mLeftLinePaint?.strokeWidth = dip2px(2.0f).toFloat()
            mLeftLinePaint?.style = Paint.Style.STROKE
            mLeftLinePaint?.color = Color.BLACK
        }
    }

    private fun setTextSize(textSize: Int) {
        mTextSize = textSize
        mPaint.textSize = textSize.toFloat()
    }

    private fun setTextColor(textColor: Int) {
        mPaint.color = textColor
        mSepPaint.color = textColor
    }

    fun setTypeface(typeface: Typeface?) {
        mPaint.typeface = typeface
    }

    fun setCharPadding(padding: Int) {
        mNormalCharPadding = padding
    }

    fun setLineSpace(lineSpace: Int) {
        mLineSpace = dip2px(lineSpace.toFloat())
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        mHeight = layoutParams.height
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)
        if (mCustomMaxHeight == 0 || mCustomMaxHeight > heightSize) {
            mCustomMaxHeight = heightSize
        }
        mWidth = measureWidth()
        setMeasuredDimension(mWidth, mHeight)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawMultipleVerticalText(canvas)
        if (mNeedLeftLine) {
            mLeftLinePaint?.also {
                canvas.drawLine(it.strokeWidth / 2.0f, dip2px(1.0f).toFloat(), it.strokeWidth / 2.0f, mMaxHeight.toFloat(), it)
            }
        }
    }

    private fun getNormalCharPaddingTop(charHeight: Float): Float {
        return if (mNormalCharPaddingTop != 0) {
            mNormalCharPaddingTop.toFloat()
        } else charHeight
    }

    private fun isLastCharEnglish(i: Int): Boolean {
        return i > 1 && isEnglishChar(mText[i - 1])
    }

    private fun drawMultipleVerticalText(canvas: Canvas) {
        var mTextPosY = 0
        var mTextPosX = mWidth - mLineWidth + mLineWidth / 2
        var i = 0
        while (i < mText.length) {
            val ch = mText[i]
            when {
                mLastCanShowCharIndex == 0 || i != mLastCanShowCharIndex -> {
                    if (i == 0 && mNeedSepLine) {
                        mTextPosY += 99
                        canvas.drawLine(mTextPosX.toFloat(), 0.0f, mTextPosX.toFloat(), 90.0f, mPaint)
                    }
                    if (ch == '\n') {
                        mTextPosX = mTextPosX - mLineWidth - mLineSpace
                        mTextPosY = 0
                    } else {
                        val isLastCharEnglish = isLastCharEnglish(i)
                        val isSpecialChar = isSpecialChar(isLastCharEnglish, ch)
                        val charHeight = getCharHeight(ch, isLastCharEnglish)
                        if (mTextPosY == 0 || i == 0) {
                            mTextPosY = ((if (isSpecialChar) 3.0f else getNormalCharPaddingTop(3.0f)) + mTextPosY.toFloat()).toInt()
                        }
                        mTextPosY = (mTextPosY.toFloat() + charHeight).toInt()
                        when {
                            i > 0 && isEnglishChar(ch) && mTextPosY.toFloat() + charHeight > mHeight.toFloat() -> {
                                if (isEnglishChar(mText[i - 1])) {
                                    canvas.drawLine(mTextPosX.toFloat(), 3.0f + (mTextPosY.toFloat() - charHeight), mTextPosX.toFloat(), (mTextPosY - 3f), mPaint)
                                }
                                mTextPosX = mTextPosX - mLineWidth - mLineSpace
                                i--
                                mTextPosY = 0
                            }
                            mTextPosY > mHeight -> {
                                mTextPosX = mTextPosX - mLineWidth - mLineSpace
                                i--
                                mTextPosY = 0
                            }
                            isSpecialChar -> {
                                canvas.save()
                                canvas.translate(mTextPosX.toFloat(), mTextPosY.toFloat())
                                canvas.rotate(90.0f)
                                mPaint.textSize = mTextSize + dip2px(1.0f).toFloat()
                                canvas.drawText(ch.toString(), -(charHeight / 2.0f), mLineWidth / 3f, mPaint)
                                mPaint.textSize = mTextSize.toFloat()
                                canvas.restore()
                                mTextPosY += mEnglishCharPadding
                            }
                            ch != ' ' -> {
                                if (ch in mChineseChars) {
                                    canvas.drawText(ch.toString(), mTextPosX.toFloat() + mLineWidth - getCharWidthHeight(ch).width() * 1.5f, mTextPosY.toFloat() - charHeight + getCharWidthHeight(ch).height() * 1.5f - mFontBaselinePadding, mPaint)
                                } else {
                                    canvas.drawText(ch.toString(), mTextPosX.toFloat(), mTextPosY.toFloat() - mFontBaselinePadding, mPaint)
                                }
                                mTextPosY += mNormalCharPadding
                            }
                        }
                    }
                    i++
                }
                else -> {
                    if (mNeedEllipsizeEnd) {
                        if (mNormalCharPaddingTop < 0) {
                            mTextPosY -= mNormalCharPaddingTop * 2
                        }
                        mSepPaint.style = Paint.Style.FILL
                        val dotSize = mPaint.textSize / 39.0f * 6.0f
                        val posY = ((if (isLastCharEnglish(i)) dotSize else 0.0f) + mTextPosY.toFloat()).toInt()
                        canvas.drawCircle(mTextPosX.toFloat(), posY.toFloat(), dotSize / 2.0f, mSepPaint)
                        canvas.drawCircle(mTextPosX.toFloat(), posY.toFloat() + 2.0f * dotSize, dotSize / 2.0f, mSepPaint)
                        canvas.drawCircle(mTextPosX.toFloat(), posY.toFloat() + 4.0f * dotSize, dotSize / 2.0f, mSepPaint)
                        mSepPaint.style = Paint.Style.STROKE
                    }
                    return
                }
            }
        }
    }

    private fun measureWidth(): Int {
        if (TextUtils.isEmpty(mText)) {
            return 0
        }
        var h = 0
        var lineSpaceCount = 0
        measureLineWidth()
        measureFontHeight()
        var realLine = 1
        val contentLength = mText.length
        var i = 0
        while (i < contentLength) {
            if (i == 0 && mNeedSepLine) {
                h = h + DOUBLE_SEP_LINE_HEIGHT + DOUBLE_SPE_LINE_PADDING
            }
            val ch = mText[i]
            if (ch == '\n') {
                if (h > mMaxHeight) {
                    mMaxHeight = h
                }
                if (i == contentLength || mMaxLines != 0 && realLine == mMaxLines) {
                    break
                }
                realLine++
                lineSpaceCount++
                h = 0
            } else {
                val isLastCharEnglish = isLastCharEnglish(i)
                val isSpecialChar = isSpecialChar(isLastCharEnglish, ch)
                val charHeight = getCharHeight(ch, isLastCharEnglish)
                if (h == 0 || i == 0) {
                    h = ((if (isSpecialChar) 3.0f else getNormalCharPaddingTop(3.0f)) + h.toFloat()).toInt()
                }
                h = (h.toFloat() + charHeight).toInt()
                val isEnglishChar = isEnglishChar(ch)
                if (mCustomMaxHeight > 0) {
                    if (isEnglishChar) {
                        if (h.toFloat() + charHeight > mHeight.toFloat() && h.toFloat() + charHeight < mCustomMaxHeight.toFloat()) {
                            mHeight = (h.toFloat() + charHeight + 1.0f).toInt()
                        }
                    } else if (h > mHeight && h < mCustomMaxHeight) {
                        mHeight = h + 1
                    }
                }
                if ((i <= 0 || !isEnglishChar || h.toFloat() + charHeight <= mHeight.toFloat()) && h <= mHeight) {
                    if (isSpecialChar) {
                        h += mEnglishCharPadding
                    } else if (ch != ' ') {
                        h += mNormalCharPadding
                    }
                    if (i == mText.length - 1 && h > mMaxHeight) {
                        mMaxHeight = h
                    }
                } else {
                    if (h.toFloat() > mMaxHeight.toFloat() + charHeight) {
                        mMaxHeight = (h.toFloat() - charHeight).toInt()
                    }
                    if (mMaxLines == 0 || realLine != mMaxLines) {
                        realLine++
                        lineSpaceCount++
                        i--
                        h = 0
                    } else if (mNeedEllipsizeEnd) {
                        //FIX ME 有问题 不合适
                        mLastCanShowCharIndex = i - 1
                    } else {
                        mLastCanShowCharIndex = i
                    }
                }
            }
            i++
        }
        if (mOnRealLineChangeListener != null) {
            mOnRealLineChangeListener?.realLineChange(realLine)
        }
        val i2 = mLineSpace * lineSpaceCount + mLineWidth * realLine
        val dip2px: Int = if (mNeedLeftLine) {
            dip2px(10.0f)
        } else {
            0
        }
        return dip2px + i2
    }

    private fun getCharHeight(ch: Char, isLastCharEnglish: Boolean): Float {
        val isSpecialChar = isSpecialChar(isLastCharEnglish, ch)
        if (ch == ' ') {
            return dip2px(10.0f).toFloat()
        }
        if (!isSpecialChar) {
            return mFontHeight.toFloat()
        }
        val space = FloatArray(1)
        mPaint.getTextWidths(ch.toString(), space)
        return space[0]
    }

    private fun getCharWidthHeight(ch: Char): Rect {
        val space = Rect()
        val str = ch.toString()
        mPaint.getTextBounds(str, 0, str.length, space)
        return space
    }

    private fun isSpecialChar(lastCharIsEnglish: Boolean, ch: Char): Boolean {
        return ch in '0'..'9' || ch in 'a'..'z' || (ch in 'A'..'Z'
                || lastCharIsEnglish && mTransAfterEngChars.contains(ch)
                || mAlwaysTransChars.contains(ch)
                || ch.toInt() in 128..255)
    }

    private fun isEnglishChar(ch: Char): Boolean {
        return ch in 'a'..'z' || ch in 'A'..'Z'
    }

    fun isChinese(c: Char): Boolean {
        return c.toInt() in 0x4E00..0x9FA5
    }

    private fun measureLineWidth() {
        if (mLineWidth == 0) {
            val widths = FloatArray(1)
            mPaint.getTextWidths("正", widths)
            mLineWidth = ceil(widths[0].toDouble()).toInt()
        }
    }

    private fun measureFontHeight() {
        val fm = mPaint.fontMetrics
        mFontHeight = ceil((fm.bottom - fm.top).toDouble()).toInt()
        mFontBaselinePadding = fm.bottom
    }

    interface OnRealLineChangeListener {
        fun realLineChange(i: Int)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        parent?.also {
            if (it is ViewGroup) {
                it.clipChildren = false
            }
        }
    }

    companion object {
        private const val DOUBLE_SEP_LINE_HEIGHT = 90
        private const val DOUBLE_SPE_LINE_PADDING = 9
        private const val DEFAULT_TEXT_SIZE = 15
        private const val DEFAULT_COLOR = Color.BLACK
        private const val DEFAULT_CUSTOM_MAX_HEIGHT = Int.MAX_VALUE
        private const val DEFAULT_MAX_LINES = 7
        private const val DEFAULT_NORMAL_CHAR_PADDINGTOP = 0
        private const val DEFAULT_NORMAL_CHAR_PADDING = 0
        private const val DEFAULT_LINE_SPACE = 0

        fun dip2px(dpValue: Float): Int {
            return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dpValue, Resources.getSystem().displayMetrics).toInt()
        }
    }
}