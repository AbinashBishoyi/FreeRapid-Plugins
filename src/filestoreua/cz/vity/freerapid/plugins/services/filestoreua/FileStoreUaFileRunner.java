package cz.vity.freerapid.plugins.services.filestoreua;

import cz.vity.freerapid.plugins.exceptions.CaptchaEntryInputMismatchException;
import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.hoster.CaptchaSupport;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.logging.Logger;

/**
 * Class which contains main code
 *
 * @author Saikek
 */
class FileStoreUaFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(FileStoreUaFileRunner.class.getName());
    private final static int CAPTCHA_MAX = 5;
    private int captchaCounter = 1;

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        addCookie(new Cookie(".filestore.com.ua", "filestore_mylang", "en", "/", 86400, false));
        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {
            checkNameAndSize(getContentAsString());
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        PlugUtils.checkName(httpFile, content, "<title>", "</title>");
        PlugUtils.checkFileSize(httpFile, content, "File size:</b></td>\n<td align=left >", "</td>");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        //GREAT THANKS TO @ntoskrnl for his help!!!
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        addCookie(new Cookie(".filestore.com.ua", "filestore_mylang", "en", "/", 86400, false));
        GetMethod getMethod = getGetMethod(fileURL);

        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkNameAndSize(getContentAsString());

            HttpMethod http;
            while (getContentAsString().contains("captcha")) {

                http = stepCaptcha(getMethod.getURI().toString());
                if (!makeRedirectedRequest(http)) {
                    throw new ServiceConnectionProblemException("Error posting captcha");
                }
            }

            logger.info("Captcha OK");

            String file_url = PlugUtils.getStringBetween(getContentAsString(),
                    "<a href=\"", "\" onmouseout='window.status");
            http = getMethodBuilder().setAction(file_url).toGetMethod();

            if (!tryDownloadAndSaveFile(http)) {
                throw new ServiceConnectionProblemException("Error downloading file");
            }

        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("\u0424\u0430\u0439\u043B \u043D\u0435 \u043D\u0430\u0439\u0434\u0435\u043D")
                || contentAsString.contains("\u0422\u0430\u043A\u043E\u0439 \u0441\u0442\u0440\u0430\u043D\u0438\u0446\u044B \u043D\u0430 \u043D\u0430\u0448\u0435\u043C \u0441\u0430\u0439\u0442\u0435 \u043D\u0435\u0442")
                || contentAsString.contains("File Not Found")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }


    private HttpMethod stepCaptcha(final String referer) throws ErrorDuringDownloadingException {
        final CaptchaSupport captchaSupport = getCaptchaSupport();
        final String captchaSrc = getMethodBuilder().setActionFromImgSrcWhereTagContains("captcha").getAction();
        logger.info("Captcha URL " + captchaSrc);

        String captcha;
        if (captchaCounter <= CAPTCHA_MAX) {
            //the captchas are really tough, but might just leave this in anyway...
            captcha = PlugUtils.recognize(captchaSupport.getCaptchaImage(captchaSrc), "-d -1 -C a-z-0-9");
            logger.info("OCR attempt " + captchaCounter + " of " + CAPTCHA_MAX + ", recognized " + captcha);
            captchaCounter++;
        } else {
            captcha = captchaSupport.getCaptcha(captchaSrc);
            if (captcha == null) throw new CaptchaEntryInputMismatchException();
            logger.info("Manual captcha " + captcha);
        }
        return getMethodBuilder().setReferer(referer).setActionFromFormWhereTagContains("captcha", true).setParameter("captchacode", captcha).setParameter("statson", "1").toPostMethod();
    }
}
