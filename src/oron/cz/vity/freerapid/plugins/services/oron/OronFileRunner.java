package cz.vity.freerapid.plugins.services.oron;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.services.recaptcha.ReCaptcha;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.hoster.CaptchaSupport;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author Thumb
 */
class OronFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(OronFileRunner.class.getName());


    @Override
    public void runCheck() throws Exception { //this method validates file
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);//make first request
        if (makeRedirectedRequest(getMethod)) {
            checkFileProblems();
            checkNameAndSize();//ok let's extract file name and size from the page
        } else {
            checkFileProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void unimplemented() throws PluginImplementationException {
        logger.warning(getContentAsString());//log the info
        throw new PluginImplementationException();//some unknown problem

    }

    private void checkNameAndSize() throws ErrorDuringDownloadingException {
        Matcher name_m = PlugUtils.matcher("Filename: <[^<>]*>([^<]+)<", getContentAsString());
        if (!name_m.find())
            unimplemented();
        httpFile.setFileName(name_m.group(1));

        PlugUtils.checkFileSize(httpFile, getContentAsString(), "Size: ", "<");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);

        runCheck();
        checkDownloadProblems();

        final HttpMethod method = getMethodBuilder()
                .setActionFromFormWhereTagContains("method_free", true)
                .setAction(fileURL)
                .setParameter("method_free", " Free Download ")
                .toPostMethod();

        if (!makeRequest(method))
            throw new ServiceConnectionProblemException();
        checkProblems();

        MethodBuilder request2 = getMethodBuilder()
                .setActionFromFormByName("F1", true)
                .setAction(fileURL);

        ReCaptcha r = new ReCaptcha("6LdzWwYAAAAAAAzlssDhsnar3eAdtMBuV21rqH2N", client);
        String imageURL = r.getImageURL();
        CaptchaSupport cs = getCaptchaSupport();
        r.setRecognized(cs.getCaptcha(imageURL));

        final HttpMethod method2 = r.modifyResponseMethod(request2)
                .toPostMethod();

        if (!makeRequest(method2))
            throw new ServiceConnectionProblemException();
        checkProblems();

        final HttpMethod method3 = getMethodBuilder()
                .setActionFromAHrefWhereATagContains("Download")
                .toHttpMethod();

        //here is the download link extraction
        if (!tryDownloadAndSaveFile(method3)) {
            checkProblems();//if downloading failed
            unimplemented();
        }
    }

    private void checkFileProblems() throws ErrorDuringDownloadingException {
        if (getContentAsString().contains("Status: <b class=\"ok\">Premium Only</b><br>")) {
            throw new URLNotAvailableAnymoreException("Premium only");
        }
        Matcher err_m = PlugUtils.matcher("<font class=\"err\">([^<>]+)<", getContentAsString());
        if (err_m.find()) {
            if (err_m.group(1).contains("No such file"))
                throw new URLNotAvailableAnymoreException("File not found"); // we could as well repair the URL...
        }
        if (getContentAsString().contains("<b>File Not Found</b>")
                || getContentAsString().contains("<meta NAME=\"description\" CONTENT=\"ORON.com - File Not Found\">"))
            throw new URLNotAvailableAnymoreException("File not found"); // we could as well repair the URL...
    }

    private void checkDownloadProblems() throws ErrorDuringDownloadingException {
        Matcher err_m = PlugUtils.matcher("<font class=\"err\">([^<>]+)<", getContentAsString());
        if (err_m.find()) {
            Matcher m = PlugUtils.matcher("You have to wait(?:.*?\\b([0-9]+) hour)?(?:.*?\\b([0-9]+) minute)?(?:.*\\b([0-9]+) second)?", err_m.group(1));
            if (m.find())
                throw new YouHaveToWaitException("You have to wait",
                        Integer.valueOf("0" + m.group(1)) * 3600 +
                                Integer.valueOf("0" + m.group(2)) * 60 +
                                Integer.valueOf("0" + m.group(3)));
            if (getContentAsString().contains("Wrong captcha"))
                throw new CaptchaEntryInputMismatchException();
            unimplemented();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        checkFileProblems();
        checkDownloadProblems();
    }
}
