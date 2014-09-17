package cz.vity.freerapid.plugins.services.coinurl;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.net.URL;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class CoinUrlFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(CoinUrlFileRunner.class.getName());

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL); //create GET request
        if (makeRedirectedRequest(method)) { //we make the main request
            final String content = getContentAsString();//check for response
            checkProblems();//check problems
            HttpMethod httpMethod = getGetMethod("http:" + PlugUtils.getStringBetween(content, "scr.src = '", "'+"));
            if (!makeRedirectedRequest(httpMethod)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            checkProblems();
            final String code = fileURL.substring(1 + fileURL.lastIndexOf("/"));
            final String ticket = PlugUtils.getStringBetween(getContentAsString(), "('", "')");
            httpMethod = getGetMethod(PlugUtils.getStringBetween(content, "ifr.src = \"", "\" ") + code + "&ticket=" + ticket + "&r=");
            if (!makeRedirectedRequest(httpMethod)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            checkProblems();
            httpMethod = getMethodBuilder().setActionFromIFrameSrcWhereTagContains("top").toGetMethod();
            if (!makeRedirectedRequest(httpMethod)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            checkProblems();
            final Matcher match = PlugUtils.matcher("id=\"long-url\"[^>]*?>[^<>]*?<[^>]+?>(.+?)</", getContentAsString());
            if (!match.find())
                throw new PluginImplementationException("redirect link not found");
            httpFile.setNewURL(new URL(match.group(1).trim()));
            httpFile.setPluginID("");
            httpFile.setState(DownloadState.QUEUED);
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("page you have requested does not exist")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
    }

}