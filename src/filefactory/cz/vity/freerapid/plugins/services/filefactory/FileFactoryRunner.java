package cz.vity.freerapid.plugins.services.filefactory;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadState;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;

import java.io.IOException;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * @author Vity
 */
class FileFactoryRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(FileFactoryRunner.class.getName());
    private static final String HTTP_FILEFACTORY_COM = "http://www.filefactory.com";
    private int deep;
    private String iframeContent;
    private static final String VERIFICATION_WAS_INCORRECT = "the verification code you entered was incorrect";

    private enum Step {
        INIT, FRAME, DOWNLOAD, FINISHED
    }

    private Step step;
    private String initURL;
    private String iframeURL;
    private String finalDownloadURL;

    FileFactoryRunner() {
        super();
        deep = 2;
    }

    @Override
    public void runCheck() throws Exception {
        super.runCheck();

        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRequest(getMethod)) {
            checkSize(getContentAsString());
        } else
            throw new PluginImplementationException("Problem with a connection to service.\nCannot find requested page content");
    }


    public void run() throws Exception {
        super.run();
        step = Step.INIT;
        initURL = fileURL;
        logger.info("Starting download in TASK " + initURL);
        iframeContent = iframeURL = "";
        httpFile.setState(DownloadState.GETTING);
        while (step != Step.FINISHED) {
            switch (step) {
                case INIT:
                    mainStep(initURL);
                    break;
                case FRAME:
                    iframeStep(iframeURL);
                    break;
                case DOWNLOAD:
                    downloadStep(finalDownloadURL);
                    break;
                case FINISHED:
                    return;
                default:
                    assert false;
            }
        }

    }

    private void downloadStep(String finalDownloadURL) throws Exception {
        saveFileOnURL(finalDownloadURL);
    }

    private void iframeStep(String iframeURL) throws Exception {
        logger.info("IFrame url " + iframeURL);
        if (stepCaptcha(iframeContent))
            return;
        if (checkRestart(iframeContent))
            return;
        if (checkDownload(iframeContent))
            return;
        throw new PluginImplementationException("Unrecognized content of page");
    }

    private boolean checkDownload(String iframeContent) {
        if (iframeContent.contains("Click here to begin your")) {
            Matcher matcher = PlugUtils.matcher("top\" href=\"(.*?)\"><img src", iframeContent);
            if (matcher.find()) {
                this.finalDownloadURL = matcher.group(1);
                step = Step.DOWNLOAD;
                return true;
            }
        }
        return false;
    }

    private void mainStep(String stepURL) throws Exception {
        if (--deep <= 0) {
            logger.warning(getContentAsString());
            throw new InvalidURLOrServiceProblemException("Captcha input timeout");
        }
        final GetMethod getMethod = getGetMethod(stepURL);
        getMethod.setFollowRedirects(true);
        if (makeRequest(getMethod)) {
            String contentAsString = getContentAsString();
            checkSize(contentAsString);
            Matcher matcher;
            String s;
            //href="/dlf/f/a3f880/b/2/h/dd8f6f6df8ec3d79aef99536de9aab06/j/0/n/KOW_-_Monica_divx_002" id="basicLink"
            matcher = getMatcherAgainstContent("href=\"(.*?)\" id=\"basicLink\"");
            if (matcher.find()) {
                s = matcher.group(1);
                logger.info("Found File URL - " + s);
                final String basicLinkURL = HTTP_FILEFACTORY_COM + s;
                GetMethod method = getGetMethod(basicLinkURL);
                if (makeRequest(method)) {
                    contentAsString = getContentAsString();
                    logger.info(contentAsString);
                    matcher = PlugUtils.matcher("<iframe src=\"(.*?)\"", contentAsString);
                    if (matcher.find()) {
                        client.setReferer(basicLinkURL);
                        s = matcher.group(1);
                        iframeURL = PlugUtils.replaceEntities(s);
                        method = getGetMethod(HTTP_FILEFACTORY_COM + iframeURL);
                        if (!makeRequest(method))
                            throw new PluginImplementationException("IFrame with captcha not found");
                        iframeContent = getContentAsString();//iframes content
                        step = Step.FRAME;
                    } else throw new PluginImplementationException("IFrame with captcha not found");
                } else throw new PluginImplementationException("Retrieving page with free download failed");
            } else throw new PluginImplementationException("Basic link not found found");

        } else
            throw new PluginImplementationException("Problem with a connection to service.\nCannot find requested page content");
    }

    private void checkSize(String contentAsString) throws URLNotAvailableAnymoreException, YouHaveToWaitException, InvalidURLOrServiceProblemException {
        Matcher matcher = PlugUtils.matcher("Size: ([0-9-\\.]* .B)", contentAsString);
        if (!matcher.find()) {
            if (contentAsString.contains("file has been deleted") || contentAsString.contains("file is no longer available")) {
                throw new URLNotAvailableAnymoreException("This file has been deleted.");
            } else {
                if (contentAsString.contains("no free download slots")) {
                    throw new YouHaveToWaitException("Sorry, there are currently no free download slots available on this server.", 120);
                }
                throw new InvalidURLOrServiceProblemException("Invalid URL or unindentified service");
            }
        }
        String s = matcher.group(1);
        httpFile.setFileSize(PlugUtils.getFileSizeFromString(s));
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    private boolean stepCaptcha(String contentAsString) throws Exception {
        if (contentAsString.contains("Please enter the following code") || contentAsString.contains(VERIFICATION_WAS_INCORRECT)) {
            //src="/securimage/securimage_show.php?f=a3f880&amp;h=eda55e0920a7371c4983ec8e19f3de88"
            Matcher matcher = PlugUtils.matcher("src=\"(/securi[^\"]*)\"", contentAsString);
            if (matcher.find()) {
                String s = PlugUtils.replaceEntities(matcher.group(1));
                final String captcha = getCaptchaSupport().getCaptcha(HTTP_FILEFACTORY_COM + s);
                if (captcha == null) {
                    throw new CaptchaEntryInputMismatchException();
                } else {
                    client.setReferer(this.iframeURL);
                    String f = PlugUtils.getParameter("f", contentAsString);
                    String h = PlugUtils.getParameter("h", contentAsString);
                    String b = PlugUtils.getParameter("b", contentAsString);

                    final PostMethod postMethod = getPostMethod(HTTP_FILEFACTORY_COM + "/check/?" + "f=" + f + "&b=" + b + "&h=" + h);
                    postMethod.addParameter("f", f);
                    postMethod.addParameter("h", h);
                    postMethod.addParameter("b", b);
                    postMethod.addParameter("captcha", captcha);

                    if (makeRequest(postMethod)) {
                        iframeContent = getContentAsString();
                        return true;
                    }
                }
            } else throw new PluginImplementationException("Captcha picture was not found");
        }
        return false;
    }

    private void saveFileOnURL(String finalFileURL) throws Exception {
        HttpMethod getMethod = getGetMethod(finalFileURL);
        if (tryDownloadAndSaveFile(getMethod)) {
            step = Step.FINISHED;
        } else {
            if (checkLimit(getContentAsString())) {
                return;
            }
            if (checkRestart(getContentAsString())) return;
            logger.warning(getContentAsString());
            throw new IOException("File input stream is empty.");
        }
    }

    private boolean checkLimit(String contentAsString) throws YouHaveToWaitException {
        if (contentAsString.contains("for free users.  Please wait")) {
            Matcher matcher = PlugUtils.matcher("for free users.  Please wait ([0-9]+?) minutes", contentAsString);
            if (matcher.find()) {
                throw new YouHaveToWaitException("Limit for free users reached", Integer.parseInt(matcher.group(1)) * 60);
            }
            matcher = PlugUtils.matcher("for free users.  Please wait ([0-9]+?) seconds", contentAsString);
            if (matcher.find()) {
                throw new YouHaveToWaitException("Limit for free users reached", Integer.parseInt(matcher.group(1)) + 1);
            }

        }
        if (contentAsString.contains("no free download slots")) {
            throw new YouHaveToWaitException("Sorry, there are currently no free download slots available on this server.", 120);
        }
        if (contentAsString.contains("too many files at once")) {
            throw new YouHaveToWaitException("You are currently downloading too many files at once.Someone else on the same IP?", 60);
        }
        return false;
    }

    private boolean checkRestart(String contentAsString) throws Exception {
        if (contentAsString.contains("the allowed time to enter a code")) {
            Matcher matcher = PlugUtils.matcher("href=\"([^\"]*)\" target=\"_top\">restart", contentAsString);
            if (!matcher.find())
                throw new PluginImplementationException("Couldn't find restart URL");
            step = Step.INIT;
            initURL = matcher.group(1);
            return true;
        }
        return false;
    }

}