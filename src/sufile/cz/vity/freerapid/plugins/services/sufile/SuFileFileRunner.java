package cz.vity.freerapid.plugins.services.sufile;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.hoster.CaptchaSupport;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class SuFileFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(SuFileFileRunner.class.getName());

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
        final Matcher match = PlugUtils.matcher("<h2 class=\"title\">.+&nbsp;(.+?)</h2>", content);
        if (!match.find()) throw new PluginImplementationException("File name not found");
        httpFile.setFileName(match.group(1).trim());
        PlugUtils.checkFileSize(httpFile, content, "文件大小：<b>", "</b>");
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
            final HttpMethod httpMethod = getMethodBuilder().setReferer(fileURL)
                    .setActionFromTextBetween("downpage_link\" href=\"", "\" ").toGetMethod();
            final Matcher match = PlugUtils.matcher("id=\"wait_input\"[^>]*>(.+?)</span>", contentAsString);
            if (match.find())
                downloadTask.sleep(Integer.parseInt(match.group(1)) + 1);
            if (!makeRedirectedRequest(httpMethod)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            checkProblems();

            final String key = PlugUtils.getStringBetween(getContentAsString(), "down_file('", "',");
            final String ppp = PlugUtils.getStringBetween(getContentAsString(), "down_file('" + key + "','", "')");
            doCaptcha(key);
            final HttpMethod http2Method = getMethodBuilder().setReferer(fileURL)
                    .setAction("dd.php")
                    .setParameter("file_key", key)
                    .setParameter("p", ppp)
                    .toGetMethod();
            if (!makeRedirectedRequest(http2Method)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            checkProblems();

            final HttpMethod dlMethod = getGetMethod(PlugUtils.getStringBetween(getContentAsString(), "<a id=\"downs\" href=\"", "\">"));
            if (!tryDownloadAndSaveFile(dlMethod)) {
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
        if (contentAsString.contains("没有找到您要访问的页面")) {
            throw new URLNotAvailableAnymoreException("没有找到您要访问的页面 (Page not found)"); //let to know user in FRD
        }
        if (contentAsString.contains("请您先完成当前下载后，再尝试下载其他文件")) {
            throw new ErrorDuringDownloadingException("请您先完成当前下载后，再尝试下载其他文件, (Please finish the current download, Then try to download other files)");
        }
    }

    private void doCaptcha(final String id) throws Exception {
        final CaptchaSupport captchaSupport = getCaptchaSupport();
        final String captchaSrc = getMethodBuilder().setActionFromImgSrcWhereTagContains("vcode_img").getEscapedURI();
        do {
            final String captcha = captchaSupport.getCaptcha(captchaSrc);
            final HttpMethod httpMethod = getMethodBuilder()
                    .setAction(captchaSrc)
                    .setParameter("action", "yz")
                    .setParameter("code", captcha)
                    .setParameter("id", id)
                    .toPostMethod();
            if (!makeRedirectedRequest(httpMethod)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            checkProblems();
        } while (!getContentAsString().equals("1"));
    }
}