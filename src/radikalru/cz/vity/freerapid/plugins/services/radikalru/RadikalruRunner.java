package cz.vity.freerapid.plugins.services.radikalru;

import cz.vity.freerapid.plugins.exceptions.InvalidURLOrServiceProblemException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.methods.GetMethod;

import java.io.IOException;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * @author Alex
 */

class RadikalruRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(RadikalruRunner.class.getName());


    @Override
    public void run() throws Exception {
        super.run();
        fileURL = processURL(fileURL);
        logger.info("Starting download in TASK " + fileURL);
        checkName(fileURL);

        GetMethod getMethod = getGetMethod(fileURL);

                        if (!tryDownloadAndSaveFile(getMethod)) {
                            checkProblems();
                            logger.warning(getContentAsString());//something was really wrong, we will explore it from the logs :-)
                            throw new IOException("File input stream is empty.");
                        }

    }

    private String processURL(String mURL) throws Exception {
        if (mURL.contains("t.jpg")) {
            return mURL.replace("t.jpg",".jpg");
        } else return mURL;
    }
    
    private void checkName(String content) throws Exception {
        Matcher matcher = PlugUtils.matcher("http://[0-9a-z]+.radikal.ru/[0-9a-z]+/[0-9a-z]+/([0-9a-z]+.jpg)",content);
        if (matcher.find()) {
            httpFile.setFileName(matcher.group(1));
            logger.info("File Name: "+ matcher.group(1));
            httpFile.setFileState(FileState.CHECKED_AND_EXISTING);

        }



    }


    private void checkProblems() throws ServiceConnectionProblemException {
        if (getContentAsString().contains("already downloading")) {
            throw new ServiceConnectionProblemException(String.format("<b>SaveFile Error:</b><br>Your IP address is already downloading a file. <br>Please wait until the download is completed."));
        }
        if (getContentAsString().contains("Currently a lot of users")) {
            throw new ServiceConnectionProblemException(String.format("<b>SaveFile Error:</b><br>Currently a lot of users are downloading files."));
        }
    }

}