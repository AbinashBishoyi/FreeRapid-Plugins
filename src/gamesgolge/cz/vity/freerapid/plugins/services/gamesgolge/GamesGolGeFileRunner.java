package cz.vity.freerapid.plugins.services.gamesgolge;

import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpStatus;

import java.util.logging.Logger;

/**
 * Class which contains main code
 *
 * @author tong2shot
 */
class GamesGolGeFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(GamesGolGeFileRunner.class.getName());

    private void checkName() {
        final String fileName = fileURL.substring(fileURL.lastIndexOf("/") + 1);
        httpFile.setFileName(fileName);
    }

    @Override
    public void run() throws Exception {
        super.run();
        checkName();
        final HttpMethod method = getGetMethod(fileURL);
        if (!tryDownloadAndSaveFile(method)) {
            if (method.getStatusCode() == HttpStatus.SC_NOT_FOUND) {
                throw new URLNotAvailableAnymoreException("File not found");
            }
            throw new ServiceConnectionProblemException("Error starting download");
        }
    }

}