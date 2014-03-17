package cz.vity.freerapid.plugins.services.wikiupload;

import cz.vity.freerapid.plugins.exceptions.CaptchaEntryInputMismatchException;
import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
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
class WikiuploadFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(WikiuploadFileRunner.class.getName());
    private static final String SERVICE_WEB = "http://www.wikiupload.com/";

    @Override
    public void runCheck() throws Exception { //this method validates file
        super.runCheck();

        checkName();//ok let's extract file name and size from the page

    }

    //Java methods always start with lower capital
    private String commonURL(String cURL) throws ErrorDuringDownloadingException {

        //notice that contains() method is case sensitive
        final String lowercased = cURL.toLowerCase();
        if (lowercased.contains("wikiupload.com/download_page.php")) {
            return lowercased;

        } else if (lowercased.contains("wikiupload.com/middle_page.php")) {
            return lowercased.replaceFirst("wikiupload.com/middle_page.php", "wikiupload.com/download_page.php");

        } else if (lowercased.contains("wikiupload.com/comment.php")) {
            return lowercased.replaceFirst("wikiupload.com/comment.php", "wikiupload.com/download_page.php");
        } else {
            throw new PluginImplementationException("Invalid URL");
            //return null;

        }

    }


    private void checkName() throws Exception {
        final String lowercased = commonURL(fileURL).toLowerCase();
        String fileCommentURL = lowercased.replaceFirst("download_page.php", "comment.php");
        final GetMethod getMethod = getGetMethod(fileCommentURL);
        if (makeRedirectedRequest(getMethod)) {
            String lName = "<span class=\"title_6\">"; //checkName takes always first match, this can simplified
            PlugUtils.checkName(httpFile, getContentAsString(), lName, "</span></a></td>");
            httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
        } else {
            throw new PluginImplementationException();
        }
    }

    @Override
    public void run() throws Exception {
        super.run();
        fileURL = commonURL(fileURL);
        logger.info("Starting download in TASK " + fileURL);
        checkName();

        if (makeRedirectedRequest(getMethodBuilder().setAction(fileURL).toHttpMethod())) {
            boolean succesful;
            do {
                succesful = true;
                if (!tryDownloadAndSaveFile(stepCaptcha())) {
                    checkProblems();
                    succesful = false;
                    logger.warning(getContentAsString());
                }
            } while (!succesful);
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        if ("".equals(getContentAsString())) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
    }

    private HttpMethod stepCaptcha
            () throws ErrorDuringDownloadingException {
        final CaptchaSupport captchaSupport = getCaptchaSupport();
        final String captchaSrc = SERVICE_WEB + "turing_image.php";
        logger.info("Captcha URL " + captchaSrc);
        final String captcha = captchaSupport.getCaptcha(captchaSrc);

        if (captcha == null) {
            throw new CaptchaEntryInputMismatchException();
        } else {
            //return getMethodBuilder().setReferer(fileURL).setAction(fileURL).setParameter("code", captcha).setParameter("id", ID).toHttpMethod();
            //logger.info("!!!!!!!!!!!!!!!! LAST CONTENT !!!!!!!!!!!!!!!!!!!" + getContentAsString());

            return getMethodBuilder().setReferer(fileURL).setActionFromFormByIndex(1, true).setAction(fileURL).setAndEncodeParameter("code", captcha).toHttpMethod();
        }


    }
}
