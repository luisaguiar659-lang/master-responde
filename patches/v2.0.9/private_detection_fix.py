from pathlib import Path

path = Path('projeto/app/src/main/java/com/masterresponde/app/WhatsAppAccessibilityService.java')
text = path.read_text(encoding='utf-8')

old_guard = '''        // Evita tratar mensagens digitadas pelo próprio usuário como entrada.\n        if (isLikelyOutgoing(candidate.node)) {\n            updateDetectionStatus("Chat aberto: última mensagem é de saída; aguardando entrada");\n            return;\n        }\n'''
new_guard = '''        // Em algumas versões recentes do WhatsApp Business, a geometria da\n        // bolha privada pode fazer uma mensagem recebida parecer "de saída".\n        // Guardamos o sinal, mas só descartamos depois de conferir se o texto\n        // bate com uma regra privada configurada.\n        boolean mrLikelyOutgoing = isLikelyOutgoing(candidate.node);\n'''

if old_guard not in text:
    raise SystemExit('ERRO private_detection_fix: guarda de mensagem de saída não encontrada')
text = text.replace(old_guard, new_guard, 1)

anchor = '''        if (normalizedIncoming.isEmpty()) {\n            updateDetectionStatus("Chat aberto: conteúdo da mensagem ficou vazio após leitura");\n            return;\n        }\n'''
inject = anchor + '''\n        // Se a bolha parece de saída, ainda aceitamos quando ela corresponde a\n        // uma regra privada real. Isso corrige falsos positivos de alinhamento\n        // sem liberar respostas para qualquer texto digitado pelo próprio dono.\n        if (mrLikelyOutgoing\n                && !mrMatchesConfiguredPrivateRule(normalizedIncoming, prefs)) {\n            updateDetectionStatus("Chat aberto: mensagem de saída ignorada; aguardando entrada privada");\n            return;\n        }\n'''
if anchor not in text:
    raise SystemExit('ERRO private_detection_fix: ponto após normalizedIncoming não encontrado')
text = text.replace(anchor, inject, 1)

helper_anchor = '''    private String findConfiguredExactMessage(\n            String normalizedRaw,\n            SharedPreferences prefs\n    ) {\n'''
helper = '''    private boolean mrMatchesConfiguredPrivateRule(\n            String normalizedIncoming,\n            SharedPreferences prefs\n    ) {\n        if (normalizedIncoming == null\n                || normalizedIncoming.trim().isEmpty()\n                || prefs == null) {\n            return false;\n        }\n\n        // Gatilhos exatos já conhecidos pelo app.\n        if (!findConfiguredExactMessage(normalizedIncoming, prefs).isEmpty()) {\n            return true;\n        }\n\n        // Regras privadas cadastradas no painel. Respeita exact/contains.\n        try {\n            JSONArray rules = new JSONArray(\n                    prefs.getString("text_rules", "[]")\n            );\n\n            for (int i = 0; i < rules.length(); i++) {\n                JSONObject rule = rules.optJSONObject(i);\n                if (rule == null) continue;\n                if (rule.has("enabled")\n                        && !rule.optBoolean("enabled", true)) {\n                    continue;\n                }\n\n                String trigger = normalize(\n                        rule.optString("trigger", "")\n                );\n                if (trigger.isEmpty()) continue;\n\n                boolean exact = rule.optBoolean("exact", false);\n                if (exact) {\n                    if (normalizedIncoming.equals(trigger)\n                            || containsStandalone(normalizedIncoming, trigger)) {\n                        return true;\n                    }\n                } else if (normalizedIncoming.contains(trigger)) {\n                    return true;\n                }\n            }\n        } catch (Exception ignored) {\n        }\n\n        return false;\n    }\n\n'''
if helper_anchor not in text:
    raise SystemExit('ERRO private_detection_fix: ponto de inserção do helper não encontrado')
text = text.replace(helper_anchor, helper + helper_anchor, 1)

path.write_text(text, encoding='utf-8')

final = path.read_text(encoding='utf-8')
for marker in [
    'boolean mrLikelyOutgoing = isLikelyOutgoing(candidate.node);',
    'mrMatchesConfiguredPrivateRule(normalizedIncoming, prefs)',
    'mensagem de saída ignorada; aguardando entrada privada',
]:
    if marker not in final:
        raise SystemExit('ERRO private_detection_fix: validação ausente: ' + marker)

# A trava Business-only deve permanecer intacta.
if 'private static final String WHATSAPP = "com.whatsapp";' in final:
    raise SystemExit('ERRO private_detection_fix: WhatsApp normal voltou ao AccessibilityService')

print('Detecção privada corrigida sem alterar o @infor de grupos e mantendo Business-only')
