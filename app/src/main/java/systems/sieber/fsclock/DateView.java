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

public class DateView extends View {

    Rect mBoundsMin = new Rect();
    String mText = "";

    private Paint mPaintMin;
    private int mGravity = Gravity.CENTER;

    public DateView(Context context) {
        super(context);
        commonInit();
    }
    public DateView(Context context, AttributeSet attrs) {
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
        mPaintMin.setTextSize(150);
    }

    void setColor(int c) {
        mPaintMin.setColor(c);
        invalidate();
    }
    void setTypeface(Typeface tf) {
        mPaintMin.setTypeface(tf);
        invalidate();
    }
    void setText(String text) {
        if(!mText.equals(text)) {
            mText = text;
            invalidate();
        }
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

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        //canvas.drawLine(0, 0, getWidth(), getHeight(), mPaintMin);

        String[] lines = mText.split("\n");
        int canvasWidth = getWidth();
        int canvasHeight = getHeight();

        // determine font size by width (longest line)
        boolean isFirstRun = true;
        for(String line : lines) {
            float s1 = mPaintMin.getTextSize();
            setTextSizeForWidth(mPaintMin, canvasWidth, line);
            if(!isFirstRun && s1 < mPaintMin.getTextSize()) {
                mPaintMin.setTextSize(s1);
            }
            isFirstRun = false;
        }
        // determine font size by lines height
        float s1 = mPaintMin.getTextSize();
        setTextSizeForHeight(mPaintMin, canvasHeight, mText);
        if(s1 < mPaintMin.getTextSize()) {
            mPaintMin.setTextSize(s1);
        }

        // get full text height
        float textHeight = 0;
        for(String line : lines) {
            mPaintMin.getTextBounds(line, 0, line.length(), mBoundsMin);
            textHeight += mBoundsMin.height() + mBoundsMin.bottom;
        }

        // set vertical starting point
        float y;
        if(mGravity == Gravity.BOTTOM) {
            y = canvasHeight - 4;
        } else {
            y = (canvasHeight / 2f) - (textHeight / 2f);
        }

        // draw all lines
        for(String line : lines) {
            mPaintMin.getTextBounds(line, 0, line.length(), mBoundsMin);
            canvas.drawText(line, canvasWidth/2f - (mBoundsMin.width()+mBoundsMin.left)/2f, y + (mBoundsMin.height()-mBoundsMin.bottom), mPaintMin);
            y += mPaintMin.getTextSize() + mBoundsMin.bottom;
        }
    }

    // Pick a reasonably large value for the test. Larger values produce
    // more accurate results, but may cause problems with hardware
    // acceleration. But there are workarounds for that, too; refer to
    // http://stackoverflow.com/questions/6253528/font-size-too-large-to-fit-in-cache
    final static float TEST_TEXT_SIZE = 48f;
    private static void setTextSizeForWidth(Paint paint, float desiredWidth, String text) {
        // Get the bounds of the text, using our testTextSize.
        paint.setTextSize(TEST_TEXT_SIZE);
        Rect bounds = new Rect();
        paint.getTextBounds(text, 0, text.length(), bounds);

        // Calculate the desired size as a proportion of our testTextSize.
        float desiredTextSize = TEST_TEXT_SIZE * desiredWidth / (bounds.width() + bounds.left);

        // Set the paint for that size.
        paint.setTextSize(desiredTextSize * 0.96f);
    }
    private static void setTextSizeForHeight(Paint paint, float desiredHeight, String text) {
        // Get the bounds of the text, using our testTextSize.
        paint.setTextSize(TEST_TEXT_SIZE);
        Rect bounds = new Rect();
        paint.getTextBounds(text, 0, text.length(), bounds);

        // Calculate the desired size as a proportion of our testTextSize.
        float desiredTextSize = TEST_TEXT_SIZE * desiredHeight / ((bounds.height() + bounds.bottom) * text.split("\n").length);

        // Set the paint for that size.
        paint.setTextSize(desiredTextSize * 0.90f);
    }

}
