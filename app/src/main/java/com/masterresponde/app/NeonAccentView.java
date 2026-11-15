package com.masterresponde.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.view.View;

/**
 * Faixas diagonais decorativas (azul + laranja) do cabeçalho e do menu lateral.
 * Modo "corner" desenha no canto direito, como no cabeçalho de referência.
 */
public class NeonAccentView extends View {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final float density;
    private boolean compact;

    public NeonAccentView(Context context) {
        super(context);
        density = getResources().getDisplayMetrics().density;
        setClickable(false);
        setFocusable(false);
    }

    /** Modo compacto: só as duas faixas finas (usado no topo do menu lateral). */
    public void setCompact(boolean compact) {
        this.compact = compact;
        invalidate();
    }

    private void band(Canvas c, float xBottom, float width, float slant, int top, int bottom) {
        float h = getHeight();
        path.reset();
        path.moveTo(xBottom + slant, 0);
        path.lineTo(xBottom + slant + width, 0);
        path.lineTo(xBottom + width, h);
        path.lineTo(xBottom, h);
        path.close();
        paint.setShader(new LinearGradient(0, 0, 0, h, top, bottom, Shader.TileMode.CLAMP));
        c.drawPath(path, paint);
    }

    @Override
    protected void onDraw(Canvas c) {
        float w = getWidth();
        float h = getHeight();
        float slant = h * 0.55f;
        paint.setStyle(Paint.Style.FILL);

        if (compact) {
            band(c, w - 40f * density, 5f * density, slant * 0.6f,
                    Color.argb(220, 0, 220, 255), Color.argb(40, 0, 160, 230));
            band(c, w - 28f * density, 8f * density, slant * 0.6f,
                    Color.argb(255, 255, 145, 20), Color.argb(60, 255, 110, 0));
            return;
        }

        // faixa azul larga (translúcida)
        band(c, w - 92f * density, 40f * density, slant,
                Color.argb(120, 0, 150, 255), Color.argb(20, 0, 110, 200));
        // faixa ciano fina
        band(c, w - 48f * density, 5f * density, slant,
                Color.argb(230, 0, 220, 255), Color.argb(90, 0, 160, 230));
        // faixa laranja
        band(c, w - 34f * density, 7f * density, slant,
                Color.argb(255, 255, 145, 20), Color.argb(150, 255, 110, 0));
    }
}
