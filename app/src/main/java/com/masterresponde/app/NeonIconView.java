package com.masterresponde.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.View;

/**
 * Ícones de traço fino desenhados em uma grade de 24x24 (estilo das telas neon).
 * Não depende de fontes, emojis nem de arquivos de imagem.
 */
public class NeonIconView extends View {

    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final RectF rect = new RectF();
    private String name;
    private int color;

    public NeonIconView(Context context, String name, int color) {
        super(context);
        this.name = name;
        this.color = color;
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeCap(Paint.Cap.ROUND);
        stroke.setStrokeJoin(Paint.Join.ROUND);
        fill.setStyle(Paint.Style.FILL);
        applyColor();
    }

    public void setIcon(String name, int color) {
        this.name = name;
        this.color = color;
        applyColor();
        invalidate();
    }

    private void applyColor() {
        stroke.setColor(color);
        fill.setColor(color);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int w = View.MeasureSpec.getSize(widthMeasureSpec);
        int h = View.MeasureSpec.getSize(heightMeasureSpec);
        int wm = View.MeasureSpec.getMode(widthMeasureSpec);
        int hm = View.MeasureSpec.getMode(heightMeasureSpec);
        int def = Math.round(24 * getResources().getDisplayMetrics().density);
        setMeasuredDimension(
                wm == View.MeasureSpec.UNSPECIFIED ? def : w,
                hm == View.MeasureSpec.UNSPECIFIED ? def : h
        );
    }

    @Override
    protected void onDraw(Canvas c) {
        float sx = getWidth() / 24f;
        float sy = getHeight() / 24f;
        float s = Math.min(sx, sy);
        c.save();
        c.translate((getWidth() - 24f * s) / 2f, (getHeight() - 24f * s) / 2f);
        c.scale(s, s);
        stroke.setStrokeWidth(1.9f);
        draw(c, name == null ? "" : name);
        c.restore();
    }

    private void line(Canvas c, float a, float b, float x, float y) {
        c.drawLine(a, b, x, y, stroke);
    }

    private void poly(Canvas c, float... p) {
        path.reset();
        path.moveTo(p[0], p[1]);
        for (int i = 2; i + 1 < p.length; i += 2) {
            path.lineTo(p[i], p[i + 1]);
        }
        c.drawPath(path, stroke);
    }

    private void circle(Canvas c, float x, float y, float r) {
        c.drawCircle(x, y, r, stroke);
    }

    private void dot(Canvas c, float x, float y, float r) {
        c.drawCircle(x, y, r, fill);
    }

    private void box(Canvas c, float l, float t, float r, float b, float rad) {
        rect.set(l, t, r, b);
        c.drawRoundRect(rect, rad, rad, stroke);
    }

