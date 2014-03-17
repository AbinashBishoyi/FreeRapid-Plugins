package cz.vity.freerapid.plugins.services.multiupload;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadState;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import cz.vity.freerapid.utilities.LogUtils;
import org.apache.commons.httpclient.methods.GetMethod;

import java.net.URI;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author ntoskrnl
 */
class MultiUploadFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(MultiUploadFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        if (PlugUtils.find("/.._", fileURL)) {
            httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
            return;
        }
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
        final List<URI> list = new LinkedList<URI>();
        if (PlugUtils.find("/.._", fileURL)) {
            processLink(fileURL, list);
        } else {
            final GetMethod getMethod = getGetMethod(fileURL);
            if (makeRedirectedRequest(getMethod)) {
                final Matcher matcher = getMatcherAgainstContent(">(http://www\\.multiupload\\.(com|co.uk|nl)/.._.+?)<");
                while (matcher.find()) {
                    processLink(matcher.group(1), list);
                }
            } else {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
        }
        // add urls to queue - let their plugins do the validation
        if (list.isEmpty()) throw new PluginImplementationException("No links found");
        getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(httpFile, list);
        httpFile.setState(DownloadState.COMPLETED);
        httpFile.getProperties().put("removeCompleted", true);
    }

    private void processLink(String Redirection_Link, List<URI> listing) throws Exception {
        try {//process redirection link to get final url
            final GetMethod method = getGetMethod(Redirection_Link);
            if (makeRedirectedRequest(method)) {
                listing.add(new URI(method.getURI().getURI()));
            }
        } catch (final Exception e) {
            LogUtils.processException(logger, e);
            throw new ServiceConnectionProblemException("Error retrieving link");
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String content = getContentAsString();
        if (content.contains("the link you have clicked is not available") || content.contains("Please select file") || content.contains("<h1>Not Found</h1>")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

    private void checkNameAndSize() throws ErrorDuringDownloadingException {
        final String content = getContentAsString();
        PlugUtils.checkName(httpFile, content, "color:#000000;\">", "<font");
        PlugUtils.checkFileSize(httpFile, content, ">(", ")<");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }


}