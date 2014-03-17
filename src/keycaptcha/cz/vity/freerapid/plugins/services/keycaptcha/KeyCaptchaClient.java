package cz.vity.freerapid.plugins.services.keycaptcha;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpDownloadClient;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import cz.vity.freerapid.utilities.LogUtils;
import org.apache.commons.httpclient.HttpMethod;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * @author ntoskrnl
 * @author tong2shot
 */
class KeyCaptchaClient {

    private final static Logger logger = Logger.getLogger(KeyCaptchaClient.class.getName());

    private final HttpDownloadClient client;
    private final Map<String, String> parameters = new HashMap<String, String>();
    private MethodBuilder responseMethodBuilder;
    private String responseUrl;
    private String gjsContent;

    public KeyCaptchaClient(final HttpDownloadClient client) {
        this.client = client;
    }

    /**
     * Populate parameters and images
     *
     * @param pageContent Main page content
     * @param pageUrl     Main page URL
     * @return Populated images
     * @throws Exception
     */
    public KeyCaptchaImages getImages(final String pageContent, final String pageUrl) throws Exception {
        try {
            final String formContent = findFormContent(pageContent);
            loadPagesAndPopulateParameters(formContent, pageUrl);

            //image url locations are in gjs
            final KeyCaptchaImages images = new KeyCaptchaImages();
            Matcher matcher = PlugUtils.matcher("\\('([^']+?)','([^']+?)',[^,\\)]+?,false\\)", gjsContent);
            if (!matcher.find()) {
                throw new PluginImplementationException("KeyCaptcha image data not found");
            }
            getBackgroundImage(images, matcher.group(1), matcher.group(2));

            matcher = PlugUtils.matcher("\\('([^']+?)','([^']+?)',[^,\\)]+?,true\\)", gjsContent);
            if (!matcher.find()) {
                throw new PluginImplementationException("KeyCaptcha image data not found");
            }
            getForegroundImages(images, matcher.group(1), matcher.group(2));
            return images;
        } catch (final PluginImplementationException e) {
            logger.warning("Content from last request:\n" + client.getContentAsString());
            throw e;
        }
    }

    public HttpMethod getResult(final List<Point> pieceLocations, final List<Point> mouseLocations) throws Exception {
        try {
            final String webServerSignParam = parameters.get("s_s_c_web_server_sign");
            final String mmUrl = responseUrl + "/swfs/mm?pS="
                    + KeyCaptchaCrypt.encrypt(webServerSignParam, webServerSignParam + "Khd21M47")
                    + "&cP=" + sscGetParams("")
                    + "&mms=" + Math.random()
                    + "&r=" + Math.random();
            logger.info("mmUrl : " + mmUrl);
            client.makeRequest(client.getGetMethod(mmUrl), true);
            Matcher matcher = PlugUtils.matcher("\\|([A-Za-z0-9]+)'\\.split", client.getContentAsString());
            if (!matcher.find()) {
                throw new PluginImplementationException("Error parsing KeyCaptcha server response (1)");
            }

            parameters.put("extra", "2813d567024c09716fa4b1244a98b7c7");
            final String cjsUrl = responseUrl + "/swfs/cjs?pS=123&cOut="
                    + KeyCaptchaCrypt.encrypt(createResponse(pieceLocations), matcher.group(1))
                    + "..." + createResponse(mouseLocations)
                    + "&cP=" + sscGetParams("");
            logger.info("cjsUrl : " + cjsUrl);
            client.makeRequest(client.getGetMethod(cjsUrl), true);
            matcher = PlugUtils.matcher("s_s_c_setcapvalue\\(\\s*\"(.+?)\"", client.getContentAsString());
            if (!matcher.find()) {
                throw new PluginImplementationException("Error parsing KeyCaptcha server response (2)");
            }
            final String result = matcher.group(1);
            logger.info("result : " + result);
            checkResponse(result);
            return responseMethodBuilder.setParameter("capcode", result).toPostMethod();
        } catch (final PluginImplementationException e) {
            logger.warning("Content from last request:\n" + client.getContentAsString());
            throw e;
        }
    }

    private String createResponse(final List<Point> points) {
        final StringBuilder sb = new StringBuilder();
        for (final Point point : points) {
            sb.append(point.x).append('.').append(point.y).append('.');
        }
        sb.setLength(sb.length() - 1);
        return sb.toString();
    }

