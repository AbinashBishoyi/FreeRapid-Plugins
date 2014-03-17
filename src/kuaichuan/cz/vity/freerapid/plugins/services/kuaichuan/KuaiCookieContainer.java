package cz.vity.freerapid.plugins.services.kuaichuan;

import org.apache.commons.httpclient.Cookie;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: Tommy
 * Date: 12-8-5
 * Time: 下午3:48
 * To change this template use File | Settings | File Templates.
 */
public class KuaiCookieContainer {
    public static final long COOKIE_TIME_OUT = 86400 * 1000;
    private HashMap<String, KuaiCookie> urlCookieMap = new HashMap<String, KuaiCookie>();
    private HashMap<String, Date> urlDateMap = new HashMap<String, Date>();
    private HashMap<String, String> urlRefererMap = new HashMap<String, String>();

    public void pushCookies(String url, Cookie[] cookies, String referer) {
        KuaiCookie kc = new KuaiCookie(cookies);
        urlCookieMap.put(url, kc);
        urlDateMap.put(url, new Date());
        urlRefererMap.put(url, referer);
        cleanTimeoutCookies();
    }

    public Cookie[] getCookies(String url) {
        KuaiCookie kc = urlCookieMap.get(url);
        if (kc == null)
            return null;
        urlDateMap.put(url, new Date());
        return kc.getCookies();
    }

    public Cookie[] popCookies(String url) {
        KuaiCookie kc = urlCookieMap.get(url);
        if (null == kc)
            return null;
        else {
            urlCookieMap.remove(url);
            urlDateMap.remove(url);
            urlRefererMap.remove(url);
        }
        return kc.getCookies();
    }

    public String getLastUrl() {
        Iterator<String> it = urlCookieMap.keySet().iterator();
        if (it.hasNext())
            return it.next();
        else
            return null;
    }

    private void cleanTimeoutCookies() {
        long now = new Date().getTime();
        for (Map.Entry<String, Date> ent : urlDateMap.entrySet()) {
            if (now - ent.getValue().getTime() > COOKIE_TIME_OUT) {
                final String key = ent.getKey();
                urlCookieMap.remove(key);
                urlDateMap.remove(key);
                urlRefererMap.remove(key);
            }
        }
    }

    public class KuaiCookie {
        private Cookie[] cookies;

        public KuaiCookie(Cookie[] cookies1) {
            setCookies(cookies1);
        }

        public void setCookies(Cookie[] cs) {
            this.cookies = cs.clone();
        }

        public Cookie[] getCookies() {
            return this.cookies;
        }
    }
}
