package cz.vity.freerapid.plugins.services.missupload;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author ntoskrnl
 */
class MissUploadFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(MissUploadFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final GetMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkNameAndSize();
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize() throws ErrorDuringDownloadingException {
        final String content = getContentAsString();
        PlugUtils.checkName(httpFile, content, "<h2>Download File ", "</h2>");
        PlugUtils.checkFileSize(httpFile, content, "</font> (", ")</font>");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkNameAndSize();

            HttpMethod httpMethod = getMethodBuilder()
                    .setReferer(fileURL)
                    .setBaseURL(fileURL)
                    .setActionFromFormWhereTagContains("Free Download", true)
                    .removeParameter("method_premium")
                    .toPostMethod();

            if (!makeRedirectedRequest(httpMethod)) throw new ServiceConnectionProblemException();

            httpMethod = getMethodBuilder()
                    .setReferer(fileURL)
                    .setBaseURL(fileURL)
                    .setActionFromFormByName("F1", true)
                    .removeParameter("method_premium")
                    .removeParameter("code")
                    .setParameter("code", stepCaptcha())
                    .toPostMethod();

            downloadTask.sleep(PlugUtils.getNumberBetween(getContentAsString(), "<span id=\"countdown\">", "</span>") + 1);

            if (!tryDownloadAndSaveFile(httpMethod)) {
                checkProblems();
                if (getContentAsString().contains("Wrong captcha"))
                    throw new PluginImplementationException("Problem with captcha");
                throw new ServiceConnectionProblemException("Error starting download");
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String content = getContentAsString();
        if (content.contains("File Not Found") || content.contains("No such user exist")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

    private String stepCaptcha() throws ErrorDuringDownloadingException {
        final Matcher matcher = getMatcherAgainstContent("padding-left:(\\d+?)px;.+?&#(\\d\\d);");

        int start = 0;
        final List<CaptchaEntry> list = new ArrayList<CaptchaEntry>(4);
        while (matcher.find(start)) {
            list.add(new CaptchaEntry(Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)) - 48));
            start = matcher.end();
        }
        Collections.sort(list);

        final StringBuilder builder = new StringBuilder();
        for (CaptchaEntry entry : list) {
            builder.append(entry.value);
        }
        final String captcha = builder.toString();

        logger.info("Processed captcha '" + captcha + "'");
        if (captcha.length() != 4) logger.warning("Possible captcha issue");
        return captcha;
    }

    private static class CaptchaEntry implements Comparable<CaptchaEntry> {
        private Integer position;
        private Integer value;

        CaptchaEntry(int position, int value) {
            this.position = position;
            this.value = value;
        }

        public int compareTo(CaptchaEntry o) {
            return position.compareTo(o.position);
        }
    }

}