    private void checkResponse(final String result) {
        final String[] split = result.split("\\|");
        if (split.length != 5) {
            logger.warning("Unable to parse captcha response: " + result);
        } else {
            if ("1".equals(split[4])) {
                logger.info("Correct captcha answer");
            } else {
                logger.info("Incorrect captcha answer");
            }
        }
    }

    /**
     * Get main page KeyCaptcha form content
     *
     * @param pageContent Main page content
     * @return KeyCaptcha form content
     * @throws ErrorDuringDownloadingException
     *
     */
    private String findFormContent(final String pageContent) throws ErrorDuringDownloadingException {
        final Matcher matcher = PlugUtils.matcher("(?s)(<form\\b.+?</form>)", pageContent);
        while (matcher.find()) {
            final String formContent = matcher.group(1);
            if (formContent.contains("var s_s_c_")) {
                return formContent;
            }
        }
        throw new PluginImplementationException("KeyCaptcha form not found");
    }

    private void loadPagesAndPopulateParameters(final String formContent, final String pageUrl) throws Exception {
        findParameters(formContent);
        parameters.put("pageUrl", pageUrl.split("#")[0]);

        //capJs (cap.js)
        final String capJsUrl = findString("<script [^<>]+?src=['\"](.+?)['\"]", "script 1 URL", formContent);
        logger.info("CapJsUrl : " + capJsUrl);
        client.makeRequest(client.getGetMethod(capJsUrl), true);
        final String capsJsUrl = findString("var _13=\"(.+?)\"", "script 2 URL", client.getContentAsString())
                + parameters.get("s_s_c_user_id")
                + "&u=" + URLEncoder.encode(parameters.get("pageUrl"), "UTF-8")
                + "&r=" + Math.random();

        //capsJs (caps.js)
        logger.info("CapsJsUrl : " + capsJsUrl);
        client.makeRequest(client.getGetMethod(capsJsUrl), true);
        String s_s_c_web_server_sign4 = findString("s_s_c_web_server_sign4=\"(.+?)\";", "parameter 4", client.getContentAsString());
        s_s_c_web_server_sign4 = s_s_c_web_server_sign4.substring(0, 10) + "690" + s_s_c_web_server_sign4.substring(10);
        parameters.put("s_s_c_web_server_sign4", s_s_c_web_server_sign4);
        parameters.put("extra",
                findString("s_s_c_check_process\\)\\{\\s*return \"(.+?)\"", "extra parameter", client.getContentAsString()));
        final String extra2 = findString("dn3iufwwoi4=\"(.+?)\"", "js3 extra2 parameter", client.getContentAsString());
        final String gjsUrl = findString("\"src\",\"(.+?)\"", "script 3 URL", client.getContentAsString())
                + sscGetParams("|" + extra2) + "&r=" + Math.random();

        //gjs
        logger.info("GjsUrl : " + gjsUrl);
        client.makeRequest(client.getGetMethod(gjsUrl), true);
        gjsContent = client.getContentAsString();
        parameters.put("s_s_c_web_server_sign3",
                findString("s_s_c_setnewws\\(\"(.+?)\"", "parameter 3", gjsContent));
        responseUrl = findString("setAttribute\\('src','(.+?)'", "response URL", gjsContent);
        responseUrl = responseUrl.substring(0, responseUrl.indexOf('/', "https://".length()));
        logger.info("responseUrl : " + responseUrl);

        //main page KeyCaptcha form methodBuilder
        responseMethodBuilder = new MethodBuilder(formContent, client)
                .setReferer(pageUrl)
                .setBaseURL(pageUrl.substring(0, pageUrl.indexOf('/', "https://".length()) + 1))
                .setActionFromFormByIndex(1, true);
        if (responseMethodBuilder.getAction() == null) {
            responseMethodBuilder.setAction(pageUrl);
        }
    }

    private void findParameters(final String formContent) throws ErrorDuringDownloadingException {
        parameters.clear();
        final Matcher matcher = PlugUtils.matcher("var (s_s_c_.+?) = '(.+?)';", formContent);
        while (matcher.find()) {
            parameters.put(matcher.group(1), matcher.group(2));
        }
        if (parameters.isEmpty()) {
            throw new PluginImplementationException("KeyCaptcha parameters not found");
        }
    }

    private String findString(final String regexp, final String name, final String content) throws ErrorDuringDownloadingException {
        final Matcher matcher = PlugUtils.matcher(regexp, content);
        if (!matcher.find()) {
            throw new PluginImplementationException("KeyCaptcha " + name + " not found");
        }
        return matcher.group(1);
    }

