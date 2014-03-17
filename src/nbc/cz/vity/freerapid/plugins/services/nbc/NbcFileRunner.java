package cz.vity.freerapid.plugins.services.nbc;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.services.rtmp.AbstractRtmpRunner;
import cz.vity.freerapid.plugins.services.rtmp.RtmpSession;
import cz.vity.freerapid.plugins.webclient.DownloadClientConsts;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.interfaces.FileStreamRecognizer;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.ByteArrayRequestEntity;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;

import java.util.Locale;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author ntoskrnl
 */
class NbcFileRunner extends AbstractRtmpRunner implements FileStreamRecognizer {
    private final static Logger logger = Logger.getLogger(NbcFileRunner.class.getName());

    private final static String AMF_STRING = "0000000000010016676574436C6970496E666F2E676574436C6970416C6C00022F310000001F0A00000004020007FFFFFFFFFFFFFF02000255530200033633320200022D31";

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkNameAndSize();
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize() throws ErrorDuringDownloadingException {
        final String name = PlugUtils.unescapeHtml(PlugUtils.getStringBetween(getContentAsString(), "<title>", " - Video - NBC.com</title>"));
        httpFile.setFileName(name + ".flv");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    private String getClipId() throws ErrorDuringDownloadingException {
        final Matcher matcher = PlugUtils.matcher("/(\\d+?)/", fileURL);
        if (!matcher.find()) {
            throw new PluginImplementationException("Error parsing file URL");
        }
        return matcher.group(1);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        setClientParameter(DownloadClientConsts.FILE_STREAM_RECOGNIZER, this);
        HttpMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkNameAndSize();

            final String clipIdHex = new String(Hex.encodeHex(getClipId().getBytes("UTF-8")));
            if (clipIdHex.length() != 14) {
                logger.warning("clipIdHex length is not 14, next step will probably fail; clipIdHex = " + clipIdHex);
            }
            final byte[] bytes = Hex.decodeHex(AMF_STRING.replace("FFFFFFFFFFFFFF", clipIdHex).toCharArray());
            method = getPostMethod("http://video.nbcuni.com/amfphp/gateway.php");
            ((PostMethod) method).setRequestEntity(new ByteArrayRequestEntity(bytes, "application/x-amf"));
            if (!makeRedirectedRequest(method)) {
                throw new ServiceConnectionProblemException();
            }
            final Matcher matcher = getMatcherAgainstContent("clipurl.{3,5}(nbcrewind[^\\s]+)");
            if (!matcher.find()) {
                throw new PluginImplementationException("Error parsing AMF response");
            }
            method = getGetMethod("http://video.nbcuni.com/" + matcher.group(1));
            if (!makeRedirectedRequest(method)) {
                throw new ServiceConnectionProblemException();
            }
            final String playName = PlugUtils.getStringBetween(getContentAsString(), "<ref src=\"", "\"").replace(".flv", "");

            method = getGetMethod("http://videoservices.nbcuni.com/player/config?configId=16009&version=2&clear=true");
            if (!makeRedirectedRequest(method)) {
                throw new ServiceConnectionProblemException();
            }
            final String host = PlugUtils.getStringBetween(getContentAsString(), "<akamaiHostName>", "</akamaiHostName>");
            final String app = PlugUtils.getStringBetween(getContentAsString(), "<akamaiAppName>", "</akamaiAppName>");

            final RtmpSession rtmpSession = new RtmpSession(host, 1935, app, playName);
            rtmpSession.getConnectParams().put("swfUrl", "http://www.nbc.com/assets/video/4-0/swf/core/video_player_extension.swf");
            rtmpSession.getConnectParams().put("pageUrl", fileURL);
            tryDownloadAndSaveFile(rtmpSession);
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        if (getContentAsString().contains("Page not found") || getContentAsString().contains(" - All Videos - ")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

    @Override
    public boolean isStream(HttpMethod method, boolean showWarnings) {
        final Header h = method.getResponseHeader("Content-Type");
        if (h == null) return false;
        final String contentType = h.getValue().toLowerCase(Locale.ENGLISH);
        return (!contentType.startsWith("text/") && !contentType.contains("xml") && !contentType.equals("application/x-amf") && !contentType.equals("application/smil"));
    }

}