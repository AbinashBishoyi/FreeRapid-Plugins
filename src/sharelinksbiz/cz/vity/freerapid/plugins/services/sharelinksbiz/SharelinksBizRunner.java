package cz.vity.freerapid.plugins.services.sharelinksbiz;

import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.interfaces.ConfigurationStorageSupport;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import cz.vity.freerapid.utilities.LogUtils;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * @author RickCL
 */
class SharelinksBizRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(SharelinksBizRunner.class.getName());

    private List<URI> queye = new LinkedList<URI>();

    private static final String HTTP_BASE = "http://share-links.biz";
    private static final String HTTP_ENGINE = HTTP_BASE + "/get/lnk/";

    private static final String FILE_COOKIE_NAME = "plugin_SharelinksBiz_cookie.xml";

    @Override
    public void run() throws Exception {
        super.run();

        ConfigurationStorageSupport storage = getPluginService().getPluginContext().getConfigurationStorageSupport();
        if (storage.configFileExists(FILE_COOKIE_NAME)) {
            Cookie[] cookies = storage.loadConfigFromFile(FILE_COOKIE_NAME, Cookie[].class);
            client.getHTTPClient().getState().addCookies(cookies);
        }

        addCookie(new Cookie(".share-links.biz", "SLlng", "en", "/", 86400, false));

        Matcher matcher;
        do {
            final GetMethod gMethod = getGetMethod(fileURL);
            updateHeader(gMethod);
            if (!makeRedirectedRequest(gMethod)) {
                throw new ServiceConnectionProblemException();
            }
            if (getContentAsString().contains("HTTP-EQUIV=\"refresh\""))
                fileURL = HTTP_BASE + PlugUtils.getStringBetween(getContentAsString(), "URL=", "\"");
        } while (getContentAsString().contains("HTTP-EQUIV=\"refresh\""));

        if (getContentAsString().contains("captcha.gif")) {
            GetMethod captchaMethod;
            boolean captchaFound = false;

            Matcher captchaMatcherImages = getMatcherAgainstContent("/captcha.gif[^\"]*");
            Matcher captchaMatcherOptions = getMatcherAgainstContent("<area\\sshape.*href=\"([^\"]*)\"");

            while (captchaMatcherImages.find()) {
                client.setReferer(fileURL);
                String captchaUrl = HTTP_BASE + captchaMatcherImages.group(0).replaceAll("&amp;", "&");
                logger.info("Reading Captcha Images " + captchaUrl);
                getCaptchaSupport().getCaptchaImage(captchaUrl);
            }

            while (!captchaFound && captchaMatcherOptions.find()) {
                client.setReferer(fileURL);
                captchaMethod = getGetMethod(HTTP_BASE + captchaMatcherOptions.group(1));
                logger.info("Try Captcha " + captchaMethod.getURI().toString());
                updateHeader(captchaMethod);
                makeRedirectedRequest(captchaMethod);
                if (!getContentAsString().contains("Your choice was wrong") &&
                        !getContentAsString().contains("Try again") &&
                        !getContentAsString().contains("Invalid") &&
                        !getContentAsString().contains("captcha.gif")) {
                    captchaFound = true;
                }
            }
            if (!captchaFound) {
                throw new PluginImplementationException("Captcha error or not implemented");
            }
            Cookie[] cookies;
            if ((cookies = client.getHTTPClient().getState().getCookies()).length > 0) {
                storage.storeConfigToFile(cookies, FILE_COOKIE_NAME);
            }
        }

        matcher = getMatcherAgainstContent("javascript:_get\\('([^']+)'[^']*''");
        if (matcher.find()) {
            HttpMethod methodPross;

            // This is need, I don't know why but really need
            methodPross = getMethodBuilder().setReferer(fileURL).setAction("http://share-links.biz/template/images/header/blank.gif").toGetMethod();
            updateHeader(methodPross);
            makeRedirectedRequest(methodPross);

            do {
                client.setReferer(fileURL);

                methodPross = getGetMethod(HTTP_ENGINE + matcher.group(1));
                updateHeader(methodPross);
                if (!makeRequest(methodPross)) {
                    throw new ServiceConnectionProblemException();
                }
                methodPross = getMethodBuilder().setActionFromIFrameSrcWhereTagContains("Main").
                        setReferer(HTTP_ENGINE + matcher.group(1)).
                        toGetMethod();
                updateHeader(methodPross);
                if (!makeRequest(methodPross)) {
                    throw new ServiceConnectionProblemException();
                }
                Matcher matcher1 = getMatcherAgainstContent("\\(\"([^\"]+)\"[^|]*([^']*)");
                if (matcher1.find()) {
                    List<String> a1 = Arrays.asList(matcher1.group(2).replace('|', ':').split(":"));
                    String value = matcher1.group(1);
                    int src1 = Integer.parseInt(matcher1.group(1).replaceAll("[^\\d]", ""));
                    value = value.replace("" + src1, a1.get(src1));
                    value = e8412ffeb32e4a7934d6edc44ca0f8d9(value);
                    logger.info(value);
                    queye.add(new URI(value));
                }
                downloadTask.sleep(1);
            } while (matcher.find());
        } else {
            logger.info(getContentAsString());
        }

        if (queye.isEmpty()) {
            throw new PluginImplementationException("No download links found");
        }
        synchronized (getPluginService().getPluginContext().getQueueSupport()) {
            getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(httpFile, queye);
        }
    }

    private static void updateHeader(HttpMethod method) {
        method.setRequestHeader("Host", "share-links.biz");
        method.setRequestHeader("User-Agent", "Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US; rv:1.9.2) Gecko/20100115 Firefox/3.6 (.NET CLR 3.5.30729)");
        method.setRequestHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,* /*;q=0.8");
        method.setRequestHeader("Accept-Language", "en-us,en;q=0.5");
        //method.setRequestHeader("Accept-Encoding","gzip,deflate");
        method.setRequestHeader("Accept-Charset", "ISO-8859-1,utf-8;q=0.7,*;q=0.7");
        method.setRequestHeader("Keep-Alive", "115");
        method.setRequestHeader("Connection", "keep-alive");
    }

    /**
     * This method have this name becose JavaScript in page have too
     * Please don't change, i like to keep same
     *
     * @param a Encrypted
     * @return Decrypted
     */
    private static String e8412ffeb32e4a7934d6edc44ca0f8d9(String a) {
        String b = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=";
        String c = "";
        int d, rhc2, rhc3;
        int e, cne2, cne3, cne4;
        int i = 0;
        a = a.replaceAll("[^A-Za-z0-9\\+/=]/g", "");
        do {
            d = rhc2 = rhc3 = 0;
            e = cne2 = cne3 = cne4 = 0;
            e = b.indexOf(a.charAt(i++));
            cne2 = b.indexOf(a.charAt(i++));
            cne3 = b.indexOf(a.charAt(i++));
            cne4 = b.indexOf(a.charAt(i++));
            d = e << 2 | cne2 >> 4;
            rhc2 = (cne2 & 15) << 4 | cne3 >> 2;
            rhc3 = (cne3 & 3) << 6 | cne4;
            c = c + ((char) d);
            if (cne3 != 64) {
                c = c + ((char) rhc2);
            }
            if (cne4 != 64) {
                c = c + ((char) rhc3);
            }
        } while (i < a.length());

        try {
            return URLDecoder.decode(c, "UTF-8");
        } catch (UnsupportedEncodingException x) {
            LogUtils.processException(logger, x);
            return "";
        }
    }

}
