package cz.vity.freerapid.plugins.services.solvemediacaptcha;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.hoster.CaptchaSupport;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpDownloadClient;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;

import java.awt.*;
import java.awt.geom.QuadCurve2D;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author tong2shot
 */
public class SolveMediaCaptcha {
    private final static Logger logger = Logger.getLogger(SolveMediaCaptcha.class.getName());
    private final static String SOLVEMEDIA_CAPTCHA_URL = "http://api.solvemedia.com/papi";
    private final static String SOLVEMEDIA_CAPTCHA_SECURE_URL = "https://api-secure.solvemedia.com/papi";

    private final HttpDownloadClient client;
    private final CaptchaSupport captchaSupport;
    private final String baseUrl;
    private final Random random = new Random();
    private String challenge;
    private String response;
    private String mediaType;

    public SolveMediaCaptcha(String publicKey, HttpDownloadClient client, CaptchaSupport captchaSupport, boolean secure) throws Exception {
        this.client = client;
        this.captchaSupport = captchaSupport;
        baseUrl = secure ? SOLVEMEDIA_CAPTCHA_SECURE_URL : SOLVEMEDIA_CAPTCHA_URL;

        //challange.script can be challange.ajax
        HttpMethod httpMethod = new MethodBuilder(client)
                .setAction(baseUrl + "/challenge.script?k=" + publicKey)
                .toGetMethod();
        setDefaultsForMethod(httpMethod);
        client.makeRequest(httpMethod, true);

        final String magic = findString("magic:\\s*'(.+?)',", "Magic", client.getContentAsString());
        final String chalApi = findString("chalapi:\\s*'(.+?)',", "Challange API", client.getContentAsString());
        final String chalStamp = findString("chalstamp:\\s*(\\d+),", "Challange Stamp", client.getContentAsString());
        final String size = findString("size:\\s*'(.+?)',", "Size", client.getContentAsString());

        /* The actual method is requesting _puzzle.js to construct fwv and ts params, but we construct them manually instead.
        httpMethod = new MethodBuilder(client)
                .setAction(baseUrl + "/_puzzle.js")
                .toGetMethod();
        setDefaultsForMethod(httpMethod);
        client.makeRequest(httpMethod, true);

        final String puzzleJsContent = client.getContentAsString();
        */
        final Map<String, String> chJsParams = new LinkedHashMap<String, String>(); //_challenge.js params, retain elements order
        final String cFormat = "js,swf11,swf11.6,swf,h5c,h5ct,svg,h5v,v/ogg,v/webm,h5a,a/ogg,ua/firefox,ua/firefox17,os/nt,os/nt5.1,%s,jslib/jquery";
        chJsParams.put("k", publicKey);
        chJsParams.put("f", "_ACPuzzleUtil.callbacks%5B0%5D");
        chJsParams.put("l", "en");
        chJsParams.put("t", "img");
        chJsParams.put("s", size);
        chJsParams.put("c", String.format(cFormat, getFwv()));
        chJsParams.put("am", magic);
        chJsParams.put("ca", chalApi);
        //chJsParams.put("ts", findString("ts=(\\d+)'", "ts", puzzleJsContent));
        chJsParams.put("ts", String.valueOf(System.currentTimeMillis() / 1000));
        chJsParams.put("ct", chalStamp);
        chJsParams.put("th", "red");
        chJsParams.put("r", String.valueOf(Math.random()));
        Matcher matcher;
        int mediaTypeCounter = 0;
        do {
            httpMethod = new MethodBuilder(client)
                    .setAction(baseUrl + "/_challenge.js?" + chJsParamsToString(chJsParams))
                    .toGetMethod();
            setDefaultsForMethod(httpMethod);
            client.makeRequest(httpMethod, true);
            matcher = PlugUtils.matcher("\"mediatype\"\\s*:\\s*\"(.+?)\",", client.getContentAsString());
            if (!matcher.find()) {
                logger.warning(client.getContentAsString());
                throw new PluginImplementationException("Captcha media type not found");
            }
            mediaType = matcher.group(1);
            logger.info("ATTEMPT " + mediaTypeCounter + ", mediaType = " + mediaType);

            chJsParams.put("c", String.format(cFormat, getFwv()));
            chJsParams.put("r", String.valueOf(Math.random()));
        }
        while (!mediaType.equals("img") && !mediaType.equals("html") && !mediaType.equals("imgmap") && (mediaTypeCounter++ < 10)); //anticipate mediaType!=(img|html)

        matcher = PlugUtils.matcher("\"chid\"\\s*:\\s*\"(.+?)\",", client.getContentAsString());
        if (!matcher.find()) {
            throw new PluginImplementationException("Captcha challenge ID not found");
        }
        challenge = matcher.group(1);
        askForCaptcha();
    }

