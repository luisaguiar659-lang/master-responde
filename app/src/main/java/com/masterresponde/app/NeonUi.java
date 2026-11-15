package com.masterresponde.app;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
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
        watch(body);
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

    // ------------------------------------------------------------------
    // Auto-estilo: aplica o visual neon a qualquer view criada, inclusive as
    // adicionadas depois (listas que são redesenhadas).
    // ------------------------------------------------------------------

    private static final ViewGroup.OnHierarchyChangeListener WATCHER =
            new ViewGroup.OnHierarchyChangeListener() {
                @Override
                public void onChildViewAdded(View parent, View child) {
                    watch(child);
                }

                @Override
                public void onChildViewRemoved(View parent, View child) {
                }
            };

    static void watch(View v) {
        if (v == null) {
            return;
        }
        style(v);
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            g.setOnHierarchyChangeListener(WATCHER);
            for (int i = 0; i < g.getChildCount(); i++) {
                watch(g.getChildAt(i));
            }
        }
    }

    private static void style(View v) {
        Context c = v.getContext();
        if (v instanceof EditText) {
            EditText e = (EditText) v;
            e.setBackgroundResource(R.drawable.input_bg);
            e.setTextColor(Color.WHITE);
            e.setHintTextColor(Color.rgb(105, 135, 165));
            e.setPadding(dp(c, 12), dp(c, 10), dp(c, 12), dp(c, 10));
            return;
        }
        if (v instanceof CompoundButton) {
            CompoundButton cb = (CompoundButton) v;
            cb.setButtonTintList(ColorStateList.valueOf(Color.rgb(0, 200, 255)));
            return;
        }
        if (v instanceof Button) {
            Button b = (Button) v;
            b.setStateListAnimator(null);
            b.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
            return;
        }
        if (v instanceof TextView) {
            TextView t = (TextView) v;
            if (t.getCompoundDrawables()[0] != null) {
                return;
            }
            CharSequence cs = t.getText();
            if (cs == null) {
                return;
            }
            String str = cs.toString();
            int col = t.getCurrentTextColor();
            boolean orange = Color.red(col) >= 230 && Color.green(col) >= 110
                    && Color.green(col) <= 190 && Color.blue(col) <= 80;
            boolean bold = t.getTypeface() != null && t.getTypeface().isBold();
            if (orange && bold && str.length() >= 3 && str.length() <= 48
                    && str.equals(str.toUpperCase())) {
                // título de seção: barra laranja + texto branco, como nas caixas do dashboard
                GradientDrawable bar = new GradientDrawable();
                bar.setColor(Color.rgb(255, 145, 20));
                bar.setCornerRadius(dp(c, 2));
                bar.setBounds(0, 0, dp(c, 4), dp(c, 16));
                t.setCompoundDrawables(bar, null, null, null);
                t.setCompoundDrawablePadding(dp(c, 8));
                t.setTextColor(Color.WHITE);
                t.setLetterSpacing(0.04f);
            }
        }
    }

    /** Quadrado com ícone colorido (usado nos cartões de status). */
    static View iconTile(Context c, String icon, int color) {
        FrameLayout f = new FrameLayout(c);
        GradientDrawable g = new GradientDrawable();
        g.setCornerRadius(dp(c, 12));
        g.setColor(Color.argb(40, Color.red(color), Color.green(color), Color.blue(color)));
        g.setStroke(dp(c, 1), Color.argb(150, Color.red(color), Color.green(color), Color.blue(color)));
        f.setBackground(g);
        f.addView(new NeonIconView(c, icon, color),
                new FrameLayout.LayoutParams(dp(c, 24), dp(c, 24), Gravity.CENTER));
        return f;
    }

    /** Cartão de status: ícone à esquerda, rótulo, valor colorido e descrição opcional. */
    static LinearLayout statusCard(Context c, String icon, int color, String label, String value, String desc) {
        LinearLayout row = new LinearLayout(c);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(c, 12), dp(c, 12), dp(c, 14), dp(c, 12));
        row.setBackgroundResource(R.drawable.card);
        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(-1, -2);
        rlp.setMargins(0, dp(c, 5), 0, dp(c, 5));
        row.setLayoutParams(rlp);

        row.addView(iconTile(c, icon, color), new LinearLayout.LayoutParams(dp(c, 46), dp(c, 46)));

        LinearLayout col = new LinearLayout(c);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(dp(c, 12), 0, 0, 0);

        TextView l = new TextView(c);
        l.setText(label.toUpperCase());
        l.setTextColor(Color.rgb(150, 180, 205));
        l.setTextSize(10.5f);
        l.setLetterSpacing(0.08f);
        l.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        col.addView(l);

        TextView val = new TextView(c);
        val.setText(value);
        val.setTextColor(color);
        val.setTextSize(16);
        val.setTypeface(Typeface.create("sans-serif-black", Typeface.NORMAL));
        col.addView(val);

        if (desc != null && !desc.isEmpty()) {
            TextView d = new TextView(c);
            d.setText(desc);
            d.setTextColor(Color.rgb(130, 155, 180));
            d.setTextSize(11);
            col.addView(d);
        }
        row.addView(col, new LinearLayout.LayoutParams(0, -2, 1f));
        return row;
    }
}
