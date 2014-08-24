package cz.vity.freerapid.plugins.services.itebooks;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadClientConsts;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author tong2shot
 * @since 0.9u2
 */
class ItEbooksFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(ItEbooksFileRunner.class.getName());

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
        String filename;
        try {
            filename = PlugUtils.getStringBetween(content, "<h1 itemprop=\"name\">", "</h1>").trim();
        } catch (PluginImplementationException e) {
            throw new PluginImplementationException("Filename not found");
        }
        try {
            filename += " - " + PlugUtils.getStringBetween(content, "<h3>", "</h3>").trim();
        } catch (PluginImplementationException e) {
            //
        }
        filename += ".pdf";
        httpFile.setFileName(filename);
        PlugUtils.checkFileSize(httpFile, content, "File size:</td><td><b>", "</b>");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            fileURL = method.getURI().toString();
            final String contentAsString = getContentAsString();
            checkProblems();
            checkNameAndSize(contentAsString);
            MethodBuilder mb;
            Matcher matcher = getMatcherAgainstContent("<a href='(http://filepi\\.com/[^']+?)'");
            if (!matcher.find()) {
                throw new PluginImplementationException("Download link not found");
            }
            mb = getMethodBuilder().setReferer(fileURL).setAction(matcher.group(1));
            setClientParameter(DownloadClientConsts.DONT_USE_HEADER_FILENAME, true);
            if (!tryDownloadAndSaveFile(mb.toHttpMethod())) {
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
        if (contentAsString.contains("Page Not Found")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

}