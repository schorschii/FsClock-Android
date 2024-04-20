package systems.sieber.fsclock;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;

import androidx.annotation.NonNull;

public class DigitalClockView extends View {

    final static float MIN_SIZE = 0.8f;
    final static float SEC_SIZE = 0.19f;
    final static String MIN_MEASURE_DUMMY = "00:00";
    final static String SEC_MEASURE_DUMMY = "00";

    String mTextMin = "00:00";
    String mTextSec = "00";
    boolean mShowSec = true;
    float mXCorr = 0f;

    Rect mBoundsMin = new Rect();
    Rect mBoundsSec = new Rect();

    private Paint mPaintMin;
    private Paint mPaintSec;
    private int mGravity = Gravity.CENTER;

    public DigitalClockView(Context context) {
        super(context);
        commonInit();
    }
    public DigitalClockView(Context context, AttributeSet attrs) {
        super(context, attrs);
        commonInit();

        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.DigitalClockView, 0, 0);
        try {
            mGravity = a.getInteger(R.styleable.DigitalClockView_android_gravity, Gravity.TOP);
        } finally {
            a.recycle();
        }
    }
    void commonInit() {
        mPaintMin = new Paint();
        mPaintMin.setColor(Color.WHITE);
        mPaintMin.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL));
        mPaintMin.setTextSize(25);
        mPaintMin.setTextAlign(Paint.Align.CENTER);

        mPaintSec = new Paint();
        mPaintSec.setColor(Color.WHITE);
        mPaintSec.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL));
        mPaintSec.setTextSize(25);
    }

    void setColor(int c) {
        mPaintMin.setColor(c);
        mPaintSec.setColor(c);
        invalidate();
    }
    void setTypeface(Typeface tf, float xCorr) {
        mPaintMin.setTypeface(tf);
        mPaintSec.setTypeface(tf);
        mXCorr = xCorr;
        invalidate();
    }
    void setText(String min, String sec) {
        mTextMin = min;
        mTextSec = sec;
        invalidate();
    }
    void setShowSec(boolean showSec) {
        mShowSec = showSec;
        invalidate();
    }
    public int getGravity() {
        return mGravity;
    }
    public void setGravity(int gravity) {
        if(mGravity != gravity) {
            mGravity = gravity;
            invalidate();
        }
    }

    float calcMinSizeHeight(int w, int h) {
        float minWidth = mShowSec ? w * MIN_SIZE : w;
        setTextSizeForWidth(mPaintMin, minWidth, MIN_MEASURE_DUMMY);
        mPaintMin.getTextBounds(MIN_MEASURE_DUMMY, 0, MIN_MEASURE_DUMMY.length(), mBoundsMin);
        setTextSizeForWidth(mPaintSec, w * SEC_SIZE, SEC_MEASURE_DUMMY);
        mPaintSec.getTextBounds(SEC_MEASURE_DUMMY, 0, SEC_MEASURE_DUMMY.length(), mBoundsSec);
        if(mBoundsMin.height() > h) {
            setTextSizeForHeight(mPaintMin, h, MIN_MEASURE_DUMMY);
            mPaintMin.getTextBounds(MIN_MEASURE_DUMMY, 0, MIN_MEASURE_DUMMY.length(), mBoundsMin);
            setTextSizeForHeight(mPaintSec, h/2f, SEC_MEASURE_DUMMY);
            mPaintSec.getTextBounds(SEC_MEASURE_DUMMY, 0, SEC_MEASURE_DUMMY.length(), mBoundsSec);
        }
        return mBoundsMin.height(); //paint.getFontMetrics().descent - paint.getFontMetrics().ascent;
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        //canvas.drawLine(0, 0, getWidth(), getHeight(), mPaintMin);

        int canvasWidth = getWidth();
        int canvasHeight = getHeight();
        float h = calcMinSizeHeight(canvasWidth, canvasHeight);
        float y;
        if(mGravity == Gravity.BOTTOM) {
            y = getHeight() - 4;
        } else {
            y = (getHeight() / 2f) + (h / 2f);
        }

        float x_corr = mXCorr * mPaintMin.getTextSize();
        if(mShowSec) {
            float fullWidth = mBoundsMin.width() + mBoundsSec.width() + x_corr;
            canvas.drawText(mTextMin, (canvasWidth/2f) - (fullWidth / 2f) + (mBoundsMin.width()/2f), y, mPaintMin);
            canvas.drawText(mTextSec, (canvasWidth/2f) - (fullWidth / 2f) + mBoundsMin.width() + x_corr, y, mPaintSec);
        } else {
            canvas.drawText(mTextMin, canvasWidth/2f, y, mPaintMin);
        }
    }

    /*@Override
    protected void onSizeChanged(int w, int h, int oldWidth, int oldHeight) {
        super.onSizeChanged(w, h, oldWidth, oldHeight);
    }*/

    /*@Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        int width;
        int height;

        calcMinSizeHeight(widthSize);
        if(widthMode == MeasureSpec.EXACTLY) { // fixed width, variable height
            width = widthSize;
            height = MeasureSpec.makeMeasureSpec((int) Math.floor(mBoundsMin.height()), MeasureSpec.AT_MOST);
        } else {
            width = MeasureSpec.makeMeasureSpec((int) mBoundsMin.width(), MeasureSpec.AT_MOST);
            height = MeasureSpec.makeMeasureSpec((int) mBoundsMin.height(), MeasureSpec.AT_MOST);
        }

        setMeasuredDimension(width, height);
    }*/

    // Pick a reasonably large value for the test. Larger values produce
    // more accurate results, but may cause problems with hardware
    // acceleration. But there are workarounds for that, too; refer to
    // http://stackoverflow.com/questions/6253528/font-size-too-large-to-fit-in-cache
    final static float TEST_TEXT_SIZE = 48f;
    private static void setTextSizeForWidth(Paint paint, float desiredWidth, String text) {
        // Get the bounds of the text, using our testTextSize.
        paint.setTextSize(TEST_TEXT_SIZE);
        float width = paint.measureText(text, 0, text.length());

        // Calculate the desired size as a proportion of our testTextSize.
        float desiredTextSize = TEST_TEXT_SIZE * desiredWidth / width;

        // Set the paint for that size.
        paint.setTextSize(desiredTextSize);
    }
    private static void setTextSizeForHeight(Paint paint, float desiredHeight, String text) {
        // Get the bounds of the text, using our testTextSize.
        paint.setTextSize(TEST_TEXT_SIZE);
        Rect bounds = new Rect();
        paint.getTextBounds(text, 0, text.length(), bounds);

        // Calculate the desired size as a proportion of our testTextSize.
        float desiredTextSize = TEST_TEXT_SIZE * desiredHeight / bounds.height();

        // Set the paint for that size.
        paint.setTextSize(desiredTextSize * 0.97f);
    }

}
