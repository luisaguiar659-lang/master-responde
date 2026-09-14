package com.masterresponde.app;

import android.content.Context;
import android.webkit.WebView;

import org.json.JSONObject;

public final class MasterflixLoginAssist {

    public interface Callback {
        void onResult(String result);
    }

    private MasterflixLoginAssist() {
    }

    public static void fillAndSubmitIfSafe(
            Context context,
            WebView webView,
            Callback callback
    ) {
        if (context == null || webView == null) {
            if (callback != null) callback.onResult("unavailable");
            return;
        }

        String user =
                SigmaPanelConfig.getUser(
                        context
                );

        String pass =
                SigmaPanelConfig.getPass(
                        context
                );

        if (user.isEmpty()
                || pass.isEmpty()) {
            if (callback != null) callback.onResult("no_credentials");
            return;
        }

        String userJson =
                JSONObject.quote(
                        user
                );

        String passJson =
                JSONObject.quote(
                        pass
                );

        String script =
                "(function(){try{" +
                        "function vis(el){" +
                        "if(!el)return false;" +
                        "var r=el.getBoundingClientRect();" +
                        "var s=getComputedStyle(el);" +
                        "return r.width>0&&r.height>0&&s.display!=='none'&&s.visibility!=='hidden';" +
                        "}" +
                        "function set(el,v){" +
                        "if(!el)return false;" +
                        "try{" +
                        "var p=HTMLInputElement.prototype;" +
                        "Object.getOwnPropertyDescriptor(p,'value').set.call(el,v);" +
                        "}catch(e){el.value=v;}" +
                        "el.dispatchEvent(new Event('input',{bubbles:true}));" +
                        "el.dispatchEvent(new Event('change',{bubbles:true}));" +
                        "return true;" +
                        "}" +
                        "var inputs=document.querySelectorAll('input');" +
                        "var u=null,p=null;" +
                        "for(var i=0;i<inputs.length;i++){" +
                        "var el=inputs[i];if(!vis(el))continue;" +
                        "var t=(el.type||'').toLowerCase();" +
                        "if(t==='password'&&!p)p=el;" +
                        "else if((t==='text'||t==='email'||!t)&&!u)u=el;" +
                        "}" +
                        "if(!u||!p)return 'fields_missing';" +
                        "set(u," + userJson + ");set(p," + passJson + ");" +
                        "var body=(document.body.innerText||'').toLowerCase();" +
                        "var verification=" +
                        "document.querySelector('iframe[src*=captcha],.g-recaptcha,[data-sitekey]')" +
                        "||body.indexOf('captcha')!==-1" +
                        "||body.indexOf('verificado')!==-1" +
                        "||body.indexOf('não sou um robô')!==-1" +
                        "||body.indexOf('nao sou um robo')!==-1;" +
                        "if(verification)return 'verification_required';" +
                        "var buttons=document.querySelectorAll('button,input[type=submit],[role=button]');" +
                        "for(var j=0;j<buttons.length;j++){" +
                        "var b=buttons[j];if(!vis(b))continue;" +
                        "var tx=((b.innerText||b.value||b.getAttribute('aria-label')||'')+'').toLowerCase();" +
                        "if(tx.indexOf('entrar')!==-1||tx.indexOf('login')!==-1||tx.indexOf('acessar')!==-1){" +
                        "b.click();return 'submitted';" +
                        "}" +
                        "}" +
                        "return 'filled';" +
                        "}catch(e){return 'error';}})();";

        webView.evaluateJavascript(
                script,
                value -> {
                    String result =
                            "";

                    try {
                        Object decoded =
                                new org.json.JSONTokener(
                                        value
                                ).nextValue();

                        result =
                                decoded == null
                                        ? ""
                                        : decoded.toString();

                    } catch (Exception ignored) {
                    }

                    if (callback != null) {
                        callback.onResult(
                                result
                        );
                    }
                }
        );
    }
}