    private void draw(Canvas c, String n) {
        switch (n) {
            case "menu":
                line(c, 4, 6.5f, 20, 6.5f);
                line(c, 4, 12, 20, 12);
                line(c, 4, 17.5f, 20, 17.5f);
                break;
            case "bell":
                path.reset();
                path.moveTo(5, 18);
                path.lineTo(7, 16);
                path.lineTo(7, 10);
                rect.set(7, 4, 17, 14);
                path.arcTo(rect, 180, 180);
                path.lineTo(17, 16);
                path.lineTo(19, 18);
                path.close();
                c.drawPath(path, stroke);
                rect.set(9.8f, 17.5f, 14.2f, 21.5f);
                c.drawArc(rect, 0, 180, false, stroke);
                break;
            case "whatsapp":
                circle(c, 12, 11.5f, 8.2f);
                poly(c, 4.6f, 20.2f, 5.6f, 16.2f);
                poly(c, 4.6f, 20.2f, 8.8f, 18.8f);
                path.reset();
                path.moveTo(8.8f, 8.2f);
                path.quadTo(9.2f, 14.2f, 15.4f, 15.2f);
                c.drawPath(path, stroke);
                break;
            case "play":
                path.reset();
                path.moveTo(7, 4.5f);
                path.lineTo(19.5f, 12);
                path.lineTo(7, 19.5f);
                path.close();
                c.drawPath(path, stroke);
                break;
            case "layers":
                path.reset();
                path.moveTo(12, 3);
                path.lineTo(21, 8);
                path.lineTo(12, 13);
                path.lineTo(3, 8);
                path.close();
                c.drawPath(path, stroke);
                poly(c, 3, 12.2f, 12, 17.2f, 21, 12.2f);
                poly(c, 3, 16.2f, 12, 21.2f, 21, 16.2f);
                break;
            case "robot":
                box(c, 5, 7.5f, 19, 19.5f, 4);
                line(c, 12, 7.5f, 12, 4.2f);
                dot(c, 12, 3.6f, 1.3f);
                dot(c, 9.3f, 12.8f, 1.5f);
                dot(c, 14.7f, 12.8f, 1.5f);
                line(c, 9.5f, 16.4f, 14.5f, 16.4f);
                line(c, 2.6f, 12, 2.6f, 15.5f);
                line(c, 21.4f, 12, 21.4f, 15.5f);
                break;
            case "chat":
                rect.set(3, 3.5f, 21, 18);
                c.drawOval(rect, stroke);
                poly(c, 7.5f, 17, 5.5f, 21.2f, 11.5f, 18.2f);
                break;
            case "reply":
                stroke.setStrokeWidth(2.6f);
                path.reset();
                path.moveTo(5, 11);
                path.lineTo(13, 11);
                path.quadTo(20.5f, 11, 20.5f, 19);
                c.drawPath(path, stroke);
                poly(c, 10, 5.5f, 4.5f, 11, 10, 16.5f);
                break;
            case "warning":
                path.reset();
                path.moveTo(12, 3.5f);
                path.lineTo(21.5f, 20);
                path.lineTo(2.5f, 20);
                path.close();
                c.drawPath(path, stroke);
                line(c, 12, 9.5f, 12, 14.3f);
                dot(c, 12, 17.2f, 1.15f);
                break;
            case "rocket":
                path.reset();
                path.moveTo(12, 2.5f);
                path.quadTo(18.2f, 6, 16.6f, 14.5f);
                path.lineTo(7.4f, 14.5f);
                path.quadTo(5.8f, 6, 12, 2.5f);
                path.close();
                c.drawPath(path, stroke);
                circle(c, 12, 9, 1.8f);
                poly(c, 7.6f, 11.5f, 4.6f, 15.5f, 7.6f, 15.5f);
                poly(c, 16.4f, 11.5f, 19.4f, 15.5f, 16.4f, 15.5f);
                poly(c, 10, 17.5f, 12, 21.5f, 14, 17.5f);
                break;
            case "person_add":
                circle(c, 9, 8, 3.4f);
                path.reset();
                path.moveTo(2.8f, 20.5f);
                path.quadTo(3, 13.8f, 9, 13.8f);
                path.quadTo(15, 13.8f, 15.2f, 20.5f);
                c.drawPath(path, stroke);
                line(c, 18.5f, 5.5f, 18.5f, 11.5f);
                line(c, 15.5f, 8.5f, 21.5f, 8.5f);
                break;
            case "people":
                circle(c, 12, 7.5f, 3);
                circle(c, 5.3f, 9.8f, 2.2f);
                circle(c, 18.7f, 9.8f, 2.2f);
                path.reset();
                path.moveTo(6.6f, 20.5f);
                path.quadTo(6.6f, 13.2f, 12, 13.2f);
                path.quadTo(17.4f, 13.2f, 17.4f, 20.5f);
                c.drawPath(path, stroke);
                path.reset();
                path.moveTo(1.8f, 18);
                path.quadTo(1.8f, 13.8f, 5.3f, 13.8f);
                c.drawPath(path, stroke);
                path.reset();
                path.moveTo(22.2f, 18);
                path.quadTo(22.2f, 13.8f, 18.7f, 13.8f);
                c.drawPath(path, stroke);
                break;
            case "person":
                circle(c, 12, 8, 4);
                path.reset();
                path.moveTo(4.5f, 21);
                path.quadTo(4.5f, 14, 12, 14);
                path.quadTo(19.5f, 14, 19.5f, 21);
                c.drawPath(path, stroke);
                break;
            case "gear":
                path.reset();
                for (int i = 0; i < 8; i++) {
                    float a = i * 45f;
                    float[] angles = {a - 17f, a - 10f, a + 10f, a + 17f};
                    float[] radii = {7f, 9.4f, 9.4f, 7f};
                    for (int k = 0; k < 4; k++) {
                        double rad = Math.toRadians(angles[k]);
                        float px = 12f + (float) (radii[k] * Math.cos(rad));
                        float py = 12f + (float) (radii[k] * Math.sin(rad));
                        if (i == 0 && k == 0) {
                            path.moveTo(px, py);
                        } else {
                            path.lineTo(px, py);
                        }
                    }
                }
                path.close();
                c.drawPath(path, stroke);
                circle(c, 12, 12, 3.1f);
                break;
            case "home":
                path.reset();
                path.moveTo(3, 11.5f);
                path.lineTo(12, 3.5f);
                path.lineTo(21, 11.5f);
                path.lineTo(18.5f, 11.5f);
                path.lineTo(18.5f, 20.5f);
                path.lineTo(14, 20.5f);
                path.lineTo(14, 15);
                path.lineTo(10, 15);
                path.lineTo(10, 20.5f);
                path.lineTo(5.5f, 20.5f);
                path.lineTo(5.5f, 11.5f);
                path.close();
                c.drawPath(path, fill);
                break;
            case "bars":
                stroke.setStrokeWidth(3.4f);
                line(c, 5.5f, 20, 5.5f, 14);
                line(c, 12, 20, 12, 5);
                line(c, 18.5f, 20, 18.5f, 10);
                break;
            case "pulse":
                poly(c, 1.8f, 12, 7, 12, 9.6f, 5.5f, 13.2f, 18.5f, 15.8f, 12, 22.2f, 12);
                break;
            case "stopwatch":
                circle(c, 12, 13.5f, 7.6f);
                line(c, 9.5f, 3, 14.5f, 3);
                line(c, 12, 3, 12, 5.8f);
                line(c, 12, 13.5f, 15.6f, 10);
                line(c, 18, 6.5f, 19.4f, 5.1f);
                break;
            case "clipboard":
                box(c, 4.8f, 4.5f, 19.2f, 21.5f, 2.2f);
                box(c, 8.8f, 2.3f, 15.2f, 6.6f, 1.6f);
                line(c, 8.5f, 11, 15.5f, 11);
                line(c, 8.5f, 14.5f, 15.5f, 14.5f);
                line(c, 8.5f, 18, 12.5f, 18);
                break;
            case "key":
                circle(c, 7.2f, 12, 3.8f);
                line(c, 11, 12, 21.5f, 12);
                line(c, 17, 12, 17, 15.8f);
                line(c, 20.2f, 12, 20.2f, 15f);
                break;
            case "book":
                path.reset();
                path.moveTo(12, 6.5f);
                path.quadTo(7, 3.8f, 2.5f, 5.2f);
                path.lineTo(2.5f, 18.8f);
                path.quadTo(7, 17.4f, 12, 20f);
                path.quadTo(17, 17.4f, 21.5f, 18.8f);
                path.lineTo(21.5f, 5.2f);
                path.quadTo(17, 3.8f, 12, 6.5f);
                c.drawPath(path, stroke);
                line(c, 12, 6.5f, 12, 20);
                break;
            case "msgtext":
                box(c, 3, 3.8f, 21, 17, 3.2f);
                poly(c, 8, 17, 6.2f, 21f, 12.2f, 17);
                line(c, 7, 8.6f, 17, 8.6f);
                line(c, 7, 12.2f, 13.2f, 12.2f);
                break;
            case "monitor":
                box(c, 2.5f, 3.8f, 21.5f, 17, 2.2f);
                line(c, 12, 17, 12, 20.5f);
                line(c, 8, 20.5f, 16, 20.5f);
                poly(c, 6, 13, 9.2f, 9.8f, 12.2f, 12.4f, 17, 8);
                break;
            case "clock":
                circle(c, 12, 12, 9);
                poly(c, 12, 6.8f, 12, 12, 15.6f, 14.2f);
                break;
            case "bolt":
                path.reset();
                path.moveTo(13.6f, 2.4f);
                path.lineTo(5, 13.6f);
                path.lineTo(11, 13.6f);
                path.lineTo(10, 21.6f);
                path.lineTo(19, 10f);
                path.lineTo(13f, 10f);
                path.close();
                c.drawPath(path, fill);
                break;
            case "database":
                rect.set(4.5f, 3, 19.5f, 8.5f);
                c.drawOval(rect, stroke);
                line(c, 4.5f, 5.75f, 4.5f, 18.2f);
                line(c, 19.5f, 5.75f, 19.5f, 18.2f);
                rect.set(4.5f, 9.5f, 19.5f, 15f);
                c.drawArc(rect, 0, 180, false, stroke);
                rect.set(4.5f, 15.5f, 19.5f, 21f);
                c.drawArc(rect, 0, 180, false, stroke);
                break;
            case "back":
                line(c, 4.5f, 12, 19.5f, 12);
                poly(c, 11, 5, 4.5f, 12, 11, 19);
                break;
            case "chevron":
                poly(c, 9, 5, 16, 12, 9, 19);
                break;
            case "close":
                line(c, 5, 5, 19, 19);
                line(c, 19, 5, 5, 19);
                break;
            case "slashes":
                // dois paralelogramos preenchidos ( // ), como nos títulos do menu de referência
                path.reset();
                path.moveTo(2.5f, 20);
                path.lineTo(8, 20);
                path.lineTo(13.5f, 4);
                path.lineTo(8, 4);
                path.close();
                c.drawPath(path, fill);
                path.reset();
                path.moveTo(11, 20);
                path.lineTo(16.5f, 20);
                path.lineTo(22, 4);
                path.lineTo(16.5f, 4);
                path.close();
                c.drawPath(path, fill);
                break;
            default:
                circle(c, 12, 12, 6);
                break;
        }
    }
}
