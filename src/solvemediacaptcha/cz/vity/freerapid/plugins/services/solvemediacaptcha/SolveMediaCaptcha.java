package cz.vity.freerapid.plugins.services.solvemediacaptcha;

import cz.vity.freerapid.plugins.exceptions.CaptchaEntryInputMismatchException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.hoster.CaptchaSupport;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpDownloadClient;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;

import java.awt.*;
import java.awt.geom.QuadCurve2D;
import java.awt.image.BufferedImage;
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

    private String challenge;
    private String response;
    private HttpDownloadClient client;
    private CaptchaSupport captchaSupport;
    private String mediaType;

    public SolveMediaCaptcha(String publicKey, HttpDownloadClient client, CaptchaSupport captchaSupport) throws Exception {
        this.client = client;
        this.captchaSupport = captchaSupport;
        HttpMethod httpMethod = new MethodBuilder(client)
                .setAction(SOLVEMEDIA_CAPTCHA_URL + "/challenge.script?k=" + publicKey)
                .toGetMethod();
        client.makeRequest(httpMethod, true);

        int mediaTypeCounter = 0;
        Matcher matcher;
        do {
            final StringBuilder fwv = new StringBuilder(18);
            final Random random = new Random();
            fwv.append("fwv/");
            for (int i = 0; i < 6; i++) {
                fwv.append(Character.toChars(Math.random() > 0.2 ? random.nextInt(26) + 65 : random.nextInt(26) + 97));
            }
            fwv.append(".");
            for (int i = 0; i < 4; i++) {
                fwv.append(Character.toChars(random.nextInt(26) + 97));
            }
            fwv.append(random.nextInt(80) + 10);
            final String captchaAction = SOLVEMEDIA_CAPTCHA_URL + "/_challenge.js" + "?k=" + publicKey + ";f=_ACPuzzleUtil.callbacks%5B0%5D;l=en;t=img;s=standard;c=js,h5c,h5ct,svg,h5v,v/ogg,v/webm,h5a,a/ogg,ua/opera,ua/opera11,os/nt,os/nt6.1," +
                    fwv + ",jslib/jquery;ts=" + Long.toString(System.currentTimeMillis()).substring(0, 10) + ";th=white;r=" + Math.random();
            httpMethod = new MethodBuilder(client)
                    .setAction(captchaAction)
                    .toGetMethod();
            client.makeRequest(httpMethod, true);

            matcher = PlugUtils.matcher("\"mediatype\"\\s*:\\s*\"(.+?)\",", client.getContentAsString());
            if (!matcher.find()) {
                throw new PluginImplementationException("Captcha media type not found");
            }
            mediaType = matcher.group(1);
            logger.info("ATTEMPT " + mediaTypeCounter + ", mediaType = " + mediaType);
        }
        while (!mediaType.equals("img") && !mediaType.equals("html") && (mediaTypeCounter++ < 10)); //anticipate mediaType!=(img|html)

        matcher = PlugUtils.matcher("\"chid\"\\s*:\\s*\"(.+?)\",", client.getContentAsString());
        if (!matcher.find()) throw new PluginImplementationException("Captcha challenge ID not found");
        challenge = matcher.group(1);
        response = askForCaptcha();
    }

    private String askForCaptcha() throws Exception {
        final String imgUrl = SOLVEMEDIA_CAPTCHA_URL + "/media?c=" + challenge + ";w=300;h=150;fg=000000;bg=f8f8f8";
        if (mediaType.equals("img")) {
            response = captchaSupport.getCaptcha(imgUrl);
            if (response == null) throw new CaptchaEntryInputMismatchException("No Input");
        } else if (mediaType.equals("html")) {
            final HttpMethod httpMethod = new MethodBuilder(client)
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
                if (response == null) throw new CaptchaEntryInputMismatchException("No Input");
            }
        } else {
            throw new ServiceConnectionProblemException("Captcha media type 'img' or 'html' not found");
        }
        return response;
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

    public String getChallenge() {
        return challenge;
    }

    public String getResponse() {
        return response;
    }

}