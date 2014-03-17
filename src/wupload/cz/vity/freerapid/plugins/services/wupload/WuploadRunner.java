package cz.vity.freerapid.plugins.services.wupload;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.services.recaptcha.ReCaptcha;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;

import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * @author Coloss
 *         I've used the filesonic and the fileserve plugins as a skeleton because wupload at this point in time is quite similar to them
 *         Thanks to the authors of these plugins:
 *         JPEXS, ntoskrnl (filesonic)
 *         RickCL (fileserve)
 */
class WuploadRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(WuploadRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final HttpMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkNameAndSize(getContentAsString());
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    @Override
    public void run() throws Exception {
        super.run();
        HttpMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkNameAndSize(getContentAsString());
            fileURL = method.getURI().toString();
            final String startUrl = fileURL + "?start=1";
            method = getMethodBuilder().setReferer(fileURL).setAction(startUrl).toPostMethod();
            while (true) {
                method.addRequestHeader("X-Requested-With", "XMLHttpRequest");
                if (!makeRedirectedRequest(method)) {
                    checkProblems();
                    throw new ServiceConnectionProblemException();
                }
                final String content = getContentAsString();
                if (content.contains("Recaptcha")) {
                    final String reCaptchaKey = PlugUtils.getStringBetween(content, "Recaptcha.create(\"", "\"");
                    final ReCaptcha r = new ReCaptcha(reCaptchaKey, client);
                    final String captcha = getCaptchaSupport().getCaptcha(r.getImageURL());
                    if (captcha == null) {
                        throw new CaptchaEntryInputMismatchException();
                    }
                    r.setRecognized(captcha);
                    method = r.modifyResponseMethod(getMethodBuilder().setReferer(fileURL).setAction(startUrl)).toPostMethod();
                } else if (content.contains("var countDownDelay =")) {
                    final int waitTime = PlugUtils.getWaitTimeBetween(content, "var countDownDelay =", ";", TimeUnit.SECONDS);
                    downloadTask.sleep(waitTime + 1);
                    try {
                        final String tm = PlugUtils.getParameter("tm", content);
                        final String tm_hash = PlugUtils.getParameter("tm_hash", content);
                        method = getMethodBuilder().setReferer(fileURL).setAction(startUrl).setParameter("tm", tm).setParameter("tm_hash", tm_hash).toPostMethod();
                    } catch (PluginImplementationException e) {
                        method = getMethodBuilder().setReferer(fileURL).setAction(startUrl).toPostMethod();
                    }
                } else if (content.contains("Download Ready")) {
                    method = getMethodBuilder().setReferer(fileURL).setActionFromAHrefWhereATagContains("Download now").toGetMethod();
                    setFileStreamContentTypes("\"application/octet-stream\"");
                    if (!tryDownloadAndSaveFile(method)) {
                        checkProblems();
                        throw new ServiceConnectionProblemException("Error starting download");
                    }
                    break;
                } else {
                    checkProblems();
                    throw new PluginImplementationException();
                }
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        PlugUtils.checkName(httpFile, content, "<title>Get", "on Wupload.com</title>");
        final String size = PlugUtils.getStringBetween(content, "class=\"size\">", "</span>");
        httpFile.setFileSize(PlugUtils.getFileSizeFromString(size.replace(",", "")));
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        if (getContentAsString().contains("Page Not Found")) {               //getContentAsString().contains("The file could not be found") || getContentAsString().contains("Page Not Found") || getContentAsString().contains("File not available"))
            throw new URLNotAvailableAnymoreException("File not found");
        }
/*
        if (getContentAsString().contains("/error.php")) {
            throw new ServiceConnectionProblemException("Temporary server issue");
        }
        Matcher matcher = getMatcherAgainstContent("You (?:have|need) to wait (\\d+) seconds to start another download");
        if (matcher.find()) {
            throw new YouHaveToWaitException(matcher.group(), Integer.parseInt(matcher.group(1)));
        }
*/
    }

}
