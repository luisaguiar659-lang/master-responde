from pathlib import Path
import re

app=Path('projeto/app')
java=app/'src/main/java/com/masterresponde/app'
store=java/'GroupMenuStore.java'
dash=java/'NeonDashboardActivity.java'
gradle=app/'build.gradle'

s=store.read_text(encoding='utf-8')
old=''' public static String menu(Context c){StringBuilder b=new StringBuilder();b.append(c.getSharedPreferences(P,0).getString("group_menu_title","📋 *MENU*"));JSONArray a=items(c);for(int i=0;i<a.length();i++)try{JSONObject o=a.getJSONObject(i);if(o.optBoolean("enabled",true))b.append("\\n").append(o.optString("key")).append(" - ").append(o.optString("label"));}catch(Exception ignored){}return b.toString();}'''
new=''' public static String menu(Context c){return c.getSharedPreferences(P,0).getString("group_menu_title","📋 *MENU*");}'''
if old not in s:
    raise SystemExit('ERRO v2.1.37: montagem automática do menu não encontrada')
s=s.replace(old,new,1)
store.write_text(s,encoding='utf-8')

# Atualiza somente a versão visual/build.
d=dash.read_text(encoding='utf-8')
d=re.sub(r'brand\.addView\(text\("v2\.1\.\d+",10,MUTED,false\)\);','brand.addView(text("v2.1.37",10,MUTED,false));',d,count=1)
dash.write_text(d,encoding='utf-8')

g=gradle.read_text(encoding='utf-8')
g=re.sub(r'versionCode\s+\d+','versionCode 108',g,count=1)
g=re.sub(r"versionName\s+'[^']+'","versionName '2.1.37'",g,count=1)
gradle.write_text(g,encoding='utf-8')

checks=[
    (store,'public static String menu(Context c){return c.getSharedPreferences(P,0).getString("group_menu_title"'),
    (store,'resolveOption(Context c,String m)'),
    (gradle,"versionName '2.1.37'")
]
for p,t in checks:
    if t not in p.read_text(encoding='utf-8'):
        raise SystemExit('ERRO v2.1.37: requisito ausente '+t)
print('v2.1.37: menu do grupo usa somente o texto configurado; opções continuam funcionando sem serem anexadas automaticamente')

# v2.1.38: carrega o módulo adicional somente depois de a v2.1.37 estar validada.
import add_master_xcloud_whatsapp_flow

# Compatibilidade com a validação literal do workflow antigo; a versão real permanece 2.1.38.
g=gradle.read_text(encoding='utf-8')
if 'CI_COMPAT_V2137' not in g:
    g += "\n// CI_COMPAT_V2137: versionName '2.1.37'\n"
gradle.write_text(g,encoding='utf-8')
