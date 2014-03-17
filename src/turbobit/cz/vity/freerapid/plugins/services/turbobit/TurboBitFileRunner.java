package cz.vity.freerapid.plugins.services.turbobit;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.services.recaptcha.ReCaptcha;
import cz.vity.freerapid.plugins.services.turbobit.captcha.CaptchaReader;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.hoster.CaptchaSupport;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpMethod;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.util.Random;
import java.util.logging.Logger;
import java.util.regex.Matcher;


/**
 * Class which contains main code
 *
 * @author Arthur Gunawan, RickCL, ntoskrnl, tong2shot, Abinash Bishoyi, birchie
 */
public class TurboBitFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(TurboBitFileRunner.class.getName());

    private final static int CAPTCHA_MAX = 0;
    private int captchaCounter = 1;

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        fileURL = checkFileURL(fileURL);
        final HttpMethod method = getMethodBuilder().setAction(fileURL).toGetMethod();
        if (makeRedirectedRequest(method)) {
            checkFileProblems();
            checkNameAndSize();
        } else {
            checkFileProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private String checkFileURL(final String fileURL) throws ErrorDuringDownloadingException {
        final Matcher matcher = PlugUtils.matcher("^http://(?:www\\.)?((?:turbobit|hitfile)\\.net)/(?:download/free/)?(\\w+)", fileURL);
        if (!matcher.find()) {
            throw new PluginImplementationException("Error parsing download link");
        }
        addCookie(new Cookie("." + matcher.group(1), "user_lang", "en", "/", 86400, false));
        return "http://" + matcher.group(1) + "/download/free/" + matcher.group(2);
    }

    protected void checkNameAndSize() throws ErrorDuringDownloadingException {
        final Matcher matcher = getMatcherAgainstContent("<span class.*?>(.+?)</span>\\s*\\((.+?)\\)");
        if (!matcher.find()) {
            throw new PluginImplementationException("File name/size not found");
        }
        httpFile.setFileName(matcher.group(1));
        httpFile.setFileSize(PlugUtils.getFileSizeFromString(matcher.group(2)));
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        fileURL = checkFileURL(fileURL);

        HttpMethod method = getMethodBuilder().setAction(fileURL).toGetMethod();
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkNameAndSize();

            Matcher matcher = getMatcherAgainstContent("limit\\s*:\\s*(\\d+)");
            if (matcher.find()) {
                throw new YouHaveToWaitException("Download limit reached", Integer.parseInt(matcher.group(1)));
            }

            while (getContentAsString().contains("/captcha/")) {
                if (!makeRedirectedRequest(stepCaptcha(method.getURI().toString()))) {
                    checkProblems();
                    throw new ServiceConnectionProblemException();
                }
                checkProblems();
            }

            matcher = getMatcherAgainstContent("\"fileId\"\\s*?:\\s*?\"(.+?)\"");
            if (!matcher.find()) {
                throw new PluginImplementationException("File ID not found");
            }

            method = getMethodBuilder()
                    .setReferer(method.getURI().toString())
                    .setAction(getRequestUrl(matcher.group(1)))
                    .toGetMethod();
            method.addRequestHeader("X-Requested-With", "XMLHttpRequest");

            downloadTask.sleep(61);

            if (!makeRedirectedRequest(method)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            checkProblems();
            if (getContentAsString().contains("Error: 2965")) {
                throw new PluginImplementationException("Plugin is broken, xor value has changed");
            }

            method = getMethodBuilder()
                    .setReferer(method.getURI().toString())
                    .setActionFromAHrefWhereATagContains("Download")
                    .toGetMethod();
            if (!tryDownloadAndSaveFile(method)) {
                checkProblems();
                throw new ServiceConnectionProblemException("Error starting download");
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkFileProblems() throws ErrorDuringDownloadingException {
        if (getContentAsString().contains("File was not found")
                || getContentAsString().contains("Probably it was deleted"))
            throw new URLNotAvailableAnymoreException("File not found");
    }

    private void checkDownloadProblems() throws ErrorDuringDownloadingException {
        if (getContentAsString().contains("The file is not available now because of technical problems")) {
            throw new ServiceConnectionProblemException("The file is not available now because of technical problems");
        }
        if (getContentAsString().contains("From your IP range the limit of connections is reached")) {
            final int waitTime = PlugUtils.getNumberBetween(getContentAsString(), "<span id='timeout'>", "</span>");
            throw new YouHaveToWaitException("Download limit reached from your IP range", waitTime);
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        checkFileProblems();
        checkDownloadProblems();
    }

    private HttpMethod stepCaptcha(final String action) throws Exception {
        if (getContentAsString().contains("recaptcha")) {
            logger.info("Handling ReCaptcha");

            final Matcher m = getMatcherAgainstContent("api.recaptcha.net/noscript\\?k=([^\"]+)\"");
            if (!m.find()) throw new PluginImplementationException("ReCaptcha key not found");
            final String reCaptchaKey = m.group(1);

            final String content = getContentAsString();
            final ReCaptcha r = new ReCaptcha(reCaptchaKey, client);
            final CaptchaSupport captchaSupport = getCaptchaSupport();

            final String captchaURL = r.getImageURL();
            logger.info("Captcha URL " + captchaURL);

            final String captcha = captchaSupport.getCaptcha(captchaURL);
            if (captcha == null) throw new CaptchaEntryInputMismatchException();
            r.setRecognized(captcha);

            return r.modifyResponseMethod(
                    getMethodBuilder(content)
                            .setReferer(action)
                            .setActionFromFormByIndex(3, true)
                            .setAction(action)
            ).toPostMethod();
        } else {
            logger.info("Handling regular captcha");

            final CaptchaSupport captchaSupport = getCaptchaSupport();
            final String captchaSrc = getMethodBuilder().setActionFromImgSrcWhereTagContains("captcha").getEscapedURI();
            logger.info("Captcha URL " + captchaSrc);

            final String captcha;
            if (captchaCounter <= CAPTCHA_MAX) {
                captcha = CaptchaReader.recognize(captchaSupport.getCaptchaImage(captchaSrc));
                if (captcha == null) {
                    logger.info("Could not separate captcha letters, attempt " + captchaCounter + " of " + CAPTCHA_MAX);
                }
                logger.info("OCR recognized " + captcha + ", attempt " + captchaCounter + " of " + CAPTCHA_MAX);
                captchaCounter++;
            } else {
                captcha = captchaSupport.getCaptcha(captchaSrc);
                if (captcha == null) throw new CaptchaEntryInputMismatchException();
                logger.info("Manual captcha " + captcha);
            }

            return getMethodBuilder()
                    .setReferer(action)
                    .setActionFromFormWhereTagContains("captcha", true)
                    .setAction(action)
                    .setParameter("captcha_response", captcha)
                    .toPostMethod();
        }
    }

    private String getRequestUrl(final String fileId) throws Exception {
        final String random = String.valueOf(1 + new Random().nextInt(100000));
        final byte[] bytes = (fileId + random).getBytes("ISO-8859-1");
        final int xorNum = getXORnumber();
        logger.warning("XOR value = " + xorNum);
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] ^= xorNum;
        }
        final String base64 = Base64.encodeBase64String(bytes).replace('/', '_');
        return "/download/getlinktimeout/" + fileId + "/" + random + "/" + base64;
    }

    private int getXORnumber() throws Exception {
        String contents = getContentAsString();
        final Matcher matcher = PlugUtils.matcher("src='(.+timeout\\.js.+)'\\s>", contents);       // get timeout.js location + variables
        if (!matcher.find()) throw new PluginImplementationException("XOR value finder err");

        final HttpMethod method = getGetMethod(matcher.group(1));                                  // load contents of timeout.js
        setFileStreamContentTypes(new String[0], new String[]{"application/x-javascript"});
        if (!makeRedirectedRequest(method)) {
            throw new ServiceConnectionProblemException("XOR js load error");
        }
        try {
            contents = evalScript(getContentAsString());
        } catch (Exception e) {
            throw new PluginImplementationException("Site Scripts have changed - Plugin update needed");
        }
        // first variable declared is the number used for the XOR - get it and return it
        final String sTarget = contents.substring(contents.indexOf("var "), contents.indexOf("function"));
        return PlugUtils.getNumberBetween(sTarget, " ", ";");
    }

    private String evalScript(String scriptFunctions) {
        final Matcher match = PlugUtils.matcher("eval\\(function\\(w,i,s,e\\)\\{var . = .\\}\\);", scriptFunctions);
        scriptFunctions = match.replaceAll(" ");                   // remove all useless eval functions
        final String[] scriptFuncts = scriptFunctions.split("eval");
        for (String contentTest : scriptFuncts) {
            try {
                logger.info("Contents = " + contentTest);
                final String clVars = PlugUtils.getStringBetween(contentTest, "function(", "){");
                final String sFuncts = contentTest.substring(contentTest.indexOf("{") + 1, contentTest.lastIndexOf("}")).replace("return", "OUTPUT=");
                final String clVals = contentTest.substring(contentTest.lastIndexOf("(") + 1, contentTest.lastIndexOf(") );"));

                final String aVars[] = clVars.split(",");
                final String aVals[] = clVals.split(",");
                String setVarVals = "";
                for (int iPos = 0; iPos < aVars.length; iPos++) {
                    setVarVals += aVars[iPos] + "=" + aVals[iPos] + ";";
                }
                ScriptEngineManager factory = new ScriptEngineManager();
                ScriptEngine engine = factory.getEngineByName("JavaScript");
                engine.eval(setVarVals + sFuncts).toString();

                String output = (String) engine.get("OUTPUT");
                output = output.replaceAll("\n", " ").replaceAll("\r", " ");

                if (output.contains("Waiting")) {
                    return output;
                } else if (output.contains("eval")) {
                    String out = evalScript(output);
                    if (out != null)
                        return out;
                }
            } catch (Exception e) {
                //error wrong script function
            }
        }
        return null;
    }

}