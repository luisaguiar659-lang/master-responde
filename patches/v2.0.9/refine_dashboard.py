from pathlib import Path

path = Path('projeto/app/src/main/java/com/masterresponde/app/NeonDashboardActivity.java')
text = path.read_text(encoding='utf-8')

def replace_once(old, new, label):
    global text
    if old not in text:
        raise SystemExit('ERRO refine_dashboard: ' + label)
    text = text.replace(old, new, 1)

replace_once(
    'private TextView wa,acc,notif,engine,mode,lastChat,lastMsg,lastReply,lastStatus,pending,sent,fail,attempts,today,totalSent,rate,totalFail,online; private ImageView power; private RingView queueRing,successRing;',
    'private TextView wa,acc,notif,engine,mode,lastChat,lastMsg,lastReply,lastStatus,pending,sent,fail,attempts,today,totalSent,rate,totalFail,online; private ImageView power; private RingView queueRing,successRing; private SwitchView modeTestSwitch;',
    'campo modeTestSwitch'
)

replace_once(
    'SwitchView testSwitch=new SwitchView();TextView test=text("MODO TESTE",10,MUTED,false);LinearLayout tr=row();tr.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);tr.addView(test);tr.addView(spaceH(8));tr.addView(testSwitch,new LinearLayout.LayoutParams(dp(36),dp(20)));',
    'modeTestSwitch=new SwitchView();modeTestSwitch.active=prefs.getBoolean("test_mode",false);modeTestSwitch.setOnClickListener(v->{boolean nv=!prefs.getBoolean("test_mode",false);prefs.edit().putBoolean("test_mode",nv).apply();modeTestSwitch.active=nv;modeTestSwitch.invalidate();});TextView test=text("MODO TESTE",10,MUTED,false);LinearLayout tr=row();tr.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);tr.addView(test);tr.addView(spaceH(8));tr.addView(modeTestSwitch,new LinearLayout.LayoutParams(dp(40),dp(22)));',
    'switch de modo teste'
)

replace_once(
    'if(power!=null){power.setAlpha(on?1.0f:0.55f);}',
    'if(power!=null){power.setAlpha(on?1.0f:0.55f);}if(modeTestSwitch!=null){modeTestSwitch.active=prefs.getBoolean("test_mode",false);modeTestSwitch.invalidate();}',
    'refresh do modo teste'
)

old_switch='private class SwitchView extends View{Paint p=new Paint(1);SwitchView(){super(NeonDashboardActivity.this);}protected void onDraw(Canvas c){float h=getHeight(),w=getWidth();p.setColor(Color.rgb(52,59,65));p.setStyle(Paint.Style.FILL);c.drawRoundRect(new RectF(0,0,w,h),h/2,h/2,p);p.setColor(Color.LTGRAY);c.drawCircle(h/2,h/2,h*.38f,p);}}'
new_switch='private class SwitchView extends View{Paint p=new Paint(1);boolean active=false;SwitchView(){super(NeonDashboardActivity.this);setClickable(true);}protected void onDraw(Canvas c){float h=getHeight(),w=getWidth();p.setStyle(Paint.Style.FILL);p.setColor(active?Color.rgb(20,125,60):Color.rgb(52,59,65));c.drawRoundRect(new RectF(0,0,w,h),h/2,h/2,p);p.setColor(active?GREEN:Color.LTGRAY);float cx=active?w-h/2:h/2;c.drawCircle(cx,h/2,h*.36f,p);}}'
replace_once(old_switch,new_switch,'desenho do switch')

old_activity='private TextView activityRow(LinearLayout p,int kind,String label){LinearLayout r=row();ServiceIconView icon=new ServiceIconView(kind==3?2:kind,CYAN);r.addView(icon,new LinearLayout.LayoutParams(dp(22),dp(22)));r.addView(text(label,9,MUTED,false),new LinearLayout.LayoutParams(dp(112),-2));TextView v=text("—",9,Color.WHITE,true);r.addView(v,new LinearLayout.LayoutParams(0,-2,1f));p.addView(r);p.addView(space(4));return v;}'
new_activity='private TextView activityRow(LinearLayout p,int kind,String label){LinearLayout r=row();String symbol=kind==0?"●":kind==1?"▣":kind==2?"➤":"✓";TextView icon=text(symbol,12,CYAN,true);icon.setGravity(Gravity.CENTER);r.addView(icon,new LinearLayout.LayoutParams(dp(22),dp(22)));r.addView(text(label,9,MUTED,false),new LinearLayout.LayoutParams(dp(112),-2));TextView v=text("—",9,Color.WHITE,true);r.addView(v,new LinearLayout.LayoutParams(0,-2,1f));p.addView(r);p.addView(space(5));return v;}'
replace_once(old_activity,new_activity,'ícones da atividade em tempo real')

text=text.replace('private TextView metric(LinearLayout p,String s){TextView t=text("• "+s,9,Color.WHITE,false);','private TextView metric(LinearLayout p,String s){TextView t=text("• "+s,10,Color.WHITE,false);',1)
text=text.replace('private void title(LinearLayout r,String s){r.addView(space(13));r.addView(text(s,10,Color.WHITE,true));','private void title(LinearLayout r,String s){r.addView(space(13));r.addView(text(s,11,Color.WHITE,true));',1)

path.write_text(text, encoding='utf-8')

final = path.read_text(encoding='utf-8')
for marker in ['test_mode','modeTestSwitch','String symbol=kind==0?"●"']:
    if marker not in final:
        raise SystemExit('ERRO refine_dashboard: validação ausente ' + marker)
print('Dashboard refinado: atividade alinhada, modo teste funcional e legibilidade melhorada')
