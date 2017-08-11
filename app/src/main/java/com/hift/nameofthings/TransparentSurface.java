package com.hift.nameofthings;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
 * Created by TeguhAS on 04-Aug-17.
 */

public class TransparentSurface extends SurfaceView implements SurfaceHolder.Callback {
    final int sz = 300;
    final int halfWidth = 540;
    final int halfHeight = 960;

    public TransparentSurface(Context context) {
        super(context);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        drawRect(holder, halfWidth - sz, halfHeight - sz, halfWidth + sz, halfHeight + sz, Color.RED);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        drawRect(holder, halfWidth - sz, halfHeight - sz, halfWidth + sz, halfHeight + sz, Color.RED);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
    }

    private void drawRect(SurfaceHolder holder, float RectLeft, float RectTop, float RectRight, float RectBottom, int color) {
        Canvas canvas = holder.lockCanvas();
        canvas.drawColor(0, PorterDuff.Mode.CLEAR);
        //border's properties
        Paint paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(color);
        paint.setStrokeWidth(3);
        canvas.drawRect(RectLeft, RectTop, RectRight, RectBottom, paint);
        holder.unlockCanvasAndPost(canvas);
    }
}
