package cz.vity.freerapid.plugins.services.wowebookcom;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.net.URL;
import java.util.logging.Logger;

/**
 * Class which contains main code
 *
 * @author CrazyCoder, Abinash Bishoyi
 */
class WowEbookComFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(WowEbookComFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL);

        if (makeRedirectedRequest(method)) {
            checkProblems();
            final String start = "<a target=\"_blank\" href=\"";
            final String end = "\"  title=\"Download from PF\"";
            String directURL = "";
            for (int i = 1; i < 20; i++) {
                directURL = PlugUtils.getStringBetween(getContentAsString(), start, end, i);
                if (directURL == null) throw new ErrorDuringDownloadingException("Can't find download link");
                if (directURL.contains("adf.ly")) break;
                if (directURL.contains("q.gs")) break;
                if (directURL.contains("sh.st")) {
                    final HttpMethod httpMethod = getMethodBuilder()
                            .setReferer(fileURL)
                            .setAction(directURL)
                            .toGetMethod();
                    // sh.st redirects automatically for non-browser user agents
                    httpMethod.setRequestHeader("User-Agent", "curl/7.18.2 (i586-pc-mingw32msvc) libcurl/7.18.2 zlib/1.2.3");
                    final int st = client.makeRequest(httpMethod, false);
                    if (st / 100 == 3) {
                        final Header locationHeader = httpMethod.getResponseHeader("Location");
                        directURL = locationHeader.getValue();
                        break;
                    }
                }
            }
            logger.info("Download Service URL: " + directURL);
            httpFile.setNewURL(new URL(directURL));
            httpFile.setPluginID("");
            httpFile.setState(DownloadState.QUEUED);
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("File Not Found")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }
}
