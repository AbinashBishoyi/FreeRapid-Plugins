package cz.vity.freerapid.plugins.services.gametrailers;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.logging.Logger;

/**
 * Class which contains main code
 *
 * @author Vity
 */
class GameTrailersFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(GameTrailersFileRunner.class.getName());

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final HttpMethod loginMethod = getMethodBuilder().setAction("http://www.gametrailers.com/users/Bob01/gamepad/?un=Bob01&un=Bob01&un=Bob01&un=Bob01&").setParameter("username", "Bob01").setParameter("password", "bob01").toPostMethod();
        if (!makeRedirectedRequest(loginMethod)) //it will set cookies
            throw new PluginImplementationException("Couldn't log in to");
        final GetMethod method = getGetMethod(fileURL); //create GET request
        if (makeRedirectedRequest(method)) { //we make the main request
//            final String contentAsString = getContentAsString();//check for response
            checkProblems();//check problems
            final MethodBuilder methodBuilder = getMethodBuilder().setReferer(fileURL).setActionFromAHrefWhereATagContains("WMV");
            final HttpMethod httpMethod = methodBuilder.toHttpMethod();
            final String action = methodBuilder.getAction();
            if (!action.isEmpty()) {
                client.getHTTPClient().getParams().setBooleanParameter("dontUseHeaderFilename", true);
                httpFile.setFileName(action.substring(action.lastIndexOf('/') + 1));
            }

            //here is the download link extraction
            if (!tryDownloadAndSaveFile(httpMethod)) {
                checkProblems();//if downloading failed
                logger.warning(getContentAsString());//log the info
                throw new PluginImplementationException();//some unknown problem
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("<title>404")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
        if (contentAsString.contains("You must be logged in to download movies")) {
            throw new NotRecoverableDownloadException("You must be logged in to download movies");
        }
    }

}