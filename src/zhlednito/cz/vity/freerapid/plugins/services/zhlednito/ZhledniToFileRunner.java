package cz.vity.freerapid.plugins.services.zhlednito;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;

import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author JPEXS
 */
class ZhledniToFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(ZhledniToFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception { //this method validates file
        super.runCheck();
        //     checkURL();
        checkWww();
        final HttpMethod getMethod = getMethodBuilder().setAction(fileURL).toHttpMethod();//make first request
        if (makeRedirectedRequest(getMethod)) {
            String contentAsString = getContentAsString();//check for response
            while (contentAsString.startsWith("<center><a href=\"http://www.netagent.cz\">")) {
                makeRedirectedRequest(getMethod);
                contentAsString = getContentAsString();
            }
            checkProblems();
            checkNameAndSize(getContentAsString());//ok let's extract file name and size from the page
        } else
            throw new PluginImplementationException();
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        Matcher m = PlugUtils.matcher(".*/w/(.*)-id[0-9]+\\.html", fileURL);
        if (m.matches()) {
            httpFile.setFileName(m.group(1) + extractExt(extractDownloadURL()));
            httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
        }
    }

    private void checkWww() {
        //inserting www, otherwise it won't work
        if (!fileURL.toLowerCase().startsWith("http://www.")) {
            fileURL = "http://www." + fileURL.substring(7);
            logger.info("Adding WWW " + fileURL);
        }
    }

//    /**
//     * Replacing UTF8 escape sequences in URL.
//     * Needed, otherwise it will fail
//     */
//    private void checkURL() {
//        try {
//            fileURL = URLDecoder.decode(fileURL, "utf8");
//        } catch (UnsupportedEncodingException uex) {
//            //ignore
//        }
//    }

    private String extractExt(String filename) {
        return filename.substring(filename.lastIndexOf("."));
    }

    private String extractDownloadURL() throws PluginImplementationException {
        return PlugUtils.getStringBetween(getContentAsString(), "hnout <a href=\"", "\"");
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
//        checkURL();
        checkWww();
        final HttpMethod method = getMethodBuilder().setAction(fileURL).toHttpMethod(); //create GET request
        if (makeRedirectedRequest(method)) { //we make the main request
            String contentAsString = getContentAsString();//check for response
            while (contentAsString.startsWith("<center><a href=\"http://www.netagent.cz\">")) {
                makeRedirectedRequest(method);
                contentAsString = getContentAsString();
            }
            checkProblems();//check problems
            checkNameAndSize(contentAsString);//extract file name and size from the page
            //client.getHTTPClient().getParams().setBooleanParameter("dontUseHeaderFilename",true);
            final HttpMethod httpMethod = getMethodBuilder().setReferer(fileURL).setAction(extractDownloadURL()).toHttpMethod();

            do {
                if (tryDownloadAndSaveFile(httpMethod)) return;
                contentAsString = getContentAsString();
            } while (contentAsString.startsWith("<center><a href=\"http://www.netagent.cz\">"));

            checkProblems();//if downloading failed
            logger.warning(getContentAsString());//log the info
            throw new PluginImplementationException();//some unknown problem
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        //final String contentAsString = getContentAsString();
        if (extractDownloadURL().endsWith("/")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
    }

}