    public SolveMediaCaptcha(String publicKey, HttpDownloadClient client, CaptchaSupport captchaSupport) throws Exception {
        this(publicKey, client, captchaSupport, false);
    }

    private String chJsParamsToString(Map<String, String> chJsParams) {
        final StringBuilder chJsParamsBuilder = new StringBuilder();
        for (Map.Entry entry : chJsParams.entrySet()) {
            chJsParamsBuilder.append(entry.getKey());
            chJsParamsBuilder.append("=");
            chJsParamsBuilder.append(entry.getValue());
            chJsParamsBuilder.append(";");
        }
        chJsParamsBuilder.deleteCharAt(chJsParamsBuilder.length() - 1);  //remove last ";"
        return chJsParamsBuilder.toString();
    }

    private String getFwv() {
        final StringBuilder fwv = new StringBuilder(18);
        fwv.append("fwv/");
        for (int i = 0; i < 6; i++) {
            fwv.append(Character.toChars(Math.random() > 0.2 ? random.nextInt(26) + 65 : random.nextInt(26) + 97));
        }
        //fwv.append(findString("fwv/(.+?)'", "FWV", content));
        fwv.append(".");
        for (int i = 0; i < 4; i++) {
            fwv.append(Character.toChars(random.nextInt(26) + 97));
        }
        fwv.append(random.nextInt(80) + 10);
        return fwv.toString();
    }

    private void askForCaptcha() throws Exception {
        final String imgUrl = baseUrl + "/media?c=" + challenge + ";w=300;h=150;fg=000000;bg=f8f8f8";
        HttpMethod httpMethod = new MethodBuilder(client)
                .setAction(imgUrl)
                .toGetMethod();
        setDefaultsForMethod(httpMethod);
        try {
            final InputStream stream = client.makeRequestForFile(httpMethod);
            if (stream == null) {
                throw new FailedToLoadCaptchaPictureException();
            }
            if (mediaType.equals("img") || mediaType.equals("imgmap")) {
                response = captchaSupport.askForCaptcha(captchaSupport.loadCaptcha(stream));
                if (response == null) {
                    throw new CaptchaEntryInputMismatchException("No Input");
                }
            } else if (mediaType.equals("html")) {
                httpMethod = new MethodBuilder(client)
                        .setAction(imgUrl)
                        .toGetMethod();
                client.makeRequest(httpMethod, true);
                logger.info(client.getContentAsString());
                if (client.getContentAsString().contains("var slog =")) { // cryptic HTML captcha + recognizer
                    final String slog = PlugUtils.unescapeUnicode(PlugUtils.getStringBetween(client.getContentAsString(), "var slog = '", "';"));
                    final String secr = PlugUtils.unescapeUnicode(PlugUtils.getStringBetween(client.getContentAsString(), "var secr = '", "';"));
                    int cn = 0;
                    char[] captchaResponse = new char[slog.length()];
                    for (int i = 0; i < slog.length(); i++) {
                        char x = (char) ((secr.charAt(i) ^ (cn | 1) ^ (((cn++ & 1) != 0) ? i : 0) ^ 0x55) ^ (slog.charAt(i) ^ (cn | 1)
                                ^ (((cn++ & 1) != 0) ? i : 0) ^ 0x55));
                        captchaResponse[i] = x;
                    }
                    response = new String(captchaResponse);
                } else { // canvas HTML captcha
                    response = captchaSupport.askForCaptcha(drawHTMLCaptcha(client.getContentAsString(), 300, 150, Color.blue));
                    if (response == null) {
                        throw new CaptchaEntryInputMismatchException("No Input");
                    }
                }
            } else {
                throw new ServiceConnectionProblemException("Captcha media type 'img' or 'html' not found");
            }
        } finally {
            httpMethod.abort();
            httpMethod.releaseConnection();
        }
    }

