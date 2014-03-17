package cz.vity.freerapid.plugins.services.x7;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author ntoskrnl
 */
class X7FileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(X7FileRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
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
        final Matcher name = getMatcherAgainstContent("<span style=\"text-shadow:#5855aa 1px 1px 2px\">(.+?)<small style=\"color:#ffe190\">(.*?)</small></span>");
        if (!name.find()) throw new PluginImplementationException("File name not found");
        httpFile.setFileName(name.group(1) + name.group(2));

        final Matcher size = getMatcherAgainstContent("\\(([^<>\\(\\)]+?B)\\)");
        if (!size.find()) throw new PluginImplementationException("File size not found");
        httpFile.setFileSize(PlugUtils.getFileSizeFromString(size.group(1)));

        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkNameAndSize();

            final String id = PlugUtils.getStringBetween(getContentAsString(), "var dlID = '", "';");
            final HttpMethod download;

            if (getContentAsString().contains("<b>Stream</b>")) { //video
                // Currently only download of the HD version of the video is supported.
                // Standard video can be downloaded by changing the last 'h' to an 'l'.
                download = getMethodBuilder().setAction("http://x7.to/stream/" + id + "/h").toGetMethod();

            } else { //regular file
                final HttpMethod httpMethod = getMethodBuilder().setReferer(fileURL).setAction("http://x7.to/james/ticket/dl/" + id).toPostMethod();
                httpMethod.addRequestHeader("X-Requested-With", "XMLHttpRequest");//use AJAX
                if (!(client.getHTTPClient().executeMethod(httpMethod) == 200))
                    throw new ServiceConnectionProblemException("Error connecting to download ticket server");

                final String content = forcedGetContentAsString(httpMethod);
                logger.info("Server response: " + content);
                if (content.contains("File not found"))
                    throw new URLNotAvailableAnymoreException("File not found on download ticket server");
                if (content.contains("err:")) {
                    final String err = PlugUtils.getStringBetween(content, "err:\"", "\"");
                    if (err.equals("limit-dl"))
                        throw new ServiceConnectionProblemException("Download limit reached");
                    else
                        throw new ServiceConnectionProblemException("Download ticket error: " + err);
                }
                if (!content.contains("wait:") || !content.contains("url:"))
                    throw new PluginImplementationException("Unexpected download ticket server response");

                final String action = PlugUtils.getStringBetween(content, "url:'", "'");
                final String wait = PlugUtils.getStringBetween(content, "wait:", ",");

                download = getMethodBuilder().setReferer(fileURL).setAction(action).toGetMethod();

                //waiting does not seem necessary
                //downloadTask.sleep(Integer.parseInt(wait) + 1);
            }

            if (!tryDownloadAndSaveFile(download)) {
                checkProblems();
                throw new ServiceConnectionProblemException("Error starting download");
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String content = getContentAsString();
        if (content.contains("<title>File not found!</title>") || content.contains("<h1>Not Found</h1>"))
            throw new URLNotAvailableAnymoreException("File not found");
    }

    private String forcedGetContentAsString(HttpMethod method) {

        String content = null;
        try {
            content = method.getResponseBodyAsString();
        } catch (IOException ex) {
            //ignore
        }
        if (content == null) {
            content = "";
            InputStream is = null;
            try {
                is = method.getResponseBodyAsStream();
                if (is != null) {
                    int i = 0;
                    while ((i = is.read()) != -1) {
                        content += (char) i;
                    }
                }
            } catch (IOException ex) {
                //ignore
            } finally {
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException ex) {
                        //ignore
                    }
                }
            }

        }
        return content;
    }

}