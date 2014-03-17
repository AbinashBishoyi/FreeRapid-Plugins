package cz.vity.freerapid.plugins.services.ifile;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.services.recaptcha.ReCaptcha;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.hoster.CaptchaSupport;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * @author JPEXS
 * @since 0.83
 */
class IFileFileRunner extends AbstractRunner {

    private final static Logger logger = Logger.getLogger(IFileFileRunner.class.getName());
    private final static String BASE_URL = "http://ifile.it/";
    private final static String REDIRECT_URL = BASE_URL + "dl";
    private String __alias_id;
    //    private String __x_c;
    private String __esn;
    private String __recaptcha_public;

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final HttpMethod httpMethod = getMethodBuilder().setAction(fileURL).setEncodePathAndQuery(true).toHttpMethod();

        if (makeRedirectedRequest(httpMethod)) {
            checkSeriousProblems();
            checkNameAndSize();
        } else {
            throw new InvalidURLOrServiceProblemException("Invalid URL or service problem");
        }
    }

    private void finalRequest() throws Exception {
        final HttpMethod httpMethod = getMethodBuilder().setAction(REDIRECT_URL).setReferer(REDIRECT_URL).toHttpMethod();
        //yes, the page needs to be downloaded twice
        makeRedirectedRequest(httpMethod);
        makeRedirectedRequest(httpMethod);
        final HttpMethod finalMethod = getMethodBuilder().setActionFromAHrefWhereATagContains("click here").setReferer(REDIRECT_URL).toHttpMethod();
        if (!tryDownloadAndSaveFile(finalMethod)) {
            logger.warning(getContentAsString());
            throw new IOException("File input stream is empty.");
        }
    }

    private String forcedGetContentAsString(HttpMethod method) {

        String content = null;
        try {
            content = method.getResponseBodyAsString();
        } catch (IOException ex) {
            //ignore
        }
        if (content == null) {
            content = "";
            InputStream is = null;
            try {
                is = method.getResponseBodyAsStream();
                if (is != null) {
                    int i = 0;
                    while ((i = is.read()) != -1) {
                        content += (char) i;
                    }
                }
            } catch (IOException ex) {
                //ignore
            } finally {
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException ex) {
                        //ignore
                    }
                }
            }

        }
        return content;
    }

    private void makeUrl(String type, String extra) throws Exception {
        String c = BASE_URL + "download:dl_request?alias_id=" + __alias_id + "&type=" + type + "&esn=" + __esn + extra;
        //c += "&" + __x_fs + additional;
        HttpMethod method = getMethodBuilder().setAction(c).toHttpMethod();
        method.addRequestHeader("X-Requested-With", "XMLHttpRequest"); //We use AJAX :-)

        /**
         * Note:
         * Because server response content type is json,
         * internal function getContentAsString does not work.
         */
        if (client.getHTTPClient().executeMethod(method) == 200) {
            String content = forcedGetContentAsString(method);
            //Response looks like this: {"status":"ok","captcha":1,"retry":0}
            String respStatus = PlugUtils.getStringBetween(content, "\"status\":\"", "\"");
            String respCaptcha = PlugUtils.getStringBetween(content, "\"captcha\":", ",");
            // Retry is not used...
            //String respRetry = PlugUtils.getStringBetween(content, "\"retry\":\"", "\"");
            if (respStatus.equals("ok")) {
                if (respCaptcha.equals("1")) {
                    stepReCaptcha();
                } else {
                    finalRequest();
                }
            } else {
                throw new PluginImplementationException("Server returned wrong status: " + respStatus);
            }
        } else {
            throw new InvalidURLOrServiceProblemException("Invalid URL or service problem");
        }
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        HttpMethod httpMethod = getMethodBuilder().setAction(fileURL).setEncodePathAndQuery(true).toHttpMethod();
        if (makeRedirectedRequest(httpMethod)) {
            checkAllProblems();
            checkNameAndSize();
            String content = getContentAsString();
            __alias_id = PlugUtils.getStringBetween(content, "var __alias_id				=	'", "';");
            __esn = "0";
            __recaptcha_public = PlugUtils.getStringBetween(content, "var __recaptcha_public		=	'", "';");
            makeUrl("na", "");
        } else {
            throw new InvalidURLOrServiceProblemException("Invalid URL or service problem");
        }
    }

    private void checkSeriousProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("file removed") || contentAsString.contains("no such file") || contentAsString.contains("file expired")) {
            throw new URLNotAvailableAnymoreException("File was not found");
        }
    }

    private void checkAllProblems() throws ErrorDuringDownloadingException {
        checkSeriousProblems();
    }

    private void checkNameAndSize() throws ErrorDuringDownloadingException {
        Matcher matcher = getMatcherAgainstContent("gray;\">\\s*([^<]*)\\s*&nbsp;\\s*([^<]*)\\s*</span>");

        if (matcher.find()) {
            final String fileName = matcher.group(1).trim();
            logger.info("File name " + fileName);
            httpFile.setFileName(fileName);
            final long fileSize = PlugUtils.getFileSizeFromString(matcher.group(2));
            logger.info("File size " + fileSize);
            httpFile.setFileSize(fileSize);
        } else {
            logger.warning("File size and size were not found");
            throw new PluginImplementationException("File name and size not found");
        }

        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    private void stepReCaptcha() throws Exception {
        ReCaptcha r = new ReCaptcha(__recaptcha_public, client);
        final CaptchaSupport captchaSupport = getCaptchaSupport();
        final String captchaSrc = r.getImageURL();
        logger.info("ReCaptcha URL " + captchaSrc);
        final String captcha = captchaSupport.getCaptcha(captchaSrc);
        if (captcha == null) {
            throw new CaptchaEntryInputMismatchException();
        } else {
            r.setRecognized(captcha);
            makeUrl("recaptcha", "&" + r.getResponseParams());
        }
    }

//    private void stepCaptchaSimple() throws Exception {
//        final CaptchaSupport captchaSupport = getCaptchaSupport();
//        final String captchaSrc = BASE_URL + "download:captcha";
//        logger.info("Simple captcha URL " + captchaSrc);
//        final String captcha = captchaSupport.getCaptcha(captchaSrc);
//
//        if (captcha == null) {
//            throw new CaptchaEntryInputMismatchException();
//        } else {
//            makeUrl("simple", "&" + __x_c + "=" + captcha);
//        }
//    }
}