    private String sscGetParams(final String extra) throws Exception {
        return URLEncoder.encode(
                getParam("s_s_c_user_id") + '|'
                        + getParam("pageUrl") + '|'
                        + getParam("s_s_c_session_id") + '|'
                        + getParam("s_s_c_captcha_field_id") + '|'
                        + getParam("s_s_c_submit_button_id") + '|'
                        + getParam("s_s_c_web_server_sign") + '|'
                        + getParam("s_s_c_web_server_sign2") + '|'
                        + getParam("s_s_c_web_server_sign3") + '|'
                        + getParam("s_s_c_web_server_sign4")
                        + "|1|" + getParam("extra") + extra
                , "UTF-8");
    }

    private String getParam(final String name) {
        final String value = parameters.get(name);
        if (value != null) {
            return value;
        } else {
            return "";
        }
    }

    private void getBackgroundImage(final KeyCaptchaImages images, final String data, final String url) throws Exception {
        final BufferedImage slices = getImage(url);
        try {
            final String[] coordinates = KeyCaptchaCrypt.decrypt(data, url.substring(url.length() - 33, url.length() - 6)).split(",");
            final BufferedImage image = new BufferedImage(450, 160, BufferedImage.TYPE_INT_RGB);
            final Graphics2D g = image.createGraphics();
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, image.getWidth(), image.getHeight());
            for (int i = 0, currentX = 0; i < coordinates.length; i += 4) {
                final int destinationX = Integer.parseInt(coordinates[i]);
                final int destinationY = Integer.parseInt(coordinates[i + 1]);
                final int width = Integer.parseInt(coordinates[i + 2]);
                final int height = Integer.parseInt(coordinates[i + 3]);
                g.drawImage(slices, destinationX, destinationY, destinationX + width, destinationY + height, currentX, 0, currentX + width, height, null);
                currentX += width;
            }
            g.dispose();
            images.setBackground(image);
        } catch (final Exception e) {
            logger.warning("data = " + data);
            logger.warning("url = " + url);
            throw new PluginImplementationException("Failed to construct KeyCaptcha image", e);
        }
    }

    private void getForegroundImages(final KeyCaptchaImages images, final String data, final String url) throws Exception {
        final BufferedImage slices = getImage(url);
        try {
            final String decryptedData = KeyCaptchaCrypt.decrypt(data, url.substring(url.length() - 33, url.length() - 6));
            final List<BufferedImage> list = new LinkedList<BufferedImage>();
            for (final String s : decryptedData.split(";")) {
                final String[] split = s.split(":");
                final String name = split[0];
                final String[] coordinates = split[1].split(",");
                final int width = Integer.parseInt(coordinates[1]) + Integer.parseInt(coordinates[5]) + Integer.parseInt(coordinates[9]);
                final int height = Integer.parseInt(coordinates[3]) + Integer.parseInt(coordinates[15]) + Integer.parseInt(coordinates[27]);
                final BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                final Graphics2D g = image.createGraphics();
                g.setColor(new Color(255, 255, 255, 0));
                g.fillRect(0, 0, image.getWidth(), image.getHeight());
                for (int i = 0, dx = 0, dy = 0; i < 36; i += 4) {
                    final int sx = Integer.parseInt(coordinates[i]);
                    final int sy = Integer.parseInt(coordinates[i + 2]);
                    final int w = Integer.parseInt(coordinates[i + 1]);
                    final int h = Integer.parseInt(coordinates[i + 3]);
                    g.drawImage(slices, dx, dy, dx + w, dy + h, sx, sy, sx + w, sy + h, null);
                    dx += w;
                    if (dx >= width) {
                        dy += h;
                        dx = 0;
                    }
                }
                g.dispose();
                if (!"kc_sample_image".equals(name)) {
                    list.add(image);
                } else {
                    images.setSample(image);
                }
            }
            images.setPieces(list);
        } catch (final Exception e) {
            logger.warning("data = " + data);
            logger.warning("url = " + url);
            throw new PluginImplementationException("Failed to construct KeyCaptcha image", e);
        }
    }

    private BufferedImage getImage(final String url) throws IOException {
        final InputStream is = client.makeRequestForFile(client.getGetMethod(url));
        if (is != null) {
            try {
                return ImageIO.read(is);
            } finally {
                try {
                    is.close();
                } catch (final Exception e) {
                    LogUtils.processException(logger, e);
                }
            }
        } else {
            throw new IOException("Failed to load captcha image");
        }
    }

}
