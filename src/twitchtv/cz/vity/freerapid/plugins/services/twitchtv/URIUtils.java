package cz.vity.freerapid.plugins.services.twitchtv;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

/**
 * @author tong2shot
 */
public final class URIUtils {

    private URIUtils(){
    }

    public static Map<String, String> getQuery(String uri) throws UnsupportedEncodingException {
        final Map<String, String> params = new HashMap<String, String>();
        final String query = org.apache.commons.httpclient.util.URIUtil.getQuery(uri);
        for (String param : query.split("&")) {
            String pair[] = param.split("=");
            String name = URLDecoder.decode(pair[0], "UTF-8");
            String value = "";
            if (pair.length > 1) {
                value = URLDecoder.decode(pair[1], "UTF-8");
            }
            params.put(name, value);
        }
        return params;
    }
}
