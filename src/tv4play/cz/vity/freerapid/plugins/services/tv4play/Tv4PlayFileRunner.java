package cz.vity.freerapid.plugins.services.tv4play;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.services.adobehds.HdsDownloader;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpStatus;

import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author ntoskrnl
 */
public class Tv4PlayFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(Tv4PlayFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final HttpMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {
            checkProblems(getContentAsString());
            checkNameAndSize();
        } else {
            checkProblems(getContentAsString());
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize() throws ErrorDuringDownloadingException {
        final Matcher matcher = getMatcherAgainstContent("<title>(.+?) \\- TV4 Play</title>");
        if (!matcher.find()) {
            throw new PluginImplementationException("File name not found");
        }
        httpFile.setFileName(matcher.group(1).replace(": ", " - ") + ".flv");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        HttpMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems(getContentAsString());
            checkNameAndSize();
            method = getGetMethod("http://prima.tv4play.se/api/web/asset/" + getId() + "/play");
            method.setRequestHeader("X-Forwarded-For", "80.76.149.158");
            // They send "Content-Encoding: utf-8", which is a violation of the HTTP spec.
            // As such, makeRedirectedRequest cannot be used here.
            final int responseCode = client.getHTTPClient().executeMethod(method);
            final String content = method.getResponseBodyAsString();
            if (responseCode != HttpStatus.SC_OK) {
                logger.warning(content);
                checkProblems(content);
                throw new ServiceConnectionProblemException("Error fetching stream info file");
            }
            final Matcher matcher = PlugUtils.matcher("<url>(.+?\\.f4m)</url>", content);
            if (!matcher.find()) {
                throw new PluginImplementationException("Manifest URL not found");
            }
            final HdsDownloader downloader = new HdsDownloader(client, httpFile, downloadTask);
            downloader.tryDownloadAndSaveFile(matcher.group(1) + "?hdcore=2.10.3&g=MCMQTYCEVAFN");
        } else {
            checkProblems(getContentAsString());
            throw new ServiceConnectionProblemException();
        }
    }

    private String getId() throws ErrorDuringDownloadingException {
        final Matcher matcher = PlugUtils.matcher("video_id=(\\d+)", fileURL);
        if (!matcher.find()) {
            throw new PluginImplementationException("Error parsing file URL");
        }
        return matcher.group(1);
    }

    protected void checkProblems(final String content) throws ErrorDuringDownloadingException {
        if (content.contains("Länken du klickat på eller adressen du skrivit in leder ingenstans")
                || content.contains("ASSET_NOT_FOUND")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
        if (content.contains("ASSET_PLAYBACK_INVALID_GEO_LOCATION")) {
            throw new NotRecoverableDownloadException("This video is not available in your geographical location");
        }
        if (content.contains("SESSION_NOT_AUTHENTICATED")) {
            throw new NotRecoverableDownloadException("This video is only available for premium users");
        }
    }

}