package cz.vity.freerapid.plugins.services.zippyshare;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.InvalidURLOrServiceProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;

import java.io.IOException;
import java.net.URLDecoder;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Vity
 * @since 0.82
 */
class ZippyShareFileRunner extends AbstractRunner {
    private static final Logger logger = Logger.getLogger(ZippyShareFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final HttpMethod httpMethod = getMethodBuilder().setAction(fileURL).toHttpMethod();

        if (makeRedirectedRequest(httpMethod)) {
            checkSeriousProblems();
            checkNameAndSize();
        } else {
            throw new InvalidURLOrServiceProblemException("Invalid URL or service problem");
        }
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        HttpMethod httpMethod = getMethodBuilder().setAction(fileURL).toHttpMethod();

        if (makeRedirectedRequest(httpMethod)) {
            checkAllProblems();
            checkNameAndSize();
            final String contentAsString = getContentAsString();
            String var = PlugUtils.getStringBetween(contentAsString, "var wannaplayagameofpong = '", "';");
            final String unescape = PlugUtils.getStringBetween(contentAsString, "= unescape(", ");");
            logger.info("unescape =" + unescape);
            final Matcher matcher = PlugUtils.matcher(".replace\\((/.+?/g?), \"(.+?)\"", unescape);
            int start = 0;
            while (matcher.find(start)) {
                final String g1 = matcher.group(1);
                final String g2 = matcher.group(2);
                if (g1.endsWith("g")) {
                    final String find = g1.substring(1, g1.length() - 2);
                    var = var.replaceAll(Pattern.quote(find), g2);
                } else {
                    final String find = g1.substring(1, g1.length() - 1);
                    var = var.replaceFirst(Pattern.quote(find), g2);
                }
                start = matcher.end();
            }

            final int number = PlugUtils.getNumberBetween(contentAsString, "substring(", ");");
            final String decodedURL = URLDecoder.decode(var, "UTF-8");
            logger.info("Decoded URL:" + decodedURL);
            httpMethod = getMethodBuilder().setReferer(fileURL).setAction(decodedURL.substring(number)).toHttpMethod();

            if (!tryDownloadAndSaveFile(httpMethod)) {
                checkAllProblems();
                logger.warning(getContentAsString());
                throw new IOException("File input stream is empty");
            }
        } else {
            throw new InvalidURLOrServiceProblemException("Invalid URL or service problem");
        }
    }

    private void checkSeriousProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();

        if (contentAsString.contains("The requsted file does not exist on this server")) {
            throw new URLNotAvailableAnymoreException("The requsted file does not exist on this server");
        }
    }

    private void checkAllProblems() throws ErrorDuringDownloadingException {
        checkSeriousProblems();
    }

    private void checkNameAndSize() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        PlugUtils.checkName(httpFile, contentAsString, "<strong>Name: </strong>", "<");
        PlugUtils.checkFileSize(httpFile, contentAsString, "Size: </strong>", "<");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }
}