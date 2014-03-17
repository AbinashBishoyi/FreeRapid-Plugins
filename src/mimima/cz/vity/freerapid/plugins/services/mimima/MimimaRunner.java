package cz.vity.freerapid.plugins.services.mimima;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * @author Alex
 */
class MimimaRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(MimimaRunner.class.getName());

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting run task " + fileURL);
        final GetMethod method = getGetMethod(fileURL);
        logger.info(fileURL);
        if (makeRequest(method)) {
            String contentAsString = getContentAsString();
            client.setReferer("http://www6.mimima.com/fetch.php");
            final PostMethod pmethod = getPostMethod("http://www6.mimima.com/fetch.php");
            String code = PlugUtils.getParameter("code", contentAsString);
            logger.info("Code" + code);
            pmethod.addParameter("code", code);
            client.getHTTPClient().getParams().setParameter("noContentTypeInHeader", true);//take everything 
            if (!tryDownloadAndSaveFile(pmethod)) {
                checkProblems();
                logger.warning(getContentAsString());
                throw new IOException("File input stream is empty.");
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("was not found")) {
            throw new URLNotAvailableAnymoreException("The page you requested was not found in our database.");
        }
    }
}
