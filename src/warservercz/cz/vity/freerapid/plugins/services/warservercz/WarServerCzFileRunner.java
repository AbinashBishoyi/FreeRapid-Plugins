package cz.vity.freerapid.plugins.services.warservercz;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.services.recaptcha.ReCaptcha;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author tong2shot
 */
class WarServerCzFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(WarServerCzFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkNameAndSize(getContentAsString());
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        final Matcher matcher = getMatcherAgainstContent("<h1.*?>(.+?)</h1>");
        if (!matcher.find()) {
            throw new PluginImplementationException("File name not found");
        }
        httpFile.setFileName(matcher.group(1).trim());
        httpFile.setFileSize(PlugUtils.getFileSizeFromString(PlugUtils.getStringBetween(content, "Velikost: <strong>", "</strong>").replace("i", "")));
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            final String contentAsString = getContentAsString();
            checkProblems();
            checkNameAndSize(contentAsString);

            while (getContentAsString().contains("recaptcha/api/noscript?k=")) {
                final HttpMethod capMethod = reCaptcha(getMethodBuilder()
                        .setActionFromFormWhereTagContains("manual_challenge", true)
                        .setAction(fileURL)
                        .setReferer(fileURL)
                ).toPostMethod();
                if (!makeRedirectedRequest(capMethod)) {
                    checkProblems();
                    throw new ServiceConnectionProblemException("Error processing captcha");
                }
            }
            checkProblems();

            Matcher matcher = getMatcherAgainstContent("startFreeDownload\\(.+?,\\s*(.+?),\\s*(.+?),\\s*(.+?)\\)");
            if (!matcher.find()) {
                throw new PluginImplementationException("File id/wait time not found ");
            }
            final String fid = PlugUtils.unescapeHtml(matcher.group(1).trim()).replace("\"", "");
            final int waitTime = Integer.parseInt(PlugUtils.unescapeHtml(matcher.group(2).trim()).replace("\"", ""));
            final String server = PlugUtils.unescapeHtml(matcher.group(3).trim()).replace("\"", "").replace("\\/", "/");
            final String downloadLink = server + "/dwn-free.php?fid=" + fid;
            downloadTask.sleep(waitTime + 1);
            final HttpMethod httpMethod = getMethodBuilder()
                    .setReferer(fileURL)
                    .setAction(downloadLink)
                    .toGetMethod();
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
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("Soubor nenalezen")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

    private MethodBuilder reCaptcha(MethodBuilder dMethod) throws Exception {
        final String reCaptchaKey = PlugUtils.getStringBetween(getContentAsString(), "recaptcha/api/noscript?k=", "\" ");
        final ReCaptcha r = new ReCaptcha(reCaptchaKey, client);
        final String captcha = getCaptchaSupport().getCaptcha(r.getImageURL());
        if (captcha == null) {
            throw new CaptchaEntryInputMismatchException();
        }
        r.setRecognized(captcha);
        return r.modifyResponseMethod(dMethod);
    }
}