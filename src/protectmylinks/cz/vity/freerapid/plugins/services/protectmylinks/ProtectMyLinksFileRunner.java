package cz.vity.freerapid.plugins.services.protectmylinks;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.hoster.CaptchaSupport;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import cz.vity.freerapid.utilities.LogUtils;
import org.apache.commons.httpclient.HttpMethod;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author ntoskrnl
 */
class ProtectMyLinksFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(ProtectMyLinksFileRunner.class.getName());
    private final int captchaMax = 0;//not worth trying
    private int captchaCounter = 1;

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);

        HttpMethod httpMethod = getMethodBuilder().setAction(fileURL).toGetMethod();
        if (!makeRedirectedRequest(httpMethod)) throw new ServiceConnectionProblemException();

        checkProblems();

        httpFile.setFileName(PlugUtils.getStringBetween(getContentAsString(), "<h1 class=\"pmclass\">", "</h1>"));

        stepCaptcha();

        parseWebsite();
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String content = getContentAsString();
        if (content.contains("This data has been removed") || content.contains("<h1>Not Found</h1>"))
            throw new URLNotAvailableAnymoreException("File not found");
    }

    private void parseWebsite() throws Exception {
        final Matcher matcher = getMatcherAgainstContent("<a href='(.+?)' target='_blank'>");
        int start = 0;
        final List<URI> uriList = new LinkedList<URI>();
        while (matcher.find(start)) {
            try {
                uriList.add(new URI(matcher.group(1)));
            } catch (URISyntaxException e) {
                LogUtils.processException(logger, e);
            }
            start = matcher.end();
        }
        getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(httpFile, uriList);
    }

    private void stepCaptcha() throws Exception {
        String password = null;
        if (getContentAsString().contains("Password :"))
            password = getPassword();

        while (getContentAsString().contains("secureCaptcha")) {
            final CaptchaSupport captchaSupport = getCaptchaSupport();
            final String captchaSrc = "http://protect-my-links.com/" + getMethodBuilder().setBaseURL("http://protect-my-links.com/").setActionFromImgSrcWhereTagContains("secureCaptcha").getAction();
            logger.info("Captcha URL " + captchaSrc);

            if (getContentAsString().contains("Password is not valid"))
                password = getPassword();

            final String captcha;
            if (captchaCounter <= captchaMax) {
                captcha = PlugUtils.recognize(captchaSupport.getCaptchaImage(captchaSrc), "-d -1 -C A-Z-0-9");
                logger.info("OCR attempt " + captchaCounter + " of " + captchaMax + ", recognized " + captcha);
                captchaCounter++;
            } else {
                captcha = captchaSupport.getCaptcha(captchaSrc);
                if (captcha == null) throw new CaptchaEntryInputMismatchException();
                logger.info("Manual captcha " + captcha);
            }

            final MethodBuilder mb = getMethodBuilder().setReferer(fileURL).setBaseURL(fileURL).setActionFromFormByIndex(2, true).setParameter("captcha", captcha);
            if (password != null) mb.setParameter("passwd", password);
            if (!makeRedirectedRequest(mb.toPostMethod())) throw new ServiceConnectionProblemException();
        }
    }

    private String getPassword() throws Exception {
        final ProtectMyLinksPasswordUI ps = new ProtectMyLinksPasswordUI();
        if (getDialogSupport().showOKCancelDialog(ps, "Secured file on Protect-My-Links")) {
            return ps.getPassword();
        } else throw new NotRecoverableDownloadException("This file is secured with a password");
    }

}