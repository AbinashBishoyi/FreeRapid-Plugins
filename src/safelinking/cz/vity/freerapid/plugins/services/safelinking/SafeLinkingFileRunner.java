package cz.vity.freerapid.plugins.services.safelinking;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.services.recaptcha.ReCaptcha;
import cz.vity.freerapid.plugins.services.solvemediacaptcha.SolveMediaCaptcha;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadState;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.methods.GetMethod;

import java.net.URI;
import java.net.URLEncoder;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class SafeLinkingFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(SafeLinkingFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception { //this method validates file
        super.runCheck();
        checkURL();
        addCookie(new Cookie(".safelinking.net", "language", "en", "/", 86400, false));
        final GetMethod getMethod = getGetMethod(fileURL);//make first request
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            httpFile.setFileName("Ready to Extract Link(s)");
            httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkURL() {
        if (fileURL.startsWith("http://"))
            fileURL = fileURL.replaceFirst("http://", "https://");
    }

    private URI stepDirectLink(final String directLinkURL) throws Exception {
        final GetMethod method = getGetMethod(directLinkURL);
        if (makeRedirectedRequest(method)) {
            return encodeUri(method.getURI().getURI());
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    @Override
    public void run() throws Exception {
        super.run();
        checkURL();
        addCookie(new Cookie(".safelinking.net", "language", "en", "/", 86400, false));
        logger.info("Starting download in TASK " + fileURL);

        final String HEADER_LINK_TYPE_1 = "<strong>Direct links</strong>";
        final String HEADER_LINK_TYPE_2 = "<strong>Live links</strong>";
        final int MAX_CAPTCHA_ATTEMPTS = 5;

        List<URI> list = new LinkedList<URI>();

        if (fileURL.contains("/d/")) {
            list.add(stepDirectLink(fileURL));
        } else if (fileURL.contains("/p/")) {
            if (!makeRedirectedRequest(getGetMethod(fileURL))) { //we make the main request
                checkProblems();//check problems
                throw new PluginImplementationException();
            }
            int count = 0;
            MethodBuilder builder;
            String content;
            while (!getContentAsString().contains(HEADER_LINK_TYPE_1) &&
                    !getContentAsString().contains(HEADER_LINK_TYPE_2) &&
                    (count++ < MAX_CAPTCHA_ATTEMPTS)) {
                builder = getMethodBuilder()
                        .setActionFromFormWhereTagContains("Protected link", true)
                        .setReferer(fileURL).setAction(fileURL);
                content = getContentAsString();
                // check 4 & complete captcha
                if (content.contains("Captcha loading, please wait") ||
                        content.contains("Prove you are human") ||
                        content.contains("The CAPTCHA code you entered was wrong")) {
                    stepCaptcha(builder);
                }
                // check 4 & complete password
                if (content.contains("Link password")) {
                    final String password = getDialogSupport().askForPassword("SafeLinking");
                    if (password == null) {
                        throw new PluginImplementationException("This file is secured with a password");
                    }
                    builder.setParameter("link-password", password);
                }
                if (!makeRedirectedRequest(builder.toPostMethod())) { //we make the main request
                    checkProblems();//check problems
                    throw new ServiceConnectionProblemException("err 1");
                }
            }

            if (getContentAsString().contains(HEADER_LINK_TYPE_1)) {
                content = PlugUtils.getStringBetween(getContentAsString(), HEADER_LINK_TYPE_1, "</fieldset>");
            } else if (getContentAsString().contains(HEADER_LINK_TYPE_2)) {
                content = PlugUtils.getStringBetween(getContentAsString(), HEADER_LINK_TYPE_2, "</fieldset>");
            } else if (count >= MAX_CAPTCHA_ATTEMPTS) {
                throw new PluginImplementationException("Excessive Incorrect Captcha Entries");
            } else {
                throw new PluginImplementationException("Captcha Text Error : SafeLinking site changed : SafeLinking feature not supported yet");
            }

            final Matcher m = PlugUtils.matcher("<a href=\"([^\"]+)\" class=\"result-a\">", content);
            while (m.find()) {
                list.add(encodeUri(m.group(1).trim()));
            }
            if (getContentAsString().contains(HEADER_LINK_TYPE_1)) {
                for (int ii = 0; ii < list.size(); ii++)
                    list.set(ii, stepDirectLink(list.get(ii).toASCIIString()));
            }

        } else {
            checkProblems();
            throw new PluginImplementationException("Invalid link");
        }
        if (list.isEmpty()) throw new PluginImplementationException("No links found");
        getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(httpFile, list);
        httpFile.setFileName("Link(s) Extracted !");
        httpFile.setState(DownloadState.COMPLETED);
        httpFile.getProperties().put("removeCompleted", true);
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String content = getContentAsString();
        if (content.contains("404 - not found") || content.contains("404 Page/File not found") ||
                content.contains("This link does not exist")) {
            throw new URLNotAvailableAnymoreException("Link does not exist"); //let to know user in FRD
        }
        if (content.contains("The links in this package are <strong>offline</strong>")) {
            throw new URLNotAvailableAnymoreException("The links in this package are offline");
        }
        if (content.contains("server is not currently responding")) {
            throw new ServiceConnectionProblemException("server is not currently responding"); //let to know user in FRD
        }
    }

    private void stepCaptcha(MethodBuilder method) throws Exception {
        if (getContentAsString().contains("solvemediaApiKey")) {
            final Matcher m = getMatcherAgainstContent("var solvemediaApiKey = '(.+?)';");
            if (!m.find()) throw new PluginImplementationException("Captcha key not found");
            final String captchaKey = m.group(1);
            final SolveMediaCaptcha solveMediaCaptcha = new SolveMediaCaptcha(captchaKey, client, getCaptchaSupport(), true);
            solveMediaCaptcha.askForCaptcha();
            solveMediaCaptcha.modifyResponseMethod(method);
            method.setParameter("solvemedia_response", solveMediaCaptcha.getResponse());

        } else if (getContentAsString().contains("recaptcha/api")) {
            final Matcher reCaptchaKeyMatcher = PlugUtils.matcher("recaptcha/api/(?:challenge|noscript)\\?k=(.+?)\"", getContentAsString());
            if (!reCaptchaKeyMatcher.find()) {
                throw new PluginImplementationException("ReCaptcha key not found");
            }
            final ReCaptcha r = new ReCaptcha(reCaptchaKeyMatcher.group(1), client);
            final String captcha = getCaptchaSupport().getCaptcha(r.getImageURL());
            if (captcha == null) {
                throw new CaptchaEntryInputMismatchException();
            }
            r.setRecognized(captcha);
            r.modifyResponseMethod(method);
        } else {
            throw new PluginImplementationException("Unknown/unsupported captcha");
        }
    }

    private URI encodeUri(final String sUri) throws Exception {
        return new URI(URLEncoder.encode(sUri, "UTF-8").replaceAll("%3A", ":").replaceAll("%2F", "/"));
    }

}