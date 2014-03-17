package cz.vity.freerapid.plugins.services.tudou;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author ntoskrnl
 */
class TudouFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(TudouFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        setPageEncoding("GBK");
        final HttpMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkNameAndSize();
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize() throws ErrorDuringDownloadingException {
        final String title = PlugUtils.getStringBetween(getContentAsString(), "<span id=\"vcate_title\">", "</span>");
        httpFile.setFileName(title + ".flv");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        setPageEncoding("GBK");
        HttpMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkNameAndSize();
            final Matcher matcher = getMatcherAgainstContent("iid\\s*=\\s*(\\d+)");
            if (!matcher.find()) {
                throw new PluginImplementationException("iid not found");
            }
            setPageEncoding("UTF-8");
            method = getGetMethod("http://v2.tudou.com/v2/kili?safekey=IAlsoNeverKnow&id="
                    + matcher.group(1) + "&noCatch=" + new Random().nextInt(100000));
            if (makeRedirectedRequest(method)) {
                final String url = getStreamUrl();
                method = getGetMethod(url);
                if (!tryDownloadAndSaveFile(method)) {
                    checkProblems();
                    throw new ServiceConnectionProblemException("Error starting download");
                }
            } else {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private String getStreamUrl() throws ErrorDuringDownloadingException {
        final List<Stream> list = new LinkedList<Stream>();
        final Matcher matcher = getMatcherAgainstContent("<f [^<>]*?size=\"(\\d+)\"[^<>]*?>([^<>]+?)</f>");
        while (matcher.find()) {
            list.add(new Stream(PlugUtils.replaceEntities(matcher.group(2)), Integer.parseInt(matcher.group(1))));
        }
        if (list.isEmpty()) {
            throw new PluginImplementationException("No streams found");
        }
        return Collections.max(list).getUrl();
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        if (getContentAsString().contains("哎呀！你想访问的网页不存在。")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

    private static class Stream implements Comparable<Stream> {
        private final String url;
        private final int size;

        public Stream(final String url, final int size) {
            this.url = url;
            this.size = size;
        }

        public String getUrl() {
            return url;
        }

        @Override
        public int compareTo(final Stream that) {
            return Integer.valueOf(this.size).compareTo(that.size);
        }
    }

}