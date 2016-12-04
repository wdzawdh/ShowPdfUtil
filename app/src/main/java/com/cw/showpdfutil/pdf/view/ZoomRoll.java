/**
 *   @function:$
 *   @description: $
 *   @param:$
 *   @return:$
 *   @history:
 * 1.date:$ $
 *           author:$
 *           modification:
 */

package com.cw.showpdfutil.pdf.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Scroller;

import com.cw.showpdfutil.R;

import org.ebookdroid.core.models.ZoomModel;

/**
 * @author Cw
 * @date 16/12/3
 */
public class ZoomRoll extends View {

    private static final int MAX_VALUE = 1000;
    private static final float MULTIPLIER = 400.0f;

    private Bitmap left;
    private Bitmap right;
    private Bitmap center;
    private Bitmap serifs;
    private float lastX;
    private Scroller scroller;
    private ZoomModel zoomModel;
    private VelocityTracker velocityTracker;

    public ZoomRoll(Context context, ZoomModel zoomModel) {
        super(context);
        this.zoomModel = zoomModel;
        left = BitmapFactory.decodeResource(context.getResources(), R.drawable.widget_pdfviewer_icon_left);
        right = BitmapFactory.decodeResource(context.getResources(), R.drawable.widget_pdfviewer_icon_right);
        center = BitmapFactory.decodeResource(context.getResources(), R.drawable.widget_pdfviewer_icon_center);
        serifs = BitmapFactory.decodeResource(context.getResources(), R.drawable.widget_pdfviewer_icon_serifs);

        scroller = new Scroller(context);

        setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), Math.max(left.getHeight(), right.getHeight()));
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        final Paint paint = new Paint();
        canvas.drawBitmap(center, new Rect(0, 0, center.getWidth(), center.getHeight()), new Rect(0, 0, getWidth(), getHeight()), paint);
        float currentOffset = -getCurrentValue() % 40;
        while (currentOffset < getWidth()) {
            canvas.drawBitmap(serifs, currentOffset, (getHeight() - serifs.getHeight()) / 2.0f, paint);
            currentOffset += serifs.getWidth();
        }
        canvas.drawBitmap(left, 0, 0, paint);
        canvas.drawBitmap(right, getWidth() - right.getWidth(), getHeight() - right.getHeight(), paint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        super.onTouchEvent(ev);

        if (velocityTracker == null) {
            velocityTracker = VelocityTracker.obtain();
        }
        velocityTracker.addMovement(ev);

        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (!scroller.isFinished()) {
                    scroller.abortAnimation();
                    zoomModel.commit();
                }
                lastX = ev.getX();
                break;
            case MotionEvent.ACTION_MOVE:
                setCurrentValue(getCurrentValue() - (ev.getX() - lastX));
                lastX = ev.getX();
                break;
            case MotionEvent.ACTION_UP:
                velocityTracker.computeCurrentVelocity(1000);
                scroller.fling((int) getCurrentValue(), 0, (int) -velocityTracker.getXVelocity(), 0, 0, MAX_VALUE, 0, 0);
                velocityTracker.recycle();
                velocityTracker = null;
                if (!scroller.computeScrollOffset()) {
                    zoomModel.commit();
                }
                break;
        }
        invalidate();
        return true;
    }

    @Override
    public void computeScroll() {
        if (scroller.computeScrollOffset()) {
            setCurrentValue(scroller.getCurrX());
            invalidate();
        } else {
            zoomModel.commit();
        }
    }

    public float getCurrentValue() {
        return (zoomModel.getZoom() - 1.0f) * MULTIPLIER;
    }

    public void setCurrentValue(float currentValue) {
        if (currentValue < 0.0) currentValue = 0.0f;
        if (currentValue > MAX_VALUE) currentValue = MAX_VALUE;
        final float zoom = 1.0f + currentValue / MULTIPLIER;
        zoomModel.setZoom(zoom);
    }
}
