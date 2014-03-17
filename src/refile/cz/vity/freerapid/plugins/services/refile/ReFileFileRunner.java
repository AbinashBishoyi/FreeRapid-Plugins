package cz.vity.freerapid.plugins.services.refile;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.services.recaptcha.ReCaptcha;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.DefaultFileStreamRecognizer;
import cz.vity.freerapid.plugins.webclient.DownloadClientConsts;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.net.URLDecoder;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author tong2shot
 */
class ReFileFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(ReFileFileRunner.class.getName());

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
        PlugUtils.checkName(httpFile, content, "<td><h1>Download ", "</h1></td>");
        final String fileSizeRule = "<br />\\s*(.+?)\\s*<br />\\s*<span id=";
        Matcher matcher = PlugUtils.matcher(fileSizeRule, content);
        matcher.find();
        httpFile.setFileSize(PlugUtils.getFileSizeFromString(PlugUtils.unescapeHtml(matcher.group(1))));
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

            while (getContentAsString().contains("recaptcha/api/challenge")) {
                //captcha
                final Matcher reCaptchaKeyMatcher = getMatcherAgainstContent("recaptcha/api/noscript\\?k=(.*?)\" width=");
                reCaptchaKeyMatcher.find();
                final String reCaptchaKey = reCaptchaKeyMatcher.group(1);
                final ReCaptcha r = new ReCaptcha(reCaptchaKey, client);
                final String captcha = getCaptchaSupport().getCaptcha(r.getImageURL());
                if (captcha == null) {
                    throw new CaptchaEntryInputMismatchException();
                }
                r.setRecognized(captcha);

                final String refererParamName = PlugUtils.getStringBetween(contentAsString, "<input type=\"hidden\" id=\"", "\" name=\"");

                logger.info("refererParamName : " + refererParamName);

                HttpMethod httpMethod = r.modifyResponseMethod(getMethodBuilder(contentAsString)
                        .setReferer(fileURL)
                        .setActionFromFormByName("aspnetForm", true)
                        .setAction(fileURL.replaceAll("/d/", "/f/"))
                        //.removeParameter(refererParamName)
                        .setParameter(refererParamName, fileURL))
                        .toPostMethod();
                if (!makeRedirectedRequest(httpMethod)) {
                    checkProblems();
                    logger.warning(getContentAsString());
                    throw new ServiceConnectionProblemException();
                }
            }

            contentAsString = getContentAsString();
            final String actionRule = "<a href=\"(http://\\w+\\.refile\\.net/file/\\?e_enc=.*?)\">";
            final Matcher actionMatcher = PlugUtils.matcher(actionRule, contentAsString);
            if (!actionMatcher.find()) {
                throw new PluginImplementationException("No download file URL found");
            }
            final String actionURL = URLDecoder.decode(actionMatcher.group(1), "UTF-8");

            logger.info("actionURL : " + actionURL);

            final HttpMethod downloadMethod = getMethodBuilder()
                    .setReferer(fileURL.replaceAll("/f/", "/d/"))
                    .setAction(actionURL)
                    .toHttpMethod();

            String allowedCT[] = {"multipart/binary"};
            DefaultFileStreamRecognizer refileRecognizer = new DefaultFileStreamRecognizer(allowedCT,false);
            client.getHTTPClient().getParams().setParameter(DownloadClientConsts.FILE_STREAM_RECOGNIZER,refileRecognizer);

            //here is the download link extraction
            if (!tryDownloadAndSaveFile(downloadMethod)) {
                checkProblems();//if downloading failed
                throw new ServiceConnectionProblemException("Error starting download");//some unknown problem
            }

        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("File Not Found")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
    }

}