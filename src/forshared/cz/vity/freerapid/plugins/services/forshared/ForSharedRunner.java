package cz.vity.freerapid.plugins.services.forshared;

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
//import java.net.URLDecoder;

/**
 * @author Alex
 */
class ForSharedRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(ForSharedRunner.class.getName());

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
        GetMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {
            String contentAsString = getContentAsString();
            checkNameandSize(contentAsString);

            //<a href="http://www.4shared.com/get/62042633/e92a8a5b/RATU_-_Aku_Pasti_Kembali_-_IndoTopSitecom.html" class="dbtn" tabindex="1"><span><span><font>Download Now</font>No virus detected</span></span></a>
            Matcher lMatcher = getMatcherAgainstContent("(http://www.4shared.com/get[^\"]+)");
            if (lMatcher.find()) {
                //System.out.println("TEST1 " + lMatcher.group(1));

                getMethod = getGetMethod(lMatcher.group(1));
                if (makeRedirectedRequest(getMethod)) {
                    //<a href='http://dc110.4shared.com/download/62042633/e92a8a5b/RATU_-_Aku_Pasti_Kembali_-_IndoTopSitecom.mp3?tsid=20081219-010049-377c6e2c'>Click here to download this file</a>
                    lMatcher = getMatcherAgainstContent("(http:[^']+)'>Click here to download this file");
                    if (lMatcher.find()) {
                        //System.out.println("TEST2 " + lMatcher.group(1));
                        getMethod = getGetMethod(lMatcher.group(1));
                        //System.out.println(lMatcher.group(1));
                        //Find delay
                        //DelayTimeSec'>10</b>
                        Matcher sMatcher = getMatcherAgainstContent("DelayTimeSec'>([0-9]+)<");
                        if (sMatcher.find()) {
                            String t = sMatcher.group(1);
                            int seconds = new Integer(t);
                            logger.info("wait - " + t);
                            downloadTask.sleep(seconds + 1);

                            if (!tryDownloadAndSaveFile(getMethod)) {
                                checkProblems();
                                logger.warning(getContentAsString());//something was really wrong, we will explore it from the logs :-)
                                throw new IOException("File input stream is empty.");
                            } //else throw new InvalidURLOrServiceProblemException("Can't find Page 3 Action");

                        } else throw new InvalidURLOrServiceProblemException("Delay time not found");

                    } else throw new InvalidURLOrServiceProblemException("Can't find download link");


                } else throw new InvalidURLOrServiceProblemException("Can't load download page");

            } else throw new InvalidURLOrServiceProblemException("Can't find download link");

            //getMatcherAgainstContent


        } else throw new InvalidURLOrServiceProblemException("Can't load download page");

    }

    private void checkNameandSize(String content) throws Exception {

        if (!content.contains("4shared.com")) {
            logger.warning(getContentAsString());
            throw new InvalidURLOrServiceProblemException("Invalid URL or unindentified service");
        }
        if (content.contains("File doesn")) {
            throw new URLNotAvailableAnymoreException("<b>4Shared error:</b><br>File doesn't exist");
        }
//<title>4shared.com - online file sharing and storage - download RATU - Aku Pasti Kembali - IndoTopSite.com.mp3</title>
        Matcher nMatcher = PlugUtils.matcher("download ([^\"]+)</title>", content);
        if (nMatcher.find()) {
            final String fileName = nMatcher.group(1).trim();
            logger.info("File name " + fileName);
            httpFile.setFileName(fileName.trim());


        } else logger.warning("File name was not found" + content);
        nMatcher = PlugUtils.matcher("(([0-9,.]+ .B))<", content);
        if (nMatcher.find()) {
            String fileSize = nMatcher.group(1);
            fileSize = fileSize.replaceAll(",", "");

            logger.info("File size " + fileSize);
            httpFile.setFileSize(PlugUtils.getFileSizeFromString(fileSize));
            //long x = PlugUtils.getFileSizeFromString(fileSize);
            //System.out.println(x);


        } else logger.warning("File size was not found" + content);


        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);

    }


    private void checkProblems() throws ServiceConnectionProblemException {
        if (getContentAsString().contains("already downloading")) {
            throw new ServiceConnectionProblemException("<b>4Shared Error:</b><br>Your IP address is already downloading a file. <br>Please wait until the download is completed.");
        }
        if (getContentAsString().contains("Currently a lot of users")) {
            throw new ServiceConnectionProblemException("<b>4Shared Error:</b><br>Currently a lot of users are downloading files.");
        }
    }

}