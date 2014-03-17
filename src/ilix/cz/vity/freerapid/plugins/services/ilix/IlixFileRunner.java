package cz.vity.freerapid.plugins.services.ilix;

import cz.vity.freerapid.plugins.exceptions.CaptchaEntryInputMismatchException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadState;
import cz.vity.freerapid.plugins.webclient.hoster.CaptchaSupport;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;

import java.net.URL;
import java.net.URLDecoder;
import java.util.logging.Logger;

/**
 * Class which contains main code
 *
 * @author ntoskrnl
 */
class IlixFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(IlixFileRunner.class.getName());
    private final static int captchaMax = 5;
    private int captchaCounter = 1;

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);

        HttpMethod httpMethod = getMethodBuilder().setAction(fileURL).toGetMethod();
        if (!makeRedirectedRequest(httpMethod)) throw new ServiceConnectionProblemException();

        if (getContentAsString().contains("captcha.php")) {
            stepCaptcha();
        } else {
            httpMethod = getMethodBuilder().setReferer(fileURL).setAction(fileURL).setParameter("n", "0").toPostMethod();
            if (!makeRedirectedRequest(httpMethod)) throw new ServiceConnectionProblemException();
        }

        httpMethod = getMethodBuilder().setReferer(fileURL).setAction("http://ilix.in/encrypt.php").toGetMethod();
        if (!makeRedirectedRequest(httpMethod)) throw new ServiceConnectionProblemException();

        final String tounescape = PlugUtils.getStringBetween(getContentAsString(), "unescape('", "')");
        logger.info("String to unescape " + tounescape);

        final String unescaped = URLDecoder.decode(tounescape, "UTF-8");
        logger.info("Unescaped string " + unescaped);

        final String link = PlugUtils.getStringBetween(unescaped, "<iframe name=\"ifram\" src=\"", "\"");
        logger.info("Extracted link " + link);

        httpFile.setNewURL(new URL(link));
        httpFile.setPluginID("");
        httpFile.setState(DownloadState.QUEUED);
    }

    private void stepCaptcha() throws Exception {
        //the captchas at ilix.in don't seem to work at all (returns error 500)
        //putting this in anyway in case they get them running again
        while (getContentAsString().contains("captcha.php")) {
            final CaptchaSupport captchaSupport = getCaptchaSupport();
            final String captchaSrc = "http://ilix.in/captcha/captcha.php";
            //logger.info("Captcha URL " + captchaSrc);

            String captcha;
            if (captchaCounter <= captchaMax) {
                captcha = PlugUtils.recognize(captchaSupport.getCaptchaImage(captchaSrc), "-d -1 -C A-z-0-9");
                logger.info("OCR attempt " + captchaCounter + " of " + captchaMax + ", recognized " + captcha);
                captchaCounter++;
            } else {
                captcha = captchaSupport.getCaptcha(captchaSrc);
                if (captcha == null) throw new CaptchaEntryInputMismatchException();
                logger.info("Manual captcha " + captcha);
            }

            final HttpMethod httpMethod = getMethodBuilder().setReferer(fileURL).setAction(fileURL).setParameter("n", "0").setParameter("captcha", captcha).toPostMethod();
            if (!makeRedirectedRequest(httpMethod)) throw new ServiceConnectionProblemException();
        }
    }

}