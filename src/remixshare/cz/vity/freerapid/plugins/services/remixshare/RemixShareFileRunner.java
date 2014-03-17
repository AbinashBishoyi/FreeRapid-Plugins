package cz.vity.freerapid.plugins.services.remixshare;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author ntoskrnl
 */
class RemixShareFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(RemixShareFileRunner.class.getName());
    private String FILE_TYPE = "file";

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        client.getHTTPClient().getState().addCookie(new Cookie(".remixshare.com", "lang_en", "english", "/", 86400, false));
        final GetMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkNameAndSize();
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize() throws ErrorDuringDownloadingException {
        if (fileURL.contains("/img/")) {
            FILE_TYPE = "image";
            PlugUtils.checkName(httpFile, getContentAsString(), "<span class='light'>", "</span>");
        } else {
            //<span title='logovq.zip'>logovq.zip</span><span class='light2'>&nbsp;(0.06&nbsp;MB)</span>
            final Matcher matcher = getMatcherAgainstContent("<span title='.+?'>(.+?)</span><span class='light2'>&nbsp;\\((.+?)&nbsp;(..)\\)</span>");
            if (!matcher.find()) throw new PluginImplementationException("File name/size not found");
            httpFile.setFileName(matcher.group(1));
            httpFile.setFileSize(PlugUtils.getFileSizeFromString(matcher.group(2) + matcher.group(3)));
        }
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("The file couldn't be found") || contentAsString.contains("This Image doesn't exists") || contentAsString.contains("Page Not Found")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        client.getHTTPClient().getState().addCookie(new Cookie(".remixshare.com", "lang_en", "english", "/", 86400, false));
        final GetMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkNameAndSize();
            HttpMethod httpMethod;

            if (FILE_TYPE.equals("image")) {
                httpMethod = getMethodBuilder().setReferer(fileURL).setActionFromAHrefWhereATagContains("Full Size").toGetMethod();
                if (!tryDownloadAndSaveFile(httpMethod)) {
                    checkProblems();
                    throw new PluginImplementationException();
                }

            } else {
                if (getContentAsString().contains("Download password")) {
                    while (getContentAsString().contains("Download password")) {
                        httpMethod = getMethodBuilder().setReferer(fileURL).setActionFromFormByName("pass", true).setParameter("passwd", getPassword()).toPostMethod();
                        makeRedirectedRequest(httpMethod);
                    }
                    checkProblems();
                }

                httpMethod = getMethodBuilder().setReferer(fileURL).setActionFromAHrefWhereATagContains("DOWNLOAD").toGetMethod();
                if (!tryDownloadAndSaveFile(httpMethod)) {
                    checkProblems();
                    throw new PluginImplementationException();
                }
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private String getPassword() throws Exception {
        RemixSharePasswordUI ps = new RemixSharePasswordUI();
        if (getDialogSupport().showOKCancelDialog(ps, "Secured file on RemixShare")) {
            return (ps.getPassword());
        } else throw new NotRecoverableDownloadException("This file is secured with a password");
    }

}