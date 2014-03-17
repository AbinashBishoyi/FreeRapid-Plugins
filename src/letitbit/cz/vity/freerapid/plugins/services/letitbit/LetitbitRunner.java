package cz.vity.freerapid.plugins.services.letitbit;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.services.recaptcha.ReCaptcha;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadClientConsts;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import cz.vity.freerapid.utilities.LogUtils;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpStatus;
import sun.org.mozilla.javascript.internal.NativeObject;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.awt.image.BufferedImage;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * @author Ladislav Vitasek, Ludek Zika, ntoskrnl
 */
public class LetitbitRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(LetitbitRunner.class.getName());

    protected void setLanguageCookie() {
        addCookie(new Cookie(".letitbit.net", "lang", "en", "/", 86400, false));
    }

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        setLanguageCookie();
        final HttpMethod httpMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(httpMethod)) {
            checkProblems();
            checkNameAndSize();
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    protected void checkNameAndSize() throws Exception {
        try {
            final String name = PlugUtils.getStringBetween(getContentAsString(), "<span class=\"file-info-name\">", "</span>");
            httpFile.setFileName(PlugUtils.unescapeHtml(name).trim());
            PlugUtils.checkFileSize(httpFile, getContentAsString(), "<span class=\"file-info-size\">[", "]</span>");
        } catch (Exception e) {
            final String name = PlugUtils.getStringBetween(getContentAsString(), ": <span>", "</span>");
            httpFile.setFileName(PlugUtils.unescapeHtml(name).trim());
            PlugUtils.checkFileSize(httpFile, getContentAsString(), "[<span>", "</span>]");
        }
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        setLanguageCookie();
        setClientParameter(DownloadClientConsts.DONT_USE_HEADER_FILENAME, true);
        HttpMethod httpMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(httpMethod)) {
            checkProblems();
            checkNameAndSize();
            //the API doesn't work at the moment
            //List<String> urls = new LetitbitApi(client).getDownloadUrls(fileURL);
            List<String> urls = null;
            if (urls == null) {
                for (int i = 1; i <= 3; i++) {
                    if (!postFreeForm()) {
                        if (i == 1) {
                            throw new PluginImplementationException("Free download button not found");
                        }
                        break;
                    }
                    logger.info("Posted form #" + i);
                }
                if (!getContentAsString().contains("recaptcha")) {
                    final String url = PlugUtils.getStringBetween(getContentAsString(), "var _direct_links = new Array(\"", "\");");
                    urls = Arrays.asList(url);
                } else {
                    if (getContentAsString().contains("seconds ="))
                        downloadTask.sleep(PlugUtils.getNumberBetween(getContentAsString(), "seconds =", ";") + 1);
                    String content = handleCaptcha();
                    logger.info("Ajax response: " + content);
                    if (content.contains("[\"")) {
                        content = PlugUtils.getStringBetween(content, "[", "]").replaceAll("(\\\\|\")", "");
                        urls = Arrays.asList(content.split(","));
                    } else {
                        urls = Arrays.asList(content);
                    }
                }
            }
            httpMethod = getGetMethod(getFinalUrl(urls));
            if (!tryDownloadAndSaveFile(httpMethod)) {
                checkProblems();
                throw new ServiceConnectionProblemException("Error starting download");
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String content = getContentAsString();
        if (content.contains("The page is temporarily unavailable")) {
            throw new ServiceConnectionProblemException("The page is temporarily unavailable");
        }
        if (content.contains("You must have static IP")) {
            throw new ServiceConnectionProblemException("You must have static IP");
        }
        if (content.contains("file was not found")
                || content.contains("\u043D\u0430\u0439\u0434\u0435\u043D")
                || content.contains("<title>404</title>")
                || (content.contains("Request file ") && content.contains(" Deleted"))
                || content.contains("File not found")
                || content.contains("<body><h1>Error</h1></body>")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

    private boolean postFreeForm() throws Exception {
        final Matcher matcher = getMatcherAgainstContent("(?is)(<form\\b.+?</form>)");
        while (matcher.find()) {
            final String content = matcher.group(1);
            if (!content.contains("/sms/check") && !content.contains("check_pinkod")) {
                HttpMethod method = null;
                if (content.contains("<script id=\"jsprotect_")) {
                    method = handleJavascriptMess(content);
                } else if (content.contains("md5crypt")) {
                    method = getMethodBuilder(content).setActionFromFormByIndex(1, true).toPostMethod();
                }
                if (method != null) {
                    if (!makeRedirectedRequest(method)) {
                        throw new ServiceConnectionProblemException();
                    }
                    return true;
                }
            }
        }
        return false;
    }

    private String getFinalUrl(final List<String> urls) throws Exception {
        for (final String url : urls) {
            final HttpMethod method = getGetMethod(url + "&check=1");
            logger.info("Checking URL: " + method.getURI().toString());
            method.setRequestHeader("X-Requested-With", "XMLHttpRequest");
            method.removeRequestHeader("Referer");
            if (!makeRequest(method)) {
                checkProblems();
                throw new PluginImplementationException();
            }
            if (method.getStatusCode() == HttpStatus.SC_OK) {
                logger.info("Final URL: " + url);
                return url;
            }
        }
        throw new ServiceConnectionProblemException("Final URL not found");
    }

    private String handleCaptcha() throws Exception {
        final String rcKey = "6Lc9zdMSAAAAAF-7s2wuQ-036pLRbM0p8dDaQdAM";
        final String rcControl = PlugUtils.getStringBetween(getContentAsString(), "var recaptcha_control_field = '", "';");
        while (true) {
            final ReCaptcha rc = new ReCaptcha(rcKey, client);
            final String captcha = getCaptchaSupport().getCaptcha(rc.getImageURL());
            if (captcha == null) {
                throw new CaptchaEntryInputMismatchException();
            }
            rc.setRecognized(captcha);
            final HttpMethod method = rc.modifyResponseMethod(getMethodBuilder()
                    .setAjax()
                    .setAction("/ajax/check_recaptcha.php"))
                    .setParameter("recaptcha_control_field", rcControl)
                    .toPostMethod();
            if (!makeRedirectedRequest(method)) {
                throw new ServiceConnectionProblemException();
            }
            final String content = getContentAsString().trim();
            if (content.contains("error_free_download_blocked")) {
                throw new ErrorDuringDownloadingException("You have reached the daily download limit");
            } else if (!content.contains("error_wrong_captcha")) {
                return content;
            }
        }
    }

    private HttpMethod handleJavascriptMess(final String formContent) throws Exception {
        final Object params = findParameters(formContent);
        final Matcher matcher = PlugUtils.matcher("(?s)<script\\b[^<>]*?>(.+?)</script>", formContent);
        if (!matcher.find()) {
            throw new PluginImplementationException("Script not found on page");
        }
        final String js = matcher.group(1);
        final String[] scripts = js.split("[\r\n]+");
        if (scripts.length < 3) {
            logger.warning(js);
            throw new PluginImplementationException("Error parsing script (1)");
        }
        final ScriptEngine engine = prepareEngine(params);
        for (int i = 0; i < scripts.length - 2; i++) {
            evalScript(engine, scripts[i], js, "(1)(" + i + ")");
        }
        final String extra1 = parseExtra(engine, scripts[scripts.length - 2], js);
        final String extra2 = parseExtra(engine, scripts[scripts.length - 1], js);
        evalScript(engine, extra1, js, "(3)");
        evalScript(engine, extra2.replaceFirst("\\$\\('#jsprotect_.+?'\\)\\.closest\\('form'\\)\\.attr\\('id'\\)", "'ifree_form'"), js, "(3)");
        final String imageData = getImageData(getImageAddress(extra1, js));
        final String func = "(function (data) {\n" +
                "    for (var i = 0; i < window.__jsp_list.length; ++i) {\n" +
                "        window.__jsp_list[i](data);\n" +
                "    }\n" +
                "})('" + imageData + "');";
        evalScript(engine, func, js, "(4)");
        @SuppressWarnings("unchecked")
        final Map<String, String> formParams = (Map<String, String>) engine.get("__outParams");
        final MethodBuilder mb = getMethodBuilder(formContent).setReferer(fileURL).setActionFromFormByIndex(1, false);
        for (final Map.Entry<String, String> entry : formParams.entrySet()) {
            mb.setParameter(entry.getKey(), entry.getValue());
        }
        return mb.toPostMethod();
    }

    private Object findParameters(final String formContent) throws Exception {
        final NativeObject params = new NativeObject();
        final Matcher matcher = PlugUtils.matcher("(?i)<input\\b[^<>]*?>", formContent);
        while (matcher.find()) {
            Matcher m = PlugUtils.matcher("(?i)id=[\"'](.+?)[\"']", matcher.group());
            if (!m.find()) {
                continue;
            }
            final String id = m.group(1);
            m = PlugUtils.matcher("(?i)value=[\"'](.+?)[\"']", matcher.group());
            if (!m.find()) {
                throw new PluginImplementationException("Input value not found in " + matcher.group());
            }
            final String value = m.group(1);
            params.defineProperty(id, value, NativeObject.READONLY);
        }
        if (params.isEmpty()) {
            throw new PluginImplementationException("Input parameters not found");
        }
        return params;
    }

    private ScriptEngine prepareEngine(final Object params) throws Exception {
        final ScriptEngine engine = new ScriptEngineManager().getEngineByName("JavaScript");
        if (engine == null) {
            throw new PluginImplementationException("JavaScript engine not found");
        }
        engine.put("__params", params);
        engine.put("__cookies", cookiesToParam());
        Reader reader = null;
        try {
            reader = new InputStreamReader(LetitbitRunner.class.getResourceAsStream("init.js"), "UTF-8");
            engine.eval(reader);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (final Exception e) {
                    LogUtils.processException(logger, e);
                }
            }
        }
        return engine;
    }

    private Object cookiesToParam() throws Exception {
        final NativeObject cookies = new NativeObject();
        for (final Cookie cookie : client.getHTTPClient().getState().getCookies()) {
            cookies.defineProperty(cookie.getName(), URLDecoder.decode(cookie.getValue(), "UTF-8"), NativeObject.READONLY);
        }
        return cookies;
    }

    private Object evalScript(final ScriptEngine engine, final String script, final String js, final String step) throws Exception {
        try {
            return engine.eval(script);
        } catch (final Exception e) {
            logger.warning(js);
            logger.warning(script);
            throw new PluginImplementationException("Script execution failed " + step, e);
        }
    }

    private String parseExtra(final ScriptEngine engine, final String extra, final String js) throws Exception {
        if (!extra.startsWith("eval")) {
            logger.warning(js);
            throw new PluginImplementationException("Error parsing script (2)");
        }
        return String.valueOf(evalScript(engine, extra.substring(4), js, "(2)"));
    }

    private String getImageAddress(final String extra, final String js) throws Exception {
        final Matcher matcher = PlugUtils.matcher("\"(/jspimggen\\.php\\?n=)\"\\+encodeURIComponent\\(\"(.+?)\"", extra);
        if (!matcher.find()) {
            logger.warning(js);
            throw new PluginImplementationException("Error parsing script (4)");
        }
        return matcher.group(1) + matcher.group(2) + "&r=" + Math.random();
    }

    private String getImageData(final String address) throws Exception {
        final MethodBuilder mb = getMethodBuilder();
        final String url = mb.setBaseURL("http://" + new URI(mb.getReferer()).getHost()).setAction(address).getEscapedURI();
        final BufferedImage image = getCaptchaSupport().getCaptchaImage(url);
        if (image == null) {
            throw new PluginImplementationException("Failed to load image");
        }
        final StringBuilder sb = new StringBuilder();
        for (int x = 0; x < image.getWidth(); x++) {
            final int red = (image.getRGB(x, 0) >>> 16) & 0xff;
            sb.append((char) red);
        }
        return sb.toString();
    }

}