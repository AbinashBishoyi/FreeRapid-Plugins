package cz.vity.freerapid.plugins.services.narod;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.hoster.CaptchaSupport;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.io.IOException;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author Thumb
 */
class NarodFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(NarodFileRunner.class.getName());


    @Override
    public void runCheck() throws Exception { //this method validates file
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);//make first request
        if (makeRedirectedRequest(getMethod)) {
            checkFileProblems();
            checkNameAndSize();//ok let's extract file name and size from the page
        } else
            throw new ServiceConnectionProblemException();
    }

    private void checkNameAndSize() throws ErrorDuringDownloadingException {
        Matcher nameMatcher = PlugUtils.matcher("<dt class=\"name\">(.*?)</dt>", getContentAsString());
        if (!nameMatcher.find())
            unimplemented();
        Matcher namePartMatcher = PlugUtils.matcher("<[^>]*>|\\s", nameMatcher.group(1));
        httpFile.setFileName(namePartMatcher.replaceAll(""));

        Matcher sizeMatcher = PlugUtils.matcher("??????:(?:<[^>]*>|\\s)*(.*?)<", getContentAsString());
        if (!sizeMatcher.find())
            unimplemented();
        httpFile.setFileSize(PlugUtils.getFileSizeFromString(sizeMatcher.group(1).replace('?', 'B')));


        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);

        runCheck();
        checkDownloadProblems();

        HttpMethod method1 = addCaptchaPrameters(getMethodBuilder()
                .setActionFromFormByName("f-capchaform", true)
                .setAction(fileURL))
                .setParameter("action", "sendcapcha")
                .toPostMethod();

        if (!makeRequest(method1))
            throw new ServiceConnectionProblemException();
        checkProblems();

        HttpMethod method2 = getMethodBuilder()
                .setActionFromAHrefWhereATagContains("http://narod.ru")
                .setBaseURL("http://narod.ru")
                .toGetMethod();

        //here is the download link extraction
        if (!tryDownloadAndSaveFile(method2)) {
            checkProblems();//if downloading failed
            unimplemented();
        }
    }

    /**
     * @throws PluginImplementationException
     */
    private void unimplemented() throws PluginImplementationException {
        logger.warning(getContentAsString());//log the info
        throw new PluginImplementationException();//some unknown problem
    }

    /**
     * @param setAction
     * @return
     * @throws ErrorDuringDownloadingException
     *
     * @throws IOException
     */
    private MethodBuilder addCaptchaPrameters(MethodBuilder builder) throws ErrorDuringDownloadingException, IOException {
        HttpMethod keyRequest = getMethodBuilder()
                .setAction("http://narod.ru/disk/getcapchaxml/?rnd=" + (int) (Math.random() * 777))
                .toGetMethod();

        if (!makeRequest(keyRequest))
            throw new ServiceConnectionProblemException();

        CaptchaSupport cs = getCaptchaSupport();

        Matcher numberMatcher = PlugUtils.matcher("<number\\s+url=\"([^\"]*)\"[^>]*>([^<]*)<", getContentAsString());
        if (!numberMatcher.find())
            unimplemented();

        return builder.setParameter("key", numberMatcher.group(2))
                .setParameter("rep", cs.getCaptcha(numberMatcher.group(1)));

    }

    private void checkFileProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("<td class=\"headCode\">404</td>")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
    }

    private void checkDownloadProblems() throws ErrorDuringDownloadingException {
        if (getContentAsString().contains("<b class=\"error-msg\"><strong>?????????</strong> ?????????? ???&nbsp;???</b>"))
            throw new CaptchaEntryInputMismatchException();
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        checkDownloadProblems();
        checkFileProblems();
		}

}