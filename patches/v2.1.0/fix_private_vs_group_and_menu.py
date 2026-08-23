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
insert='''        if (message.isEmpty()) return;\n\n        // Mesma estratégia do projeto original funcional:\n        // primeiro EXTRA_IS_GROUP_CONVERSATION; fallback em EXTRA_CONVERSATION_TITLE.\n        boolean isGroup = isGroupNotification(n);'''
if anchor not in s: raise SystemExit('ERRO: ponto de detecção de mensagem não encontrado')
s=s.replace(anchor,insert,1)
needle='''        String resolvedReply = CommandEngine.resolve(this, message);'''
route='''        // GRUPO: encerra aqui. CommandEngine e RESPOSTA_PADRAO nunca recebem mensagem de grupo.\n        if (isGroup) {\n            String menuTrigger = GroupMenuStore.trigger(this);\n            String groupReply = null;\n            if (menuTrigger != null && !menuTrigger.trim().isEmpty() && message.trim().equalsIgnoreCase(menuTrigger.trim())) {\n                groupReply = GroupMenuStore.menu(this);\n            } else {\n                groupReply = GroupMenuStore.resolveOption(this, message);\n            }\n            if (groupReply != null && !groupReply.trim().isEmpty()) {\n                prefs().edit().putString("last_matched_command", "MENU_GRUPO").apply();\n                sendDirectReply(n, conversation, groupReply);\n            } else {\n                prefs().edit().putString("accessibility_last_status", "Grupo capturado • silencioso fora do menu").apply();\n            }\n            return;\n        }\n\n        // PRIVADO: somente daqui em diante entram comandos/resposta padrão.\n        String resolvedReply = CommandEngine.resolve(this, message);'''
if needle not in s: raise SystemExit('ERRO: CommandEngine.resolve não encontrado')
s=s.replace(needle,route,1)
pos=s.rfind('}')
helper=r'''
    private boolean isGroupNotification(Notification notification) {
        if (notification == null || notification.extras == null) return false;
        try {
            // Indicador oficial usado pelo projeto antigo.
            if (notification.extras.getBoolean(Notification.EXTRA_IS_GROUP_CONVERSATION, false)) {
                return true;
            }
            // Fallback do projeto antigo para aparelhos/versões onde o boolean não vem.
            CharSequence conversationTitle = notification.extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE);
            return conversationTitle != null && conversationTitle.toString().trim().length() > 0;
        } catch (Exception ignored) {
            return false;
        }
    }
'''
s=s[:pos]+helper+s[pos:]; svc.write_text(s,encoding='utf-8')
v=gradle.read_text(encoding='utf-8');v=re.sub(r'versionCode\s+\d+','versionCode 87',v,count=1);v=re.sub(r"versionName\s+'[^']+'","versionName '2.1.16'",v,count=1);gradle.write_text(v,encoding='utf-8')
for p,m in [(gm,'void render()'),(svc,'Notification.EXTRA_IS_GROUP_CONVERSATION'),(svc,'Notification.EXTRA_CONVERSATION_TITLE'),(svc,'MENU_GRUPO'),(gradle,"versionName '2.1.16'")]:
 if m not in p.read_text(encoding='utf-8'): raise SystemExit('ERRO requisito: '+m)
print('v2.1.16: detecção grupo/privado portada do projeto original; grupo retorna antes do motor privado')