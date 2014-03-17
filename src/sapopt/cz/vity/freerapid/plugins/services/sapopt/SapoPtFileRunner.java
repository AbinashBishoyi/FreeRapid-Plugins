package cz.vity.freerapid.plugins.services.sapopt;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author tong2shot
 */
class SapoPtFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(SapoPtFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkNameAndSize(getContentAsString());
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        try {
            PlugUtils.checkName(httpFile, content, "<div class=\"tit\">", "</div>");
        } catch (Exception e) {
            PlugUtils.checkName(httpFile, content, "<div class=\"titvideo\">", "</div>");
        }
        httpFile.setFileName(httpFile.getFileName() + ".mp4");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkNameAndSize(getContentAsString());
            checkURL();
            final String fileId = fileURL.substring(fileURL.lastIndexOf("/") + 1);
            final String videoURL = PlugUtils.getStringBetween(getContentAsString(), "videoVerifyMrec(\"", "\"");
            final Matcher matcher = getMatcherAgainstContent("<script type=\"text/javascript\" src=\"(http://\\w*\\.?sapo\\.pt/sapovideo/js/script\\.js.*?)\"");
            if (!matcher.find()) {
                throw new PluginImplementationException("Video javascript URL not found");
            }
            final String jsURL = matcher.group(1);
            HttpMethod httpMethod = getMethodBuilder().setReferer(fileURL).setAction(jsURL).toGetMethod();
            setFileStreamContentTypes(new String[0], new String[]{"application/x-javascript"});
            if (!makeRedirectedRequest(httpMethod)) {
                throw new ServiceConnectionProblemException("Error loading video javascript");
            }
            final String swfURL = PlugUtils.getStringBetween(getContentAsString(), "swfobject.embedSWF('", "'");
            final String player = swfURL.contains("flvplayer.swf") ? "INTERNO" : "EXTERNO";
            final long time = System.currentTimeMillis() / 1000;
            final String token = DigestUtils.md5Hex("sve9f#73s" + fileId + time);
            httpMethod = getMethodBuilder()
                    .setReferer(swfURL)
                    .setAction(videoURL)
                    .setParameter("player", player)
                    .setParameter("time", String.valueOf(time))
                    .setParameter("token", token)
                    .toGetMethod();
            if (!tryDownloadAndSaveFile(httpMethod)) {
                checkProblems();
                throw new ServiceConnectionProblemException("Error starting download");
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkURL() {
        if (fileURL.endsWith("/")) fileURL = fileURL.substring(0, fileURL.length() - 1); // remove "/" at tail
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("aceder não existe")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

}