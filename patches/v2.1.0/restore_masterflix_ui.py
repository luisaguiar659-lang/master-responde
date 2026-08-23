from pathlib import Path
import re, zipfile

app=Path('projeto/app'); java=app/'src/main/java/com/masterresponde/app'; manifest=app/'src/main/AndroidManifest.xml'; dash=java/'NeonDashboardActivity.java'; gradle=app/'build.gradle'; source_zip=Path('MASTER-RESPONDE-RAIZ.zip')

# Reaproveita somente componentes seguros de configuração/credenciais do projeto original.
with zipfile.ZipFile(source_zip,'r') as z:
    for name in ['SecureStore.java','SigmaPanelConfig.java']:
        src='app/src/main/java/com/masterresponde/app/'+name
        (java/name).write_bytes(z.read(src))

# Activity reconstruída com a mesma função original de login/sessão e WebView,
# sem religar automações antigas nesta etapa.
(java/'MasterflixActivity.java').write_text(r'''package com.masterresponde.app;

import android.app.*;import android.os.*;import android.content.*;import android.graphics.*;import android.graphics.Typeface;import android.view.*;import android.webkit.*;import android.widget.*;

public class MasterflixActivity extends Activity{
 private SecureStore secure; private EditText user,pass; private TextView status; private WebView web;
 @Override public void onCreate(Bundle b){super.onCreate(b);secure=new SecureStore(this);setContentView(build());loadSaved();openDashboard();}
 private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);} 
 private TextView tv(String s,int z,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(z);t.setTextColor(Color.WHITE);if(bold)t.setTypeface(null,Typeface.BOLD);t.setPadding(8,10,8,10);return t;}
 private EditText field(String hint){EditText e=new EditText(this);e.setHint(hint);e.setTextColor(Color.WHITE);e.setHintTextColor(Color.GRAY);e.setBackgroundColor(Color.rgb(5,18,25));e.setPadding(16,14,16,14);return e;}
 private Button btn(String s){Button b=new Button(this);b.setText(s);return b;}
 private View build(){LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(Color.rgb(1,5,9));LinearLayout h=new LinearLayout(this);h.setGravity(Gravity.CENTER_VERTICAL);Button back=btn("←");back.setOnClickListener(v->finish());h.addView(back,new LinearLayout.LayoutParams(dp(58),dp(52)));LinearLayout tt=new LinearLayout(this);tt.setOrientation(LinearLayout.VERTICAL);tt.addView(tv(SigmaPanelConfig.getPanelName(this),22,true));TextView st=tv("Login e sessão do painel Sigma",12,false);st.setTextColor(Color.rgb(150,160,170));tt.addView(st);h.addView(tt,new LinearLayout.LayoutParams(0,-2,1));root.addView(h);
 ScrollView sc=new ScrollView(this);LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(16),dp(12),dp(16),dp(24));status=tv("● Sessão não verificada",15,true);status.setTextColor(Color.rgb(255,160,0));c.addView(status);c.addView(tv("CREDENCIAIS",12,true));user=field("Usuário ou E-mail");pass=field("Senha");pass.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);c.addView(user);c.addView(pass);Button save=btn("SALVAR CREDENCIAIS");save.setOnClickListener(v->save());c.addView(save);Button fill=btn("PREENCHER LOGIN SALVO");fill.setOnClickListener(v->fillLogin());c.addView(fill);LinearLayout br=new LinearLayout(this);Button login=btn("LOGIN");login.setOnClickListener(v->openLogin());Button panel=btn("PAINEL");panel.setOnClickListener(v->openDashboard());br.addView(login,new LinearLayout.LayoutParams(0,-2,1));br.addView(panel,new LinearLayout.LayoutParams(0,-2,1));c.addView(br);web=new WebView(this);web.setLayoutParams(new LinearLayout.LayoutParams(-1,dp(520)));WebSettings ws=web.getSettings();ws.setJavaScriptEnabled(true);ws.setDomStorageEnabled(true);ws.setLoadsImagesAutomatically(true);CookieManager.getInstance().setAcceptCookie(true);if(Build.VERSION.SDK_INT>=21)CookieManager.getInstance().setAcceptThirdPartyCookies(web,true);web.setWebViewClient(new WebViewClient(){@Override public void onPageFinished(WebView v,String url){status.setText("● "+(url!=null&&url.contains("sign-in")?"Login necessário":"Painel carregado"));status.setTextColor(url!=null&&url.contains("sign-in")?Color.rgb(255,160,0):Color.rgb(25,255,70));}});c.addView(web);sc.addView(c);root.addView(sc,new LinearLayout.LayoutParams(-1,0,1));return root;}
 private void loadSaved(){user.setText(secure.get("sigma_user"));pass.setText(secure.get("sigma_pass"));}
 private void save(){try{secure.put("sigma_user",user.getText().toString().trim());secure.put("sigma_pass",pass.getText().toString());SigmaPanelConfig.saveCredentials(this,user.getText().toString().trim(),pass.getText().toString());Toast.makeText(this,"Credenciais salvas com segurança",Toast.LENGTH_SHORT).show();}catch(Exception e){Toast.makeText(this,"Não foi possível salvar",Toast.LENGTH_SHORT).show();}}
 private void fillLogin(){String u=secure.get("sigma_user"),p=secure.get("sigma_pass");user.setText(u);pass.setText(p);if(web==null)return;String js="(function(){var u="+org.json.JSONObject.quote(u)+",p="+org.json.JSONObject.quote(p)+";var es=document.querySelectorAll('input');for(var i=0;i<es.length;i++){var x=es[i],t=(x.type||'').toLowerCase();if(t==='password'){x.value=p;x.dispatchEvent(new Event('input',{bubbles:true}));}else if(t==='email'||t==='text'){if(!x.value){x.value=u;x.dispatchEvent(new Event('input',{bubbles:true}));}}}})();";web.evaluateJavascript(js,null);}
 private void openLogin(){web.loadUrl(SigmaPanelConfig.loginUrl(this));}
 private void openDashboard(){web.loadUrl(SigmaPanelConfig.dashboardUrl(this));}
 @Override protected void onDestroy(){if(web!=null){web.destroy();web=null;}super.onDestroy();}
}''',encoding='utf-8')

