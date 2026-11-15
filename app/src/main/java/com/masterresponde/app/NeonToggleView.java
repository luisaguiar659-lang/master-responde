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

/** Botão liga/desliga neon (pílula verde com bolinha branca), como na tela de referência. */
public class NeonToggleView extends View {

    public interface OnChangeListener {
        void onChanged(boolean checked);
    }

    private final Paint track = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glow = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint border = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint thumb = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint thumbShadow = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final float density;

    private boolean checked;
    private float progress; // 0 = desligado, 1 = ligado
    private ValueAnimator animator;
    private OnChangeListener listener;

    public NeonToggleView(Context context) {
        super(context);
        density = getResources().getDisplayMetrics().density;
        border.setStyle(Paint.Style.STROKE);
        border.setStrokeWidth(1.5f * density);
        glow.setStyle(Paint.Style.FILL);
        thumbShadow.setColor(Color.argb(90, 0, 0, 0));
        setClickable(true);
        setFocusable(true);
        setContentDescription("Ativar ou pausar o bot");
        setOnClickListener(v -> {
            setChecked(!checked, true);
            if (listener != null) {
                listener.onChanged(checked);
            }
        });
    }

    public void setOnChangeListener(OnChangeListener l) {
        this.listener = l;
    }

    public boolean isChecked() {
        return checked;
    }

    /** Define o estado sem disparar o listener. */
    public void setCheckedSilently(boolean value) {
        setChecked(value, false);
    }

    private void setChecked(boolean value, boolean animate) {
        checked = value;
        if (animator != null) {
            animator.cancel();
        }
        float target = value ? 1f : 0f;
        if (!animate) {
            progress = target;
            invalidate();
            return;
        }
        animator = ValueAnimator.ofFloat(progress, target);
        animator.setDuration(180L);
        animator.addUpdateListener(a -> {
            progress = (float) a.getAnimatedValue();
            invalidate();
        });
        animator.start();
    }

    @Override
    protected void onMeasure(int w, int h) {
        int defW = Math.round(64 * density);
        int defH = Math.round(34 * density);
        int ww = View.MeasureSpec.getMode(w) == View.MeasureSpec.EXACTLY ? View.MeasureSpec.getSize(w) : defW;
        int hh = View.MeasureSpec.getMode(h) == View.MeasureSpec.EXACTLY ? View.MeasureSpec.getSize(h) : defH;
        setMeasuredDimension(ww, hh);
    }

    private int mix(int a, int b, float t) {
        return Color.rgb(
                Math.round(Color.red(a) + (Color.red(b) - Color.red(a)) * t),
                Math.round(Color.green(a) + (Color.green(b) - Color.green(a)) * t),
                Math.round(Color.blue(a) + (Color.blue(b) - Color.blue(a)) * t));
    }

    @Override
    protected void onDraw(Canvas c) {
        float w = getWidth();
        float h = getHeight();
        float pad = 3f * density;
        float trackH = h - pad * 2f;
        float radius = trackH / 2f;

        int offA = Color.rgb(52, 64, 82);
        int offB = Color.rgb(34, 44, 60);
        int onA = Color.rgb(40, 235, 110);
        int onB = Color.rgb(10, 170, 80);

        // brilho externo quando ligado
        if (progress > 0.01f) {
            glow.setColor(Color.argb(Math.round(70 * progress), 40, 255, 120));
            rect.set(0, 0, w, h);
            c.drawRoundRect(rect, h / 2f, h / 2f, glow);
        }

        rect.set(pad, pad, w - pad, h - pad);
        track.setShader(new LinearGradient(0, rect.top, 0, rect.bottom,
                mix(offA, onA, progress), mix(offB, onB, progress), Shader.TileMode.CLAMP));
        c.drawRoundRect(rect, radius, radius, track);

        border.setColor(mix(Color.rgb(90, 104, 124), Color.rgb(150, 255, 190), progress));
        c.drawRoundRect(rect, radius, radius, border);

        float thumbR = radius - 3f * density;
        float minX = rect.left + radius;
        float maxX = rect.right - radius;
        float cx = minX + (maxX - minX) * progress;
        float cy = h / 2f;

        c.drawCircle(cx, cy + 1.5f * density, thumbR, thumbShadow);
        thumb.setShader(new LinearGradient(0, cy - thumbR, 0, cy + thumbR,
                Color.WHITE, Color.rgb(205, 215, 225), Shader.TileMode.CLAMP));
        c.drawCircle(cx, cy, thumbR, thumb);
    }
}
