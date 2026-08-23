package com.masterresponde.app;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class NeonSettingsActivity extends Activity {
    private static final int BG=Color.rgb(3,7,12), CARD=Color.rgb(8,15,23), GREEN=Color.rgb(0,255,102), CYAN=Color.rgb(0,214,255), RED=Color.rgb(255,47,92), MUTED=Color.rgb(147,164,178);
    private SharedPreferences prefs;

    @Override protected void onCreate(Bundle b){super.onCreate(b);prefs=getSharedPreferences("master_responde",MODE_PRIVATE);getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);setContentView(build());}

    private ScrollView build(){ScrollView s=new ScrollView(this);s.setBackgroundColor(BG);LinearLayout root=col();root.setPadding(dp(16),dp(18),dp(16),dp(30));s.addView(root,new ViewGroup.LayoutParams(-1,-2));
        LinearLayout header=row();TextView back=text("‹",34,CYAN,true);back.setGravity(Gravity.CENTER);back.setOnClickListener(v->finish());header.addView(back,new LinearLayout.LayoutParams(dp(48),dp(48)));TextView title=text("CONFIGURAÇÕES",24,Color.WHITE,true);header.addView(title,new LinearLayout.LayoutParams(0,-2,1f));root.addView(header);
        root.addView(text("MASTER RESPONDE • FUTURISTA NEON",11,MUTED,false));space(root,18);
        section(root,"BOT");root.addView(toggleButton());
        section(root,"PERMISSÕES");root.addView(action("♿  ACESSIBILIDADE","Abrir permissão do serviço de respostas",v->openAccessibility()));space(root,8);root.addView(action("♢  NOTIFICAÇÕES","Abrir acesso às notificações",v->openNotificationAccess()));
        section(root,"RESPOSTAS E COMANDOS");root.addView(action("▦  CONFIGURAR MENSAGENS","Editar gatilhos, menus e respostas automáticas",v->openMessages()));
        section(root,"SEGURANÇA");LinearLayout sec=card();sec.addView(text("🛡  WHATSAPP BUSINESS ONLY",16,GREEN,true));space(sec,8);sec.addView(text("Pacote permitido: com.whatsapp.w4b\nWhatsApp normal: bloqueado\nAnti-eco: ativo\nProteção contra conversa errada: ativa",13,Color.WHITE,false));root.addView(sec);
        section(root,"SISTEMA");root.addView(action("↻  ATUALIZAR STATUS","Recarregar estados e permissões",v->Toast.makeText(this,"Status atualizado",Toast.LENGTH_SHORT).show()));space(root,8);root.addView(action("ℹ  SOBRE","MASTER RESPONDE v2.0.9 • Interface Futurista Neon",v->Toast.makeText(this,"MASTER RESPONDE v2.0.9",Toast.LENGTH_SHORT).show()));return s;}

    private Button toggleButton(){boolean enabled=prefs.getBoolean("bot_enabled",false);Button b=new Button(this);b.setAllCaps(false);b.setTextSize(16);b.setTypeface(Typeface.DEFAULT_BOLD);b.setText(enabled?"⏸  PAUSAR BOT":"⏻  ATIVAR BOT");b.setTextColor(enabled?Color.WHITE:GREEN);b.setPadding(dp(14),dp(16),dp(14),dp(16));b.setBackground(bg(enabled?Color.rgb(42,7,18):Color.rgb(2,31,16),enabled?RED:GREEN,16,2));b.setOnClickListener(v->{boolean next=!prefs.getBoolean("bot_enabled",false);prefs.edit().putBoolean("bot_enabled",next).apply();recreate();});return b;}
    private Button action(String title,String sub,android.view.View.OnClickListener l){Button b=new Button(this);b.setAllCaps(false);b.setGravity(Gravity.LEFT|Gravity.CENTER_VERTICAL);b.setText(title+"\n"+sub);b.setTextSize(13);b.setTextColor(Color.WHITE);b.setPadding(dp(14),dp(14),dp(14),dp(14));b.setBackground(bg(CARD,Color.rgb(25,89,116),14,1));b.setOnClickListener(l);return b;}
    private void openAccessibility(){try{startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));}catch(Throwable e){Toast.makeText(this,"Não foi possível abrir Acessibilidade",Toast.LENGTH_SHORT).show();}}
    private void openNotificationAccess(){try{startActivity(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"));}catch(Throwable e){startActivity(new Intent(Settings.ACTION_SETTINGS));}}
    private void openMessages(){try{startActivity(new Intent(this,MessageSettingsActivity.class));}catch(Throwable e){Toast.makeText(this,"Configuração de mensagens indisponível",Toast.LENGTH_SHORT).show();}}
    private void section(LinearLayout r,String t){space(r,22);TextView v=text(t,12,Color.WHITE,true);v.setLetterSpacing(.06f);r.addView(v);space(r,9);} private LinearLayout card(){LinearLayout l=col();l.setPadding(dp(14),dp(14),dp(14),dp(14));l.setBackground(bg(CARD,CYAN,16,1));return l;}
    private LinearLayout col(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;} private LinearLayout row(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.HORIZONTAL);l.setGravity(Gravity.CENTER_VERTICAL);return l;} private TextView text(String s,int sp,int c,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(sp);t.setTextColor(c);t.setTypeface(Typeface.DEFAULT,bold?Typeface.BOLD:Typeface.NORMAL);return t;} private GradientDrawable bg(int fill,int stroke,int radius,int width){GradientDrawable d=new GradientDrawable();d.setColor(fill);d.setCornerRadius(dp(radius));d.setStroke(dp(width),stroke);return d;} private void space(LinearLayout l,int h){TextView v=new TextView(this);v.setHeight(dp(h));l.addView(v);} private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
}
