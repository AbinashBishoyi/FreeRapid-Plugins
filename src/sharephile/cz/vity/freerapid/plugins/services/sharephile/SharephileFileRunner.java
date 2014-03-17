package cz.vity.freerapid.plugins.services.sharephile;

import cz.vity.freerapid.plugins.exceptions.CaptchaEntryInputMismatchException;
import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.exceptions.YouHaveToWaitException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.hoster.CaptchaSupport;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import java.util.concurrent.TimeUnit;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.logging.Logger;

/**
 * Class which contains main code
 *
 * @author JPEXS
 */
class SharephileFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(SharephileFileRunner.class.getName());
    private static final String SERVER_URL="http://sharephile.com";
    @Override
    public void runCheck() throws Exception { //this method validates file
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);//make first request
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkNameAndSize(getContentAsString());//ok let's extract file name and size from the page
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        PlugUtils.checkName(httpFile, content, "<span class='file-icon1 image'>", "</span>");
        PlugUtils.checkFileSize(httpFile, content, "</span>\t\t(", ")");
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
            
            
            final HttpMethod method2 = getMethodBuilder().setReferer(fileURL).setBaseURL(SERVER_URL).setActionFromAHrefWhereATagContains("Regular Download").toHttpMethod();
            final String secondUrl=method2.getURI().toString();            
            if(makeRedirectedRequest(method2)){
                 checkProblems();
                 int captchaRetryes=0;
                 while(getContentAsString().contains("Type the characters you see in the picture"))
                 {
                       captchaRetryes++;
                       if(captchaRetryes>3){
                          throw new CaptchaEntryInputMismatchException();
                       }
                       CaptchaSupport captchaSupport = getCaptchaSupport();
                       String captchaURL = PlugUtils.getStringBetween(getContentAsString(), "<img alt=\"Captcha\" src=\"", "\"");
                       logger.info("Captcha URL " + captchaURL);
                       String captcha = captchaSupport.getCaptcha(captchaURL);
                       if (captcha == null) {
                           throw new CaptchaEntryInputMismatchException();
                       }
                       final HttpMethod method3 = getMethodBuilder().setReferer(secondUrl).setActionFromFormWhereTagContains("method='post' action='#'", true).setBaseURL(secondUrl).setParameter("captcha_response", captcha).toHttpMethod();
                       if(!makeRedirectedRequest(method3)){
                          checkProblems();
                          throw new ServiceConnectionProblemException();
                       }
                 }
                                   
                     contentAsString=getContentAsString();        
                     checkProblems();
                     downloadTask.sleep(PlugUtils.getWaitTimeBetween(contentAsString, "limit : ", ",", TimeUnit.SECONDS));
                     int maxLimit=PlugUtils.getNumberBetween(contentAsString, "maxLimit : ", ",");
                     
                     final HttpMethod method4 = getMethodBuilder().setReferer(secondUrl).setAction(PlugUtils.getStringBetween(contentAsString, "$(\"#timeoutBox\").load(\"", "\"")+maxLimit).setBaseURL(SERVER_URL).toGetMethod();
                     method4.addRequestHeader("X-Requested-With", "XMLHttpRequest"); //AJAX request
                
                     if(makeRedirectedRequest(method4)){
                        while(getContentAsString().contains("The link will be available in")){
                           downloadTask.sleep(PlugUtils.getWaitTimeBetween(contentAsString, "Timeout.limit = ", ";", TimeUnit.SECONDS));                     
                           if(!makeRedirectedRequest(method4)){
                              throw new ServiceConnectionProblemException();
                           }
                        }
                        final HttpMethod method5 = getMethodBuilder().setReferer(secondUrl).setActionFromTextBetween("jQuery(\"#popunder2\").attr(\"href\", \"", "\");").setBaseURL(SERVER_URL).toGetMethod();
                
                        if (!tryDownloadAndSaveFile(method5)) {
                            checkProblems();//if downloading failed
                            throw new ServiceConnectionProblemException("Error starting download");//some unknown problem
                        }
                     }else{
                        checkProblems();
                        throw new ServiceConnectionProblemException();
                     }
            }
            else{
               checkProblems();
               throw new ServiceConnectionProblemException();
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if(contentAsString.contains("You have reached the limit of connections")){
           throw new YouHaveToWaitException("You have reached the limit of connections", PlugUtils.getNumberBetween(contentAsString, "<span id='timeout'>", "</span>"));
        }
        if(contentAsString.contains("From your IP range the limit of connections is reached")){
           throw new YouHaveToWaitException("From your IP range the limit of connections is reached", PlugUtils.getNumberBetween(contentAsString, "<span id='timeout'>", "</span>"));
        }
        if (contentAsString.contains("File was not found")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
    }      

}