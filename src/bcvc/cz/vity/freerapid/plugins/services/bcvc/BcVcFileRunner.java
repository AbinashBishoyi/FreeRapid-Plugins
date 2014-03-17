package cz.vity.freerapid.plugins.services.bcvc;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadState;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.net.URL;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author tong2shot
 */
class BcVcFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(BcVcFileRunner.class.getName());

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            final Matcher matcher = getMatcherAgainstContent("aid\\:(.*?)\\,lid\\:(.*?)\\,oid\\:(.*?)\\,ref\\: ?'(.*?)'");
            if (!matcher.find()) {
                throw new PluginImplementationException("Cannot find parameters");
            }
            final String aid = matcher.group(1);
            final String lid = matcher.group(2);
            final String oid = matcher.group(3);
            final String ref = matcher.group(4);

            final MethodBuilder methodBuilder = getMethodBuilder()
                    .setReferer(fileURL)
                    .setAjax()
                    .setAction("http://bc.vc/fly/ajax.fly.php")
                    .setParameter("opt", "check_log")
                    .setParameter("args[aid]", aid)
                    .setParameter("args[lid]", lid)
                    .setParameter("args[oid]", oid)
                    .setParameter("args[ref]", ref);
            HttpMethod httpMethod = methodBuilder.toPostMethod();
            if (!makeRedirectedRequest(httpMethod)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            downloadTask.sleep(8);
            methodBuilder.setParameter("opt", "make_log");
            httpMethod = methodBuilder.toPostMethod();
            if (!makeRedirectedRequest(httpMethod)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            final String url = PlugUtils.getStringBetween(getContentAsString(), "url\":\"", "\"").replace("\\/", "/");
            httpFile.setNewURL(new URL(url));
            httpFile.setPluginID("");
            httpFile.setState(DownloadState.QUEUED);
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("404 Not Found")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

}