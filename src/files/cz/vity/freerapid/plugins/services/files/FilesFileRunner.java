package cz.vity.freerapid.plugins.services.files;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.hoster.CaptchaSupport;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.logging.Logger;

/**
 * Class which contains main code
 *
 * @author Arthur Gunawan
 */
class FilesFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(FilesFileRunner.class.getName());


    @Override
    public void runCheck() throws Exception { //this method validates file
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);//make first request
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkNameAndSize(getContentAsString());//ok let's extract file name and size from the page
        } else
            throw new PluginImplementationException();
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        PlugUtils.checkName(httpFile, content, "<title>", "</title>");//TODO
        PlugUtils.checkFileSize(httpFile, content, "File size:</b></td>\n\t<td align=left>", "</td>");//TODO
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL); //create GET request
        if (makeRedirectedRequest(method)) { //we make the main request
            String contentAsString = getContentAsString();//check for response
            checkProblems();//check problems
            checkNameAndSize(contentAsString);//extract file name and size from the page
            boolean exit=false;

           // while(contentAsString.contains("Redraw") || (exit==false)) {
                  HttpMethod httpMethod = stepCaptcha(fileURL);
                if(!makeRedirectedRequest(httpMethod)) {
                    checkProblems();//if downloading failed
                logger.warning(getContentAsString());//log the info
                throw new PluginImplementationException();//some unknown pro
                }
                


            //}

            logger.info("Captha OK!");
            //downloadTask.sleep(60);
               contentAsString=getContentAsString();
                checkProblems();

           String finURL = "http://files.fm/getfile" + PlugUtils.getStringBetween(contentAsString, "http://files.fm/getfile","\";") ;
            logger.info("FIN URL : " + finURL);


          httpMethod = getMethodBuilder().setAction(finURL).toGetMethod();



            //here is the download link extraction
            if (!tryDownloadAndSaveFile(httpMethod)) {
                checkProblems();//if downloading failed
                logger.warning(getContentAsString());//log the info
                throw new PluginImplementationException();//some unknown problem
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("File Not Found")) {//TODO
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }          //no free download slots available
        if (contentAsString.contains("captcha error")) {//TODO
            throw new YouHaveToWaitException("Retry",5); //let to know user in FRD

        }
         if (contentAsString.contains("no free download slots available")) {//TODO
            throw new YouHaveToWaitException("Free slot not available",60); //let to know user in FRD
             
        }
         if (contentAsString.contains("You have got max allowed bandwidth size per hour")) {//TODO
            throw new YouHaveToWaitException("Maxium bandwidth limit reach",3600); //let to know user in FRD

        }

    }
       private HttpMethod stepCaptcha(String redirectURL) throws ErrorDuringDownloadingException {
        final CaptchaSupport captchaSupport = getCaptchaSupport();
        final String captchaSrc = "http://files.fm/captcha.php";

        logger.info("Captcha URL " + captchaSrc);
        final String captcha = captchaSupport.getCaptcha(captchaSrc);

        if (captcha == null) {
            throw new CaptchaEntryInputMismatchException();
        } else {
            return getMethodBuilder().setReferer(redirectURL).setActionFromFormByName("myform", true).setBaseURL(fileURL).setParameter("captchacode", captcha).toHttpMethod();
        }
    }
}