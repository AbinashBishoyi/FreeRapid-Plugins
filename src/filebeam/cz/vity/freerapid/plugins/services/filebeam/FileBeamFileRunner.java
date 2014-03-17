package cz.vity.freerapid.plugins.services.filebeam;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author ntoskrnl
 */
class FileBeamFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(FileBeamFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        checkURL();
        final GetMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            if (!isPassworded()) checkNameAndSize();
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkURL() {
        final int length = fileURL.length();
        if (length > 5) {//to avoid StringIndexOutOfBounds
            if (fileURL.charAt(length - 4) == '.') {
                fileURL = fileURL.substring(0, length - 4);
            }
        }
    }

    private boolean isPassworded() {
        return getContentAsString().contains("This File is Password Protected");
    }

    private void checkNameAndSize() throws ErrorDuringDownloadingException {
        final String content = getContentAsString();
        PlugUtils.checkName(httpFile, content, "<h3>", "</h3>");
        PlugUtils.checkFileSize(httpFile, content, "File Size - ", " <br");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String content = getContentAsString();
        if (content.contains("This file was either delete")) {
            throw new URLNotAvailableAnymoreException("This file was either deleted by the uploader, or the file has expired.");
        }
    }

    @Override
    public void run() throws Exception {
        super.run();
        checkURL();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();

            if (isPassworded()) {
                while (isPassworded()) {
                    final HttpMethod httpMethod = getMethodBuilder().setReferer(fileURL).setBaseURL("http://" + httpFile.getFileUrl().getHost()).setActionFromFormByIndex(1, true).setParameter("password", getPassword()).toPostMethod();
                    if (!makeRedirectedRequest(httpMethod))
                        throw new ServiceConnectionProblemException("Error posting password");
                }
            }

            checkNameAndSize();

            //can't use setActionFromAHrefWhereATagContains() because of no quotes around href
            final Matcher matcher = getMatcherAgainstContent("<a href=(http://(?:www\\.)?filebeam\\.com/download[^<>]+?)>");
            if (!matcher.find()) throw new PluginImplementationException("Download link not found");
            final HttpMethod httpMethod = getMethodBuilder().setReferer(fileURL).setAction(matcher.group(1)).toGetMethod();

            if (!tryDownloadAndSaveFile(httpMethod)) {
                checkProblems();
                logger.warning(getContentAsString());
                throw new ServiceConnectionProblemException("Error starting download");
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private String getPassword() throws Exception {
        FileBeamPasswordUI ps = new FileBeamPasswordUI();
        if (getDialogSupport().showOKCancelDialog(ps, "Secured file on FileBeam")) {
            return (ps.getPassword());
        } else throw new NotRecoverableDownloadException("This file is secured with a password");
    }

}