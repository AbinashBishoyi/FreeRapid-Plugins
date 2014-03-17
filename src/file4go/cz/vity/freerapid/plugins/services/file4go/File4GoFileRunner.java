package cz.vity.freerapid.plugins.services.file4go;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.services.recaptcha.ReCaptcha;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class File4GoFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(File4GoFileRunner.class.getName());

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
        PlugUtils.checkName(httpFile, content, "<b>Nome:</b> ", "</span>");
        PlugUtils.checkFileSize(httpFile, content, "<b>Tamanho:</b>", "</span>");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL); //create GET request
        if (makeRedirectedRequest(method)) { //we make the main request
            final String contentAsString = getContentAsString();//check for response
            Matcher matcher = Pattern.compile("var time = (\\d+)").matcher(contentAsString);
            if (matcher.find()) {
                throw new YouHaveToWaitException("Wait " + matcher.group(1), Integer.parseInt(matcher.group(1)));
            }
            checkProblems();//check problems
            checkNameAndSize(contentAsString);//extract file name and size from the page
            final HttpMethod httpMethod = handleCaptcha();//getGetMethod(PlugUtils.getStringBetween(getContentAsString(), "window.location.href='", "'\">Download"));
            if (!tryDownloadAndSaveFile(httpMethod)) {
                checkProblems();//if downloading failed
                throw new ServiceConnectionProblemException("Error starting download");//some unknown problem
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private HttpMethod handleCaptcha() throws Exception {
        final String rcKey = "6Lebss8SAAAAAFCpZbm6hIMsfMF3rulH2IJyGvns";
        //final String rcControl = PlugUtils.getStringBetween(getContentAsString(), "var recaptcha_control_field = '", "';");
        final String id = fileURL.substring(fileURL.indexOf("/d/") + 3, fileURL.lastIndexOf('/'));
        while (true) {
            final ReCaptcha rc = new ReCaptcha(rcKey, client);
            final String captcha = getCaptchaSupport().getCaptcha(rc.getImageURL());
            if (captcha == null) {
                throw new CaptchaEntryInputMismatchException();
            }
            rc.setRecognized(captcha);
            final HttpMethod method = rc.modifyResponseMethod(getMethodBuilder()
                    .setAjax()
                    .setAction("http://www.file4go.com/getdownload.php"))
                    //.setParameter("recaptcha_control_field", rcControl)
                    .setParameter("id", id)
                    .toPostMethod();
            if (!makeRedirectedRequest(method)) {
                throw new ServiceConnectionProblemException();
            }
            final String content = getContentAsString().trim();
            //return getMethodBuilder().setActionFromAHrefWhereATagContains("Download").toGetMethod();
            try {
                return getMethodBuilder().setActionFromTextBetween("<span id=\"boton_download\" ><a href=\"", "\" class=\"ddda\"").toGetMethod();
            } catch (BuildMethodException bme) {
                //var time = 1180 -- then wait
                logger.info(content);
                throw new YouHaveToWaitException("Wait", 180);
            }
            /*
            if (content.contains("error_free_download_blocked")) {
                throw new ErrorDuringDownloadingException("You have reached the daily download limit");
            } else if (!content.contains("error_wrong_captcha")) {
                return content;
            }/**/
        }
    }


    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("Not Found") ||
                contentAsString.contains("Arquivo Temporariamente Indisponivel") ||
                contentAsString.contains("Arquivos Removidor POR DMCA Infregimento dos termos de uso")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
    }

}