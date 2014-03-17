package cz.vity.freerapid.plugins.services.forshared;

import cz.vity.freerapid.plugins.exceptions.NotRecoverableDownloadException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import cz.vity.freerapid.utilities.LogUtils;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * @author Alex, ntoskrnl
 */
class ForSharedRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(ForSharedRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        fileURL = fileURL.replace("/account/", "/").replace("/get/", "/file/");
        addCookie(new Cookie(".4shared.com", "4langcookie", "en", "/", 86400, false));
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
        fileURL = fileURL.replace("/account/", "/").replace("/get/", "/file/");
        logger.info("Starting download in TASK " + fileURL);
        addCookie(new Cookie(".4shared.com", "4langcookie", "en", "/", 86400, false));
        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkNameAndSize();

            if (fileURL.contains("/dir/")) {
                parseWebsite();
                httpFile.getProperties().put("removeCompleted", true);
            } else {
                HttpMethod httpMethod = getMethodBuilder().setReferer(fileURL).setActionFromAHrefWhereATagContains("Download Now").toGetMethod();
                if (makeRedirectedRequest(httpMethod)) {

                    httpMethod = getMethodBuilder().setReferer(httpMethod.getURI().toString()).setActionFromAHrefWhereATagContains("ownload").toGetMethod();

                    final int wait = PlugUtils.getNumberBetween(getContentAsString(), "DelayTimeSec'>", "<");
                    downloadTask.sleep(wait + 1);

                    if (!tryDownloadAndSaveFile(httpMethod)) {
                        checkProblems();
                        throw new ServiceConnectionProblemException("Error starting download");
                    }

                } else {
                    checkProblems();
                    throw new ServiceConnectionProblemException("Can't load download page");
                }
            }

        } else {
            checkProblems();
            throw new ServiceConnectionProblemException("Can't load download page");
        }

    }

    private void checkNameAndSize() throws Exception {
        if (fileURL.contains("/dir/")) {
            PlugUtils.checkName(httpFile, getContentAsString(), "<b style=\"font-size:larger;\">\n", "\n</b>");
        } else {
            PlugUtils.checkName(httpFile, getContentAsString(), "download ", "</title>");

            final Matcher size = getMatcherAgainstContent("Size:</b></td>\\s+?<td class=\"finforight\">([^<>]+?)</td>");
            if (!size.find()) throw new PluginImplementationException("File size not found");
            httpFile.setFileSize(PlugUtils.getFileSizeFromString(size.group(1).replace(",", "")));
        }

        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }


    private void checkProblems() throws ServiceConnectionProblemException, NotRecoverableDownloadException {
        final String content = getContentAsString();
        if (content.contains("The file link that you requested is not valid")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
        if (content.contains("already downloading")) {
            throw new ServiceConnectionProblemException("Your IP address is already downloading a file");
        }
        if (content.contains("Currently a lot of users")) {
            throw new ServiceConnectionProblemException("Currently a lot of users are downloading files");
        }
        if (content.contains("You must enter a password to access this file")) {
            throw new NotRecoverableDownloadException("Files with password are not supported");
        }
    }

    private void parseWebsite() throws Exception {
        final Matcher matcher = getMatcherAgainstContent("<a href=\"(http://.+?)\" target=\"_blank\" >");
        int start = 0;
        final List<URI> uriList = new LinkedList<URI>();
        while (matcher.find(start)) {
            try {
                uriList.add(new URI(matcher.group(1)));
            } catch (URISyntaxException e) {
                LogUtils.processException(logger, e);
            }
            start = matcher.end();
        }
        getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(httpFile, uriList);
    }

}