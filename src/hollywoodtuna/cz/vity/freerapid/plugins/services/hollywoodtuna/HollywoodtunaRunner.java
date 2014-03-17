package cz.vity.freerapid.plugins.services.hollywoodtuna;

import cz.vity.freerapid.plugins.exceptions.InvalidURLOrServiceProblemException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.methods.GetMethod;

import java.io.IOException;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * @author Alex
 */
class HollywoodtunaRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(HollywoodtunaRunner.class.getName());

    //   @Override
//    public void runCheck() throws Exception {
//        super.runCheck();
//        final GetMethod getMethod = getGetMethod(fileURL);
//        if (makeRequest(getMethod)) {
//            checkNameandSize(getContentAsString());
//        } else
//            throw new PluginImplementationException();
//    }
//    //<div id="buttonz" align="center"> <a href="http://www4.indowebster.com/f8a03b1118fc754d82a8fe7d3be43278.rar" onclick="return poppop('addd.php')" class="hintanchor" onmouseover="showhint('Download link for Indonesia only.', this, event, '200px')">Download from IDWS</a> </div><center>

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        GetMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {
            final String contentAsString = getContentAsString();
            Matcher matcher = PlugUtils.matcher("href=\"http://www.hollywoodtuna.com\"><img src=\"(http://www.hollywoodtuna.com/[^\"]+)", contentAsString);
            if (matcher.find()) {
                String mUrl = matcher.group(1);
                if (mUrl.contains("bolGallery/medium2_")) {
                    mUrl = mUrl.replace("bolGallery/medium2_", "");
                }
                logger.info(mUrl);
                int index = mUrl.lastIndexOf('/');
                String fn = mUrl.substring(index + 1);
                logger.info("FileName : " + fn);
                httpFile.setFileName(fn);
                String myLink = mUrl;

                logger.info("Link : " + myLink);

                getMethod = getGetMethod(myLink);
                if (!tryDownloadAndSaveFile(getMethod)) {
                    checkProblems();
                    logger.warning(getContentAsString());//something was really wrong, we will explore it from the logs :-)
                    throw new IOException("File input stream is empty.");
                }
            } else {
                throw new PluginImplementationException("Cant find download link");//something is wrong with plugin
            }

        } else {
            throw new InvalidURLOrServiceProblemException("Cant load download link - ");
        }
    }
//
//    private void checkNameandSize(String content) throws Exception {
//
//        if (!content.contains("indowebster.com")) {
//            logger.warning(getContentAsString());
//            throw new InvalidURLOrServiceProblemException("Invalid URL or unindentified service");
//        }
//        if (content.contains("File doesn")) {
//            throw new URLNotAvailableAnymoreException("<b>Indowebster error:</b><br>File doesn't exist");
//        }
//
//        Matcher xmatcher = PlugUtils.matcher("<div class=\"views\"><b>Size :</b>(.+?)<", content);
//        if (xmatcher.find()) {
//            final String fileSize = xmatcher.group(1);
//            logger.info("File size " + fileSize);
//            httpFile.setFileSize(PlugUtils.getFileSizeFromString(fileSize));
//
//        } else logger.warning("File size was not found" + content);
//
//        xmatcher = PlugUtils.matcher("<div>Original name : &quot;<!--INFOLINKS_ON--> (.+?)<!--INFOLINKS_OFF-->&quot; <br><br />" , content);
//        if (xmatcher.find()) {
//            final String fileName = xmatcher.group(1).trim(); //method trim removes white characters from both sides of string
//            logger.info("File name :" + fileName);
//            httpFile.setFileName(fileName);
//
//        } else logger.warning("File name was not found" + content);
//
//
//        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
//
//    }


    private void checkProblems() throws ServiceConnectionProblemException {
        if (getContentAsString().contains("already downloading")) {
            throw new ServiceConnectionProblemException(String.format("<b>Hollywoodtuna Error:</b><br>Your IP address is already downloading a file. <br>Please wait until the download is completed."));
        }
        if (getContentAsString().contains("Currently a lot of users")) {
            throw new ServiceConnectionProblemException(String.format("<b>Hollywoodtuna Error:</b><br>Currently a lot of users are downloading files."));
        }
    }

}