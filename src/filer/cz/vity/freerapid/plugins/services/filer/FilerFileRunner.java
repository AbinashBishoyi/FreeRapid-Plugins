package cz.vity.freerapid.plugins.services.filer;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadState;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * @author benpicco
 */
class FilerFileRunner extends AbstractRunner {
    private final static String HTTP_BASE = "http://www.filer.net";
    
    private static final Logger logger = Logger.getLogger(FilerFileRunner.class.getName());
       
    @Override
    public void run() throws Exception {
        super.run();
               
        if(fileURL.contains("/get/"))
            processFile();
        else if (fileURL.contains("/folder/"))
            processFolder();
        else
            throw new InvalidURLOrServiceProblemException("Invalid URL"); 
        
    }
        
    private void processFile() throws Exception {
        logger.info("Starting download in TASK " + fileURL);
        
        final GetMethod getMethod = new GetMethod(fileURL);
        do {
           ;//makeRequest(getMethod);
        } while(!stepCaptcha());
        
        checkNameAndSize();
        
        Matcher matcher = getMatcherAgainstContent("Bitte warten Sie (.+?) Min");
        if(matcher.find()) {
            int waitMinutes = Integer.parseInt(matcher.group(1));
            throw new YouHaveToWaitException("You have reached the download limit for free users", 60 * waitMinutes);
        }         
        
        matcher = getMatcherAgainstContent("form method\\=\"post\" action\\=\"/dl\\/(.+?)\"");
        if(matcher.find()) {
            String s = HTTP_BASE + "/dl/" + matcher.group(1);
            final PostMethod postMethod = getPostMethod(s);
            if (!tryDownloadAndSaveFile(postMethod)) {
                checkProblems();
                logger.warning(getContentAsString());
                throw new IOException("File input stream is empty");
            }
        } else {
            logger.warning("Can not locate download form");
            System.err.println(this.getContentAsString());
        }

    }

    private void processFolder() throws Exception {
        logger.info("Starting processing list in TASK " + fileURL);
        final GetMethod getMethod = this.getGetMethod(fileURL);
        if(makeRequest(getMethod)) {
            List<URI> uriList = new LinkedList<URI>();
            
            Matcher matcher = this.getMatcherAgainstContent("<a href\\=\"/get/(.+?)\"><img src\\=\"/images/browser/download.png\"");
            String url;
            while(matcher.find()) {
                url = HTTP_BASE + "/get/" + matcher.group(1);
                uriList.add(new URI(url));
                logger.info(url);
            }
            
            if(uriList.size()>0) {
                // We convert this download task in order to keep the list clean
                this.fileURL = uriList.remove(0).toString();
                getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(httpFile, uriList);
                this.httpFile.setNewURL(new URL(fileURL));       
                this.httpFile.setPluginID("");
                this.httpFile.setState(DownloadState.QUEUED);
            }
        }
    }
    
    private boolean stepCaptcha() throws FailedToLoadCaptchaPictureException, CaptchaEntryInputMismatchException, PluginImplementationException, IOException {

        logger.info("Starting Captcha recognition");

        String captcha = getCaptchaSupport().getCaptcha(HTTP_BASE + "/captcha.png");
        if (captcha == null) {
            throw new CaptchaEntryInputMismatchException();
        } else {

            
            String url = fileURL + "/file/";

            PostMethod postMethod = getPostMethod(url);
            postMethod.addParameter("Download", "Download");
            postMethod.addParameter("captcha", captcha);

            postMethod.addRequestHeader("Referer", fileURL);

            if (makeRedirectedRequest(postMethod)) {
                if(!this.getContentAsString().contains("captcha")) {
                    logger.info("Request succseeded");
                    return true;}
                else {
                    logger.info("Captcha wrong");
                    return false;
                }
            } else {
                logger.warning("Request to "+url+" failed!");
                return false;
            }
        }
    }
    
    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();

        if (contentAsString.contains("Page Not Found")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }

        if (contentAsString.contains("This file has been deleted")) {
            throw new URLNotAvailableAnymoreException("This file has been deleted");
        }
    }
    
    private void checkNameAndSize() throws ErrorDuringDownloadingException {
        Matcher matcher = getMatcherAgainstContent("<td>(.*? .B)</td>");

        if (matcher.find()) {
            
            final long fileSize = PlugUtils.getFileSizeFromString(matcher.group(1));
            logger.info("File size " + fileSize);
            httpFile.setFileSize(fileSize);
        } else {
            logger.warning("File size was not found");
            throw new PluginImplementationException();
        }

        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }
}