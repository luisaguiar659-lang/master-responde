from pathlib import Path
import re

app=Path('projeto/app')
java=app/'src/main/java/com/masterresponde/app'
activity=java/'MasterXCloudActivity.java'
gradle=app/'build.gradle'

a=activity.read_text(encoding='utf-8')
old="""const ins=[...document.querySelectorAll('input')];const s=ins.find(i=>i.type==='search')||ins.find(i=>/search|busca|filtr/i.test(i.placeholder||''))||ins.find(i=>i.offsetParent!==null&&(i.type==='text'||!i.type));if(!s){if(n>=20){clearInterval(t);post('error','SEARCH_INPUT_NOT_FOUND','Campo de pesquisa não encontrado.');}return;}clearInterval(t);setReactInput(s,MAC);"""
new="""const ins=[...document.querySelectorAll('input')];const visible=i=>{try{const r=i.getBoundingClientRect();const cs=getComputedStyle(i);return r.width>0&&r.height>0&&cs.display!=='none'&&cs.visibility!=='hidden';}catch(e){return true;}};let s=ins.find(i=>visible(i)&&i.type==='search')||ins.find(i=>visible(i)&&/search|busca|filtr|device|key|mac/i.test((i.placeholder||'')+' '+(i.getAttribute('aria-label')||'')+' '+(i.name||'')));if(!s){const rows=[...document.querySelectorAll('tr')];if(rows.some(r=>(r.innerText||'').toUpperCase().includes(MAC.toUpperCase()))){clearInterval(t);const r=row();const m=r&&menu(r);if(!m){post('error','DEVICE_MENU_NOT_FOUND','Menu do dispositivo não encontrado.');return;}clickReal(m);setTimeout(()=>{const de=action(['deactivate','desativar','disable','inactiv']);const del=action(['delete','excluir','deletar','remove','apagar','trash','lixeira']);if(de){post('progress','DEACTIVATING','Desativando no painel...');clickReal(de);setTimeout(()=>{confirm();setTimeout(()=>{const r2=row();if(!r2){post('error','DEVICE_NOT_VISIBLE_AFTER_DEACTIVATE','Dispositivo saiu da lista após desativar.');return;}const m2=menu(r2);if(!m2){post('error','SECOND_MENU_NOT_FOUND','Não foi possível reabrir o menu.');return;}clickReal(m2);setTimeout(()=>{const d2=action(['delete','excluir','deletar','remove','apagar','trash','lixeira']);if(!d2){post('error','DELETE_ACTION_NOT_FOUND','Ação Delete não encontrada.');return;}post('progress','DELETING','Removendo dispositivo...');clickReal(d2);setTimeout(()=>{confirm();setTimeout(verify,900);},900);},1200);},2200);},800);return;}if(del){post('progress','DELETING','Removendo dispositivo...');clickReal(del);setTimeout(()=>{confirm();setTimeout(verify,900);},900);return;}post('error','DELETE_ACTION_NOT_FOUND','Deactivate/Delete não encontrado.');},1000);return;}if(n>=30){clearInterval(t);post('error','SEARCH_INPUT_NOT_FOUND','Campo de pesquisa não encontrado.');}return;}clearInterval(t);setReactInput(s,MAC);"""
if old not in a:
    raise SystemExit('ERRO v2.1.44: trecho de pesquisa do XCloud não encontrado')
a=a.replace(old,new,1)
activity.write_text(a,encoding='utf-8')

g=gradle.read_text(encoding='utf-8')
g=re.sub(r'versionCode\s+\d+','versionCode 119',g,count=1)
g=re.sub(r"versionName\s+'[^']+'","versionName '2.1.44'",g,count=1)
gradle.write_text(g,encoding='utf-8')

if "versionName '2.1.44'" not in gradle.read_text(encoding='utf-8'):
    raise SystemExit('ERRO v2.1.44: versão não aplicada')
if "rows.some(r=>(r.innerText||'').toUpperCase().includes(MAC.toUpperCase()))" not in activity.read_text(encoding='utf-8'):
    raise SystemExit('ERRO v2.1.44: fallback sem campo de pesquisa não aplicado')
print('v2.1.44: Reset/Excluir localizam a Key mesmo quando o painel não renderiza campo de pesquisa')
