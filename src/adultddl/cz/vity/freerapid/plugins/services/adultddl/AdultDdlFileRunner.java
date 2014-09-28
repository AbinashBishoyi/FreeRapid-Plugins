package cz.vity.freerapid.plugins.services.adultddl;

import cz.vity.freerapid.plugins.container.FileInfo;
import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadState;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.net.URI;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class AdultDdlFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(AdultDdlFileRunner.class.getName());

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL); //create GET request
        if (fileURL.contains("img.adultddl")) {
            httpFile.setFileName(fileURL.substring(1 + fileURL.lastIndexOf("/")));
            if (!tryDownloadAndSaveFile(method)) {
                checkProblems();//if downloading failed
                throw new ServiceConnectionProblemException("Error starting download");//some unknown problem
            }
        } else {
            if (makeRedirectedRequest(method)) { //we make the main request
                checkProblems();//check problems
                if (fileURL.contains("secure.adultddl")) {
                    final HttpMethod captchaMethod = getMethodBuilder().setReferer(fileURL)
                            .setActionFromFormWhereActionContains("captcha", true).toPostMethod();
                    boolean captchaLoop;
                    do {
                        captchaLoop = false;
                        if (!makeRequest(captchaMethod)) {
                            checkProblems();
                            throw new ServiceConnectionProblemException("Error posting login info");
                        }
                        final HttpMethod verifyMethod = stepCaptcha(getMethodBuilder().setReferer(fileURL)
                                .setActionFromFormWhereActionContains("verify", true)).toPostMethod();
                        if (!makeRequest(verifyMethod)) {
                            checkProblems();
                            throw new ServiceConnectionProblemException("Error posting login info");
                        }
                        checkProblems();
                        if (getContentAsString().contains("The CAPTCHA entered was incorrect"))
                            captchaLoop = true;
                    } while (captchaLoop);

                    List<URI> list = new LinkedList<URI>();
                    final Matcher match = PlugUtils.matcher("href=['\"](http.+?)['\"][^>]*?>\\1<", getContentAsString());
                    while (match.find()) {
                        list.add(new URI(match.group(1)));
                    }
                    if (list.isEmpty()) throw new PluginImplementationException("No links found");
                    getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(httpFile, list);
                    httpFile.setFileName("Link(s) Extracted !");
                    httpFile.setState(DownloadState.COMPLETED);
                    httpFile.getProperties().put("removeCompleted", true);
                } else {
                    final List<FileInfo> list = new LinkedList<FileInfo>();
                    list.add(new FileInfo(new URL(getMethodBuilder().setReferer(fileURL).setActionFromImgSrcWhereTagContains("aligncenter").getEscapedURI())));
                    final Matcher matchL = PlugUtils.matcher("href=['\"](http.+?)['\"][^>]*?_blank[^>]*?><strong>", getContentAsString());
                    while (matchL.find()) {
                        list.add(new FileInfo(new URL(matchL.group(1))));
                    }
                    FileInfo linkList = new FileInfo(new URL(getMethodBuilder().setReferer(fileURL).setActionFromIFrameSrcWhereTagContains("secure").getEscapedURI()));
                    linkList.setFileName("Extract links > ");
                    try { linkList.setFileName(linkList.getFileName() + PlugUtils.getStringBetween(getContentAsString(), "<h1>", "</h1>").trim()); } catch (Exception e) { /**/ }
                    list.add(linkList);
                    if (list.isEmpty()) throw new PluginImplementationException("No links found");
                    getPluginService().getPluginContext().getQueueSupport().addLinksToQueueFromContainer(httpFile, list);
                    httpFile.setFileName("Link(s) Extracted !");
                    httpFile.setState(DownloadState.COMPLETED);
                    httpFile.getProperties().put("removeCompleted", true);
                }
            } else {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("404 Not Found") || contentAsString.contains("404 - Not Found") ||
                contentAsString.contains("page could not be found"))
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        if (contentAsString.contains("Unable to open links"))
            throw new ErrorDuringDownloadingException("Unable to open links, does not exist");

    }

    private MethodBuilder stepCaptcha(MethodBuilder builder) throws Exception {
        final String captchaSrc = getMethodBuilder().setActionFromImgSrcWhereTagContains("captcha").getEscapedURI();
        logger.info("Captcha URL " + captchaSrc);
        final String captcha = getCaptchaSupport().getCaptcha(captchaSrc);
        if (captcha == null) throw new CaptchaEntryInputMismatchException();
        logger.info("Manual captcha " + captcha);
        return builder.setParameter("captcha_code", captcha);
    }
}