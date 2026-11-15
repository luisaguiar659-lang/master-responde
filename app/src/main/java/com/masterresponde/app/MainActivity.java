package com.masterresponde.app;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.masterresponde.app.engines.AutomationCloudEngine;
import com.masterresponde.app.engines.MasterIBOEngine;

import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;

public class MainActivity extends Activity {

    // Paleta das telas de referência (fundo azul-marinho + ciano/laranja neon)
    private static final int BG = Color.rgb(3, 10, 24);
    private static final int CYAN = Color.rgb(0, 220, 255);
    private static final int BLUE = Color.rgb(0, 140, 255);
    private static final int GREEN = Color.rgb(0, 230, 125);
    private static final int ORANGE = Color.rgb(255, 145, 20);
    private static final int PURPLE = Color.rgb(176, 84, 255);
    private static final int RED = Color.rgb(255, 65, 85);
    private static final int YELLOW = Color.rgb(255, 196, 30);
    private static final int GREY = Color.rgb(145, 158, 172);
    private static final int PANEL_EDGE = Color.rgb(8, 78, 120);

    private static final int PERIOD_TODAY = 0;
    private static final int PERIOD_WEEK = 1;
    private static final int PERIOD_ALL = 2;
    private static final long DAY_MS = 86400000L;
    private static final long REFRESH_MS = 4000L;

