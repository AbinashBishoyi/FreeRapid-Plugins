package cz.vity.freerapid.plugins.services.ruutu;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.services.rtmp.AbstractRtmpRunner;
import cz.vity.freerapid.plugins.services.rtmp.RtmpSession;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;

import java.net.URLDecoder;
import java.util.logging.Logger;

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
        final HttpMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkName();
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkName() throws ErrorDuringDownloadingException {
        final String name = PlugUtils.getStringBetween(getContentAsString(), "<meta property=\"og:title\" content=\"", "\"");
        httpFile.setFileName(name + ".flv");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);

        HttpMethod method = getGetMethod(fileURL);
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
                httpFile.setFileName(httpFile.getFileName().replaceFirst("\\.flv$", ".mp3"));
                final String sourceFile = PlugUtils.getStringBetween(getContentAsString(), "<AudioSourceFile>", "</AudioSourceFile>");
                method = getGetMethod(sourceFile);
                if (!tryDownloadAndSaveFile(method)) {
                    checkProblems();
                    throw new ServiceConnectionProblemException("Error starting download");
                }
            } else {
                final String sourceFile = PlugUtils.getStringBetween(getContentAsString(), "<SourceFile>", "</SourceFile>");
                final RtmpSession rtmpSession = new RtmpSession(sourceFile);
                tryDownloadAndSaveFile(rtmpSession);
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        if (getContentAsString().contains("Sivua ei löydy")) {
            throw new URLNotAvailableAnymoreException("Page not found");
        }
        if (getContentAsString().contains("<ErrorText>")) {
            throw new URLNotAvailableAnymoreException(PlugUtils.getStringBetween(getContentAsString(), "<ErrorText>", "</ErrorText>"));
        }
    }

}