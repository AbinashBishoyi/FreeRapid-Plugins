package cz.vity.freerapid.plugins.services.zippyshare;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import cz.vity.freerapid.plugins.webclient.utils.ScriptUtils;
import org.apache.commons.httpclient.HttpMethod;

import java.net.URLDecoder;
import java.util.logging.Logger;
import java.util.regex.Matcher;


/**
 * @author Vity+ntoskrnl+tonyk+CapCap
 */
class ZippyShareFileRunner extends AbstractRunner {
    private static final Logger logger = Logger.getLogger(ZippyShareFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final HttpMethod httpMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(httpMethod)) {
            checkProblems();
            checkNameAndSize();
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        HttpMethod httpMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(httpMethod)) {
            checkProblems();
            checkNameAndSize();
            final String url;
            Matcher matcher = getMatcherAgainstContent("<script[^<>]*?>([^<>]*?)document\\.getElementById\\('dlbutton'\\)\\.href\\s*=\\s*([^<>]+?)</script>");
            if (matcher.find()) {
                final String script = matcher.group(1) + matcher.group(2);
                logger.info(script);
                url = ScriptUtils.evaluateJavaScriptToString(script);
            } else {
                matcher = getMatcherAgainstContent("url\\s*:\\s*'(.+?)'");
                if (!matcher.find()) {
                    throw new PluginImplementationException("Download link not found");
                }
                final String urlParam = matcher.group(1);
                matcher = getMatcherAgainstContent("seed\\s*:\\s*(\\d+)");
                if (!matcher.find()) {
                    throw new PluginImplementationException("Seed parameter not found");
                }
                final int seed = Integer.parseInt(matcher.group(1));
                url = urlParam + "&time=" + (3 * seed % 1424574);
            }
            httpMethod = getMethodBuilder().setReferer(fileURL).setAction(url).toGetMethod();
            if (!tryDownloadAndSaveFile(httpMethod)) {
                checkProblems();
                throw new ServiceConnectionProblemException("Error starting download");
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }


    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("The requsted file does not exist on this server")
                || contentAsString.contains("File has expired")
                || contentAsString.contains("<h1>HTTP Status")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

    private void checkNameAndSize() throws Exception {
        Matcher matcher = getMatcherAgainstContent("document\\.getElementById\\('dlbutton'\\)\\.href.+/(.+?)\";");
        if (matcher.find()) {
            httpFile.setFileName(URLDecoder.decode(matcher.group(1), "UTF-8"));
        } else {
            matcher = getMatcherAgainstContent("Name:\\s*?<.+?>\\s*?<.+?>(.+?)<.+?>");
            if (!matcher.find()) {
                throw new PluginImplementationException("File name not found");
            }
            httpFile.setFileName(matcher.group(1));
        }
        matcher = getMatcherAgainstContent("Size:\\s*?<.+?>\\s*?<.+?>(.+?)<.+?>");
        if (!matcher.find()) {
            throw new PluginImplementationException("File size not found");
        }
        httpFile.setFileSize(PlugUtils.getFileSizeFromString(matcher.group(1)));
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

}