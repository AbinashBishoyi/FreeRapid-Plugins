package cz.vity.freerapid.plugins.services.safelinking;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadState;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.hoster.CaptchaSupport;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpMethod;
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

    protected URI stepDirectLink(String directLinkURL) throws Exception {
        final GetMethod method = getGetMethod(directLinkURL);
        if (makeRedirectedRequest(method)) {
            return new URI(method.getURI().getURI());
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

        List<URI> list = new LinkedList<URI>();

        if (fileURL.contains("/d/")) {
            list.add(stepDirectLink(fileURL));
        } else if (fileURL.contains("/p/")) {
            HttpMethod method = getGetMethod(fileURL); //create GET request
            if (!makeRedirectedRequest(method)) { //we make the main request
                checkProblems();//check problems
                throw new ServiceConnectionProblemException();
            }
            int count = 0;
            while (!getContentAsString().contains(HEADER_LINK_TYPE_1) &&
                    !getContentAsString().contains(HEADER_LINK_TYPE_2) &&
                    (count++ < 3)) {
                MethodBuilder builder = getMethodBuilder()
                        .setActionFromFormWhereTagContains("Protected link", true)
                        .setReferer(fileURL).setAction(fileURL);
                String content = getContentAsString();
                // check 4 & complete captcha
                if (content.contains("Captcha loading, please wait") ||
                        content.contains("The CAPTCHA code you entered was wrong")) {
                    stepCaptcha(builder);
                }
                // check 4 & complete password
                if (content.contains("Link password")) {
                    final String password = getDialogSupport().askForPassword("SafeLinking");
                    if (password == null) {
                        throw new ServiceConnectionProblemException("This file is secured with a password");
                    }
                    builder.setParameter("link-password", password);
                }
                method = builder.toPostMethod();
                if (!makeRedirectedRequest(method)) { //we make the main request
                    checkProblems();//check problems
                    throw new ServiceConnectionProblemException("err 1");
                }
            }

            String content;
            if (getContentAsString().contains(HEADER_LINK_TYPE_1)) {
                content = PlugUtils.getStringBetween(getContentAsString(), HEADER_LINK_TYPE_1, "</fieldset>");
            } else if (getContentAsString().contains(HEADER_LINK_TYPE_2)) {
                content = PlugUtils.getStringBetween(getContentAsString(), HEADER_LINK_TYPE_2, "</fieldset>");
            } else {
                throw new PluginImplementationException("Captcha Text Error : SafeLinking site changed : SafeLinking feature not supported yet");
            }

            Matcher m = PlugUtils.matcher("<a href=\"([^\"]+)\" class=\"result-a\">", content);
            while (m.find()) {
                list.add(new URI(m.group(1)));
            }
            if (getContentAsString().contains(HEADER_LINK_TYPE_1)) {
                for (int ii = 0; ii < list.size(); ii++)
                    list.set(ii, stepDirectLink(list.get(ii).toASCIIString()));
            }

        } else {
            checkProblems();
            throw new ServiceConnectionProblemException("Invalid link");
        }
        if (list.isEmpty()) throw new PluginImplementationException("No links found");
        getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(httpFile, list);
        httpFile.setFileName("Link(s) Extracted !");
        httpFile.setState(DownloadState.COMPLETED);
        httpFile.getProperties().put("removeCompleted", true);
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String content = getContentAsString();
        if (content.contains("404 - not found") || content.contains("This link does not exist")) {
            throw new URLNotAvailableAnymoreException("Link does not exist"); //let to know user in FRD
        }
        if (content.contains("server is not currently responding")) {
            throw new ServiceConnectionProblemException("server is not currently responding"); //let to know user in FRD
        }
    }

    private void stepCaptcha(MethodBuilder method) throws Exception {
        Matcher m = getMatcherAgainstContent("ckey:'(.+?)',apiserver");
        if (!m.find()) throw new PluginImplementationException("Captcha key not found");
        final String captchaKey = m.group(1);

        String mediaType;
        do {
            HttpMethod httpMethod = getMethodBuilder()
                    .setReferer(fileURL).setAction("https://api-secure.solvemedia.com/papi/_challenge.js")
                    .setParameter("k", captchaKey + ";f=_ACPuzzleUtil.callbacks%5B0%5D;l=en;t=img;s=standard;c=js,swf11,swf11.2,swf,h5c,h5ct,svg,h5v,v/h264,v/ogg,v/webm,h5a,a/mp3,a/ogg,ua/chrome,ua/chrome18,os/nt,os/nt6.0,fwv/htyg64,jslib/jquery,jslib/jqueryui;ts=1339103245;th=custom;r=" + Math.random())
                    .toGetMethod();
            if (!makeRedirectedRequest(httpMethod)) {
                throw new ServiceConnectionProblemException();
            }
            final Matcher mediaTypeMatcher = getMatcherAgainstContent("\"mediatype\"\\s*:\\s*\"(.+?)\",");
            if (!mediaTypeMatcher.find()) {
                throw new PluginImplementationException("Captcha media type not found");
            }
            mediaType = mediaTypeMatcher.group(1);
        } while (!mediaType.equals("img"));

        m = getMatcherAgainstContent("\"chid\"\\s*:\\s*\"(.+?)\",");
        if (!m.find()) throw new PluginImplementationException("Captcha ID not found");
        final String captchaChID = m.group(1);
        final String captchaImg = "https://api-secure.solvemedia.com/papi/media?c=" + captchaChID + ";w=300;h=150;fg=333333;bg=ffffff";

        final CaptchaSupport captchaSupport = getCaptchaSupport();
        final String captchaTxt = captchaSupport.getCaptcha(captchaImg);
        if (captchaTxt == null) throw new CaptchaEntryInputMismatchException("No Input");

        method.setParameter("adcopy_challenge", captchaChID);
        method.setParameter("solvemedia_response", captchaTxt);
    }

}