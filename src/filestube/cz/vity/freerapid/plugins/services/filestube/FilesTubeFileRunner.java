package cz.vity.freerapid.plugins.services.filestube;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.methods.GetMethod;

import java.net.URI;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class which contains main code
 *
 * @author ntoskrnl
 */
class FilesTubeFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(FilesTubeFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkNameAndSize();
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkNameAndSize();
            final String content = getContentAsString();
            if (content.contains("copy_paste_links")) {
                final String urlListRegex = "<pre id=\"copy_paste_links\".*?>(.+?)</pre>";
                final Pattern pattern = Pattern.compile(urlListRegex, Pattern.MULTILINE | Pattern.DOTALL);
                final Matcher urlListMatcher = pattern.matcher(content);
                if (urlListMatcher.find()) {
                    final StringTokenizer st = new StringTokenizer(urlListMatcher.group(1), "\n\r");
                    final List<URI> uriList = new LinkedList<URI>();
                    while (st.hasMoreTokens()) {
                        uriList.add(new URI(st.nextToken()));
                    }
                    getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(httpFile, uriList);
                    httpFile.getProperties().put("removeCompleted", true);
                } else {
                    throw new PluginImplementationException("Plugin is broken - links not found");
                }

            } else {
                throw new PluginImplementationException("Plugin is broken - links not found");
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize() throws ErrorDuringDownloadingException {
        final String content = getContentAsString();
        try {
            PlugUtils.checkName(httpFile, content, "<meta name=\"Keywords\" content=\"", ",");
            if (content.contains("Total size: <span>")) {
                PlugUtils.checkFileSize(httpFile, content, "Total size: <span>", "</span>");
            } else {
                final Matcher matcher = getMatcherAgainstContent("<td class\\s*=\\s*\"tright.*?>(.+?)</td>");
                if (!matcher.find()) throw new PluginImplementationException("Filesize not found");
                httpFile.setFileSize(PlugUtils.getFileSizeFromString(matcher.group(1).trim()));
            }
        } catch (PluginImplementationException e) {
            logger.warning("File name/size not found");
        }
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String content = getContentAsString();
        if (content.contains("Requested file was not found") || content.contains("Requested page was not found") || content.equals("Not found") || content.contains("no longer available")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

}