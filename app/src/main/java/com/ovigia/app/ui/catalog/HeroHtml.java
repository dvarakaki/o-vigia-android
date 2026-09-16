package com.ovigia.app.ui.catalog;

import java.util.regex.Pattern;

/**
 * Transforma a biografia HTML da Comic Vine num documento com a cara do app
 * (fundo transparente, títulos dourados, imagens arredondadas e responsivas).
 *
 * A Comic Vine carrega imagens com JavaScript ({@code img.js-lazy-load-image}) e
 * repete a versão real em {@code <noscript>}; como o WebView roda sem JavaScript,
 * as de carregamento tardio são escondidas e as do {@code noscript} aparecem.
 */
final class HeroHtml {

    private static final Pattern SCRIPT = Pattern.compile("(?is)<script.*?</script>");
    private static final Pattern STYLE = Pattern.compile("(?is)<style.*?</style>");
    private static final Pattern IFRAME = Pattern.compile("(?is)<iframe.*?</iframe>");

    private static final String CSS =
            "html,body{margin:0;padding:0;background:transparent}"
            + "body{color:#E9ECF8;font-family:sans-serif;font-size:16px;line-height:1.65;"
            + "overflow-wrap:anywhere;-webkit-text-size-adjust:100%}"
            + "h1,h2{color:#E8C468;font-size:20px;line-height:1.3;margin:26px 0 10px;padding-bottom:6px;"
            + "border-bottom:1px solid rgba(232,196,104,.25)}"
            + "body>h1:first-child,body>h2:first-child,body>h3:first-child{margin-top:0}"
            + "h3{color:#fff;font-size:17px;margin:22px 0 8px}"
            + "h4,h5,h6{color:#C9D3FF;font-size:13px;margin:18px 0 6px;text-transform:uppercase;letter-spacing:.05em}"
            + "p{margin:0 0 14px}"
            + "a{color:#8FB0FF;text-decoration:none}"
            + "figure{margin:18px auto!important;width:auto!important;max-width:100%!important;float:none!important;"
            + "text-align:center}"
            + "figure a{display:block;padding:0!important;height:auto!important}"
            + "img{max-width:100%!important;height:auto!important;border-radius:14px;display:block;margin:0 auto}"
            + "img.js-lazy-load-image{display:none!important}"
            + "figcaption{color:rgba(255,255,255,.6);font-size:13px;font-style:italic;margin-top:8px}"
            + "ul,ol{padding-left:22px;margin:0 0 14px}li{margin:4px 0}li::marker{color:#E8C468}"
            + "blockquote{margin:16px 0;padding:8px 14px;border-left:3px solid #E8C468;"
            + "background:rgba(255,255,255,.05);border-radius:0 10px 10px 0}"
            + "table{width:100%;border-collapse:collapse;margin:14px 0;font-size:14px;display:block;overflow-x:auto}"
            + "th,td{border:1px solid rgba(255,255,255,.15);padding:6px 8px;text-align:left;vertical-align:top}"
            + "th{background:rgba(25,87,216,.25)}"
            + "hr{border:0;border-top:1px solid rgba(255,255,255,.12);margin:20px 0}";

    static String document(String descriptionHtml) {
        String body = descriptionHtml == null ? "" : descriptionHtml;
        body = SCRIPT.matcher(body).replaceAll("");
        body = STYLE.matcher(body).replaceAll("");
        body = IFRAME.matcher(body).replaceAll("");
        return "<!DOCTYPE html><html><head><meta charset=\"utf-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">"
                + "<style>" + CSS + "</style></head><body>" + body + "</body></html>";
    }

    private HeroHtml() { }
}
