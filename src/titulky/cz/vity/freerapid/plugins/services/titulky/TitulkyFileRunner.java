package cz.vity.freerapid.plugins.services.titulky;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.logging.Logger;

/**
 * Class which contains main code
 *
 * @author Vity
 */
class TitulkyFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(TitulkyFileRunner.class.getName());


    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);

        final HttpMethod loginMethod = getMethodBuilder().setAction("http://www.titulky.com/index.php").setParameter("Login", "Bob01").setParameter("Detail2", "").setParameter("Password", "0123456789").toPostMethod();
        if (!makeRedirectedRequest(loginMethod))  //we make the main request
            throw new PluginImplementationException();
        //index.php?welcome=
        final HttpMethod welcomeMethod = getMethodBuilder().setAction("http://www.titulky.com/index.php?welcome=").toGetMethod();
        if (!makeRedirectedRequest(welcomeMethod))  //we make the main request
            throw new PluginImplementationException();

        final GetMethod method = getGetMethod(fileURL); //create GET request
        if (makeRedirectedRequest(method)) { //we make the main request
            checkProblems();//check problems
            final String number = PlugUtils.getStringBetween(getContentAsString(), "//OpenDownload('", "','");
            client.getHTTPClient().getParams().setParameter("considerAsStream", "archive/zip");
            HttpMethod httpMethod = getMethodBuilder().setReferer(fileURL).setAction("http://www.titulky.com/idown.php?R=65456454545454545454654654466415454&titulky=" + number + "&zip=z&histstamp=").toHttpMethod();
            if (makeRedirectedRequest(httpMethod)) {
                downloadTask.sleep(PlugUtils.getNumberBetween(getContentAsString(), "CountDown(", ")"));
                final String id = PlugUtils.getStringBetween(getContentAsString(), "href=\"/idown.php?id=", "\"");
                httpMethod = getMethodBuilder().setAction("http://titulky.com//idown.php?id=" + id).toHttpMethod();
                //here is the download link extraction
                client.getHTTPClient().getParams().setBooleanParameter("noContentLengthAvailable", true);
                if (!tryDownloadAndSaveFile(httpMethod)) {
                    checkProblems();//if downloading failed
                    logger.warning(getContentAsString());//log the info
                    throw new PluginImplementationException();//some unknown problem
                }
            } else throw new PluginImplementationException();
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        //unknown errors

    }

}