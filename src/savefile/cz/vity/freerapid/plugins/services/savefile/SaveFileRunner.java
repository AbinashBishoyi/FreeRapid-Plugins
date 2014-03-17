package cz.vity.freerapid.plugins.services.savefile;

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
class SaveFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(SaveFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRequest(getMethod)) {
            checkNameandSize(getContentAsString());
        } else
            throw new PluginImplementationException();
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

            //            //<div class="download"><a href="/download/1918981?PHPSESSID=79692e667cdfa171fcf6c90e7d69315c"></a></div>
            Matcher matcher = PlugUtils.matcher("<div class=\"download\"><a href=\"([^\"<]*)", contentAsString);
            if (matcher.find()) {
                String MyPHP = "http://www.savefile.com" + matcher.group(1);
                final GetMethod getPHP = getGetMethod(MyPHP);
                //getPHP.setFollowRedirects(true);

                //go on the next webpage (no redirection is being used)
                if (makeRequest(getPHP)) {
                    final String PHPString = getContentAsString();//read its content
                    //If it does not, try	<a href="http://dl4u.savefile.com/a75b81f7d82cd659974e50fa7527acdf/sfdrvrem.zip">Download file now</a>
                    matcher = PlugUtils.matcher("href=\"([^\"]*)\">Download file now", PHPString); //parses it
                    //matcher = PlugUtils.matcher("(Download file now)", PHPString);
                    if (matcher.find()) {
                        String s = matcher.group(1);
                        logger.info("Found File URL - " + s);
                        client.setReferer(s);
                        client.getHTTPClient().getParams().setBooleanParameter("noContentTypeInHeader", true);
                        final GetMethod getMethod = getGetMethod(s);
                        if (!tryDownloadAndSaveFile(getMethod)) {
                            checkProblems();
                            logger.warning(getContentAsString());//something was really wrong, we will explore it from the logs :-)
                            throw new IOException("File input stream is empty.");
                        }
                    } else throw new PluginImplementationException();//something is wrong with plugin
                } else throw new InvalidURLOrServiceProblemException("Cant find download link - " + getPHP);
            } else throw new InvalidURLOrServiceProblemException("Cant find php download link");
        } else throw new InvalidURLOrServiceProblemException("Cant get main URL");
    }

    private void checkNameandSize(String content) throws Exception {

        if (!content.contains("savefile.com")) {
            logger.warning(getContentAsString());
            throw new InvalidURLOrServiceProblemException("Invalid URL or unindentified service");
        }
        if (content.contains("File doesn")) {
            throw new URLNotAvailableAnymoreException("<b>SaveFile error:</b><br>File doesn't exist");
        }

        Matcher xmatcher = PlugUtils.matcher("Filesize: ([0-9.]*[K|M]B)", content);
        if (xmatcher.find()) {
            final String fileSize = xmatcher.group(1);
            logger.info("File size " + fileSize);
            httpFile.setFileSize(PlugUtils.getFileSizeFromString(fileSize));

        } else logger.warning("File size was not found" + content);

        xmatcher = PlugUtils.matcher("Filename: ([^<]*)", content);
        if (xmatcher.find()) {
            final String fileName = xmatcher.group(1).trim(); //method trim removes white characters from both sides of string
            logger.info("File name " + fileName);
            httpFile.setFileName(fileName);

        } else logger.warning("File name was not found" + content);


        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);

    }


    private void checkProblems() throws ServiceConnectionProblemException {
        if (getContentAsString().contains("already downloading")) {
            throw new ServiceConnectionProblemException("<b>SaveFile Error:</b><br>Your IP address is already downloading a file. <br>Please wait until the download is completed.");
        }
        if (getContentAsString().contains("Currently a lot of users")) {
            throw new ServiceConnectionProblemException("<b>SaveFile Error:</b><br>Currently a lot of users are downloading files.");
        }
    }

}