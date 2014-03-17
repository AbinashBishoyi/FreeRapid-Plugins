package cz.vity.freerapid.plugins.services.localhost;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;

import java.util.logging.Logger;

/**
 * Class which contains main code
 *
 * @author Vity
 */
class LocalhostFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(LocalhostFileRunner.class.getName());


    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        //here is the download link extraction
        if (fileURL.endsWith("/"))
            fileURL = fileURL.substring(0, fileURL.length() - 2);
        final int i = fileURL.lastIndexOf('/');
        if (i != -1) {
            httpFile.setFileName(fileURL.substring(i + 1));
        }
        if (!tryDownloadAndSaveFile(getGetMethod(fileURL))) {
            checkProblems();//if downloading failed
            logger.warning(getContentAsString());//log the info
            throw new PluginImplementationException();//some unknown problem
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
//        final String contentAsString = getContentAsString();
//        if (contentAsString.contains("File Not Found")) {
//            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
//        }
    }

}
