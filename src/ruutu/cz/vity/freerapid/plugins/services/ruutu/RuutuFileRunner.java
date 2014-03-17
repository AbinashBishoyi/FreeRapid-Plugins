package cz.vity.freerapid.plugins.services.ruutu;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.services.rtmp.AbstractRtmpRunner;
import cz.vity.freerapid.plugins.services.rtmp.RtmpSession;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.methods.GetMethod;

import java.net.URLDecoder;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author ntoskrnl
 */
class RuutuFileRunner extends AbstractRtmpRunner {
    private final static Logger logger = Logger.getLogger(RuutuFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final GetMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkName();
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkName() throws ErrorDuringDownloadingException {
        if (getContentAsString().contains("<p class=\"name\"></p>")) {
            final Matcher matcher = PlugUtils.matcher("avid=(\\d+)", fileURL);
            if (matcher.find()) {
                httpFile.setFileName(matcher.group(1));
            } else {
                httpFile.setFileName("Ruutu-" + System.currentTimeMillis());
            }
        } else {
            PlugUtils.checkName(httpFile, getContentAsString(), "<p class=\"name\">", "</p>");
        }
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    private void checkFileExt(final String url) throws ErrorDuringDownloadingException {
        final int index = url.lastIndexOf('.');
        if (index > -1) {
            final String ext = url.substring(index);
            httpFile.setFileName(httpFile.getFileName() + ext);
        }
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);

        GetMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkName();

            final String providerURL = URLDecoder.decode(PlugUtils.getStringBetween(getContentAsString(), "'providerURL', '", "'"), "UTF-8");
            method = getGetMethod(providerURL);
            if (!makeRedirectedRequest(method)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            checkProblems();

            if (getContentAsString().contains("<AudioSourceFile>")) {
                final String sourceFile = PlugUtils.getStringBetween(getContentAsString(), "<AudioSourceFile>", "</AudioSourceFile>");
                checkFileExt(sourceFile);
                method = getGetMethod(sourceFile);
                if (!tryDownloadAndSaveFile(method)) {
                    checkProblems();
                    throw new ServiceConnectionProblemException("Error starting download");
                }
            } else {
                final String sourceFile = PlugUtils.getStringBetween(getContentAsString(), "<SourceFile>", "</SourceFile>");
                checkFileExt(sourceFile);
                final RtmpSession rtmpSession = new RtmpSession(sourceFile);
                tryDownloadAndSaveFile(rtmpSession);
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        if (getContentAsString().contains("Sivua ei löytynyt")) {
            throw new URLNotAvailableAnymoreException("Page not found");
        }
        if (getContentAsString().contains("<ErrorText>")) {
            throw new URLNotAvailableAnymoreException(PlugUtils.getStringBetween(getContentAsString(), "<ErrorText>", "</ErrorText>"));
        }
    }

}