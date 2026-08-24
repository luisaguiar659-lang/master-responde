from pathlib import Path
import re

app=Path('projeto/app')
java=app/'src/main/java/com/masterresponde/app'
acc=java/'TelegramWhatsAppAccessibilityService.java'
gradle=app/'build.gradle'
dash=java/'NeonDashboardActivity.java'

# v2.1.44
# NÃO altera TelegramApiAlbumService.java.
# Corrige somente o clique no RESULTADO da pesquisa do grupo no WhatsApp Business.
# Problema observado em vídeo: o serviço digitava o nome, encontrava o grupo visualmente,
# mas confundia o texto do campo de pesquisa com o item real da lista e não confirmava.

s=acc.read_text(encoding='utf-8')

old='''    private void selectGroup(AccessibilityNodeInfo root){
        String wanted=currentGroup();
        AccessibilityNodeInfo exact=findText(root,wanted,true);
        if(exact!=null&&click(exact)){
            stage=1;lastAction=System.currentTimeMillis();
            getSharedPreferences("master_responde",MODE_PRIVATE).edit().putString("telegram_accessibility_diag","GRUPO LOCALIZADO: "+wanted).apply();
            TelegramAutoDeliveryStore.status(this,"Grupo localizado: "+wanted);
            return;
        }
        AccessibilityNodeInfo edit=findEditable(root);
        if(edit!=null){
            Bundle b=new Bundle();b.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,wanted);
            if(edit.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT,b)){lastAction=System.currentTimeMillis();return;}
        }
        AccessibilityNodeInfo search=findAny(root,new String[]{"Pesquisar","Search","Buscar"});
        if(search!=null&&click(search)){lastAction=System.currentTimeMillis();}
    }'''

new='''    private void selectGroup(AccessibilityNodeInfo root){
        String wanted=currentGroup();

        // 1) PRIMEIRO procura o resultado real da lista. Ignora campos editáveis,
        // porque o texto digitado na busca também é exatamente igual ao nome do grupo.
        AccessibilityNodeInfo result=findExactNonEditable(root,wanted);
        if(result!=null && clickResultRow(result)){
            stage=1;lastAction=System.currentTimeMillis();
            getSharedPreferences("master_responde",MODE_PRIVATE).edit()
                .putString("telegram_accessibility_diag","GRUPO CONFIRMADO: "+wanted).apply();
            TelegramAutoDeliveryStore.status(this,"Grupo confirmado automaticamente: "+wanted);
            return;
        }

        // 2) Se ainda não apareceu resultado, garante que o nome está no campo de pesquisa.
        AccessibilityNodeInfo edit=findEditable(root);
        if(edit!=null){
            CharSequence current=edit.getText();
            String typed=current==null?"":current.toString().trim();
            if(!typed.equalsIgnoreCase(wanted)){
                Bundle b=new Bundle();
                b.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,wanted);
                if(edit.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT,b)){
                    getSharedPreferences("master_responde",MODE_PRIVATE).edit()
                        .putString("telegram_accessibility_diag","PESQUISANDO GRUPO: "+wanted).apply();
                    lastAction=System.currentTimeMillis();
                    h.postDelayed(()->{
                        if(busy && stage==0){
                            AccessibilityNodeInfo r=getRootInActiveWindow();
                            if(r!=null){try{selectGroup(r);}finally{try{r.recycle();}catch(Throwable ignored){}}}
                        }
                    },700L);
                    return;
                }
            }
        }

        // 3) Caso a tela ainda esteja antes da busca, abre a pesquisa.
        AccessibilityNodeInfo search=findAny(root,new String[]{"Pesquisar","Search","Buscar"});
        if(search!=null&&click(search)){
            getSharedPreferences("master_responde",MODE_PRIVATE).edit()
                .putString("telegram_accessibility_diag","ABRINDO PESQUISA DO GRUPO").apply();
            lastAction=System.currentTimeMillis();
        }
    }

    private AccessibilityNodeInfo findExactNonEditable(AccessibilityNodeInfo n,String text){
        if(n==null||text==null)return null;
        try{
            boolean editable=n.isEditable();
            CharSequence t=n.getText(),d=n.getContentDescription();
            if(!editable && (matches(t,text,true)||matches(d,text,true))) return n;
        }catch(Throwable ignored){}
        for(int i=0;i<n.getChildCount();i++){
            AccessibilityNodeInfo r=findExactNonEditable(n.getChild(i),text);
            if(r!=null)return r;
        }
        return null;
    }

    private boolean clickResultRow(AccessibilityNodeInfo node){
        AccessibilityNodeInfo x=node;
        // O texto do grupo geralmente fica dentro de uma linha/contêiner clicável.
        for(int i=0;i<8&&x!=null;i++){
            try{
                if(x.isClickable() && x.isEnabled() && x.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return true;
            }catch(Throwable ignored){}
            try{x=x.getParent();}catch(Throwable e){x=null;}
        }
        // Última tentativa: clique direto no próprio nó.
        try{return node.performAction(AccessibilityNodeInfo.ACTION_CLICK);}catch(Throwable ignored){return false;}
    }'''

if old not in s:
    raise SystemExit('ERRO v2.1.44: selectGroup esperado da v2.1.43 não encontrado')
s=s.replace(old,new,1)
acc.write_text(s,encoding='utf-8')

# versão
G=gradle.read_text(encoding='utf-8')
G=re.sub(r'versionCode\s+\d+','versionCode 115',G,count=1)
G=re.sub(r"versionName\s+'[^']+'","versionName '2.1.44'",G,count=1)
gradle.write_text(G,encoding='utf-8')
D=dash.read_text(encoding='utf-8')
D=re.sub(r'brand\.addView\(text\("v2\.1\.\d+",10,MUTED,false\)\);','brand.addView(text("v2.1.44",10,MUTED,false));',D,count=1)
dash.write_text(D,encoding='utf-8')

for pth,mark in [
    (acc,'findExactNonEditable'),
    (acc,'clickResultRow'),
    (acc,'GRUPO CONFIRMADO:'),
    (gradle,"versionName '2.1.44'")
]:
    if mark not in pth.read_text(encoding='utf-8'):
        raise SystemExit('ERRO v2.1.44: requisito ausente '+mark)

print('v2.1.44: clique automático no resultado real do grupo corrigido; captura Telegram API preservada')
