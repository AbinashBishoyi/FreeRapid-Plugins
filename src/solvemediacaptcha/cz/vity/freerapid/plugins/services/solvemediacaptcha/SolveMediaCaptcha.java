package cz.vity.freerapid.plugins.services.solvemediacaptcha;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.hoster.CaptchaSupport;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpDownloadClient;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.httpclient.HttpMethod;

import java.awt.*;
import java.awt.geom.QuadCurve2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class for manipulating SolveMediaCaptcha.<p/>
 * Example Usage:
 * <pre>
 *      SolveMediaCaptcha solveMediaCaptcha = new SolveMediaCaptcha(captchaKey, client, getCaptchaSupport());
 *      solveMediaCaptcha.askForCaptcha();
 *      HttpMethod method = solveMediaCaptcha.modifyResponseMethod(getMethodBuilder()).toHttpMethod();
 *     ...
 * </pre>
 *
 * @author tong2shot
 */
public class SolveMediaCaptcha {
    public final static String THEME_UNDEFINED = "solvemedia_theme_undefined";

    private final static Logger logger = Logger.getLogger(SolveMediaCaptcha.class.getName());
    private final static String SOLVEMEDIA_CAPTCHA_URL = "http://api.solvemedia.com/papi/";
    private final static String SOLVEMEDIA_CAPTCHA_SECURE_URL = "https://api-secure.solvemedia.com/papi/";
    private final static String C_FORMAT = "js,swf11,swf11.2,swf,h5c,h5ct,svg,h5v,v/ogg,v/webm,h5a,a/ogg,ua/firefox,ua/firefox31,os/nt,os/nt6.1,%s,jslib/jquery,jslib/jqueryui";

    private final String publicKey;
    private final HttpDownloadClient client;
    private final CaptchaSupport captchaSupport;
    private final Random random = new Random();
    private boolean secure;
    private String theme;
    private String challenge;
    private String response;

    /**
     * Constructor of SolveMediaCaptcha
     *
     * @param publicKey      Solve media captcha public key
     * @param client         Client to do request with
     * @param captchaSupport Captcha support instance, to show captcha dialog
     * @param secure         Flag to construct solve media captcha base URL, whether uses the secure URL one or the plain URL one
     * @param theme          Theme of solve media captcha, for example : white, red, custom
     * @throws Exception
     */
    public SolveMediaCaptcha(String publicKey, HttpDownloadClient client, CaptchaSupport captchaSupport, boolean secure, String theme) throws Exception {
        this.publicKey = publicKey;
        this.client = client;
        this.captchaSupport = captchaSupport;
        this.secure = secure;
        this.theme = theme;
    }

    public SolveMediaCaptcha(String publicKey, HttpDownloadClient client, CaptchaSupport captchaSupport, boolean secure) throws Exception {
        this(publicKey, client, captchaSupport, secure, THEME_UNDEFINED);
    }

    public SolveMediaCaptcha(String publicKey, HttpDownloadClient client, CaptchaSupport captchaSupport) throws Exception {
        this(publicKey, client, captchaSupport, false);
    }

