package cz.vity.freerapid.plugins.services.xun6;

import cz.vity.freerapid.plugins.exceptions.CaptchaEntryInputMismatchException;
import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.hoster.CaptchaSupport;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class which contains main code
 *
 * @author JPEXS
 */
class XUN6FileRunner extends AbstractRunner {

    private final static Logger logger = Logger.getLogger(XUN6FileRunner.class.getName());

    @Override
    public void runCheck() throws Exception { //this method validates file
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);//make first request
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkNameAndSize(getContentAsString());//ok let's extract file name and size from the page
        } else {
            throw new PluginImplementationException();
        }
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {

        Pattern urlPattern = Pattern.compile("http\\:\\/\\/www\\.xun6\\.com/file/[0-9a-f]+/(.*)\\.html");
        Matcher m = urlPattern.matcher(fileURL);
        if (m.matches()) {
            try {
                httpFile.setFileName(URLDecoder.decode(m.group(1), "utf8"));
            } catch (UnsupportedEncodingException ex) {
                //ignored
            }
        } else { //if above url match does not work
            PlugUtils.checkName(httpFile, content, "\u6587\u4EF6\u540D\u7A31:</th><td>", "</td></tr>");
        }
        PlugUtils.checkFileSize(httpFile, content, "\u6587\u4EF6\u5927\u5C0F:</th><td>", "</td></tr>");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL); //create GET request
        if (makeRedirectedRequest(method)) { //we make the main request
            final String contentAsString = getContentAsString();//check for response
            checkProblems();//check problems
            checkNameAndSize(contentAsString);//extract file name and size from the page

            CaptchaSupport captchaSupport = getCaptchaSupport();
            do {
                String captchaURL = PlugUtils.getStringBetween(getContentAsString(), "<img id=\"dynimg\" src=\"", "\"");
                String captchaR = captchaSupport.getCaptcha(captchaURL);
                if (captchaR == null) {
                    throw new CaptchaEntryInputMismatchException();
                }

                final HttpMethod method2 = getMethodBuilder().setReferer(fileURL).setActionFromFormByName("myform", true).setParameter("captchacode", captchaR).toHttpMethod();
                if (!makeRedirectedRequest(method2)) {
                    throw new PluginImplementationException();
                }
            } while (getContentAsString().contains("\u8ACB\u91CD\u65B0\u8F38\u5165\u9A57\u8B49\u78BC"));
            String content = getContentAsString();
            int waitTime = PlugUtils.getWaitTimeBetween(content, "var timeout=\"", "\";", TimeUnit.SECONDS);

            String protocol = PlugUtils.getStringBetween(content, "var protocol = \"", "\";");
            String domain = PlugUtils.getStringBetween(content, "var domain = \"", "\";");
            String dirname = PlugUtils.getStringBetween(content, "var dirname = \"", "\";");
            String basename = PlugUtils.getStringBetween(content, "var basename = \"", "\";");

            final HttpMethod method3 = getMethodBuilder().setReferer(fileURL).setAction(protocol + "://" + domain + dirname + "/" + basename).toHttpMethod();
            downloadTask.sleep(waitTime);

            if (!tryDownloadAndSaveFile(method3)) {
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
        if (contentAsString.contains("\u7528\u6236\u7121\u6CD5\u7E7C\u7E8C\u4E0B\u8F09\u8A72\u6587\u4EF6")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
    }
}
