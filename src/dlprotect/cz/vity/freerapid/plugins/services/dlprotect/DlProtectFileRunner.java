package cz.vity.freerapid.plugins.services.dlprotect;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.utilities.LogUtils;
import org.apache.commons.httpclient.Cookie;
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
class DlProtectFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(DlProtectFileRunner.class.getName());

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        addCookie(new Cookie(".dl-protect.com", "l", "en", "/", 86400, false));
        final HttpMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            do {
                final MethodBuilder mb = getMethodBuilder()
                        .setReferer(fileURL)
                        .setActionFromFormByName("ccerure", true)
                        .setAction(fileURL);
                if (isCaptcha()) {
                    stepCaptcha(mb);
                }
                if (!makeRedirectedRequest(mb.toPostMethod())) {
                    throw new ServiceConnectionProblemException();
                }
            } while (isCaptcha());

            final List<URI> list = new LinkedList<URI>();
            final Matcher matcher = getMatcherAgainstContent("<a href=\"(.+?)\" target=\"_blank\">");
            while (matcher.find()) {
                try {
                    list.add(new URI(matcher.group(1)));
                } catch (final URISyntaxException e) {
                    LogUtils.processException(logger, e);
                }
            }
            if (list.isEmpty()) {
                throw new PluginImplementationException("No links found");
            }
            getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(httpFile, list);
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        if (getContentAsString().contains("the link you are looking for is not found")
                || getContentAsString().contains("<h1>Not Found</h1>")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

    private boolean isCaptcha() {
        return getContentAsString().contains("Security Code");
    }

    private void stepCaptcha(final MethodBuilder mb) throws Exception {
        final String captchaURL = getMethodBuilder().setActionFromImgSrcWhereTagContains("captcha.php").getEscapedURI();
        logger.info("Captcha URL " + captchaURL);
        final String captcha = getCaptchaSupport().getCaptcha(captchaURL);
        if (captcha == null) {
            throw new CaptchaEntryInputMismatchException();
        }
        logger.info("Manual captcha " + captcha);
        mb.setParameter("secure", captcha);
    }

}