    /**
     * Show captcha dialog, or solve the captcha if captcha type is cryptic HTML captcha
     *
     * @throws Exception
     */
    public void askForCaptcha() throws Exception {
        final Matcher matcher;
        if (theme.equals(THEME_UNDEFINED)) {
            theme = "white";
            try {
                matcher = Pattern.compile("var ACPuzzleOptions\\s*?=\\s*?\\{.*?[\"']?theme[\"']?\\s*?:\\s*?[\"'](.+?)[\"']", Pattern.DOTALL).matcher(client.getContentAsString());
                if (matcher.find()) {
                    theme = matcher.group(1);
                }
            } catch (Exception e) {
                //
            }
        }

        final String chScriptContent = getChScriptContent(); //challenge.script content
        final Map<String, String> chJsParams = new LinkedHashMap<String, String>();
        constructChJsParams(chJsParams, chScriptContent);
        final String mediaType = getMediaType(chJsParams); //request _challenge.js to get media type
        final String chJsContent = client.getContentAsString(); //_challenge.js content
        challenge = getChallenge(chJsContent);

        final String imgUrl = "media?c=" + challenge + ";w=300;h=150;fg=000000;bg=f8f8f8";
        final HttpMethod httpMethod = getSolveMediaGetMethod(imgUrl);
        try {
            if (mediaType.equals("img") || mediaType.equals("imgmap")) {
                final InputStream stream = client.makeRequestForFile(httpMethod);
                if (stream == null) {
                    throw new FailedToLoadCaptchaPictureException();
                }
                response = captchaSupport.askForCaptcha(captchaSupport.loadCaptcha(stream));
            } else if (mediaType.equals("html")) {
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
                } else if (client.getContentAsString().contains("base64")) {
                    String base64Str;
                    try {
                        base64Str = PlugUtils.getStringBetween(client.getContentAsString().replaceAll("\\s", ""), "base64,", "\"");
                    } catch (PluginImplementationException e) {
                        throw new PluginImplementationException("Base64 of image representation not found");
                    }
                    ByteArrayInputStream bais = new ByteArrayInputStream(Base64.decodeBase64(base64Str));
                    response = captchaSupport.askForCaptcha(captchaSupport.loadCaptcha(bais));
                } else { // canvas HTML captcha
                    response = captchaSupport.askForCaptcha(drawHTMLCaptcha(client.getContentAsString(), 300, 150, Color.blue));
                }
            } else {
                throw new ServiceConnectionProblemException("Captcha media type 'img', 'imgmap', or 'html' not found");
            }
            if (response == null) {
                throw new CaptchaEntryInputMismatchException("No Input");
            }
        } finally {
            httpMethod.abort();
            httpMethod.releaseConnection();
        }
    }

    /**
     * Modifies the method of sending form with captcha parameters
     *
     * @param methodBuilder MethodBuilder to modify
     * @return Modified MethodBuilder
     */
    public MethodBuilder modifyResponseMethod(MethodBuilder methodBuilder) {
        return methodBuilder.setParameter("adcopy_challenge", challenge).setParameter("adcopy_response", response);
    }

    /**
     * Get challenge.script content
     *
     * @return challenge.script content
     * @throws BuildMethodException
     * @throws IOException
     */
    private String getChScriptContent() throws BuildMethodException, IOException {
        //challange.script can be challange.ajax
        final HttpMethod httpMethod = getSolveMediaGetMethod("challenge.script?k=" + publicKey);
        client.makeRequest(httpMethod, true);
        return client.getContentAsString();
    }

    /**
     * Construct initial _challenge.js parameters
     *
     * @param chJsParams      Data structure to store _challenge.js parameters
     * @param chScriptContent challenge.script content
     * @throws IOException
     * @throws ErrorDuringDownloadingException
     */
    private void constructChJsParams(Map<String, String> chJsParams, String chScriptContent) throws IOException, ErrorDuringDownloadingException {
        final String magic = findString("magic:\\s*'(.+?)',", "Magic", chScriptContent);
        final String chalApi = findString("chalapi:\\s*'(.+?)',", "Challenge API", chScriptContent);
        final String chalStamp = findString("chalstamp:\\s*(\\d+),", "Challenge Stamp", chScriptContent);
        final String size = findString("size:\\s*'(.+?)',", "Size", chScriptContent);

        /* The actual method is requesting _puzzle.js to construct fwv and ts params, but we construct them manually instead.
        httpMethod = getSolveMediaGetMethod(baseUrl + "/_puzzle.js");
        client.makeRequest(httpMethod, true);

        final String puzzleJsContent = client.getContentAsString();
        */
        chJsParams.put("k", publicKey);
        chJsParams.put("f", "_ACPuzzleUtil.callbacks%5B0%5D");
        chJsParams.put("l", "en");
        chJsParams.put("t", "img");
        chJsParams.put("s", size);
        chJsParams.put("c", String.format(C_FORMAT, getFwv()));
        chJsParams.put("am", magic);
        chJsParams.put("ca", chalApi);
        //chJsParams.put("ts", findString("ts=(\\d+)'", "ts", puzzleJsContent));
        chJsParams.put("ts", String.valueOf(System.currentTimeMillis() / 1000));
        chJsParams.put("ct", chalStamp);
        chJsParams.put("th", theme);
        chJsParams.put("r", String.valueOf(Math.random()));
    }

    /**
     * Construct string version of _challenge.js parameters.
     *
     * @param chJsParams _challenge.js parameters
     * @return String version of _challenge.js parameters
     */
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

    /**
     * Get media type by requesting _challenge.js
     *
     * @param chJsParams _challenge.js parameters
     * @return Media type
     * @throws IOException
     * @throws PluginImplementationException
     */
    private String getMediaType(Map<String, String> chJsParams) throws IOException, PluginImplementationException {
        HttpMethod httpMethod;
        Matcher matcher;
        String mediaType;
        int mediaTypeCounter = 0;
        do {
            httpMethod = getSolveMediaGetMethod("_challenge.js?" + chJsParamsToString(chJsParams));
            this.client.makeRequest(httpMethod, true);
            matcher = PlugUtils.matcher("\"mediatype\"\\s*:\\s*\"(.+?)\",", this.client.getContentAsString());
            if (!matcher.find()) {
                logger.warning(this.client.getContentAsString());
                throw new PluginImplementationException("Captcha media type not found");
            }
            mediaType = matcher.group(1);
            logger.info("ATTEMPT " + mediaTypeCounter + ", mediaType = " + mediaType);

            chJsParams.put("c", String.format(C_FORMAT, getFwv()));
            chJsParams.put("r", String.valueOf(Math.random()));
        }
        while (!mediaType.equals("img") && !mediaType.equals("html") && !mediaType.equals("imgmap") && (mediaTypeCounter++ < 10)); //anticipate mediaType!=(img|html|imgmap)
        return mediaType;
    }

    /**
     * Get challenge from _challenge.js content
     *
     * @param chJsContent _challenge.js content
     * @return challenge
     * @throws PluginImplementationException
     */
    private String getChallenge(String chJsContent) throws PluginImplementationException {
        final Matcher matcher = PlugUtils.matcher("\"chid\"\\s*:\\s*\"(.+?)\",", chJsContent);
        if (!matcher.find()) {
            throw new PluginImplementationException("Captcha challenge ID not found");
        }
        return matcher.group(1);
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

    /**
     * Get HttpMethod with default header
     *
     * @param action Request URL minus base URL. Base URL is set according to secure flag.
     * @return HttpMethod with default header
     * @throws BuildMethodException
     */
    private HttpMethod getSolveMediaGetMethod(String action) throws BuildMethodException {
        final String baseUrl = secure ? SOLVEMEDIA_CAPTCHA_SECURE_URL : SOLVEMEDIA_CAPTCHA_URL;
        final HttpMethod method = new MethodBuilder(client)
                .setBaseURL(baseUrl)
                .setAction(action)
                .toGetMethod();
        method.setRequestHeader("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:31.0) Gecko/20100101 Firefox/31.0");
        method.setRequestHeader("Accept", "*/*");
        method.setRequestHeader("Accept-Language", "en-US,en;q=0.5");
        method.setRequestHeader("Connection", "keep-alive");
        method.removeRequestHeader("Accept-Charset");
        method.removeRequestHeader("Keep-Alive");
        method.removeRequestHeader("Referer");
        return method;
    }

    public void setSecure(boolean secure) {
        this.secure = secure;
    }

    public void setTheme(String theme) {
        this.theme = theme;
    }

    public String getChallenge() {
        return challenge;
    }

    public String getResponse() {
        return response;
    }

}