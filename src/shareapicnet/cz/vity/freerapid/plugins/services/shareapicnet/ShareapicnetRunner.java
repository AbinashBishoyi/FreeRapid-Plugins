package cz.vity.freerapid.plugins.services.shareapicnet;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;

import java.io.IOException;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * @author Alex
 */
class ShareapicnetRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(ShareapicnetRunner.class.getName());

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting run task " + fileURL);
        GetMethod method = getGetMethod(fileURL);
        logger.info(fileURL);
        if (!makeRequest(method)) {
            checkProblems();
            throw new ServiceConnectionProblemException();

        }

        String contentAsString = getContentAsString();
        client.setReferer(fileURL);
        //final PostMethod pmethod = getPostMethod("http://www6.mimima.com/fetch.php");
        Matcher mMatch = PlugUtils.matcher("(http://images.shareapic.net/.+/(.+.jpg))", contentAsString);

        if (!mMatch.find()) {
            checkProblems();
            throw new ServiceConnectionProblemException();

        }
        httpFile.setFileName(mMatch.group(2));
        
        method = getGetMethod(mMatch.group(1));

        client.getHTTPClient().getParams().setParameter("noContentTypeInHeader", true);//take everything
        if (!tryDownloadAndSaveFile(method)) {
            checkProblems();
            logger.warning(getContentAsString());
            throw new IOException("File input stream is empty.");

        }
    }


    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("was not found")) {
            throw new URLNotAvailableAnymoreException("The page you requested was not found in our database.");
        }
    }
}
