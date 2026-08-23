from pathlib import Path
import re
app=Path('projeto/app'); java=app/'src/main/java/com/masterresponde/app'; svc=java/'WhatsAppBusinessCaptureService.java'; gm=java/'GroupMenuActivity.java'; gradle=app/'build.gradle'

g=gm.read_text(encoding='utf-8')
g=g.replace('''o.put("enabled",true);items.put(o);}catch(Exception ignored){}load();});''','''o.put("enabled",true);items.put(o);}catch(Exception ignored){}render();});''')
g=g.replace('''items=n;load();});''','''items=n;render();});''')
old=''' public void onCreate(Bundle b){super.onCreate(b);load();}\n void load(){SharedPreferences p=getSharedPreferences("master_prefs",0);try{items=new JSONArray(p.getString("group_menu_items","[]"));}catch(Exception e){items=new JSONArray();}ScrollView sc='''
new=''' public void onCreate(Bundle b){super.onCreate(b);load();}\n void load(){SharedPreferences p=getSharedPreferences("master_prefs",0);try{items=new JSONArray(p.getString("group_menu_items","[]"));}catch(Exception e){items=new JSONArray();}render();}\n void render(){SharedPreferences p=getSharedPreferences("master_prefs",0);ScrollView sc='''
if old not in g: raise SystemExit('ERRO: bloco load GroupMenuActivity não encontrado')
g=g.replace(old,new,1); gm.write_text(g,encoding='utf-8')

s=svc.read_text(encoding='utf-8')
anchor='''        if (message.isEmpty()) return;'''
insert='''        if (message.isEmpty()) return;\n\n        boolean isGroup = isGroupNotification(extras);'''
if anchor not in s: raise SystemExit('ERRO: ponto de detecção de mensagem não encontrado')
s=s.replace(anchor,insert,1)
needle='''        String resolvedReply = CommandEngine.resolve(this, message);'''
route='''        if (isGroup) {\n            String menuTrigger = GroupMenuStore.trigger(this);\n            String groupReply = null;\n            if (menuTrigger != null && !menuTrigger.trim().isEmpty() && message.trim().equalsIgnoreCase(menuTrigger.trim())) groupReply = GroupMenuStore.menu(this);\n            else groupReply = GroupMenuStore.resolveOption(this, message);\n            if (groupReply != null && !groupReply.trim().isEmpty()) {\n                prefs().edit().putString("last_matched_command", "MENU_GRUPO").apply();\n                sendDirectReply(n, conversation, groupReply);\n            } else prefs().edit().putString("accessibility_last_status", "Grupo capturado • sem ação do menu").apply();\n            return;\n        }\n\n        String resolvedReply = CommandEngine.resolve(this, message);'''
if needle not in s: raise SystemExit('ERRO: CommandEngine.resolve não encontrado')
s=s.replace(needle,route,1)
pos=s.rfind('}')
helper=r'''
    private boolean isGroupNotification(Bundle extras) {
        try {
            CharSequence conv = extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE);
            CharSequence title = extras.getCharSequence(Notification.EXTRA_TITLE);
            CharSequence sub = extras.getCharSequence(Notification.EXTRA_SUB_TEXT);
            if (conv != null && !conv.toString().trim().isEmpty()) return true;
            if (sub != null && title != null) {
                String a=sub.toString().trim(), b=title.toString().trim();
                if (!a.isEmpty() && !b.isEmpty() && !a.equalsIgnoreCase(b)) return true;
            }
        } catch (Exception ignored) {}
        return false;
    }
'''
s=s[:pos]+helper+s[pos:]; svc.write_text(s,encoding='utf-8')
v=gradle.read_text(encoding='utf-8');v=re.sub(r'versionCode\s+\d+','versionCode 86',v,count=1);v=re.sub(r"versionName\s+'[^']+'","versionName '2.1.15'",v,count=1);gradle.write_text(v,encoding='utf-8')
for p,m in [(gm,'void render()'),(svc,'boolean isGroup'),(svc,'MENU_GRUPO'),(svc,'GroupMenuStore.resolveOption'),(gradle,"versionName '2.1.15'")]:
 if m not in p.read_text(encoding='utf-8'): raise SystemExit('ERRO requisito: '+m)
if 'getNotificationStyle' in svc.read_text(encoding='utf-8'): raise SystemExit('ERRO: API inválida ainda presente')
print('v2.1.15 fix: privado/grupo + menu, sem API Notification inexistente')