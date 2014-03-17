package cz.vity.freerapid.plugins.services.billionuploads;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingRunner;
import cz.vity.freerapid.plugins.services.xfilesharing.captcha.ReCaptchaType;
import cz.vity.freerapid.plugins.services.xfilesharing.nameandsize.FileNameHandler;
import cz.vity.freerapid.plugins.services.xfilesharing.nameandsize.FileSizeHandler;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.httpclient.HttpMethod;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.net.URLDecoder;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class BillionUploadsFileRunner extends XFileSharingRunner {

    @Override
    protected List<FileNameHandler> getFileNameHandlers() {
        final List<FileNameHandler> fileNameHandlers = super.getFileNameHandlers();
        fileNameHandlers.add(0, new BillionUploadsFileNameHandler());
        return fileNameHandlers;
    }

    @Override
    protected List<FileSizeHandler> getFileSizeHandlers() {
        final List<FileSizeHandler> fileSizeHandlers = super.getFileSizeHandlers();
        fileSizeHandlers.add(0, new BillionUploadsFileSizeHandler());
        return fileSizeHandlers;
    }

    @Override
    protected List<String> getDownloadPageMarkers() {
        final List<String> downloadPageMarkers = super.getDownloadPageMarkers();
        downloadPageMarkers.add("Download you file easily with Billion Uploads download manager");
        return downloadPageMarkers;
    }

    @Override
    protected List<String> getDownloadLinkRegexes() {
        final List<String> downloadLinkRegexes = new LinkedList<String>();
        downloadLinkRegexes.add("<a href\\s?=\\s?(?:\"|')(http.+?)(?:\"|') id=\"_tlink\"");
        downloadLinkRegexes.add("<span subway=\"metro\">.+?XXX(.+?)XXX.+?<");
        return downloadLinkRegexes;
    }

    @Override
    protected String getDownloadLinkFromRegexes() throws ErrorDuringDownloadingException {
        final String url = super.getDownloadLinkFromRegexes();
        if (url.contains("http:"))
            return url;
        return new String(Base64.decodeBase64(new String(Base64.decodeBase64(url))));
    }

    @Override
    protected MethodBuilder getXFSMethodBuilder() throws Exception {
        final String content = getContentAsString();
        if (!content.contains("decodeURIComponent("))
            throw new PluginImplementationException("error loading page");
        final String secretInput = URLDecoder.decode(PlugUtils.getStringBetween(content, "decodeURIComponent(\"", "\")"), "UTF-8");
        final String name = PlugUtils.getStringBetween(secretInput, " name=\"", "\"");
        final String value = PlugUtils.getStringBetween(secretInput, " value=\"", "\"");
        final String hiddenInput = PlugUtils.getStringBetween(content, "$('form[name=\"F1\"]').append($(document.createElement('input'))", "))");
        final String name2 = PlugUtils.getStringBetween(hiddenInput, "'name','", "'");
        String value2;
        try {
            value2 = PlugUtils.getStringBetween(hiddenInput, ".val('", "'");
        } catch (Exception e) {
            final String tag = PlugUtils.getStringBetween(hiddenInput, "source=\"", "\"");
            final Matcher matchTag = PlugUtils.matcher("<.+?\"" + tag + "\".+?>(.+?)<", content);
            if (!matchTag.find())
                throw new PluginImplementationException("Error processing download form");
            value2 = matchTag.group(1);
        }
        MethodBuilder builder = super.getXFSMethodBuilder().setParameter(name, value).setParameter(name2, value2);
        final Matcher match2remove = PlugUtils.matcher("\\('input\\[name=\"(.+?)\"\\]'\\).remove\\(\\)", content);
        while (match2remove.find())
            builder.removeParameter(match2remove.group(1));
        return builder;
    }

    @Override
    protected void checkFileProblems() throws ErrorDuringDownloadingException {
        final String content = getContentAsString();
        if (content.contains("File Not found") || content.contains("File was removed")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
        if (getContentAsString().contains("Access To Website Blocked"))
            throw new ServiceConnectionProblemException("FreeRapid detected as robot - Access To Website Blocked");
        if (content.contains("META NAME=\"ROBOTS\"") || content.contains("META NAME=\"robots\"")) {
            Logger.getLogger(BillionUploadsFileRunner.class.getName()).info("FREERAPID DETECTED AS ROBOT");
            checkRobotDetectorProblem();
        }
        super.checkFileProblems();
    }

    private void checkRobotDetectorProblem() throws ErrorDuringDownloadingException {
        if (getContentAsString().contains("recaptcha/api")) {
            final MethodBuilder builder = getMethodBuilder()
                    .setActionFromFormWhereActionContains("Incapsula_Resource", true);
            try {
                new ReCaptchaType().handleCaptcha(builder, client, getCaptchaSupport());
                if (!makeRedirectedRequest(builder.toPostMethod()))
                    throw new ServiceConnectionProblemException();
                if (getContentAsString().contains("window.parent.location.reload(true);")) {
                    if (!makeRedirectedRequest(getGetMethod(fileURL)))
                        throw new ServiceConnectionProblemException();
                }
            } catch (Exception e) {
                throw new PluginImplementationException("Robot captcha processing error");
            }
        } else if (getContentAsString().contains("iframe src=\"")) {
            try {
                if (!makeRedirectedRequest(getMethodBuilder().setAction(PlugUtils.getStringBetween(getContentAsString(), "iframe src=\"", "\" ")).toGetMethod()))
                    throw new ServiceConnectionProblemException();
            } catch (Exception e) {
                throw new PluginImplementationException("#$^%&?* BillionUploads !!!");
            }
        } else {
            throw new YouHaveToWaitException("RobotDecection MUST DIE BillionUploads DIE", 5);
        }
        checkFileProblems();
    }

    @Override
    protected void checkNameAndSize() throws ErrorDuringDownloadingException {
        setSecurityPageCookie();
        super.checkNameAndSize();
    }

    private void setSecurityPageCookie() throws ErrorDuringDownloadingException {
        if (getContentAsString().contains("This process is automatic. Your browser will redirect to your requested content shortly")) {
            Integer eval = evaluate(PlugUtils.getStringBetween(getContentAsString(), "a.value =", ";"));
            final String answer = "" + (eval + ("billionuploads.com").length());
            final HttpMethod securityMethod = getMethodBuilder()
                    .setActionFromFormByName("challenge-form", true)
                    .setParameter("jschl_answer", answer)
                    .toGetMethod();
            try {
                makeRequest(securityMethod);
            } catch (Exception e) {
                throw new ErrorDuringDownloadingException(e.getMessage());
            }
            super.checkFileProblems();
        }
    }


    private int evaluate(final String equationString) throws ErrorDuringDownloadingException {
        try {
            ScriptEngineManager mgr = new ScriptEngineManager();
            ScriptEngine engine = mgr.getEngineByName("JavaScript");
            return Integer.parseInt("" + engine.eval(equationString));
        } catch (Exception e) {
            throw new ErrorDuringDownloadingException(e.getMessage());
        }
    }
}