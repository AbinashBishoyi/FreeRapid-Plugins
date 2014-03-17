package cz.vity.freerapid.plugins.services.filestoreua;

import cz.vity.freerapid.plugins.exceptions.*;
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
    private int captchaCounter = 1, captchaMax = 5;

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {
            checkNameAndSize(getContentAsString());
        } else
            throw new PluginImplementationException();
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        PlugUtils.checkName(httpFile, content, "\u0424\u0430\u0439\u043B:</b></td>\n" +
                "\n" +
                "       <td align=left width=400px>", "</td>");
        PlugUtils.checkFileSize(httpFile, content, "\u0420\u0430\u0437\u043C\u0435\u0440:</b></td>\n" +
                "       <td align=left >", "</td>");

        logger.info("Name and size OK");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        //GREAT THANKS TO @ntoskrnl for his help!!!
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        GetMethod getMethod = getGetMethod(fileURL);

        if (makeRedirectedRequest(getMethod)) {
            checkProblems();         
            checkNameAndSize(getContentAsString());


        String url = ".filestore.com.ua";
        addCookie(new Cookie(url, "lang", "en", "/", 86400, false));
        addCookie(new Cookie(url, "acopendivids", "jason,kelly,michael,cat,dog,rabbit", "/", 86400, false));
        addCookie(new Cookie(url, "filestore_logined", "0", "/", 86400, false));
        addCookie(new Cookie(url, "acgroupswithpersist", "nada", "/", 86400, false));
        addCookie(new Cookie(url, "b", "b", "/", 86400, false));
        addCookie(new Cookie(url, "iua", "1", "/", 86400, false));

            HttpMethod http;
            while (getContentAsString().contains("captcha")) {

                http = stepCaptcha(getMethod.getURI().toString());
                        if (!makeRedirectedRequest(http)) {
                           throw new ServiceConnectionProblemException("Error posting captcha");
                       }
                    }

           logger.info("Captcha OK");

            String file_url = PlugUtils.getStringBetween(getContentAsString(), "<a href=\"", "\"><img src=\"http://filestore.com.ua/dwnru.gif\">");
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
        if (contentAsString.contains("\u0424\u0430\u0439\u043B \u043D\u0435 \u043D\u0430\u0439\u0434\u0435\u043D")) {
            throw new URLNotAvailableAnymoreException("\u0424\u0430\u0439\u043B \u043D\u0435 \u043D\u0430\u0439\u0434\u0435\u043D");
        }

        if (contentAsString.contains("\u0422\u0430\u043A\u043E\u0439 \u0441\u0442\u0440\u0430\u043D\u0438\u0446\u044B \u043D\u0430 \u043D\u0430\u0448\u0435\u043C \u0441\u0430\u0439\u0442\u0435 \u043D\u0435\u0442")) {
            throw new URLNotAvailableAnymoreException("\u0422\u0430\u043A\u043E\u0439 \u0441\u0442\u0440\u0430\u043D\u0438\u0446\u044B \u043D\u0430 \u043D\u0430\u0448\u0435\u043C \u0441\u0430\u0439\u0442\u0435 \u043D\u0435\u0442"); //let to know user in FRD
        }

        if (contentAsString.contains("File Not Found")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
    }


    private HttpMethod stepCaptcha(final String referer) throws ErrorDuringDownloadingException {
        final CaptchaSupport captchaSupport = getCaptchaSupport();
        final String captchaSrc = getMethodBuilder().setActionFromImgSrcWhereTagContains("captcha").getAction();
        logger.info("Captcha URL " + captchaSrc);

        String captcha;
        if (captchaCounter <= captchaMax) {
            //the captchas are really tough, but might just leave this in anyway...
            captcha = PlugUtils.recognize(captchaSupport.getCaptchaImage(captchaSrc), "-d -1 -C a-z-0-9");
            logger.info("OCR attempt " + captchaCounter + " of " + captchaMax + ", recognized " + captcha);
            captchaCounter++;
        } else {
            captcha = captchaSupport.getCaptcha(captchaSrc);
            if (captcha == null) throw new CaptchaEntryInputMismatchException();
            logger.info("Manual captcha " + captcha);
        }
        return getMethodBuilder().setReferer(referer).setActionFromFormWhereTagContains("captcha", true).setParameter("captchacode", captcha).setParameter("statson", "1").toPostMethod();
    }
}
