package cz.vity.freerapid.plugins.services.fileape;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author tong2shot
 */
class FileApeFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(FileApeFileRunner.class.getName());


    private String transformURL(String url) {
        String tempURL = url;
        if (tempURL.contains("/dl/")) {
            Matcher matcher = PlugUtils.matcher("([^/]+)$", tempURL);
            matcher.find();
            tempURL = "http://fileape.com/index.php?act=download&id="+matcher.group(1);
        }
        return tempURL;
    }


    @Override
    public void run() throws Exception {
        super.run();
        String tempURL = transformURL(fileURL);
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(tempURL+"&g=1"); //create GET request
        if (makeRedirectedRequest(method)) { //we make the main request
            checkProblems();//check problems
            final int waitTime = PlugUtils.getWaitTimeBetween(getContentAsString(),"wait = ",";", TimeUnit.SECONDS);
            downloadTask.sleep(waitTime);
            
            tempURL = method.getURI().toString();
            final String actionURL = PlugUtils.getStringBetween(getContentAsString(),"window.location = '","';");
            //logger.info(actionURL);
            HttpMethod httpMethod =getMethodBuilder()
                    .setReferer(tempURL)
                    .setAction(actionURL)
                    .toGetMethod();
            if (!makeRedirectedRequest(httpMethod)) {
                checkProblems();
                throw new PluginImplementationException();
            }
            tempURL = httpMethod.getURI().toString();

            httpMethod = getMethodBuilder()
                    .setReferer(tempURL)
                    .setActionFromAHrefWhereATagContains("http://fileape.com/img/click.png")
                    .toGetMethod();
            //logger.info("Final URL : "+httpMethod.getURI().toString());
            
            //here is the download link extraction
            if (!tryDownloadAndSaveFile(httpMethod)) {
                checkProblems();//if downloading failed
                throw new ServiceConnectionProblemException("Error starting download");//some unknown problem
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("does not exist")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
    }

}