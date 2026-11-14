package com.masterresponde.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.View;

/** Gráfico de barras dos últimos 7 dias com eixo Y e rótulos dos dias. */
public class NeonBarChartView extends View {

    private final Paint grid = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint label = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bar = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF r = new RectF();
    private int[] values = new int[7];
    private String[] labels = {"", "", "", "", "", "", ""};

    public NeonBarChartView(Context context) {
        super(context);
        float d = getResources().getDisplayMetrics().density;
        float sd = getResources().getDisplayMetrics().scaledDensity;
        grid.setColor(Color.argb(70, 120, 170, 210));
        grid.setStrokeWidth(Math.max(1f, d * 0.8f));
        label.setColor(Color.rgb(200, 215, 230));
        label.setTextSize(8.5f * sd);
    }

    public void setData(int[] values, String[] labels) {
        if (values != null && values.length == 7) {
            this.values = values;
        }
        if (labels != null && labels.length == 7) {
            this.labels = labels;
        }
        invalidate();
    }

    @Override
    protected void onMeasure(int w, int h) {
        int def = Math.round(120 * getResources().getDisplayMetrics().density);
        int ww = View.MeasureSpec.getMode(w) == View.MeasureSpec.UNSPECIFIED ? def : View.MeasureSpec.getSize(w);
        int hh = View.MeasureSpec.getMode(h) == View.MeasureSpec.UNSPECIFIED ? def : View.MeasureSpec.getSize(h);
        setMeasuredDimension(ww, hh);
    }

    @Override
    protected void onDraw(Canvas c) {
        float d = getResources().getDisplayMetrics().density;
        int max = 0;
        for (int v : values) {
            max = Math.max(max, v);
        }
        int step = 20;
        if (max > 80) {
            step = (int) (Math.ceil(max / 4.0 / 10.0) * 10);
        } else if (max <= 8) {
            step = 2;
        } else if (max <= 40) {
            step = 10;
        }
        int top = step * 4;

        float left = label.measureText(String.valueOf(top)) + 6f * d;
        float bottomLabelH = 16f * d;
        float chartTop = 6f * d;
        float chartBottom = getHeight() - bottomLabelH;
        float chartH = chartBottom - chartTop;
        float width = getWidth() - left;

        for (int i = 0; i <= 4; i++) {
            float y = chartBottom - chartH * i / 4f;
            c.drawLine(left, y, getWidth(), y, grid);
            String t = String.valueOf(step * i);
            c.drawText(t, left - 4f * d - label.measureText(t), y + label.getTextSize() * 0.35f, label);
        }

        float slot = width / 7f;
        float barW = slot * 0.52f;
        for (int i = 0; i < 7; i++) {
            float cx = left + slot * i + slot / 2f;
            float h = chartH * Math.min(values[i], top) / (float) top;
            if (values[i] > 0 && h < 3f * d) {
                h = 3f * d;
            }
            r.set(cx - barW / 2f, chartBottom - h, cx + barW / 2f, chartBottom);
            bar.setShader(new LinearGradient(0, r.top, 0, r.bottom,
                    Color.rgb(20, 235, 255), Color.rgb(0, 120, 215), Shader.TileMode.CLAMP));
            c.drawRoundRect(r, 2f * d, 2f * d, bar);
            float tw = label.measureText(labels[i]);
            c.drawText(labels[i], cx - tw / 2f, getHeight() - 3f * d, label);
        }
    }
}
