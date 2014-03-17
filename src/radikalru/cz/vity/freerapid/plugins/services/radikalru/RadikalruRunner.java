package cz.vity.freerapid.plugins.services.radikalru;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import org.apache.commons.httpclient.methods.GetMethod;

import java.net.URLDecoder;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * @author Alex
 * @author tong2shot
 */

//URL that ends with t.jpg is thumbnail image, we don't want it to be downloaded, see plugin.xml
class RadikalruRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(RadikalruRunner.class.getName());

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        GetMethod getMethod;
        final String imgUrl;
        if (fileURL.endsWith(".jpg")) {
            imgUrl = fileURL;
        } else {
            getMethod = getGetMethod(fileURL);
            if (!makeRedirectedRequest(getMethod)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            checkProblems();
            final Matcher matcher = getMatcherAgainstContent("MainImg\\.Url\\s*=\\s*'(.*?)'");
            if (!matcher.find()) throw new PluginImplementationException("Image url not found");
            imgUrl = matcher.group(1);
            if (imgUrl.isEmpty()) throw new URLNotAvailableAnymoreException("Image not found");
        }
        final String filename = URLDecoder.decode(imgUrl.substring(imgUrl.lastIndexOf("/") + 1), "UTF-8");
        httpFile.setFileName(filename);
        getMethod = getGetMethod(imgUrl);
        if (!tryDownloadAndSaveFile(getMethod)) {
            checkProblems();
            logger.warning(getContentAsString());
            throw new ServiceConnectionProblemException();
        }

    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        if (getContentAsString().contains("already downloading")) {
            throw new ServiceConnectionProblemException(String.format("<b>SaveFile Error:</b><br>Your IP address is already downloading a file. <br>Please wait until the download is completed."));
        }
        if (getContentAsString().contains("Currently a lot of users")) {
            throw new ServiceConnectionProblemException(String.format("<b>SaveFile Error:</b><br>Currently a lot of users are downloading files."));
        }
        if (getContentAsString().contains("might have been removed")) {
            throw new URLNotAvailableAnymoreException("Image not found");
        }
    }

}