package cz.vity.freerapid.plugins.services.netloadin;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.services.netloadin.captcha.CaptchaReader;
import cz.vity.freerapid.plugins.services.recaptcha.ReCaptcha;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.params.HttpClientParams;

import java.io.InputStream;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * @author Ladislav Vitasek, Ludek Zika, JPEXS (Captcha), birchie
 */
class NetloadInRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(NetloadInRunner.class.getName());
    private String HTTP_NETLOAD = "http://netload.in";

    private String initURL;
    private String enterURL;
    private int captchaCount = 0;
    private static final int CAPTCHARETRIES = 5;

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        checkURL(fileURL);
        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            Matcher matcher = getMatcherAgainstContent("we don't host the requested file");
            if (matcher.find()) {
                throw new URLNotAvailableAnymoreException("<b>Requested file isn't hosted. Probably was deleted.</b>");
            }
            checkNameAndSize();
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize() throws ErrorDuringDownloadingException {
        Matcher matcher = getMatcherAgainstContent("dl_first_filename\">\\s*(.*)<span");
        if (matcher.find()) {
            httpFile.setFileName(matcher.group(1));
        }
        matcher = getMatcherAgainstContent(">, ([0-9.]+ .B)</span>");
        if (matcher.find()) {
            httpFile.setFileSize(PlugUtils.getFileSizeFromString(matcher.group(1)));
        }
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        checkURL(fileURL);
        initURL = fileURL;
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {
            checkNameAndSize();
            captchaCount = 0;
            do {
                stepEnterPage(getContentAsString());
                if (!getContentAsString().contains("Please enter the security code")) {
                    throw new PluginImplementationException("Captcha not found");
                }
                stepCaptcha(getContentAsString());
                captchaCount++;
            } while (getContentAsString().contains("You may forgot the security code or it might be wrong")
                    || getContentAsString().contains("Please enter the security code"));
            Matcher matcher = getMatcherAgainstContent(">countdown\\(([0-9]+)");
            if (matcher.find()) {
                int time = Integer.parseInt(matcher.group(1)) / 100;
                if (time <= 60) {
                    downloadTask.sleep(time + 1);
                } else {
                    throw new YouHaveToWaitException(String.format("You could download your next file in %s minutes", (time / 60)), time + 5);
                }
            }
            matcher = PlugUtils.matcher("href=\"([^\"]*)\">Or click here.</a></b><br/>", getContentAsString());
            if (matcher.find()) {
                String s = matcher.group(1);
                logger.info("Found File URL - " + s);
                final GetMethod method = getGetMethod(s);
                if (!tryDownloadAndSaveFile(method)) {
                    checkProblems();
                    throw new ServiceConnectionProblemException("Error starting download");
                }
            } else {
                throw new PluginImplementationException("Download link not found");
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private boolean stepEnterPage(String contentAsString) throws Exception {
        if (contentAsString.contains("This file is secured with a password")) {
            stepPasswordPage();
            contentAsString = getContentAsString();
        }
        if (contentAsString.contains("recaptcha_challenge"))
            return true;

        Matcher matcher = PlugUtils.matcher("class=\"Free_dl\">(.|\\W)*?<a href=\"([^\"]*)\"", contentAsString);
        if (!matcher.find()) {
            throw new PluginImplementationException("Download link not found");
        }
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
        String s = "/" + PlugUtils.replaceEntities(matcher.group(2));
        client.setReferer(initURL);

        logger.info("Go to URL - " + s);
        GetMethod method1 = getGetMethod(HTTP_NETLOAD + s);
        enterURL = HTTP_NETLOAD + s;
        client.getHTTPClient().getParams().setBooleanParameter(HttpClientParams.ALLOW_CIRCULAR_REDIRECTS, true);

        if (!makeRedirectedRequest(method1)) {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }

        return true;
    }


    private boolean stepCaptcha(String contentAsString) throws Exception {
        if (contentAsString.contains("Please enter the security code")) {

            Matcher matcher = PlugUtils.matcher("src=\"(share/includes/captcha.*?)\"", contentAsString);
            if (matcher.find()) {
                String s = "/" + PlugUtils.replaceEntities(matcher.group(1));
                String captcha = "";
                if (captchaCount < CAPTCHARETRIES) {
                    logger.info("Getting captcha image");
                    GetMethod methodC = getGetMethod(HTTP_NETLOAD + s);
                    client.getHTTPClient().executeMethod(methodC);

                    logger.info("Reading captcha...");
                    InputStream is = null;
                    try {
                        is = methodC.getResponseBodyAsStream();
                        captcha = CaptchaReader.recognize(is);
                    } catch (Exception ex) {
                        logger.severe(ex.toString());
                    } finally {
                        if (is != null) {
                            try {
                                is.close();
                            } catch (Exception ex) {
                                //ignore
                            }
                        }
                    }
                    logger.info("Captcha read:" + captcha);

                    methodC.releaseConnection();
                    if (captcha == null) {
                        logger.warning("Cannot read captcha (retry " + captchaCount + ") - wrong number separation");
                        return false;
                    }
                } else {
                    captcha = getCaptchaSupport().getCaptcha(HTTP_NETLOAD + s);
                }
                if (captcha == null) {
                    throw new CaptchaEntryInputMismatchException();
                } else {

                    String file_id = PlugUtils.getParameter("file_id", contentAsString);
                    matcher = PlugUtils.matcher("form method=\"post\" action=\"([^\"]*)\"", contentAsString);
                    if (!matcher.find()) {
                        throw new PluginImplementationException("Captcha form action was not found");
                    }
                    s = "/" + matcher.group(1);
                    client.setReferer(enterURL);
                    final PostMethod postMethod = getPostMethod(HTTP_NETLOAD + s);
                    postMethod.addParameter("file_id", file_id);
                    postMethod.addParameter("captcha_check", captcha);
                    postMethod.addParameter("start", "");

                    if (makeRequest(postMethod)) {

                        return true;
                    }
                }
            } else if (contentAsString.contains("recaptcha_challenge")) {
                final String reCaptchaKey = PlugUtils.getStringBetween(contentAsString, "recaptcha/api/challenge?k=", "\"");
                final ReCaptcha r = new ReCaptcha(reCaptchaKey, client);
                final String captcha = getCaptchaSupport().getCaptcha(r.getImageURL());
                if (captcha == null) {
                    throw new CaptchaEntryInputMismatchException();
                }
                r.setRecognized(captcha);
                final HttpMethod httpMethod = r.modifyResponseMethod(getMethodBuilder(contentAsString)
                        .setActionFromFormWhereTagContains("recaptcha", true)).toPostMethod();
                if (makeRedirectedRequest(httpMethod)) {
                    return true;
                }
            } else {
                throw new PluginImplementationException("Captcha picture was not found");
            }

        }
        return false;
    }

    private void checkURL(String fileURL) {
        if (fileURL.toLowerCase().contains("www.netload.in")) HTTP_NETLOAD = "http://www.netload.in";
    }

    private void stepPasswordPage() throws Exception {
        while (getContentAsString().contains("This file is secured with a password")) {
            Matcher matcher = getMatcherAgainstContent("name=\"form\" method=\"post\" action=\"([^\"]*)\"");
            if (!matcher.find()) {
                throw new PluginImplementationException("Password form not found");
            }
            String tar = HTTP_NETLOAD + "/" + matcher.group(1);
            logger.info("Post url to - " + tar);
            PostMethod post1 = getPostMethod(tar);
            matcher = getMatcherAgainstContent("value=\"([^\"]*)\" name=\"file_id\"");
            if (!matcher.find()) {
                throw new PluginImplementationException("Password form problem");
            }
            String file_id = matcher.group(1);
            post1.addParameter("file_id", file_id);
            post1.addParameter("password", getPassword());

            if (!makeRedirectedRequest(post1)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
        }
    }

    private String getPassword() throws Exception {
        NetloadPasswordUI ps = new NetloadPasswordUI();
        if (getDialogSupport().showOKCancelDialog(ps, "Secured file on Netload.in")) {
            return (ps.getPassword());
        } else throw new NotRecoverableDownloadException("This file is secured with a password!");

    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        if (getContentAsString().contains("404 - Not Found") || getContentAsString().contains("The file was deleted")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
        Matcher matcher;
        matcher = getMatcherAgainstContent("You could download your next file in.*countdown\\(([0-9]+)");
        if (matcher.find()) {
            final int time = Integer.parseInt(matcher.group(1)) / 6000;
            throw new YouHaveToWaitException(String.format("<b> You could download your next file in %s minutes", time), time * 60);
        }
        if (getContentAsString().contains("Sorry, we don't host the requested file")) {
            throw new URLNotAvailableAnymoreException("<b>Requested file isn't hosted. Probably was deleted.</b>");
        }
        if (getContentAsString().contains("unknown_file_data")) {
            throw new URLNotAvailableAnymoreException("Unknown file data");
        }
        if (getContentAsString().contains("This file is only for Premium Users")) {
            throw new NotRecoverableDownloadException("This File is Only for Premium Users");
        }
        if (getContentAsString().contains("currently in maintenance work")) {
            throw new ServiceConnectionProblemException("This Server is currently in maintenance work. Please try again in a few hours.");
        }
        if (getContentAsString().contains("This file was damaged")) {
            throw new URLNotAvailableAnymoreException("This file was damaged by a hard disc crash.");
        }
    }

}