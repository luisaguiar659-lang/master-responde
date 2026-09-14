package com.masterresponde.app;

import android.content.Context;

import org.json.JSONObject;

public final class SigmaTestSelector {

    private SigmaTestSelector() {
    }

    public static String buildClickScript(
            Context context
    ) {
        String preferred =
                JSONObject.quote(
                        SigmaPanelConfig.getPreferredTest(
                                context
                        )
                );

        return "(function(){try{" +
                "function n(v){return (v||'').replace(/\\s+/g,' ').trim().toUpperCase();}" +
                "var preferred=n(" + preferred + ");" +
                "var all=document.querySelectorAll('body *');" +
                "var best=null,bestScore=-999999,bestLen=999999;" +
                "for(var i=0;i<all.length;i++){" +
                "var el=all[i];" +
                "var text=n(el.innerText||el.textContent);" +
                "if(!text||text.length>220)continue;" +
                "if(text==='TESTE RÁPIDO'||text==='TESTE RAPIDO'||text==='PESQUISAR')continue;" +
                "if(text.indexOf('TESTE')===-1)continue;" +
                "if(text.indexOf('GERAR TESTE')!==-1)continue;" +
                "if(text.indexOf('ADICIONAR CLIENTE')!==-1)continue;" +
                "var score=0;" +
                "if(preferred&&text.indexOf(preferred)!==-1)score+=10000;" +
                "if(text.indexOf('COMPLETO')!==-1)score+=500;" +
                "if(text.indexOf('1H')!==-1)score+=120;" +
                "if(text.indexOf('SEM/ADULTOS')!==-1||text.indexOf('S/ADULTOS')!==-1)score+=30;" +
                "if(text.indexOf('COM/ADULTOS')!==-1||text.indexOf('C/ADULTOS')!==-1)score+=20;" +
                "score-=Math.min(text.length,200);" +
                "if(score>bestScore||(score===bestScore&&text.length<bestLen)){" +
                "best=el;bestScore=score;bestLen=text.length;" +
                "}" +
                "}" +
                "if(!best)return 'not_found';" +
                "var clickable=best.closest('button,a,[role=button],[tabindex],li')||best;" +
                "try{best.scrollIntoView({behavior:'auto',block:'center',inline:'nearest'});}catch(e){}" +
                "try{clickable.click();return 'clicked';}" +
                "catch(e){" +
                "try{best.dispatchEvent(new MouseEvent('click',{bubbles:true,cancelable:true,view:window}));return 'clicked';}" +
                "catch(e2){return 'error';}" +
                "}" +
                "}catch(e){return 'error';}})();";
    }
}
