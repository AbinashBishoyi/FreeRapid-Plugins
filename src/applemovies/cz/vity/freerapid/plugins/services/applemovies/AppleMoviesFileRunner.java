package cz.vity.freerapid.plugins.services.applemovies;

import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.logging.Logger;

/**
 * Class which contains main code
 *
 * @author ntoskrnl
 */
class AppleMoviesFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(AppleMoviesFileRunner.class.getName());

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);

        final int lastIndex = fileURL.lastIndexOf("/") + 1;
        final String firstPartOfURL = fileURL.substring(0, lastIndex);
        final String lastPartOfURL = fileURL.substring(lastIndex);

        httpFile.setFileName(lastPartOfURL);

        String downloadURL = fileURL;
        if (!lastPartOfURL.contains("_h")) {
            downloadURL = firstPartOfURL + (lastPartOfURL.replace("_", "_h"));
        }

        //cannot use getGetMethod(), custom user agent is necessary
        final GetMethod method = new GetMethod(downloadURL);
        method.setRequestHeader("User-Agent", "QuickTime/7.6.5 (qtver=7.6.5;os=Windows NT 5.1Service Pack 3)");

        logger.info("Downloading from " + downloadURL);
        if (!tryDownloadAndSaveFile(method)) {
            throw new ServiceConnectionProblemException("Error starting download");
        }
    }

}