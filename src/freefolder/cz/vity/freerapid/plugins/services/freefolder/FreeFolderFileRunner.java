package cz.vity.freerapid.plugins.services.freefolder;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.hoster.CaptchaSupport;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;

import java.util.logging.Logger;

/**
 * Class which contains main code
 *
 * @author Arthur Gunawan
 */
class FreeFolderFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(FreeFolderFileRunner.class.getName());


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
        //<a href="http://freefolder.net/f/B2D79838CFCF1328477F79238CB0D519">LUH_199_midres.zip</a> | 70.67 Mb            </td>
        //">LUH_199_midres.zip</a> | 70.67 Mb            <
        String smallNameContent = PlugUtils.getStringBetween(content, fileURL, "/td>");

        PlugUtils.checkName(httpFile, smallNameContent, "\">", "</a>");//TODO
        PlugUtils.checkFileSize(httpFile, content, " | ", "<");//TODO
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final String freeURL = "http://freefolder.net/free/";

        GetMethod method = getGetMethod(fileURL); //create GET request
        if (!makeRedirectedRequest(method)) { //we make the main request
            checkProblems();//if downloading failed
            logger.warning(getContentAsString());//log the info
            throw new PluginImplementationException();//some unknown problem
        }

        final String contentAsString = getContentAsString();//check for response
        checkProblems();//check problems
        checkNameAndSize(contentAsString);//extract file name and size from the page

        method = getGetMethod(freeURL);

        if (!makeRedirectedRequest(method)) { //we make the main request
            checkProblems();//if downloading failed
            logger.warning(getContentAsString());//log the info
            throw new PluginImplementationException();//some unknown problem
        }


        //get Wait Time
        //timeleft = 61
        String waitString = PlugUtils.getStringBetween(getContentAsString(), "timeleft =", ";");
        int waitTime = new Integer(waitString);
        downloadTask.sleep(waitTime);

        //logger.info(getContentAsString());


        PostMethod postMethod = stepCaptcha(freeURL);
        //logger.info(getContentAsString());
        if (!makeRedirectedRequest(postMethod)) { //we make the main request
            checkProblems();//if downloading failed
            logger.warning(getContentAsString());//log the info
            throw new PluginImplementationException();//some unknown problem
        }
        logger.info("Free Content: ");

//        looping for captcha...
//        while (getContentAsString().contains("captcha.php")) {
//        waitString = PlugUtils.getStringBetween(getContentAsString(), "timeleft =", ";");
//        int waitTime = new Integer(waitString);
//        downloadTask.sleep(waitTime);
//             postMethod = stepCaptcha(freeURL);
//            //logger.info(getContentAsString());
//            if (!makeRedirectedRequest(postMethod)) { //we make the main request
//                checkProblems();//if downloading failed
//                logger.warning(getContentAsString());//log the info
//                throw new PluginImplementationException();//some unknown problem
//            }
//            logger.info("Free Content: ");
//        }
        if (getContentAsString().contains("captcha.php"))
            throw new PluginImplementationException("Failed to post captcha");

        String finalURL = PlugUtils.getStringBetween(getContentAsString(), "replace('", "'");
        logger.info("Final URL: " + finalURL);
        method = getGetMethod(finalURL);

//
//        //here is the download link extraction
        if (!tryDownloadAndSaveFile(method)) {
            checkProblems();//if downloading failed
            logger.warning(getContentAsString());//log the info
            throw new PluginImplementationException();//some unknown problem
        }

    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("File Not Found")) {//TODO
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
    }

    private PostMethod stepCaptcha(String redirectURL) throws ErrorDuringDownloadingException {
        final CaptchaSupport captchaSupport = getCaptchaSupport();
        final String captchaSrc = "http://freefolder.net/captcha/captcha.php?";

        logger.info("Captha string : " + captchaSrc);

        final String captcha = captchaSupport.getCaptcha(captchaSrc);
        final PostMethod postMethod = getPostMethod(redirectURL);
        client.setReferer("http://freefolder.net/free/");
        String[] parameters = new String[]{"id"};
        PlugUtils.addParameters(postMethod, getContentAsString(), parameters);
        postMethod.addParameter("captcha", captcha);


        if (captcha == null) {
            throw new CaptchaEntryInputMismatchException();
        } else {
            return postMethod;//getMethodBuilder().setBaseURL("http://freefolder.net/free/").setAction("http://freefolder.net/free/").setReferer("http://freefolder.net/free/").setActionFromFormWhereTagContains("captcha", true).setParameter("captcha", captcha).toHttpMethod();
        }
    }
}