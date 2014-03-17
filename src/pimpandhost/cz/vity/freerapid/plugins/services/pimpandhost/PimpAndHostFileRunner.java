package cz.vity.freerapid.plugins.services.pimpandhost;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.net.URLDecoder;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author tong2shot
 */
class PimpAndHostFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(PimpAndHostFileRunner.class.getName());

    @Override
    public void run() throws Exception {
        super.run();
        checkURL();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            final String contentAsString = getContentAsString();
            checkProblems();
            final String imgUrl = PlugUtils.getStringBetween(contentAsString, "id=\"image\" src=\"", "\"/");
            final Matcher matcher = PlugUtils.matcher("/([^/]+\\.\\w+$)", imgUrl);
            if (!matcher.find()) {
                throw new PluginImplementationException("File name not found");
            }
            httpFile.setFileName(URLDecoder.decode(matcher.group(1), "UTF-8"));
            final HttpMethod httpMethod = getMethodBuilder().setReferer(fileURL).setAction(imgUrl).toHttpMethod();
            if (!tryDownloadAndSaveFile(httpMethod)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkURL() {
        if (!PlugUtils.find("-original\\.html$", fileURL)) {
            if (PlugUtils.find("\\.html$", fileURL)) {
                fileURL = fileURL.replaceFirst("-(small|medium)\\.html$", "-original.html");
            } else {
                fileURL = fileURL.replaceFirst("/show/id/(\\d+)$", "/$1-original.html");
            }
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("Image was removed")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

}