package cz.vity.freerapid.plugins.services.safeurl;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.services.recaptcha.ReCaptcha;
import cz.vity.freerapid.plugins.services.solvemediacaptcha.SolveMediaCaptcha;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadState;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.methods.GetMethod;

import java.net.URI;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class SafeUrlFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(SafeUrlFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception { //this method validates file
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);//make first request
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            httpFile.setFileName("Ready to Extract Link(s)");
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private URI stepDirectLink(final String link) throws Exception {
        if (link.contains("/d/")) {
            final GetMethod method = getGetMethod(link);
            if (makeRedirectedRequest(method)) {
                return new URI(method.getURI().getURI());
            } else {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
        } else
            return new URI(link);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);

        final int MAX_CAPTCHA_ATTEMPTS = 5;
        List<URI> list = new LinkedList<URI>();

        if (fileURL.contains("/d/")) {
            list.add(stepDirectLink(fileURL));
        } else if (fileURL.contains("/p/")) {
            if (!makeRedirectedRequest(getGetMethod(fileURL))) { //we make the main request
                checkProblems();//check problems
                throw new PluginImplementationException();
            }
            MethodBuilder builder;

            // check 4 & complete password
            int count = 0;
            if (getContentAsString().contains("This link is protected by a password")) {
                builder = getMethodBuilder().setActionFromFormWhereTagContains("Unlock", true).setReferer(fileURL);
                do {
                    final String password = getDialogSupport().askForPassword("SafeURL");
                    if (password == null) {
                        throw new PluginImplementationException("This link is protected with a password");
                    }
                    builder.setParameter("password", password);
                    if (!makeRedirectedRequest(builder.toPostMethod())) { //we make the main request
                        checkProblems();//check problems
                        throw new ServiceConnectionProblemException("err 1");
                    }
                } while (!getContentAsString().contains("\"status\":\"success\"") && (count++ < MAX_CAPTCHA_ATTEMPTS));
                if (count >= MAX_CAPTCHA_ATTEMPTS)
                    throw new PluginImplementationException("Excessive Incorrect Password Attempts");
                //load next page
                if (!makeRedirectedRequest(getGetMethod(fileURL))) { //we make the main request
                    checkProblems();//check problems
                    throw new PluginImplementationException();
                }
            }

            // check 4 & complete captcha
            count = 0;
            if (getContentAsString().contains("Captcha Verification")) {
                builder = getMethodBuilder().setActionFromFormWhereTagContains("Captcha", true).setReferer(fileURL).setAction(fileURL);
                do {
                    stepCaptcha(builder);
                    if (!makeRedirectedRequest(builder.toPostMethod())) { //we make the main request
                        checkProblems();//check problems
                        throw new ServiceConnectionProblemException("err 2");
                    }
                } while (getContentAsString().contains("Captcha Verification") && count++ < MAX_CAPTCHA_ATTEMPTS);
                if (count >= MAX_CAPTCHA_ATTEMPTS)
                    throw new PluginImplementationException("Excessive Incorrect Captcha Entries");
            }

            // find all links on the page
            final Matcher m = PlugUtils.matcher("<div class=\"link-trigger\">\\s*?.+?\\s*?<a href=\"(.+?)\">", getContentAsString());
            while (m.find()) {
                list.add(stepDirectLink(m.group(1).trim()));
            }

        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
        if (list.isEmpty()) throw new PluginImplementationException("No links found");
        getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(httpFile, list);
        httpFile.setFileName("Link(s) Extracted !");
        httpFile.setState(DownloadState.COMPLETED);
        httpFile.getProperties().put("removeCompleted", true);
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String content = getContentAsString();
        if (content.contains("404 - not found") ||
                content.contains("Could not find links based on the given ID")) {
            throw new URLNotAvailableAnymoreException("Link does not exist"); //let to know user in FRD
        }
    }

    private void stepCaptcha(MethodBuilder method) throws Exception {
        if (getContentAsString().contains("recaptcha"))
            stepReCaptcha(method);
        else if (getContentAsString().contains("solvemedia"))
            stepSolveMediaCaptcha(method);
        else if (getContentAsString().contains("fancycaptcha"))
            stepFancyCaptcha(method);
        else
            throw new PluginImplementationException("Error: Unsupported captcha type");
    }

    private void stepReCaptcha(MethodBuilder method) throws Exception {
        final Matcher m = getMatcherAgainstContent("(?:challenge|noscript)\\?k=(.+?)\"");
        if (!m.find()) throw new PluginImplementationException("Captcha key not found");
        final String reCaptchaKey = m.group(1);
        final ReCaptcha r = new ReCaptcha(reCaptchaKey, client);
        final String captcha = getCaptchaSupport().getCaptcha(r.getImageURL());
        if (captcha == null) {
            throw new CaptchaEntryInputMismatchException();
        }
        r.setRecognized(captcha);
        r.modifyResponseMethod(method);
    }

    private void stepSolveMediaCaptcha(MethodBuilder method) throws Exception {
        final Matcher m = getMatcherAgainstContent("challenge\\.(?:no)?script\\?k=(.+?)\"");
        if (!m.find()) throw new PluginImplementationException("Captcha key not found");
        final String captchaKey = m.group(1);
        final SolveMediaCaptcha solveMediaCaptcha = new SolveMediaCaptcha(captchaKey, client, getCaptchaSupport(), true);
        solveMediaCaptcha.askForCaptcha();
        solveMediaCaptcha.modifyResponseMethod(method);
    }

    private void stepFancyCaptcha(MethodBuilder method) throws Exception {
        MethodBuilder builder = getMethodBuilder().setAction(fileURL).setAction(fileURL)
                .setParameter("captchaVerify", "1").setParameter("fancycaptcha", "true");
        if (!makeRedirectedRequest(builder.toPostMethod())) { //we make the main request
            checkProblems();//check problems
            throw new ServiceConnectionProblemException("error loading captcha answer");
        }
        method.setParameter("captchaVerify", "1").setParameter("captcha", getContentAsString());
    }

}