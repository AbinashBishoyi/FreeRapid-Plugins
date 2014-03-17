package cz.vity.freerapid.plugins.services.mediafire;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import cz.vity.freerapid.plugins.webclient.utils.ScriptUtils;
import cz.vity.freerapid.utilities.LogUtils;
import org.apache.commons.httpclient.HttpMethod;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * @author Ladislav Vitasek, Ludek Zika, ntoskrnl
 */
public class MediafireRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(MediafireRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final HttpMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkNameAndSize();
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize() throws Exception {
        if (isList()) return;
        final String content = getContentAsString();
        PlugUtils.checkName(httpFile, content, "<div class=\"download_file_title\">", "</div>");
        PlugUtils.checkFileSize(httpFile, content, "Download <span>(", ")</span>");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String content = getContentAsString();
        if (content.contains("The key you provided for file download")
                || content.contains("How can MediaFire help you?")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

    @Override
    public void run() throws Exception {
        super.run();
        HttpMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            if (isList()) {
                runList();
                return;
            }
            checkNameAndSize();
            while (getContentAsString().contains("dh('');")) { //handle password
                HttpMethod postPwd = getMethodBuilder()
                        .setReferer(fileURL)
                        .setBaseURL("http://www.mediafire.com/")
                        .setActionFromFormByName("form_password", true)
                        .setAndEncodeParameter("downloadp", getPassword())
                        .toPostMethod();
                if (!makeRedirectedRequest(postPwd)) {
                    throw new ServiceConnectionProblemException("Some issue while posting password");
                }
            }
            method = findDownloadUrl();
            setFileStreamContentTypes("text/plain");
            if (!tryDownloadAndSaveFile(method)) {
                checkProblems();
                throw new ServiceConnectionProblemException("Error starting download");
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private HttpMethod findDownloadUrl() throws Exception {
        final List<DownloadElement> elements = DownloadElement.findDownloadElements(getContentAsString(), findZDivisor());
        final String url = Collections.max(elements).getUrl();
        return getGetMethod(url);
    }

    private int findZDivisor() throws ErrorDuringDownloadingException {
        // gysl8luzk='';oq1w66x=unescape(....;eval(gysl8luzk);
        //(...................................................) <-- this part is what we want
        Matcher matcher = getMatcherAgainstContent("(([a-z\\d]+?)\\s*?=\\s*?\\\\?'\\\\?';\\s*?[a-z\\d]+?\\s*?=\\s*?unescape\\(.+?)eval\\(\\2\\);");
        if (!matcher.find()) {
            throw new PluginImplementationException("Error parsing page JavaScript (1)");
        }
        final String script = matcher.group(1) + matcher.group(2) + ";";
        final String result;
        try {
            result = ScriptUtils.evaluateJavaScriptToString(script);
        } catch (final Exception e) {
            logger.warning(script);
            throw new PluginImplementationException("Error executing page JavaScript", e);
        }
        matcher = PlugUtils.matcher("%\\s*(\\d+)", result);
        if (!matcher.find()) {
            logger.warning(result);
            throw new PluginImplementationException("Error parsing page JavaScript (2)");
        }
        return Integer.parseInt(matcher.group(1));
    }

    private static class DownloadElement implements Comparable<DownloadElement> {
        private final String url;
        private final int zIndex;

        public static List<DownloadElement> findDownloadElements(final String content, final int zDivisor) throws ErrorDuringDownloadingException {
            final List<DownloadElement> list = new LinkedList<DownloadElement>();
            final Matcher matcher = PlugUtils.matcher("<div class=\"download_link\"[^<>]*?z\\-index:(\\d+)[^<>]*?>\\s*<a href=\"(.+?)\"", content);
            while (matcher.find()) {
                list.add(new DownloadElement(matcher.group(2), Integer.parseInt(matcher.group(1)) % zDivisor));
            }
            if (list.isEmpty()) {
                throw new PluginImplementationException("Download link not found");
            }
            return list;
        }

        private DownloadElement(final String url, final int zIndex) {
            this.url = url;
            this.zIndex = zIndex;
        }

        public String getUrl() {
            return url;
        }

        @Override
        public int compareTo(final DownloadElement that) {
            return Integer.valueOf(this.zIndex).compareTo(that.zIndex);
        }
    }

    private void runList() throws Exception {
        final Matcher matcher = getMatcherAgainstContent("src=\"(/js/myfiles.php[^\"]+?)\"");
        if (!matcher.find()) throw new PluginImplementationException("URL to list not found");
        final HttpMethod listMethod = getMethodBuilder().setReferer(fileURL).setAction(matcher.group(1)).toGetMethod();

        if (makeRedirectedRequest(listMethod)) {
            parseList();
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void parseList() {
        final Matcher matcher = getMatcherAgainstContent("oe\\[[0-9]+\\]=Array\\('([^']+?)'");
        final List<URI> uriList = new LinkedList<URI>();
        while (matcher.find()) {
            final String link = "http://www.mediafire.com/download.php?" + matcher.group(1);
            try {
                uriList.add(new URI(link));
            } catch (URISyntaxException e) {
                LogUtils.processException(logger, e);
            }
        }
        getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(httpFile, uriList);
    }

    private boolean isList() {
        return (fileURL.contains("?sharekey="));
    }

    private String getPassword() throws Exception {
        final String password = getDialogSupport().askForPassword("MediaFire");
        if (password == null) {
            throw new NotRecoverableDownloadException("This file is secured with a password");
        } else {
            return password;
        }
    }

}
