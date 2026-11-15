package com.masterresponde.app;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Peças visuais compartilhadas pelas telas secundárias (mesmo estilo do dashboard). */
final class NeonUi {

    private NeonUi() {
    }

    static int dp(Context c, float v) {
        return Math.round(v * c.getResources().getDisplayMetrics().density);
    }

    /** Tela completa: fundo azul escuro, cabeçalho com voltar + título e o conteúdo abaixo. */
    static View wrap(Activity a, String title, String subtitle, View body) {
        LinearLayout outer = new LinearLayout(a);
        outer.setOrientation(LinearLayout.VERTICAL);
        outer.setBackgroundResource(R.drawable.app_background);
        outer.addView(header(a, title, subtitle), new LinearLayout.LayoutParams(-1, dp(a, 72)));

        View line = new View(a);
        line.setBackgroundResource(R.drawable.line);
        outer.addView(line, new LinearLayout.LayoutParams(-1, dp(a, 3)));

        outer.addView(body, new LinearLayout.LayoutParams(-1, 0, 1f));
        styleButtons(body, new boolean[]{true});
        return outer;
    }

    private static View header(Activity a, String title, String subtitle) {
        FrameLayout frame = new FrameLayout(a);
        frame.setBackgroundResource(R.drawable.header_bg);
        frame.addView(new NeonAccentView(a), new FrameLayout.LayoutParams(-1, -1));

        LinearLayout row = new LinearLayout(a);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(a, 6), 0, dp(a, 6), 0);

        FrameLayout back = new FrameLayout(a);
        back.addView(new NeonIconView(a, "back", Color.WHITE),
                new FrameLayout.LayoutParams(dp(a, 26), dp(a, 26), Gravity.CENTER));
        back.setContentDescription("Voltar");
        back.setOnClickListener(v -> a.finish());
        row.addView(back, new LinearLayout.LayoutParams(dp(a, 48), dp(a, 48)));

        LinearLayout titles = new LinearLayout(a);
        titles.setOrientation(LinearLayout.VERTICAL);
        titles.setGravity(Gravity.CENTER_VERTICAL);
        TextView t = new TextView(a);
        t.setText(title);
        t.setTextColor(Color.WHITE);
        t.setTextSize(20);
        t.setMaxLines(1);
        t.setTypeface(Typeface.create("sans-serif-black", Typeface.NORMAL));
        titles.addView(t, new LinearLayout.LayoutParams(-1, -2));
        TextView s = new TextView(a);
        s.setText(subtitle);
        s.setTextColor(Color.rgb(0, 190, 240));
        s.setTextSize(12);
        s.setMaxLines(1);
        titles.addView(s, new LinearLayout.LayoutParams(-1, -2));
        row.addView(titles, new LinearLayout.LayoutParams(0, -2, 1f));

        frame.addView(row, new FrameLayout.LayoutParams(-1, -1));
        return frame;
    }

    /** O primeiro botão de cada tela fica laranja (ação principal); os demais ficam azuis. */
    static void styleButtons(View v, boolean[] first) {
        if (v instanceof Button) {
            Button b = (Button) v;
            b.setBackgroundResource(first[0] ? R.drawable.button_orange : R.drawable.button_dark);
            first[0] = false;
            b.setTextColor(Color.WHITE);
            b.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
            b.setAllCaps(true);
            b.setStateListAnimator(null);
            ViewGroup.LayoutParams lp = b.getLayoutParams();
            if (lp instanceof ViewGroup.MarginLayoutParams) {
                ViewGroup.MarginLayoutParams m = (ViewGroup.MarginLayoutParams) lp;
                m.height = dp(b.getContext(), 52);
                m.topMargin = dp(b.getContext(), 8);
                m.bottomMargin = dp(b.getContext(), 4);
            }
            return;
        }
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) {
                styleButtons(g.getChildAt(i), first);
            }
        }
    }
}
