package cz.vity.freerapid.plugins.services.toshared;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;

import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;
import java.util.logging.Logger;

/**
 * @author Tiago Hillebrandt <tiagohillebrandt@gmail.com>, ntoskrnl
 */
class ToSharedRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(ToSharedRunner.class.getName());

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

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download is TASK " + fileURL);
        HttpMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkNameAndSize();
            String url = PlugUtils.getStringBetween(getContentAsString(), "$.get('", "',");
            String l2surl = url.substring(url.length() - 32);
            int viw = Calendar.getInstance(TimeZone.getTimeZone("GMT"), Locale.ENGLISH).get(Calendar.DAY_OF_WEEK);
            if (l2surl.charAt(0) % 2 == 1) {
                l2surl = l2surl.substring(0, viw) + l2surl.substring(16 + viw);
            } else {
                l2surl = l2surl.substring(0, 16 - viw) + l2surl.substring(l2surl.length() - viw, viw);
            }
            url = url.substring(0, url.indexOf("id=") + 3) + l2surl;
            method = getMethodBuilder().setReferer(fileURL).setBaseURL("http://www.2shared.com").setAction(url).toGetMethod();
            method.addRequestHeader("X-Requested-With", "XMLHttpRequest");
            if (makeRedirectedRequest(method)) {
                final String content = getContentAsString().trim();
                if (content.isEmpty() || content.equals("#")) {
                    throw new PluginImplementationException("Download link not found");
                }
                method = getMethodBuilder().setReferer(fileURL).setAction(content).toGetMethod();
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

    private void checkNameAndSize() throws Exception {
        PlugUtils.checkName(httpFile, getContentAsString(), "download", "</title>");
        final String fileSize = PlugUtils.getStringBetween(getContentAsString(), "File size:</span>\n", "&nbsp;");
        httpFile.setFileSize(PlugUtils.getFileSizeFromString(fileSize.replace(",", "")));
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String content = getContentAsString();
        if (content.contains("The file link that you requested is not valid")) {
            throw new URLNotAvailableAnymoreException("The file link that you requested is not valid");
        }
        if (content.contains("User downloading session limit is reached")) {
            throw new ServiceConnectionProblemException("Your IP address is already downloading a file or your session limit is reached! Try again in a few minutes.");
        }
    }
}
