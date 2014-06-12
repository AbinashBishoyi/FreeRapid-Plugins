package cz.vity.freerapid.plugins.services.cloudstores;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.util.URIUtil;

import java.net.URI;
import java.net.URLDecoder;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author ntoskrnl
 */
class CloudStoresFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(CloudStoresFileRunner.class.getName());

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

    private void checkNameAndSize() throws ErrorDuringDownloadingException {
        final Matcher matcher = getMatcherAgainstContent("(?s)<h1>(.+?)</h1>.*?\\|(.+?)</");
        if (!matcher.find()) {
            throw new PluginImplementationException("File name/size not found");
        }
        httpFile.setFileName(matcher.group(1));
        httpFile.setFileSize(PlugUtils.getFileSizeFromString(matcher.group(2)));
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        HttpMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkNameAndSize();
            Matcher matcher = getMatcherAgainstContent("url: '(.+?)',\\s*data: \\{(.+?)\\}");
            if (!matcher.find()) {
                throw new PluginImplementationException("Download parameters not found");
            }
            final MethodBuilder mb = getMethodBuilder().setAjax().setReferer(fileURL)
                    .setBaseURL("http://cloudstor.es").setAction(matcher.group(1));
            matcher = PlugUtils.matcher("([a-z]+?): '(.+?)'", matcher.group(2));
            while (matcher.find()) {
                mb.setParameter(matcher.group(1), matcher.group(2));
            }
            method = mb.toPostMethod();
            if (makeRedirectedRequest(method)) {
                final String url = getContentAsString().trim();
                final String path = new URI(URIUtil.encodePathQuery(url)).getPath();
                httpFile.setFileName(URLDecoder.decode(path.substring(path.lastIndexOf('/') + 1), "UTF-8"));
                method = getMethodBuilder().setReferer(fileURL).setAction(url).toGetMethod();
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

    private void checkProblems() throws ErrorDuringDownloadingException {
        if (getContentAsString().contains("The page you requested could not be found")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
        if (getContentAsString().contains("You are currently downloading a file from this server")) {
            throw new ServiceConnectionProblemException("You are currently downloading a file from this server");
        }
    }

}