package cz.vity.freerapid.plugins.services.metacafe;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.net.URLDecoder;
import java.util.Locale;
import java.util.logging.Logger;

/**
 * Class which contains main code
 *
 * @author ntoskrnl
 */
class MetaCafeFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(MetaCafeFileRunner.class.getName());

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
        PlugUtils.checkName(httpFile, getContentAsString(), "<h1 id=\"ItemTitle\" >", "<");
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

            String mediaData = URLDecoder.decode(PlugUtils.getStringBetween(getContentAsString(), "mediaData=", "&"), "UTF-8");

            //prefer HD
            final int index = mediaData.indexOf("highDefinition");
            if (index > -1) {
                logger.info("Grabbing HD version");
                mediaData = "{\"" + mediaData.substring(index + 14);
            }

            final String fileExt = PlugUtils.getStringBetween(mediaData, "{\"", "\"").toLowerCase(Locale.ENGLISH);
            httpFile.setFileName(httpFile.getFileName() + (fileExt.startsWith(".") ? fileExt : "." + fileExt));

            final String mediaURL = PlugUtils.getStringBetween(mediaData, "\"mediaURL\":\"", "\"").replace("\\", "");
            final String key = PlugUtils.getStringBetween(mediaData, "\"key\":\"", "\"");

            final HttpMethod httpMethod = getMethodBuilder().setAction(mediaURL).setParameter("__gda__", key).toGetMethod();

            client.getHTTPClient().getParams().setParameter("considerAsStream", "text/plain");
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
        if (getContentAsString().contains("the requested page was not found") || !getContentAsString().contains("<object")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
        if (getContentAsString().contains("An error occurred while processing your request")) {
            throw new ServiceConnectionProblemException("An error occurred while processing your request");
        }
    }

}