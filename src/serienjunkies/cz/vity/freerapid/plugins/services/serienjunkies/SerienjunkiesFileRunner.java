package cz.vity.freerapid.plugins.services.serienjunkies;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.hoster.CaptchaSupport;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import cz.vity.freerapid.utilities.LogUtils;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * @author benpicco, ntoskrnl
 */
class SerienjunkiesFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(SerienjunkiesFileRunner.class.getName());
    private final static String BASE_URL = "http://download.serienjunkies.org";

    @Override
    public void run() throws Exception {
        super.run();

        logger.info("Starting download in TASK " + fileURL);
        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            try {
                PlugUtils.checkName(httpFile, getContentAsString(), "<H1>", "</H1>");
            } catch (PluginImplementationException e) {
                LogUtils.processException(logger, e);
            }
            while (getContentAsString().contains("IMG SRC=\"/secure/")) {
                try {   // We must not reload the page too quickly
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    //ignore
                }
                if (!makeRedirectedRequest(stepCaptcha())) {
                    checkProblems();
                    throw new ServiceConnectionProblemException();
                }
            }
            final List<URI> uriList = new LinkedList<URI>();
            final Matcher matcher = getMatcherAgainstContent("ACTION=\"(http://download\\.serienjunkies\\.org/.+?)\"");
            while (matcher.find()) {
                // We have to wait again, otherwise we will be detected as a bot
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    //ignore
                }
                final HttpMethod method = getMethodBuilder().setReferer(fileURL).setAction(matcher.group(1)).toPostMethod();
                client.makeRequest(method, false);
                final Header locationHeader = method.getResponseHeader("Location");
                if (locationHeader == null) throw new PluginImplementationException("Redirect location not found");
                final String url = locationHeader.getValue();
                try {
                    uriList.add(new URI(url));
                } catch (URISyntaxException e) {
                    LogUtils.processException(logger, e);
                }
            }
            if (uriList.isEmpty()) throw new PluginImplementationException("No links found");
            getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(httpFile, uriList);
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private HttpMethod stepCaptcha() throws Exception {
        final CaptchaSupport captchaSupport = getCaptchaSupport();
        final String captchaSrc = getMethodBuilder().setActionFromImgSrcWhereTagContains("secure").getEscapedURI();
        logger.info("Captcha URL " + captchaSrc);

        final String captcha = captchaSupport.getCaptcha(captchaSrc);
        if (captcha == null) throw new CaptchaEntryInputMismatchException();
        logger.info("Manual captcha " + captcha);

        return getMethodBuilder().setReferer(fileURL).setBaseURL(fileURL).setActionFromFormWhereTagContains("secure", true).setParameter("c", captcha).toPostMethod();
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        if (getContentAsString().contains("404 Not Found")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

    @Override
    protected String getBaseURL() {
        return BASE_URL;
    }

}
