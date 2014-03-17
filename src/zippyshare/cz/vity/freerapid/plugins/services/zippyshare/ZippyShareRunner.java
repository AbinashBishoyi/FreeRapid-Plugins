package cz.vity.freerapid.plugins.services.zippyshare;

import cz.vity.freerapid.plugins.exceptions.InvalidURLOrServiceProblemException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.methods.GetMethod;

import java.io.IOException;
import java.net.URLDecoder;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * @author Alex
 */
class ZippyShareRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(ZippyShareRunner.class.getName());
    public String mLink;

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRequest(getMethod)) {
            checkNameandSize(getContentAsString());
        } else {
            throw new PluginImplementationException();
        }
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod getMethoda = getGetMethod(fileURL);
        //getMethoda.setFollowRedirects(true);
        //you can now use makeRedirectedRequest() for working with redirects
        if (makeRedirectedRequest(getMethoda)) {
            final String contentAsString = getContentAsString();
            checkNameandSize(contentAsString);
            client.setReferer(mLink);

            final GetMethod getMethod = getGetMethod(mLink);
            if (!tryDownloadAndSaveFile(getMethod)) {
                checkProblems();
                logger.warning(getContentAsString());//something was really wrong, we will explore it from the logs :-)
                throw new IOException("File input stream is empty.");
            }
        } else {
            throw new PluginImplementationException();//something is wrong with plugin
        }
    }

    private void checkNameandSize(String content) throws Exception {

        if (!content.contains("zippyshare.com")) {
            logger.warning(getContentAsString());
            throw new InvalidURLOrServiceProblemException("Invalid URL or unindentified service");
        }
        if (content.contains("File does not exist")) {
            throw new URLNotAvailableAnymoreException("<b>Zippyshare error:</b><br>File doesn't exist");
        }
//<strong>Name: </strong>Jordi Villalta - Amnesia Terrace (Original mix)...</font><br>
//             <font style="line-height: 16px; font-size: 11px; color: rgb(0, 0, 0); text-decoration: none;"><strong>Size: </strong>22.02 MB</font><br>

        Matcher xmatcher = PlugUtils.matcher("<strong>Size: </strong>((.+?)+)</", content);
        if (xmatcher.find()) {
            final String fileSize = xmatcher.group(1);
            logger.info("File size " + fileSize);
            httpFile.setFileSize(PlugUtils.getFileSizeFromString(fileSize));

        } else {
            logger.warning("File size was not found" + content);
        }

        xmatcher = PlugUtils.matcher("Name: </strong>((.+?)+)</", content);
        if (xmatcher.find()) {
            final String fileName = xmatcher.group(1).trim(); //method trim removes white characters from both sides of string
            logger.info("File name " + fileName);
            httpFile.setFileName(fileName);

        } else {
            logger.warning("File name was not found" + content);
        }

        xmatcher = PlugUtils.matcher("unescape\\('((.+?)+)'", content);
        if (xmatcher.find()) {
            mLink = URLDecoder.decode(xmatcher.group(1), "UTF-8");
            logger.info("Download Link: " + mLink);
        } else {
            throw new PluginImplementationException("Can't find download link");
        }
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);

    }


    private void checkProblems() throws ServiceConnectionProblemException {
        if (getContentAsString().contains("already downloading")) {
            throw new ServiceConnectionProblemException(String.format("<b>ZippyShare Error:</b><br>Your IP address is already downloading a file. <br>Please wait until the download is completed."));
        }
        if (getContentAsString().contains("Currently a lot of users")) {
            throw new ServiceConnectionProblemException(String.format("<b>ZippyShare Error:</b><br>Currently a lot of users are downloading files."));
        }
    }

}