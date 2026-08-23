from pathlib import Path
import re
p=Path('projeto/app/src/main/java/com/masterresponde/app/NeonDashboardActivity.java')
d=p.read_text(encoding='utf-8')
if 'capture_listener_connected' not in d:
    d,n=re.subn(r'\bac\s*=\s*false\b','ac=prefs.getBoolean("capture_listener_connected",false)',d,count=1)
    if n==0:
        d,n=re.subn(r'\bac\s*=\s*isNotificationAccessEnabled\(\)','ac=prefs.getBoolean("capture_listener_connected",false)',d,count=1)
    if n==0:
        raise SystemExit('ERRO: variável ac do Dashboard não encontrada')
d=d.replace('acc=service(services,1,"Acessibilidade",PURPLE);','acc=service(services,1,"Captura",PURPLE);',1)
d=d.replace('set(acc,ac?"ATIVA":"OFF",ac);','set(acc,ac?"ATIVA":"RECONECTAR",ac);',1)
d=d.replace('set(engine,"EM CONSTRUÇÃO",false);','set(engine,"CAPTURA ONLY",false);',1)
p.write_text(d,encoding='utf-8')
assert 'capture_listener_connected' in d
print('Dashboard integrado ao listener de captura')
