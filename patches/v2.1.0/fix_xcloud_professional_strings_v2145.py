from pathlib import Path

flow=Path('projeto/app/src/main/java/com/masterresponde/app/MasterXCloudWhatsAppFlow.java')
s=flow.read_text(encoding='utf-8')

bad1='''send(c,n,MasterXCloudOps.msg(c,"xcloud_msg_processing","⏳ MASTER XCLOUD

Dados recebidos. Processando, aguarde..."));'''
good1='''send(c,n,MasterXCloudOps.msg(c,"xcloud_msg_processing","⏳ MASTER XCLOUD\\n\\nDados recebidos. Processando, aguarde..."));'''
bad2='''if(pos>1)send(c,n,"🕒 MASTER XCLOUD

Seu pedido entrou na fila. Posição: "+pos+".");'''
good2='''if(pos>1)send(c,n,"🕒 MASTER XCLOUD\\n\\nSeu pedido entrou na fila. Posição: "+pos+".");'''

if bad1 not in s: raise SystemExit('ERRO v2.1.45 strings: mensagem processamento não encontrada')
if bad2 not in s: raise SystemExit('ERRO v2.1.45 strings: mensagem fila não encontrada')
s=s.replace(bad1,good1,1).replace(bad2,good2,1)
flow.write_text(s,encoding='utf-8')

if 'MASTER XCLOUD\\n\\nDados recebidos' not in s: raise SystemExit('ERRO v2.1.45 strings: escape processamento não aplicado')
if 'MASTER XCLOUD\\n\\nSeu pedido entrou na fila' not in s: raise SystemExit('ERRO v2.1.45 strings: escape fila não aplicado')
print('v2.1.45: escapes Java das mensagens profissionais corrigidos')
