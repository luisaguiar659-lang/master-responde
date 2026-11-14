package com.masterresponde.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.SweepGradient;
import android.graphics.Typeface;
import android.view.View;

/** Anel de progresso (taxa de resposta) com número e legenda no centro. */
public class NeonRingView extends View {

    private final Paint track = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint arc = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glow = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint value = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint caption = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF oval = new RectF();
    private int percent;
    private String captionText = "Taxa de resposta";

    public NeonRingView(Context context) {
        super(context);
        float d = getResources().getDisplayMetrics().density;
        float sd = getResources().getDisplayMetrics().scaledDensity;

        track.setStyle(Paint.Style.STROKE);
        track.setStrokeWidth(9f * d);
        track.setColor(Color.rgb(10, 52, 44));

        arc.setStyle(Paint.Style.STROKE);
        arc.setStrokeWidth(9f * d);
        arc.setStrokeCap(Paint.Cap.ROUND);

        glow.setStyle(Paint.Style.STROKE);
        glow.setStrokeWidth(15f * d);
        glow.setStrokeCap(Paint.Cap.ROUND);

        value.setColor(Color.WHITE);
        value.setTextAlign(Paint.Align.CENTER);
        value.setTypeface(Typeface.create("sans-serif-black", Typeface.NORMAL));
        value.setTextSize(24f * sd);

        caption.setColor(Color.rgb(205, 222, 235));
        caption.setTextAlign(Paint.Align.CENTER);
        caption.setTextSize(9f * sd);
    }

    public void setPercent(int percent) {
        this.percent = Math.max(0, Math.min(100, percent));
        invalidate();
    }

    @Override
    protected void onMeasure(int w, int h) {
        int def = Math.round(92 * getResources().getDisplayMetrics().density);
        int ww = View.MeasureSpec.getMode(w) == View.MeasureSpec.UNSPECIFIED ? def : View.MeasureSpec.getSize(w);
        int hh = View.MeasureSpec.getMode(h) == View.MeasureSpec.UNSPECIFIED ? def : View.MeasureSpec.getSize(h);
        setMeasuredDimension(ww, hh);
    }

    @Override
    protected void onDraw(Canvas c) {
        float d = getResources().getDisplayMetrics().density;
        float pad = 10f * d;
        float size = Math.min(getWidth(), getHeight());
        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;
        float half = size / 2f - pad;
        oval.set(cx - half, cy - half, cx + half, cy + half);

        c.drawArc(oval, 0, 360, false, track);

        float sweep = 360f * percent / 100f;
        if (percent > 0) {
            SweepGradient g = new SweepGradient(cx, cy,
                    new int[]{Color.rgb(0, 190, 120), Color.rgb(30, 255, 120), Color.rgb(0, 190, 120)},
                    null);
            c.save();
            c.rotate(-90f, cx, cy);
            glow.setShader(g);
            glow.setAlpha(60);
            arc.setShader(g);
            c.drawArc(oval, 0, sweep, false, glow);
            c.drawArc(oval, 0, sweep, false, arc);
            c.restore();
        }

        c.drawText(percent + "%", cx, cy + value.getTextSize() * 0.12f, value);
        c.drawText(captionText, cx, cy + value.getTextSize() * 0.12f + caption.getTextSize() * 1.45f, caption);
    }
}