    public MethodBuilder modifyResponseMethod(MethodBuilder methodBuilder) {
        return methodBuilder.setParameter("adcopy_challenge", challenge).setParameter("adcopy_response", response);
    }

    private BufferedImage drawHTMLCaptcha(final String content, final int width, final int height, final Color color) {
        final BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        final Graphics2D g = (Graphics2D) image.getGraphics();
        g.setColor(Color.white);
        g.drawRect(1, 1, width, height);
        g.fillRect(1, 1, width, height);
        g.setStroke(new BasicStroke(3));
        g.setColor(color);
        final Matcher matcher = PlugUtils.matcher("CM\\((\\d{1,3}),(\\d{1,3})\\);(.*?)CS\\(\\);\\s", content);
        while (matcher.find()) {
            int prevX, prevY;
            prevX = Integer.parseInt(matcher.group(1));
            prevY = Integer.parseInt(matcher.group(2));
            final Matcher matcher2 = PlugUtils.matcher("CL\\((\\d{1,3}),(\\d{1,3})\\);|CQ\\((\\d{1,3}),(\\d{1,3}),(\\d{1,3}),(\\d{1,3})\\);", matcher.group(3));
            while (matcher2.find()) {
                if (matcher2.group(0).contains("CL")) { //draw line
                    g.drawLine(prevX, prevY, Integer.parseInt(matcher2.group(1)), Integer.parseInt(matcher2.group(2)));
                    prevX = Integer.parseInt(matcher2.group(1));
                    prevY = Integer.parseInt(matcher2.group(2));
                } else if (matcher2.group(0).contains("CQ")) { //draw quadratic curve
                    final QuadCurve2D q = new QuadCurve2D.Float();
                    q.setCurve(prevX, prevY, Integer.parseInt(matcher2.group(3)), Integer.parseInt(matcher2.group(4)), Integer.parseInt(matcher2.group(5)), Integer.parseInt(matcher2.group(6)));
                    g.draw(q);
                    prevX = Integer.parseInt(matcher2.group(5));
                    prevY = Integer.parseInt(matcher2.group(6));
                }
            }
        }
        g.dispose();
        return image;
    }

    private String findString(final String regexp, final String name, final String content) throws ErrorDuringDownloadingException {
        final Matcher matcher = PlugUtils.matcher(regexp, content);
        if (!matcher.find()) {
            throw new PluginImplementationException("SolveMediaCaptcha " + name + " not found");
        }
        return matcher.group(1);
    }

    private void setDefaultsForMethod(HttpMethod method) {
        method.setRequestHeader("User-Agent", "Mozilla/5.0 (Windows NT 5.1; rv:17.0) Gecko/20100101 Firefox/17.0");
        method.setRequestHeader("Accept", "*/*");
        method.setRequestHeader("Accept-Language", "en-US,en;q=0.5");
        method.setRequestHeader("Connection", "keep-alive");
        method.removeRequestHeader("Accept-Charset");
        method.removeRequestHeader("Keep-Alive");
        method.removeRequestHeader("Referer");
    }

    public String getChallenge() {
        return challenge;
    }

    public String getResponse() {
        return response;
    }

}