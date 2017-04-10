package eu.faircode.netguard.view;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewDebug;
import android.widget.Checkable;

import eu.faircode.netguard.R;

/**
 * Created by Carlos on 4/9/17.
 */

public class FloatActionSwitch extends FloatingActionButton implements Checkable, View
        .OnClickListener {
    private Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int colorChecked;
    private int colorNotChecked;
    private boolean mChecked;
    private boolean mBroadcasting;
    private long mDuration;
    private long mStartAnmationTime;
    private OnCheckedChangeListener mOnCheckedChangeListener;

    public FloatActionSwitch(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, 0);
    }

    public FloatActionSwitch(final Context context, final AttributeSet attrs, final int
            defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr);
    }

    private void init(final Context context, final AttributeSet attrs, final int
            defStyleAttr) {

        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable
                .FloatActionSwitch, defStyleAttr, 0);
        mPaint.setColor(typedArray.getColor(R.styleable.FloatActionSwitch_fas_strokeColor,
                getResources().getColor(android.R.color.white)));
        mPaint.setStrokeWidth(typedArray.getDimensionPixelSize(R.styleable
                .FloatActionSwitch_fas_strokeSize, 6));

        TypedValue tv = new TypedValue();
        context.getTheme().resolveAttribute(R.attr.colorAccent, tv, true);
        int colorAccent = tv.data;

        colorChecked = typedArray.getColor(R.styleable.FloatActionSwitch_fas_colorChecked,
                colorAccent);
        colorNotChecked = typedArray.getColor(R.styleable.FloatActionSwitch_fas_colorNotCheck,
                colorAccent);
        mChecked = typedArray.getBoolean(R.styleable.FloatActionSwitch_fas_checked, false);
        if (mChecked) {
            setBackgroundTintList(ColorStateList.valueOf(colorChecked));
        } else {
            setBackgroundTintList(ColorStateList.valueOf(colorNotChecked));
        }
        mDuration = typedArray.getInteger(R.styleable.FloatActionSwitch_fas_durationMillis, 200);

        typedArray.recycle();


        super.setOnClickListener(this);
    }

    // to checked = from 100 to 0
    private void animationToState(boolean checked) {
        mStartAnmationTime = System.currentTimeMillis();
        invalidate();
    }

    @Override protected void onDraw(final Canvas canvas) {
        super.onDraw(canvas);

        Drawable drawable = getDrawable();
        if (drawable == null) {
            return;
        }

        if (mStartAnmationTime == 0 && mChecked) {
            return;
        }


        final int saveCount = canvas.getSaveCount();
        canvas.save();

        if (getCropToPadding()) {
            final int scrollX = getScrollX();
            final int scrollY = getScrollY();
            canvas.clipRect(scrollX + getPaddingLeft(), scrollY + getPaddingTop(),
                    scrollX + getRight() - getLeft() - getPaddingRight(),
                    scrollY + getBottom() - getTop() - getPaddingBottom());
        }

        canvas.translate(getPaddingLeft(), getPaddingTop());

        Rect bounds = drawable.getBounds();

        long time = System.currentTimeMillis() - mStartAnmationTime;
        long per = Math.min(time * 100 / mDuration, 100);
        per = mChecked ? 100 - per : per;

        canvas.drawLine(bounds.left, bounds.top,
                (bounds.right - bounds.left) * per / 100 + bounds.left,
                (bounds.bottom - bounds.top) * per / 100 + bounds.top,
                mPaint);

        canvas.restoreToCount(saveCount);

        if (time > mDuration) {
            mStartAnmationTime = 0;
            setBackgroundTintList(ColorStateList.valueOf(mChecked ? colorChecked :
                    colorNotChecked));
        } else {
            invalidate();
        }
    }

    @ViewDebug.ExportedProperty
    @Override public boolean isChecked() {
        return mChecked;
    }

    @Override public void setChecked(final boolean checked) {
        if (mChecked != checked) {
            mChecked = checked;

            if (this.getWindowToken() != null && ViewCompat.isLaidOut(this) && this.isShown()) {
                animationToState(checked);
            } else {
                setBackgroundTintList(ColorStateList.valueOf(mChecked ? colorChecked :
                        colorNotChecked));
            }

            // Avoid infinite recursions if setChecked() is called from a listener
            if (mBroadcasting) {
                return;
            }

            mBroadcasting = true;
            if (mOnCheckedChangeListener != null) {
                mOnCheckedChangeListener.onCheckedChanged(mChecked);
            }

            mBroadcasting = false;
        }
    }

    @Override public void toggle() {
        setChecked(!mChecked);
    }

    /**
     * Register a callback to be invoked when the checked state of this button
     * changes.
     *
     * @param listener the callback to call on checked state change
     */
    public void setOnCheckedChangeListener(OnCheckedChangeListener listener) {
        mOnCheckedChangeListener = listener;
    }

    @Override public void onClick(final View v) {
        toggle();
    }

    /**
     * Interface definition for a callback to be invoked when the checked state
     * of a compound button changed.
     */
    public static interface OnCheckedChangeListener {
        /**
         * Called when the checked state of a compound button has changed.
         *
         * @param isChecked The new checked state of buttonView.
         */
        void onCheckedChanged(boolean isChecked);
    }
}