    private LinearLayout content;
    private SharedPreferences prefs;
    private final Handler ui = new Handler(Looper.getMainLooper());
    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            refreshStatus();
            refreshData();
            ui.postDelayed(this, REFRESH_MS);
        }
    };

    private TextView dashboardBotBadge;
    private View bellDot;
    private Chip botChip;
    private Chip waChip;
    private Chip masterflixChip;
    private Chip stvChip;
    private Chip engineChip;

    private NeonToggleView botToggle;

    // Dashboard (atualizados no lugar, sem recriar a tela)
    private TextView tvReceived;
    private TextView tvSent;
    private TextView tvErrors;
    private TextView tvRingSuffix;
    private NeonRingView ring;
    private NeonBarChartView chart;
    private TextView tvSumTotal;
    private TextView tvSumToday;
    private TextView tvSumTests;
    private TextView tvSumResellers;
    private final TextView[] periodTabs = new TextView[3];
    private int period = PERIOD_TODAY;
    private long lastRebindAt;

    private FrameLayout menuOverlay;
    private ScrollView drawerScroll;
    private LinearLayout drawerPanel;
    private int drawerWidth;

    /** Pílula de status: bolinha colorida + texto, com a cor da borda seguindo o estado. */
    private final class Chip {
        final TextView label;
        final View dot;
        final View pill;

        Chip(TextView label, View dot, View pill) {
            this.label = label;
            this.dot = dot;
            this.pill = pill;
        }

        void set(String text, int color) {
            label.setText(text);
            GradientDrawable d = new GradientDrawable();
            d.setShape(GradientDrawable.OVAL);
            d.setColor(color);
            dot.setBackground(d);
            pill.setBackground(roundedBackground(tint(color, 0.16f), tint(color, 0.55f), 12, 1));
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = getSharedPreferences("master_responde", MODE_PRIVATE);

        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 100);
        }

        buildScreen();

        // Inicia motores integrados automaticamente
        if (prefs.getBoolean("auto_start_engines", true)) {
            content.postDelayed(() -> {
                AutomationCloudEngine.start(this);
                MasterIBOEngine.start(this);
            }, 1200L);
        }

        // Inicia o MasterFlix automaticamente ao abrir o aplicativo
        if (prefs.getBoolean("auto_start_masterflix", true)) {
            content.postDelayed(() -> {
                try {
                    Intent intent = new Intent(this, MasterflixActivity.class);
                    startActivity(intent);
                } catch (Exception ignored) {
                }
            }, 800L);
        }

        if (prefs.getBoolean("auto_reconnect_engine", true)) {
            BackgroundRuntime.requestListenerRebind(this);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (prefs.getBoolean("auto_reconnect_engine", true)) {
            BackgroundRuntime.requestListenerRebind(this);
        }

        if (content != null) {
            ui.removeCallbacks(ticker);
            ticker.run();

            boolean returnToMenu = prefs.getBoolean("return_to_menu_after_child", false);

            if (returnToMenu) {
                prefs.edit().putBoolean("return_to_menu_after_child", false).apply();

                if (prefs.getBoolean("return_to_menu_enabled", true)) {
                    content.postDelayed(this::openMenu, 180L);
                }
            }
        }
    }

    @Override
    protected void onPause() {
        ui.removeCallbacks(ticker);
        super.onPause();
    }

    @Override
    public void onBackPressed() {
        if (menuOverlay != null && menuOverlay.getVisibility() == View.VISIBLE) {
            closeMenu();
            return;
        }

        super.onBackPressed();
    }

    // ------------------------------------------------------------------
    // Estrutura da tela
    // ------------------------------------------------------------------

    private void buildScreen() {
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        getWindow().getDecorView().setSystemUiVisibility(0);

        FrameLayout shell = new FrameLayout(this);
        shell.setBackgroundResource(R.drawable.app_background);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);

        root.addView(buildHeader(), new LinearLayout.LayoutParams(-1, dp(72)));

        View line = new View(this);
        line.setBackgroundResource(R.drawable.line);
        root.addView(line, new LinearLayout.LayoutParams(-1, dp(2)));

        ScrollView scroll = new ScrollView(this);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setClipToPadding(false);

        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(12), dp(10), dp(12), dp(14));
        content.setClipChildren(false);
        scroll.addView(content);

        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));

        buildBottomNavigation(root);

        shell.addView(root, new FrameLayout.LayoutParams(-1, -1));
        buildDrawer(shell);
        setContentView(shell);

        buildDashboard();
        refreshData();
    }

    private View buildHeader() {
        FrameLayout header = new FrameLayout(this);
        GradientDrawable bg = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{Color.rgb(2, 12, 28), Color.rgb(4, 40, 78), Color.rgb(3, 14, 32), Color.rgb(60, 32, 6)});
        header.setBackground(bg);
        header.addView(new NeonAccentView(this), new FrameLayout.LayoutParams(-1, -1));

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(6), 0, dp(6), 0);

        FrameLayout menuBtn = new FrameLayout(this);
        menuBtn.addView(icon("menu", Color.WHITE), new FrameLayout.LayoutParams(dp(28), dp(28), Gravity.CENTER));
        menuBtn.setContentDescription("Abrir menu");
        menuBtn.setOnClickListener(v -> openMenu());
        row.addView(menuBtn, new LinearLayout.LayoutParams(dp(46), dp(46)));

        LinearLayout center = new LinearLayout(this);
        center.setOrientation(LinearLayout.HORIZONTAL);
        center.setGravity(Gravity.CENTER);

        ImageView head = new ImageView(this);
        head.setImageResource(R.drawable.mascot_head);
        head.setScaleType(ImageView.ScaleType.FIT_CENTER);
        center.addView(head, new LinearLayout.LayoutParams(dp(64), dp(52)));

        LinearLayout titles = new LinearLayout(this);
        titles.setOrientation(LinearLayout.VERTICAL);
        titles.setPadding(dp(6), 0, 0, 0);
        TextView title = brandTitle(21, true);
        title.setMaxLines(1);
        autoFit(title, 12, 21);
        titles.addView(title, new LinearLayout.LayoutParams(-1, -2));
        TextView sub = text("Central Inteligente de Atendimento", 11, CYAN, false);
        sub.setMaxLines(1);
        autoFit(sub, 8, 12);
        titles.addView(sub, new LinearLayout.LayoutParams(-1, -2));
        center.addView(titles, new LinearLayout.LayoutParams(0, -2, 1f));

        row.addView(center, new LinearLayout.LayoutParams(0, -1, 1f));

        FrameLayout bell = new FrameLayout(this);
        bell.addView(icon("bell", Color.WHITE), new FrameLayout.LayoutParams(dp(26), dp(26), Gravity.CENTER));
        bellDot = new View(this);
        GradientDrawable dotBg = new GradientDrawable();
        dotBg.setShape(GradientDrawable.OVAL);
        dotBg.setColor(Color.rgb(255, 60, 60));
        bellDot.setBackground(dotBg);
        bellDot.setVisibility(View.GONE);
        FrameLayout.LayoutParams dotLp = new FrameLayout.LayoutParams(dp(9), dp(9), Gravity.TOP | Gravity.END);
        dotLp.topMargin = dp(9);
        dotLp.rightMargin = dp(10);
        bell.addView(bellDot, dotLp);
        bell.setContentDescription("Histórico de eventos");
        bell.setOnClickListener(v -> startActivity(new Intent(this, EventHistoryActivity.class)));
        row.addView(bell, new LinearLayout.LayoutParams(dp(46), dp(46)));

        header.addView(row, new FrameLayout.LayoutParams(-1, -1));
        return header;
    }

    // ------------------------------------------------------------------
    // Menu lateral
    // ------------------------------------------------------------------

    private void buildDrawer(FrameLayout shell) {
        menuOverlay = new FrameLayout(this);
        menuOverlay.setBackgroundColor(Color.argb(190, 0, 4, 12));
        menuOverlay.setVisibility(View.GONE);
        menuOverlay.setOnClickListener(v -> closeMenu());

        int screenW = getResources().getDisplayMetrics().widthPixels;
        drawerWidth = Math.min(dp(360), Math.max(dp(290), Math.round(screenW * 0.72f)));

        drawerScroll = new ScrollView(this);
        drawerScroll.setVerticalScrollBarEnabled(false);
        drawerScroll.setBackground(gradientBorder(
                new int[]{CYAN, BLUE, ORANGE},
                GradientDrawable.Orientation.TL_BR,
                radii(0, 24, 24, 0), Color.rgb(4, 14, 32), Color.rgb(3, 9, 22), 2));
        drawerScroll.setClickable(true);

        drawerPanel = new LinearLayout(this);
        drawerPanel.setOrientation(LinearLayout.VERTICAL);
        drawerPanel.setPadding(dp(12), dp(6), dp(12), dp(24));
        drawerScroll.addView(drawerPanel);

        // Cabeçalho com o robô
        FrameLayout banner = new FrameLayout(this);
        ImageView mascot = new ImageView(this);
        mascot.setImageResource(R.drawable.mascot_robot);
        mascot.setScaleType(ImageView.ScaleType.FIT_CENTER);
        banner.addView(mascot, new FrameLayout.LayoutParams(dp(112), dp(100), Gravity.START | Gravity.CENTER_VERTICAL));

        LinearLayout brand = new LinearLayout(this);
        brand.setOrientation(LinearLayout.VERTICAL);
        brand.setGravity(Gravity.CENTER_VERTICAL);
        TextView master = text("MASTER", 24, Color.WHITE, true);
        master.setTypeface(Typeface.create("sans-serif-black", Typeface.NORMAL));
        TextView responde = text("RESPONDE", 24, ORANGE, true);
        responde.setTypeface(Typeface.create("sans-serif-black", Typeface.NORMAL));
        master.setMaxLines(1);
        responde.setMaxLines(1);
        autoFit(master, 14, 24);
        autoFit(responde, 14, 24);
        brand.addView(master, new LinearLayout.LayoutParams(-1, -2));
        brand.addView(responde, new LinearLayout.LayoutParams(-1, -2));
        brand.addView(text("Menu Principal", 14, Color.rgb(180, 205, 225), false));
        FrameLayout.LayoutParams brandLp = new FrameLayout.LayoutParams(-1, -1);
        brandLp.leftMargin = dp(108);
        brandLp.rightMargin = dp(34);
        banner.addView(brand, brandLp);

        NeonAccentView stripes = new NeonAccentView(this);
        stripes.setCompact(true);
        banner.addView(stripes, new FrameLayout.LayoutParams(dp(70), -1, Gravity.END));

        FrameLayout close = new FrameLayout(this);
        close.addView(icon("close", Color.WHITE), new FrameLayout.LayoutParams(dp(22), dp(22), Gravity.CENTER));
        close.setContentDescription("Fechar menu");
        close.setOnClickListener(v -> closeMenu());
        banner.addView(close, new FrameLayout.LayoutParams(dp(40), dp(40), Gravity.END | Gravity.TOP));

        drawerPanel.addView(banner, new LinearLayout.LayoutParams(-1, dp(104)));

        // Atalhos rápidos
        LinearLayout shortcutsBox = drawerBox("rocket", ORANGE, "ATALHOS RÁPIDOS", CYAN);
        LinearLayout shortcuts = new LinearLayout(this);
        shortcuts.setOrientation(LinearLayout.HORIZONTAL);
        drawerShortcut(shortcuts, "play", PURPLE, "STV+", true, () ->
                launchFromMenu(new Intent(this, StvPlusActivity.class)));
        drawerShortcut(shortcuts, "play", ORANGE, "GERAR\nTESTE", false, () ->
                confirmGenerateTest("MasterFlix", MasterflixActivity.class, true));
        drawerShortcut(shortcuts, "person_add", GREEN, "NOVA\nREVENDA", false, () ->
                launchFromMenu(new Intent(this, ResellersActivity.class)));
        shortcutsBox.addView(shortcuts, new LinearLayout.LayoutParams(-1, dp(88)));

        // Módulos
        LinearLayout modules = drawerBox("layers", ORANGE, "MÓDULOS", CYAN);
        drawerLink(modules, "Regras", "book", CYAN, () -> launchFromMenu(new Intent(this, RulesActivity.class)));
        drawerLink(modules, "Revendas", "people", CYAN, () -> launchFromMenu(new Intent(this, ResellersActivity.class)));
        drawerLink(modules, "Mensagens e Tempos", "msgtext", CYAN, () -> launchFromMenu(new Intent(this, MessageSettingsActivity.class)));
        drawerLink(modules, "Grupos", "people", CYAN, () -> launchFromMenu(new Intent(this, GroupsActivity.class)));
        drawerLink(modules, "Categorias", "layers", CYAN, () -> launchFromMenu(new Intent(this, CategoriesActivity.class)));

        // Controle e diagnóstico
        LinearLayout control = drawerBox("gear", ORANGE, "CONTROLE E DIAGNÓSTICO", ORANGE);
        drawerLink(control, "Monitoramento", "monitor", CYAN, () -> launchFromMenu(new Intent(this, MonitoringActivity.class)));
        drawerLink(control, "Histórico", "clock", CYAN, () -> launchFromMenu(new Intent(this, EventHistoryActivity.class)));
        drawerLink(control, "Simulador de Gatilhos", "bolt", ORANGE, () -> launchFromMenu(new Intent(this, TriggerSimulatorActivity.class)));
        drawerLink(control, "Backup / Restauração", "database", CYAN, () -> launchFromMenu(new Intent(this, BackupActivity.class)));
        drawerLink(control, "Central de Diagnóstico", "pulse", CYAN, () -> launchFromMenu(new Intent(this, DiagnosticsActivity.class)));

        // Configurações (destaque laranja)
        LinearLayout settings = new LinearLayout(this);
        settings.setOrientation(LinearLayout.HORIZONTAL);
        settings.setGravity(Gravity.CENTER_VERTICAL);
        settings.setPadding(dp(14), 0, dp(12), 0);
        settings.setBackground(gradientBorder(
                new int[]{ORANGE, Color.rgb(255, 190, 60), ORANGE},
                GradientDrawable.Orientation.LEFT_RIGHT,
                radii(16, 16, 16, 16), Color.rgb(34, 22, 12), Color.rgb(8, 14, 28), 2));
        settings.addView(icon("gear", ORANGE), new LinearLayout.LayoutParams(dp(26), dp(26)));
        TextView settingsLabel = text("Configurações", 15, Color.WHITE, true);
        settingsLabel.setPadding(dp(14), 0, 0, 0);
        settings.addView(settingsLabel, new LinearLayout.LayoutParams(0, -2, 1f));
        settings.addView(icon("chevron", Color.WHITE), new LinearLayout.LayoutParams(dp(20), dp(20)));
        settings.setOnClickListener(v -> {
            closeMenu();
            launchFromMenu(new Intent(this, AppSettingsActivity.class));
        });
        LinearLayout.LayoutParams settingsLp = new LinearLayout.LayoutParams(-1, dp(54));
        settingsLp.topMargin = dp(14);
        drawerPanel.addView(settings, settingsLp);

        TextView version = text("MASTER RESPONDE  •  v" + appVersion(), 10.5f, Color.rgb(120, 145, 170), false);
        version.setGravity(Gravity.CENTER);
        drawerPanel.addView(version, lp(-1, -2, 0, dp(14), 0, 0));

        menuOverlay.addView(drawerScroll, new FrameLayout.LayoutParams(drawerWidth, -1, Gravity.START));
        shell.addView(menuOverlay, new FrameLayout.LayoutParams(-1, -1));
    }

    /** Caixa com título (ícone + nome + traços decorativos) onde entram os itens do menu. */
    private LinearLayout drawerBox(String iconName, int iconColor, String title, int slashColor) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(8), dp(4), dp(8), dp(8));
        box.setBackground(gradientBorder(
                new int[]{CYAN, BLUE, Color.rgb(0, 90, 150)},
                GradientDrawable.Orientation.TL_BR,
                radii(20, 20, 20, 20), Color.rgb(6, 22, 44), Color.rgb(4, 14, 32), 1));

        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        head.setPadding(dp(4), dp(6), dp(4), dp(8));
        head.addView(icon(iconName, iconColor), new LinearLayout.LayoutParams(dp(24), dp(24)));
        TextView t = text(title, 14, Color.WHITE, true);
        t.setPadding(dp(10), 0, 0, 0);
        t.setMaxLines(1);
        autoFit(t, 10, 14);
        head.addView(t, new LinearLayout.LayoutParams(0, -2, 1f));
        head.addView(icon("slashes", slashColor), new LinearLayout.LayoutParams(dp(26), dp(20)));
        box.addView(head, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = dp(12);
        drawerPanel.addView(box, lp);
        return box;
    }

    private void drawerShortcut(LinearLayout parent, String iconName, int accent, String label, boolean stv, Runnable action) {
        LinearLayout tile = new LinearLayout(this);
        tile.setOrientation(LinearLayout.VERTICAL);
        tile.setGravity(Gravity.CENTER);
        tile.setPadding(dp(3), dp(6), dp(3), dp(6));
        tile.setBackground(gradientBorder(
                new int[]{accent, accent},
                GradientDrawable.Orientation.TL_BR,
                radii(16, 16, 16, 16), tint(accent, 0.28f), Color.rgb(5, 12, 28), 2));
        glow(tile, accent);

        tile.addView(icon(iconName, accent), new LinearLayout.LayoutParams(dp(34), dp(34)));

        TextView caption;
        if (stv) {
            SpannableString s = new SpannableString("STV+");
            s.setSpan(new ForegroundColorSpan(ORANGE), 3, 4, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            caption = text("", 15, Color.WHITE, true);
            caption.setText(s);
        } else {
            caption = text(label, 11, Color.WHITE, true);
        }
        caption.setGravity(Gravity.CENTER);
        caption.setPadding(0, dp(4), 0, 0);
        tile.addView(caption);

        tile.setOnClickListener(v -> {
            closeMenu();
            action.run();
        });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -1, 1f);
        lp.setMargins(dp(3), 0, dp(3), 0);
        parent.addView(tile, lp);
    }

    private void drawerLink(LinearLayout parent, String label, String iconName, int iconColor, Runnable action) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(12), 0, dp(10), 0);
        row.setBackground(gradientBorder(
                new int[]{Color.rgb(0, 170, 235), Color.rgb(0, 110, 190)},
                GradientDrawable.Orientation.LEFT_RIGHT,
                radii(13, 13, 13, 13), Color.rgb(8, 34, 58), Color.rgb(6, 24, 46), 1));
        row.addView(icon(iconName, iconColor), new LinearLayout.LayoutParams(dp(24), dp(24)));
        TextView title = text(label, 14, Color.WHITE, true);
        title.setPadding(dp(14), 0, 0, 0);
        title.setMaxLines(1);
        title.setEllipsize(TextUtils.TruncateAt.END);
        row.addView(title, new LinearLayout.LayoutParams(0, -2, 1f));
        row.addView(icon("chevron", Color.WHITE), new LinearLayout.LayoutParams(dp(18), dp(18)));
        row.setClickable(true);
        row.setFocusable(true);
        row.setOnClickListener(v -> {
            closeMenu();
            if (action != null) {
                action.run();
            }
        });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(44));
        lp.setMargins(0, dp(2), 0, dp(4));
        parent.addView(row, lp);
    }

    private String appVersion() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (Exception e) {
            return "";
        }
    }

    /** Pede confirmação antes de gerar um teste, para um toque acidental não disparar a automação. */
    private void confirmGenerateTest(String name, Class<?> target, boolean fromMenu) {
        new AlertDialog.Builder(this)
                .setTitle("Gerar teste " + name)
                .setMessage("Iniciar a geração automática de um teste agora?")
                .setPositiveButton("Gerar", (dialog, which) -> {
                    Intent intent = new Intent(this, target);
                    intent.putExtra("auto_generate_test", true);
                    if (fromMenu) {
                        launchFromMenu(intent);
                    } else {
                        startActivity(intent);
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void openMenu() {
        if (menuOverlay == null) {
            return;
        }

        menuOverlay.setVisibility(View.VISIBLE);
        menuOverlay.setAlpha(0f);
        drawerScroll.setTranslationX(-drawerWidth * 0.35f);
        menuOverlay.animate().alpha(1f).setDuration(170L).start();
        drawerScroll.animate().translationX(0f).setDuration(190L).start();
    }

    private void closeMenu() {
        if (menuOverlay == null || menuOverlay.getVisibility() != View.VISIBLE) {
            return;
        }

        drawerScroll.animate().translationX(-drawerWidth * 0.35f).setDuration(130L).start();
        menuOverlay.animate()
                .alpha(0f)
                .setDuration(130L)
                .withEndAction(() -> {
                    menuOverlay.setVisibility(View.GONE);
                    menuOverlay.setAlpha(1f);
                    drawerScroll.setTranslationX(0f);
                })
                .start();
    }

    // ------------------------------------------------------------------
    // Dashboard
    // ------------------------------------------------------------------

    private void buildDashboard() {
        content.removeAllViews();
        period = Math.max(0, Math.min(2, prefs.getInt("dash_period", PERIOD_TODAY)));

        // ---- Hero ----
        content.addView(buildHero(), lp(-1, dp(150), 0, 0, 0, dp(10)));

        // ---- Status do sistema ----
        LinearLayout statusBox = sectionBox();
        statusBox.addView(sectionHeader("pulse", CYAN, "STATUS DO SISTEMA", ""));
        LinearLayout statusRow = new LinearLayout(this);
        statusRow.setOrientation(LinearLayout.HORIZONTAL);
        waChip = addStatusCard(statusRow, "whatsapp", GREEN, "WhatsApp Business", GREEN, this::openNotificationAccess);
        masterflixChip = addStatusCard(statusRow, "play", ORANGE, "MasterFlix", ORANGE,
                () -> startActivity(new Intent(this, MasterflixActivity.class)));
        stvChip = addStatusCard(statusRow, "stv", PURPLE, "STV+", PURPLE,
                () -> startActivity(new Intent(this, StvPlusActivity.class)));
        botChip = addStatusCard(statusRow, "robot", CYAN, "BOT", BLUE, null);
        engineChip = addStatusCard(statusRow, "layers", YELLOW, "2º Plano", YELLOW, this::openNotificationAccess);
        statusBox.addView(statusRow, lp(-1, -2, 0, dp(8), 0, 0));
        content.addView(statusBox, lp(-1, -2, 0, 0, 0, dp(10)));

        // ---- Atividade + desempenho ----
        LinearLayout activityRow = new LinearLayout(this);
        activityRow.setOrientation(LinearLayout.HORIZONTAL);

        LinearLayout activityBox = sectionBox();
        activityBox.addView(sectionHeader("bars", BLUE, "ATIVIDADE DO ATENDIMENTO", ""));
        activityBox.addView(buildPeriodTabs(), lp(-1, dp(24), 0, dp(6), 0, 0));
        LinearLayout metrics = new LinearLayout(this);
        metrics.setOrientation(LinearLayout.HORIZONTAL);
        tvReceived = addMetric(metrics, "chat", BLUE, "Recebidas", BLUE);
        tvSent = addMetric(metrics, "reply", GREEN, "Respondidas", GREEN);
        tvErrors = addMetric(metrics, "warning", RED, "Falhas", RED);
        activityBox.addView(metrics, lp(-1, dp(92), 0, dp(8), 0, 0));
        activityRow.addView(activityBox, new LinearLayout.LayoutParams(0, -1, 1.62f));

        LinearLayout perfBox = sectionBox();
        View perfHeader = sectionHeader("stopwatch", CYAN, "DESEMPENHO", "(HOJE)");
        tvRingSuffix = (TextView) ((LinearLayout) perfHeader).getChildAt(1);
        perfBox.addView(perfHeader);
        ring = new NeonRingView(this);
        perfBox.addView(ring, lp(-1, dp(128), 0, dp(6), 0, 0));
        LinearLayout.LayoutParams perfLp = new LinearLayout.LayoutParams(0, -1, 1f);
        perfLp.leftMargin = dp(8);
        activityRow.addView(perfBox, perfLp);

        content.addView(activityRow, lp(-1, -2, 0, 0, 0, dp(10)));

        // ---- Acesso rápido ----
        LinearLayout quickBox = sectionBox();
        quickBox.addView(sectionHeader("rocket", CYAN, "ACESSO RÁPIDO", ""));
        LinearLayout quickRow = new LinearLayout(this);
        quickRow.setOrientation(LinearLayout.HORIZONTAL);
        addQuick(quickRow, "play", BLUE, "Gerar Teste\nMasterFlix",
                () -> confirmGenerateTest("MasterFlix", MasterflixActivity.class, false));
        addQuick(quickRow, "play", PURPLE, "Gerar Teste\nSTV+",
                () -> confirmGenerateTest("STV+", StvPlusActivity.class, false));
        addQuick(quickRow, "person_add", ORANGE, "Cadastrar\nRevenda",
                () -> startActivity(new Intent(this, ResellersActivity.class)));
        addQuick(quickRow, "people", CYAN, "Clientes\nAtivos",
                () -> startActivity(new Intent(this, EventHistoryActivity.class)));
        addQuick(quickRow, "gear", BLUE, "Configurações\ndo BOT",
                () -> startActivity(new Intent(this, AppSettingsActivity.class)));
        quickBox.addView(quickRow, lp(-1, dp(84), 0, dp(8), 0, 0));
        content.addView(quickBox, lp(-1, -2, 0, 0, 0, dp(10)));

        // ---- Resumo + gráfico ----
        LinearLayout bottomRow = new LinearLayout(this);
        bottomRow.setOrientation(LinearLayout.HORIZONTAL);

        LinearLayout summaryBox = sectionBox();
        summaryBox.addView(sectionHeader("clipboard", CYAN, "RESUMO GERAL", ""));
        tvSumTotal = addSummaryRow(summaryBox, "chat", CYAN, "Total de atendimentos");
        tvSumToday = addSummaryRow(summaryBox, "person", GREEN, "Atendimentos hoje");
        tvSumTests = addSummaryRow(summaryBox, "key", GREEN, "Testes gerados");
        tvSumResellers = addSummaryRow(summaryBox, "people", GREEN, "Revendas cadastradas");
        bottomRow.addView(summaryBox, new LinearLayout.LayoutParams(0, -1, 1.1f));

        LinearLayout chartBox = sectionBox();
        chartBox.addView(sectionHeader("bars", CYAN, "USO DIÁRIO", "(ÚLTIMOS 7 DIAS)"));
        chart = new NeonBarChartView(this);
        chartBox.addView(chart, lp(-1, dp(150), 0, dp(8), 0, 0));
        LinearLayout.LayoutParams chartLp = new LinearLayout.LayoutParams(0, -1, 1f);
        chartLp.leftMargin = dp(8);
        bottomRow.addView(chartBox, chartLp);

        content.addView(bottomRow, lp(-1, -2, 0, 0, 0, 0));

        applyPeriodStyles();
    }

    /** Seletor Hoje / 7 dias / Total para o card de atividade. */
    private View buildPeriodTabs() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setBackground(roundedBackground(Color.rgb(4, 16, 34), Color.rgb(14, 60, 96), 12, 1));
        row.setPadding(dp(2), dp(2), dp(2), dp(2));
        String[] names = {"HOJE", "7 DIAS", "TOTAL"};
        for (int i = 0; i < 3; i++) {
            final int index = i;
            TextView tab = text(names[i], 10, Color.rgb(170, 195, 215), true);
            tab.setGravity(Gravity.CENTER);
            tab.setMaxLines(1);
            tab.setOnClickListener(v -> {
                period = index;
                prefs.edit().putInt("dash_period", index).apply();
                applyPeriodStyles();
                refreshData();
            });
            periodTabs[i] = tab;
            LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(0, -1, 1f);
            tlp.setMargins(dp(1), 0, dp(1), 0);
            row.addView(tab, tlp);
        }
        return row;
    }

    private void applyPeriodStyles() {
        for (int i = 0; i < 3; i++) {
            TextView tab = periodTabs[i];
            if (tab == null) {
                continue;
            }
            boolean on = i == period;
            tab.setTextColor(on ? Color.WHITE : Color.rgb(170, 195, 215));
            tab.setBackground(on ? roundedBackground(tint(BLUE, 0.55f), BLUE, 10, 1) : null);
        }
        if (tvRingSuffix != null) {
            String[] names = {"DESEMPENHO (HOJE)", "DESEMPENHO (7 DIAS)", "DESEMPENHO (TOTAL)"};
            SpannableString t = new SpannableString(names[period]);
            t.setSpan(new ForegroundColorSpan(Color.rgb(175, 195, 215)), 10, t.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            tvRingSuffix.setText(t);
        }
    }

    /** Lê os contadores permanentes e atualiza números, anel e gráfico no lugar. */
    private void refreshData() {
        if (tvReceived == null) {
            return;
        }

        EventLog.Stats stats = EventLog.stats(this);
        long now = System.currentTimeMillis();

        int[] today = stats.day(now);
        int[] week = new int[4];
        int[] daily = new int[7];
        for (int ago = 0; ago < 7; ago++) {
            int[] d = stats.day(daysAgo(now, ago));
            for (int k = 0; k < 4; k++) {
                week[k] += d[k];
            }
            daily[6 - ago] = d[0];
        }
        int[] shown = period == PERIOD_TODAY ? today : (period == PERIOD_WEEK ? week : stats.total);

        tvReceived.setText(String.valueOf(shown[0]));
        tvSent.setText(String.valueOf(shown[1]));
        tvErrors.setText(String.valueOf(shown[2]));

        int rate = shown[0] <= 0 ? 0 : Math.min(100, Math.round(shown[1] * 100f / shown[0]));
        ring.setPercent(rate);

        tvSumTotal.setText(String.valueOf(stats.total[0]));
        tvSumToday.setText(String.valueOf(today[0]));
        tvSumTests.setText(String.valueOf(stats.total[3]));
        tvSumResellers.setText(String.valueOf(countResellers()));

        chart.setData(daily, lastSevenDayLabels());

        if (bellDot != null) {
            bellDot.setVisibility(today[2] > 0 ? View.VISIBLE : View.GONE);
        }
    }

    private long daysAgo(long now, int ago) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(now);
        c.add(Calendar.DAY_OF_YEAR, -ago);
        return c.getTimeInMillis();
    }

    private View buildHero() {
        FrameLayout frame = new FrameLayout(this);
        frame.setClipChildren(false);

        FrameLayout panel = new FrameLayout(this);
        panel.setBackground(gradientBorder(
                new int[]{ORANGE, Color.rgb(90, 80, 120), CYAN},
                GradientDrawable.Orientation.LEFT_RIGHT,
                radii(20, 20, 20, 20), Color.rgb(8, 22, 46), Color.rgb(3, 10, 26), 2));
        frame.addView(panel, new FrameLayout.LayoutParams(-1, dp(126), Gravity.BOTTOM));

        ImageView mascot = new ImageView(this);
        mascot.setImageResource(R.drawable.mascot_robot);
        mascot.setScaleType(ImageView.ScaleType.FIT_START);
        mascot.setAdjustViewBounds(true);
        frame.addView(mascot, new FrameLayout.LayoutParams(dp(140), dp(134), Gravity.BOTTOM | Gravity.START));

        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        texts.setGravity(Gravity.CENTER_VERTICAL);
        TextView eyebrow = text("CENTRAL DE AUTOMAÇÃO", 9, Color.rgb(190, 215, 235), false);
        eyebrow.setLetterSpacing(0.14f);
        eyebrow.setMaxLines(1);
        texts.addView(eyebrow, lp(-1, -2, 0, 0, dp(96), 0));
        TextView title = brandTitle(26, false);
        title.setMaxLines(1);
        autoFit(title, 14, 26);
        texts.addView(title, lp(-1, -2, 0, dp(3), 0, dp(3)));
        TextView sub = text("Atendimentos automáticos, testes e gestão de revendas em um só lugar.", 11.5f, Color.rgb(205, 222, 238), false);
        texts.addView(sub, lp(-1, -2, 0, 0, dp(78), 0));
        FrameLayout.LayoutParams textsLp = new FrameLayout.LayoutParams(-1, -1);
        textsLp.leftMargin = dp(144);
        textsLp.rightMargin = dp(12);
        textsLp.topMargin = dp(6);
        textsLp.bottomMargin = dp(8);
        panel.addView(texts, textsLp);

        boolean enabled = prefs.getBoolean("bot_enabled", false);
        dashboardBotBadge = text(enabled ? "● BOT ATIVO" : "● BOT PAUSADO", 10, enabled ? GREEN : ORANGE, true);
        dashboardBotBadge.setGravity(Gravity.CENTER);
        dashboardBotBadge.setPadding(dp(10), dp(4), dp(10), dp(4));
        dashboardBotBadge.setBackground(roundedBackground(Color.rgb(4, 36, 38), enabled ? GREEN : ORANGE, 12, 2));
        FrameLayout.LayoutParams badgeLp = new FrameLayout.LayoutParams(-2, -2, Gravity.TOP | Gravity.END);
        badgeLp.topMargin = dp(10);
        badgeLp.rightMargin = dp(10);
        panel.addView(dashboardBotBadge, badgeLp);

        botToggle = new NeonToggleView(this);
        botToggle.setCheckedSilently(enabled);
        botToggle.setOnChangeListener(checked -> {
            prefs.edit().putBoolean("bot_enabled", checked).apply();
            if (checked) {
                BackgroundRuntime.requestListenerRebind(this);
            }
            refreshStatus();
            Toast.makeText(this, checked ? "Bot ativado." : "Bot desativado.", Toast.LENGTH_SHORT).show();
        });
        FrameLayout.LayoutParams swLp = new FrameLayout.LayoutParams(dp(64), dp(34), Gravity.END | Gravity.BOTTOM);
        swLp.rightMargin = dp(14);
        swLp.bottomMargin = dp(22);
        panel.addView(botToggle, swLp);

        View underline = new View(this);
        underline.setBackground(new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{ORANGE, Color.argb(0, 255, 145, 20)}));
        FrameLayout.LayoutParams ulLp = new FrameLayout.LayoutParams(-1, dp(2), Gravity.BOTTOM);
        ulLp.leftMargin = dp(144);
        ulLp.rightMargin = dp(84);
        ulLp.bottomMargin = dp(10);
        panel.addView(underline, ulLp);

        return frame;
    }

    private LinearLayout sectionBox() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(10), dp(10), dp(10), dp(10));
        box.setBackground(gradientBorder(
                new int[]{PANEL_EDGE, Color.rgb(12, 50, 84)},
                GradientDrawable.Orientation.TL_BR,
                radii(18, 18, 18, 18), Color.rgb(6, 20, 40), Color.rgb(3, 12, 28), 1));
        return box;
    }

    private View sectionHeader(String iconName, int iconColor, String title, String suffix) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.addView(icon(iconName, iconColor), new LinearLayout.LayoutParams(dp(22), dp(22)));

        TextView t = text(title, 12.5f, Color.WHITE, true);
        t.setLetterSpacing(0.04f);
        if (!suffix.isEmpty()) {
            SpannableString s = new SpannableString(title + " " + suffix);
            s.setSpan(new ForegroundColorSpan(Color.rgb(175, 195, 215)), title.length(), s.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            t.setText(s);
        }
        t.setPadding(dp(8), 0, 0, 0);
        t.setMaxLines(1);
        autoFit(t, 8, 13);
        row.addView(t, new LinearLayout.LayoutParams(0, -2, 1f));
        return row;
    }

    private Chip addStatusCard(LinearLayout parent, String iconName, int iconColor, String name, int border, Runnable click) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER_HORIZONTAL);
        card.setPadding(dp(3), dp(8), dp(3), dp(7));
        card.setBackground(gradientBorder(
                new int[]{border, tint(border, 0.4f)},
                GradientDrawable.Orientation.TL_BR,
                radii(14, 14, 14, 14), tint(border, 0.22f), Color.rgb(4, 14, 30), 2));
        glow(card, border);

        if ("stv".equals(iconName)) {
            SpannableString s = new SpannableString("STV+");
            s.setSpan(new ForegroundColorSpan(YELLOW), 3, 4, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            TextView logo = text("", 19, Color.WHITE, true);
            logo.setTypeface(Typeface.create("sans-serif-black", Typeface.NORMAL));
            logo.setText(s);
            logo.setGravity(Gravity.CENTER);
            logo.setMaxLines(1);
            autoFit(logo, 12, 19);
            card.addView(logo, new LinearLayout.LayoutParams(-1, dp(30)));
        } else {
            card.addView(icon(iconName, iconColor), new LinearLayout.LayoutParams(dp(30), dp(30)));
        }

        TextView label = text(name, 10.5f, Color.WHITE, true);
        label.setGravity(Gravity.CENTER);
        label.setMaxLines(2);
        autoFit(label, 8, 11);
        card.addView(label, lp(-1, dp(30), 0, dp(3), 0, dp(3)));

        LinearLayout pill = new LinearLayout(this);
        pill.setOrientation(LinearLayout.HORIZONTAL);
        pill.setGravity(Gravity.CENTER);
        pill.setPadding(dp(4), dp(3), dp(4), dp(3));
        pill.setBackground(roundedBackground(Color.rgb(3, 34, 34), Color.rgb(14, 100, 78), 12, 1));
        View dot = new View(this);
        TextView state = text("...", 8.5f, Color.WHITE, false);
        state.setMaxLines(1);
        autoFit(state, 6, 9);
        state.setPadding(dp(3), 0, 0, 0);
        pill.addView(dot, new LinearLayout.LayoutParams(dp(6), dp(6)));
        pill.addView(state, new LinearLayout.LayoutParams(0, -2, 1f));
        card.addView(pill, new LinearLayout.LayoutParams(-1, dp(20)));

        if (click != null) {
            card.setClickable(true);
            card.setFocusable(true);
            card.setOnClickListener(v -> click.run());
        }

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(104), 1f);
        lp.setMargins(dp(2), 0, dp(2), 0);
        parent.addView(card, lp);
        return new Chip(state, dot, pill);
    }

    private TextView addMetric(LinearLayout parent, String iconName, int iconColor, String label, int accent) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.setPadding(dp(3), dp(6), dp(3), dp(6));
        box.setBackground(gradientBorder(
                new int[]{accent, tint(accent, 0.5f)},
                GradientDrawable.Orientation.TL_BR,
                radii(14, 14, 14, 14), tint(accent, 0.26f), Color.rgb(4, 14, 30), 2));
        box.addView(icon(iconName, iconColor), new LinearLayout.LayoutParams(dp(24), dp(24)));
        TextView l = text(label, 10.5f, Color.WHITE, false);
        l.setGravity(Gravity.CENTER);
        l.setMaxLines(1);
        autoFit(l, 7, 11);
        box.addView(l, lp(-1, -2, 0, dp(2), 0, 0));
        TextView v = text("0", 24, accent, true);
        v.setTypeface(Typeface.create("sans-serif-black", Typeface.NORMAL));
        v.setGravity(Gravity.CENTER);
        v.setMaxLines(1);
        autoFit(v, 14, 26);
        box.addView(v, new LinearLayout.LayoutParams(-1, 0, 1f));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -1, 1f);
        lp.setMargins(dp(2), 0, dp(2), 0);
        parent.addView(box, lp);
        return v;
    }

    private void addQuick(LinearLayout parent, String iconName, int accent, String label, Runnable action) {
        LinearLayout tile = new LinearLayout(this);
        tile.setOrientation(LinearLayout.VERTICAL);
        tile.setGravity(Gravity.CENTER);
        tile.setPadding(dp(2), dp(6), dp(2), dp(6));
        tile.setBackground(gradientBorder(
                new int[]{accent, tint(accent, 0.5f)},
                GradientDrawable.Orientation.TL_BR,
                radii(14, 14, 14, 14), tint(accent, 0.24f), Color.rgb(4, 14, 30), 2));
        glow(tile, accent);
        tile.addView(icon(iconName, accent), new LinearLayout.LayoutParams(dp(28), dp(28)));
        TextView caption = text(label, 10, Color.WHITE, true);
        caption.setGravity(Gravity.CENTER);
        caption.setMaxLines(2);
        autoFit(caption, 7, 10);
        tile.addView(caption, lp(-1, 0, 0, dp(4), 0, 0));
        ((LinearLayout.LayoutParams) caption.getLayoutParams()).weight = 1f;
        tile.setClickable(true);
        tile.setFocusable(true);
        tile.setOnClickListener(v -> action.run());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -1, 1f);
        lp.setMargins(dp(2), 0, dp(2), 0);
        parent.addView(tile, lp);
    }

    private TextView addSummaryRow(LinearLayout parent, String iconName, int iconColor, String label) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(8), 0, dp(8), 0);
        row.setBackground(roundedBackground(Color.rgb(7, 26, 46), Color.rgb(20, 70, 105), 10, 1));
        row.addView(icon(iconName, iconColor), new LinearLayout.LayoutParams(dp(16), dp(16)));
        TextView l = text(label, 11, Color.WHITE, false);
        l.setPadding(dp(8), 0, dp(4), 0);
        l.setMaxLines(1);
        autoFit(l, 7, 11);
        row.addView(l, new LinearLayout.LayoutParams(0, -2, 1f));
        TextView v = text("0", 13, Color.WHITE, true);
        v.setMaxLines(1);
        row.addView(v);
        parent.addView(row, lp(-1, dp(35), 0, dp(6), 0, 0));
        return v;
    }

    private void buildBottomNavigation(LinearLayout root) {
        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setGravity(Gravity.CENTER);
        nav.setPadding(dp(4), dp(5), dp(4), dp(5));
        nav.setBackground(gradientBorder(
                new int[]{CYAN, BLUE, ORANGE},
                GradientDrawable.Orientation.LEFT_RIGHT,
                radii(20, 20, 20, 20), Color.rgb(4, 16, 34), Color.rgb(3, 10, 24), 2));
        addNavItem(nav, "home", "Dashboard", true, null);
        addNavItem(nav, "robot", "Atendimentos", false, () -> startActivity(new Intent(this, EventHistoryActivity.class)));
        addNavItem(nav, "layers", "Módulos", false, this::openMenu);
        addNavItem(nav, "people", "Revendas", false, () -> startActivity(new Intent(this, ResellersActivity.class)));
        addNavItem(nav, "bars", "Relatórios", false, () -> startActivity(new Intent(this, MonitoringActivity.class)));
        addNavItem(nav, "gear", "Configurações", false, () -> startActivity(new Intent(this, AppSettingsActivity.class)));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(64));
        lp.setMargins(dp(8), dp(4), dp(8), dp(8));
        root.addView(nav, lp);
    }

    private void addNavItem(LinearLayout parent, String iconName, String label, boolean selected, Runnable action) {
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.VERTICAL);
        item.setGravity(Gravity.CENTER);
        item.setPadding(dp(1), dp(3), dp(1), dp(3));
        int color = selected ? ORANGE : CYAN;
        if (selected) {
            item.setBackground(gradientBorder(
                    new int[]{ORANGE, Color.rgb(255, 190, 60)},
                    GradientDrawable.Orientation.TL_BR,
                    radii(14, 14, 14, 14), Color.rgb(60, 32, 8), Color.rgb(26, 16, 10), 2));
            glow(item, ORANGE);
        }
        item.addView(icon(iconName, color), new LinearLayout.LayoutParams(dp(24), dp(24)));
        TextView caption = text(label, 9, selected ? ORANGE : Color.rgb(210, 224, 236), false);
        caption.setGravity(Gravity.CENTER);
        caption.setMaxLines(1);
        autoFit(caption, 6, 9);
        item.addView(caption, lp(-1, -2, 0, dp(2), 0, 0));
        item.setClickable(true);
        item.setOnClickListener(v -> {
            if (action != null) {
                action.run();
            }
        });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -1, 1f);
        lp.setMargins(dp(1), 0, dp(1), 0);
        parent.addView(item, lp);
    }

    // ------------------------------------------------------------------
    // Status
    // ------------------------------------------------------------------

    private void refreshStatus() {
        boolean enabled = prefs.getBoolean("bot_enabled", false);
        boolean access = hasNotificationAccess();
        boolean connected = prefs.getBoolean(BackgroundRuntime.KEY_LISTENER_CONNECTED, false);

        if (botChip != null) {
            botChip.set(enabled ? "Ativo" : "Desativado", enabled ? GREEN : GREY);
        }

        if (botToggle != null && botToggle.isChecked() != enabled) {
            botToggle.setCheckedSilently(enabled);
        }

        if (dashboardBotBadge != null) {
            dashboardBotBadge.setText(enabled ? "● BOT ATIVO" : "● BOT PAUSADO");
            dashboardBotBadge.setTextColor(enabled ? GREEN : ORANGE);
            dashboardBotBadge.setBackground(roundedBackground(Color.rgb(4, 36, 38), enabled ? GREEN : ORANGE, 12, 2));
        }

        if (waChip != null) {
            boolean installed = isPackageInstalled("com.whatsapp.w4b");
            if (!installed) {
                waChip.set("Não instalado", ORANGE);
            } else if (!access) {
                waChip.set("Sem permissão", ORANGE);
            } else if (connected) {
                waChip.set("Conectado", GREEN);
            } else {
                waChip.set("Reconectando", ORANGE);
                long nowMs = System.currentTimeMillis();
                if (nowMs - lastRebindAt > 30000L) {
                    lastRebindAt = nowMs;
                    BackgroundRuntime.requestListenerRebind(this);
                }
            }
        }

        if (stvChip != null) {
            SharedPreferences stvPrefs = getSharedPreferences("master_responde_stv", MODE_PRIVATE);
            boolean credentials = stvPrefs.getBoolean("stvplus_credentials_saved",
                    stvPrefs.getBoolean("credentials_saved", false));
            boolean activeSession = stvPrefs.getBoolean("stvplus_session_active", false);

            if (StvPlusBackgroundAutomation.isRunning()) {
                stvChip.set("Gerando teste", CYAN);
            } else if (activeSession) {
                stvChip.set("Sessão ativa", GREEN);
            } else if (credentials) {
                stvChip.set("Login salvo", ORANGE);
            } else {
                stvChip.set("Login necessário", ORANGE);
            }
        }

        if (engineChip != null) {
            if (!enabled) {
                engineChip.set("Desligado", GREY);
            } else if (!access) {
                engineChip.set("Sem permissão", ORANGE);
            } else if (connected) {
                engineChip.set("Ativo", GREEN);
            } else {
                engineChip.set("Reconectando", ORANGE);
            }
        }

        if (masterflixChip != null) {
            boolean active = prefs.getBoolean("masterflix_session_active", false);
            boolean credentials = prefs.getBoolean("masterflix_credentials_saved", false);

            if (active) {
                masterflixChip.set("Sessão ativa", GREEN);
            } else if (credentials) {
                masterflixChip.set("Login salvo", ORANGE);
            } else {
                masterflixChip.set("Login necessário", ORANGE);
            }
        }
    }

    private boolean hasNotificationAccess() {
        return BackgroundRuntime.hasNotificationAccess(this);
    }

    private boolean isPackageInstalled(String packageName) {
        try {
            getPackageManager().getPackageInfo(packageName, 0);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void openNotificationAccess() {
        try {
            startActivity(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"));
        } catch (Exception e) {
            Toast.makeText(this, "Abra Configurações > Acesso às notificações.", Toast.LENGTH_LONG).show();
        }
    }

    private void launchFromMenu(Intent intent) {
        if (intent == null) {
            return;
        }

        prefs.edit().putBoolean("return_to_menu_after_child", true).apply();
        startActivity(intent);
    }

    // ------------------------------------------------------------------
    // Dados auxiliares
    // ------------------------------------------------------------------

    private int countResellers() {
        Set<String> keys = new HashSet<>();
        try {
            for (String key : prefs.getAll().keySet()) {
                if (key.startsWith("reseller_submit_time_")) {
                    keys.add(key.substring("reseller_submit_time_".length()));
                }
            }
            SharedPreferences stvPrefs = getSharedPreferences("master_responde_stv", MODE_PRIVATE);
            for (String key : stvPrefs.getAll().keySet()) {
                if (key.startsWith("stv_reseller_submit_time_")) {
                    keys.add(key.substring("stv_reseller_submit_time_".length()));
                }
            }
        } catch (Exception ignored) {
        }
        return keys.size();
    }

    private long startOfDay(long time) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(time);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    private String[] lastSevenDayLabels() {
        String[] names = {"Dom", "Seg", "Ter", "Qua", "Qui", "Sex", "Sáb"};
        String[] out = new String[7];
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DAY_OF_YEAR, -6);
        for (int i = 0; i < 7; i++) {
            out[i] = names[c.get(Calendar.DAY_OF_WEEK) - 1];
            c.add(Calendar.DAY_OF_YEAR, 1);
        }
        return out;
    }

    // ------------------------------------------------------------------
    // Helpers visuais
    // ------------------------------------------------------------------

    private NeonIconView icon(String name, int color) {
        return new NeonIconView(this, name, color);
    }

    private TextView text(String value, float size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setIncludeFontPadding(false);
        if (bold) {
            t.setTypeface(null, Typeface.BOLD);
        }
        return t;
    }

    /** "MASTER RESPONDE" com RESPONDE em laranja, fonte pesada e itálica. */
    private TextView brandTitle(float size, boolean compact) {
        SpannableString s = new SpannableString("MASTER RESPONDE");
        s.setSpan(new ForegroundColorSpan(ORANGE), 7, s.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        TextView t = text("", size, Color.WHITE, true);
        t.setText(s);
        t.setTypeface(Typeface.create("sans-serif-black", Typeface.NORMAL));
        return t;
    }

    private void autoFit(TextView t, int minSp, int maxSp) {
        t.setAutoSizeTextTypeUniformWithConfiguration(minSp, maxSp, 1, TypedValue.COMPLEX_UNIT_SP);
    }

    private LinearLayout.LayoutParams lp(int w, int h, int l, int t, int r, int b) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(w, h);
        p.setMargins(l, t, r, b);
        return p;
    }

    private float[] radii(int tl, int tr, int br, int bl) {
        return new float[]{dp(tl), dp(tl), dp(tr), dp(tr), dp(br), dp(br), dp(bl), dp(bl)};
    }

    /** Fundo com borda em degradê: camada externa colorida + miolo escuro inset. */
    private Drawable gradientBorder(int[] border, GradientDrawable.Orientation orientation,
                                    float[] radiiPx, int fillTop, int fillBottom, int widthDp) {
        int[] colors = border.length >= 2 ? border : new int[]{border[0], border[0]};
        GradientDrawable outer = new GradientDrawable(orientation, colors);
        outer.setCornerRadii(radiiPx);

        float[] innerRadii = new float[8];
        float w = dp(widthDp);
        for (int i = 0; i < 8; i++) {
            innerRadii[i] = Math.max(0f, radiiPx[i] - w);
        }
        GradientDrawable inner = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{fillTop, fillBottom});
        inner.setCornerRadii(innerRadii);

        LayerDrawable layers = new LayerDrawable(new Drawable[]{outer, inner});
        int inset = dp(widthDp);
        layers.setLayerInset(1, inset, inset, inset, inset);
        return layers;
    }

    private GradientDrawable roundedBackground(int fill, int stroke, int radiusDp, int strokeDp) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(fill);
        d.setCornerRadius(dp(radiusDp));
        d.setStroke(dp(strokeDp), stroke);
        return d;
    }

    /** Mistura a cor com o fundo escuro (0 = fundo, 1 = cor pura). */
    private int tint(int color, float amount) {
        int r = Math.round(Color.red(color) * amount + 4 * (1f - amount));
        int g = Math.round(Color.green(color) * amount + 12 * (1f - amount));
        int b = Math.round(Color.blue(color) * amount + 28 * (1f - amount));
        return Color.rgb(r, g, b);
    }

    private void glow(View v, int color) {
        if (Build.VERSION.SDK_INT >= 28) {
            v.setElevation(dp(4));
            v.setOutlineSpotShadowColor(color);
            v.setOutlineAmbientShadowColor(color);
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
