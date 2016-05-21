package com.example.joseph.yipandroid3;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;

/**
 * Created by Joseph on 5/20/16.
 * This class will allow me to create my own custom circular overlay through native ImageView
 */
public class CircleOverlay extends ImageView {
    /** Content container */
    private RectF circleRect;

    /** Circle specification */
    private int radius;

    /** Color specification */
    private int color;

    /** Constructor
     * @param context Context of application calling for overlay */
    public CircleOverlay(Context context) {
        super(context);
        //In versions > 3.0 need to define layer Type
        if (android.os.Build.VERSION.SDK_INT >= 11)
        {
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }
    }

    /** Constructor
     * @param context Context of application calling for overlay
     * @param attrs AttributeSet specifying overlay attributes
     * @param color int color -- Use android.R.color Enum to easily specify */
    public CircleOverlay(Context context, AttributeSet attrs, int color) {
        super(context, attrs);
        //In versions > 3.0 need to define layer Type
        if (android.os.Build.VERSION.SDK_INT >= 11)
        {
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }
    }

    /** Set Circle size and containing RectF */
    public void setCircle(RectF rect, int radius) {
        this.circleRect = rect;
        this.radius = radius;

        // update view
        postInvalidate();
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if(this.circleRect != null) {
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setColor(getResources().getColor(android.R.color.black));
            paint.setStyle(Paint.Style.FILL);
            canvas.drawPaint(paint);

            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
            canvas.drawRoundRect(circleRect, radius, radius, paint);
        }
    }
}
