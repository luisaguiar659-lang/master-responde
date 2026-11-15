package com.masterresponde.app;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.View;

/** Gráfico de barras dos últimos 7 dias: eixo Y automático, grade, valores e animação. */
public class NeonBarChartView extends View {

    private static final int[] STEPS = {1, 2, 5, 10, 20, 25, 50, 100, 200, 250, 500, 1000, 2000, 5000};

    private final Paint grid = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint label = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint valuePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bar = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint cap = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF r = new RectF();
    private final RectF capRect = new RectF();
    private int[] values = new int[7];
    private String[] labels = {"", "", "", "", "", "", ""};
    private float grow = 1f;
    private ValueAnimator animator;

    public NeonBarChartView(Context context) {
        super(context);
        float d = getResources().getDisplayMetrics().density;
        float sd = getResources().getDisplayMetrics().scaledDensity;
        grid.setColor(Color.argb(60, 120, 170, 210));
        grid.setStrokeWidth(Math.max(1f, d * 0.8f));
        label.setColor(Color.rgb(200, 215, 230));
        label.setTextSize(8.5f * sd);
        valuePaint.setColor(Color.WHITE);
        valuePaint.setTextSize(8f * sd);
        valuePaint.setFakeBoldText(true);
        cap.setColor(Color.argb(220, 190, 250, 255));
    }

    public void setData(int[] values, String[] labels) {
        if (values != null && labels != null && java.util.Arrays.equals(this.values, values)
                && java.util.Arrays.equals(this.labels, labels)) {
            return; // nada mudou: não reinicia a animação
        }
        if (values != null && values.length == 7) {
            this.values = values.clone();
        }
        if (labels != null && labels.length == 7) {
            this.labels = labels;
        }
        if (animator != null) {
            animator.cancel();
        }
        grow = 0f;
        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(550L);
        animator.addUpdateListener(a -> {
            grow = (float) a.getAnimatedValue();
            invalidate();
        });
        animator.start();
    }

    @Override
    protected void onMeasure(int w, int h) {
        int def = Math.round(120 * getResources().getDisplayMetrics().density);
        int ww = View.MeasureSpec.getMode(w) == View.MeasureSpec.UNSPECIFIED ? def : View.MeasureSpec.getSize(w);
        int hh = View.MeasureSpec.getMode(h) == View.MeasureSpec.UNSPECIFIED ? def : View.MeasureSpec.getSize(h);
        setMeasuredDimension(ww, hh);
    }

    private int niceStep(int max) {
        int need = Math.max(1, (int) Math.ceil(max / 4.0));
        for (int s : STEPS) {
            if (s >= need) {
                return s;
            }
        }
        return (int) (Math.ceil(need / 1000.0) * 1000);
    }

    @Override
    protected void onDraw(Canvas c) {
        float d = getResources().getDisplayMetrics().density;
        int max = 0;
        for (int v : values) {
            max = Math.max(max, v);
        }
        int step = niceStep(max);
        int top = step * 4;

        float left = label.measureText(String.valueOf(top)) + 6f * d;
        float bottomLabelH = 16f * d;
        float chartTop = 6f * d;
        float chartBottom = getHeight() - bottomLabelH;
        float chartH = chartBottom - chartTop;
        float width = getWidth() - left;

        label.setColor(Color.rgb(200, 215, 230));
        for (int i = 0; i <= 4; i++) {
            float y = chartBottom - chartH * i / 4f;
            c.drawLine(left, y, getWidth(), y, grid);
            String t = String.valueOf(step * i);
            c.drawText(t, left - 4f * d - label.measureText(t), y + label.getTextSize() * 0.35f, label);
        }

        float slot = width / 7f;
        float barW = Math.min(slot * 0.56f, 16f * d);
        for (int i = 0; i < 7; i++) {
            float cx = left + slot * i + slot / 2f;
            float h = chartH * Math.min(values[i], top) / (float) top * grow;
            if (values[i] > 0 && h < 3f * d) {
                h = 3f * d;
            }
            r.set(cx - barW / 2f, chartBottom - h, cx + barW / 2f, chartBottom);

            boolean today = i == 6;
            int c1 = today ? Color.rgb(90, 245, 255) : Color.rgb(20, 220, 250);
            int c2 = today ? Color.rgb(0, 150, 240) : Color.rgb(0, 110, 205);
            bar.setShader(new LinearGradient(0, r.top, 0, r.bottom, c1, c2, Shader.TileMode.CLAMP));
            c.drawRoundRect(r, 3f * d, 3f * d, bar);

            if (values[i] > 0 && h > 5f * d) {
                capRect.set(r.left, r.top, r.right, r.top + 2f * d);
                c.drawRoundRect(capRect, 1f * d, 1f * d, cap);
            }

            if (values[i] > 0 && grow > 0.95f) {
                String v = String.valueOf(values[i]);
                float tw = valuePaint.measureText(v);
                float ty = Math.max(chartTop + valuePaint.getTextSize(), r.top - 3f * d);
                c.drawText(v, cx - tw / 2f, ty, valuePaint);
            }

            float lw = label.measureText(labels[i]);
            label.setColor(today ? Color.WHITE : Color.rgb(200, 215, 230));
            c.drawText(labels[i], cx - lw / 2f, getHeight() - 3f * d, label);
        }
    }
}
