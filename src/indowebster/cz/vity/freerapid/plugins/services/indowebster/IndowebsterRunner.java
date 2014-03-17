package cz.vity.freerapid.plugins.services.indowebster;

import cz.vity.freerapid.plugins.exceptions.InvalidURLOrServiceProblemException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * @author JPEXS
 */
class IndowebsterRunner extends AbstractRunner {

    private static final String SERVICE_WEB = "http://www.indowebster.com/";
    private final static Logger logger = Logger.getLogger(IndowebsterRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);
        makeRequest(getMethod);
        checkNameandSize(getContentAsString());
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method1 = getGetMethod(fileURL);


        if (makeRedirectedRequest(method1)) {
            checkNameandSize(getContentAsString());
            Matcher m = getMatcherAgainstContent("href=\"(download=[^\"]+)\"");
            if (m.find()) {
                final HttpMethod method2 = getMethodBuilder(getContentAsString()).setBaseURL(SERVICE_WEB).setAction(m.group(1)).toGetMethod();
                if (makeRedirectedRequest(method2)) {
                    final HttpMethod method3 = getMethodBuilder(getContentAsString()).setBaseURL(SERVICE_WEB).setActionFromFormByName("form1", true).toPostMethod();
                    if (makeRedirectedRequest(method3)) {
                        Header hRefresh = method3.getResponseHeader("refresh");
                        if (hRefresh == null)
                            throw new PluginImplementationException("No refresh header");
                        String refreshVal = hRefresh.getValue();
                        refreshVal = refreshVal.substring(refreshVal.indexOf("url=") + 4);
                        GetMethod method4 = getGetMethod(refreshVal);
                        if (!tryDownloadAndSaveFile(method4)) {
                            checkProblems();
                            throw new ServiceConnectionProblemException("Error starting download");
                        }
                    } else {
                        throw new PluginImplementationException("Cannot connect to third link");
                    }
                } else {
                    throw new PluginImplementationException("Cannot connect to second link");
                }
            } else {
                throw new PluginImplementationException("Cannot find to second link");
            }
        } else {
            throw new ServiceConnectionProblemException();
        }

    }

    private void checkNameandSize(String content) throws Exception {

        if (!content.contains("indowebster.com")) {
            checkProblems();
            logger.warning(getContentAsString());
            throw new InvalidURLOrServiceProblemException("Invalid URL or unindentified service");
        }
        if (getContentAsString().contains("reported and removed")) {
            throw new URLNotAvailableAnymoreException(String.format("<b>Indowebster Error:</b><br>This files has been reported and removed due to terms of use violation"));
        }
        if (content.contains("File doesn")) {
            throw new URLNotAvailableAnymoreException("<b>Indowebster error:</b><br>File doesn't exist");
        }
        Matcher m1 = getMatcherAgainstContent("<b> *Original name[^:]*:[^<]*</b>[^<]*<!--INFOLINKS_ON--> *([^<]+)<");
        Matcher m2 = getMatcherAgainstContent("<b> *Original name[^:]*:[^<]*</b> *([^<]+)<");

        if (m1.find()) {
            httpFile.setFileName(m1.group(1));
        } else if (m2.find()) {
            httpFile.setFileName(m2.group(1));
        } else {
            throw new PluginImplementationException("File name not found");
        }

        Matcher m = getMatcherAgainstContent("<b> *Size[^:]*:[^<]*</b> *([^<]+)</div>");
        if (m.find()) {
            httpFile.setFileSize(PlugUtils.getFileSizeFromString(m.group(1)));
        } else {
            throw new PluginImplementationException("File size not found");
        }
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);

    }

    private void checkProblems() throws ServiceConnectionProblemException {
        if (getContentAsString().contains("<b>Warning</b>:  session_start()")) {
            throw new ServiceConnectionProblemException(String.format("<b>Indowebster Error:</b><br>PHP session error."));
        }
        if (getContentAsString().contains("already downloading")) {
            throw new ServiceConnectionProblemException(String.format("<b>Indowebster Error:</b><br>Your IP address is already downloading a file. <br>Please wait until the download is completed."));
        }
        if (getContentAsString().contains("Currently a lot of users")) {
            throw new ServiceConnectionProblemException(String.format("<b>Indowebster Error:</b><br>Currently a lot of users are downloading files."));
        }
    }
}