m=manifest.read_text(encoding='utf-8')
if 'android.permission.INTERNET' not in m:m=m.replace('<manifest xmlns:android="http://schemas.android.com/apk/res/android">','<manifest xmlns:android="http://schemas.android.com/apk/res/android">\n    <uses-permission android:name="android.permission.INTERNET"/>')
if '.MasterflixActivity' not in m:m=m.replace('</application>','<activity android:name=".MasterflixActivity" android:screenOrientation="portrait" android:exported="false"/>\n</application>')
manifest.write_text(m,encoding='utf-8')

d=dash.read_text(encoding='utf-8')
anchor='''  sideSection(panel,"AUTOMAÇÃO");'''
if anchor not in d: raise SystemExit('ERRO v2.1.22: seção AUTOMAÇÃO não encontrada')
if 'MasterflixActivity.class' not in d:
    d=d.replace(anchor,'''  sideSection(panel,"MASTERFLIX");\n  panel.addView(sideItem("▶","MasterFlix",Color.WHITE,v->{d.dismiss();startActivity(new Intent(this,MasterflixActivity.class));}));\n  panel.addView(space(10));\n  sideSection(panel,"AUTOMAÇÃO");''',1)
d=d.replace('brand.addView(text("v2.1.21",10,MUTED,false));','brand.addView(text("v2.1.22",10,MUTED,false));')
d=d.replace('brand.addView(text("v2.1.20",10,MUTED,false));','brand.addView(text("v2.1.22",10,MUTED,false));')
dash.write_text(d,encoding='utf-8')

g=gradle.read_text(encoding='utf-8');g=re.sub(r'versionCode\s+\d+','versionCode 93',g,count=1);g=re.sub(r"versionName\s+'[^']+'","versionName '2.1.22'",g,count=1);gradle.write_text(g,encoding='utf-8')
for p,mk in [(java/'MasterflixActivity.java','PREENCHER LOGIN SALVO'),(java/'SecureStore.java','AndroidKeyStore'),(java/'SigmaPanelConfig.java','DEFAULT_BASE_URL'),(dash,'MasterflixActivity.class'),(manifest,'.MasterflixActivity'),(gradle,"versionName '2.1.22'")]:
 if mk not in p.read_text(encoding='utf-8'):raise SystemExit('ERRO v2.1.22: requisito ausente '+mk)
print('v2.1.22: MasterFlix restaurado com tela/login/WebView do fluxo antigo; automações antigas continuam desligadas')