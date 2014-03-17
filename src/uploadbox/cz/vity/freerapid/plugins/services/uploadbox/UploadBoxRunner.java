package cz.vity.freerapid.plugins.services.uploadbox;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.hoster.CaptchaSupport;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;

import java.io.IOException;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * @author Ladislav Vitasek, Ludek Zika
 */
class UploadBoxRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(UploadBoxRunner.class.getName());
    private final static String WEB = "http://uploadbox.com";

    public UploadBoxRunner() {
        super();
    }

    public void runCheck() throws Exception {
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkNameAndSize(getContentAsString());
        } else
            throw new ServiceConnectionProblemException();
    }

    public void run() throws Exception {
        super.run();
        client.setReferer(fileURL);
        GetMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkNameAndSize(getContentAsString());
            final PostMethod postMethod = getPostMethod(fileURL);
            postMethod.addParameter("free", "yes");
            if (makeRedirectedRequest(postMethod)) {

                if (getContentAsString().contains("captcha1")) {

                    while (getContentAsString().contains("captcha1")) {
//                        Matcher matcher = getMatcherAgainstContent("<span id=\"countdown\">([0-9]+)</span>");
                        PostMethod method = stepCaptcha(getContentAsString());
//                        if (matcher.find()) {
//                            int time = Integer.parseInt(matcher.group(1));
//                            downloadTask.sleep(time - 1);
//                        }
                        if (!makeRedirectedRequest(method))
                            throw new ServiceConnectionProblemException();
                    }

                    final Matcher matcher = getMatcherAgainstContent("2 seconds, please <a href=\"(.+?)\"");
                    if (matcher.find()) {
                        getMethod = getGetMethod(matcher.group(1));
                        if (!tryDownloadAndSaveFile(getMethod)) {
                            checkProblems();
                            logger.warning(getContentAsString());
                            throw new IOException("File input stream is empty.");
                        }

                    } else throw new PluginImplementationException();


                } else {
                    checkProblems();
                    logger.info(getContentAsString());
                    throw new PluginImplementationException();
                }
            } else throw new PluginImplementationException();
        } else
            throw new ServiceConnectionProblemException();
    }


    private void checkNameAndSize(String content) throws Exception {

        if (!content.contains("UploadBox.com")) {
            logger.warning(getContentAsString());
            throw new InvalidURLOrServiceProblemException("Invalid URL or unindentified service");
        }

        Matcher matcher = getMatcherAgainstContent("File name:</strong>(.+?)<");
        if (matcher.find()) {
            String fn = matcher.group(1).trim();
            logger.info("File name " + fn);
            httpFile.setFileName(fn);
            httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
        } else logger.warning("File name not found");

        matcher = getMatcherAgainstContent("File size:</strong>(.+?)<");
        if (matcher.find()) {
            Long a = PlugUtils.getFileSizeFromString(matcher.group(1));
            logger.info("File size " + a);
            httpFile.setFileSize(a);
        } else logger.warning("File size not found");
    }


    private PostMethod stepCaptcha(String contentAsString) throws Exception {
        if (contentAsString.contains("captcha1")) {
            CaptchaSupport captchaSupport = getCaptchaSupport();
            Matcher matcher = PlugUtils.matcher("in:<img src=\"(.+?)\"", contentAsString);
            if (matcher.find()) {
                String s = PlugUtils.replaceEntities(matcher.group(1));
                logger.info("Captcha URL " + s);
                String captcha = captchaSupport.getCaptcha(WEB + s);

                if (captcha == null) {
                    throw new CaptchaEntryInputMismatchException();
                } else {

                    client.setReferer(fileURL);
                    final PostMethod postMethod = getPostMethod(fileURL);
                    PlugUtils.addParameters(postMethod, contentAsString, new String[]{"code", "go"});
                    postMethod.addParameter("enter", captcha);
                    return postMethod;
                }
            } else {
                logger.warning(contentAsString);
                throw new PluginImplementationException("Captcha picture was not found");
            }
        }
        return null;
    }

    private void checkProblems() throws ServiceConnectionProblemException, YouHaveToWaitException, URLNotAvailableAnymoreException {
        if (getContentAsString().contains("class=\"not_found\"")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
        if (getContentAsString().contains("Please finish download and try")) {
            throw new ServiceConnectionProblemException("You already download some file. Please finish download and try again.");
        }
    }

}