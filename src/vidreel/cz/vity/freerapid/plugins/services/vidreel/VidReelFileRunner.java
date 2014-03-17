package cz.vity.freerapid.plugins.services.vidreel;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.services.rtmp.AbstractRtmpRunner;
import cz.vity.freerapid.plugins.services.rtmp.RtmpSession;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import cz.vity.freerapid.utilities.LogUtils;
import org.apache.commons.httpclient.HttpMethod;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.zip.InflaterInputStream;

/**
 * Class which contains main code
 *
 * @author ntoskrnl
 */
class VidReelFileRunner extends AbstractRtmpRunner {
    private final static Logger logger = Logger.getLogger(VidReelFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        if (makeRedirectedRequest(getGetMethod(fileURL))
                && makeRedirectedRequest(getGetMethod(fileURL))) {
            checkProblems();
            checkNameAndSize();
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize() throws ErrorDuringDownloadingException {
        final String name = PlugUtils.getStringBetween(getContentAsString(), "<h4>", "<");
        httpFile.setFileName(name + ".flv");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        runCheck();
        final String playName = "mp4:" + PlugUtils.getStringBetween(getContentAsString(), ".addVariable('file','", "')");
        final String swfUrlRelative = PlugUtils.getStringBetween(getContentAsString(), "new SWFObject('", "'");
        final String swfUrl = new URI(fileURL).resolve(new URI(swfUrlRelative)).toString();
        final String streamServer = getStreamServer(swfUrl);
        final RtmpSession rtmpSession = new RtmpSession(streamServer, playName);
        rtmpSession.getConnectParams().put("swfUrl", swfUrl);
        rtmpSession.getConnectParams().put("pageUrl", fileURL);
        tryDownloadAndSaveFile(rtmpSession);
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        if (getContentAsString().contains("Video Not Found")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

    private String getStreamServer(final String swfUrl) throws Exception {
        final HttpMethod method = getMethodBuilder()
                .setReferer(fileURL)
                .setAction(swfUrl)
                .toGetMethod();
        final InputStream is = client.makeRequestForFile(method);
        if (is == null) {
            throw new ServiceConnectionProblemException("Error downloading SWF");
        }
        final String swf = readSwfStreamToString(is);
        final Matcher matcher = PlugUtils.matcher("(rtmp://[\\w\\.]+?/vod/)", swf);
        if (!matcher.find()) {
            throw new PluginImplementationException("Error parsing SWF");
        }
        return matcher.group(1);
    }

    private static String readSwfStreamToString(InputStream is) throws IOException {
        try {
            final byte[] bytes = new byte[2048];
            if (readBytes(is, bytes, 8) != 8) {
                throw new IOException("Error reading from stream");
            }
            if (bytes[0] == 'C' && bytes[1] == 'W' && bytes[2] == 'S') {
                bytes[0] = 'F';
                is = new InflaterInputStream(is);
            } else if (bytes[0] != 'F' || bytes[1] != 'W' || bytes[2] != 'S') {
                throw new IOException("Invalid SWF stream");
            }
            final StringBuilder sb = new StringBuilder(8192);
            sb.append(new String(bytes, 0, 8, "ISO-8859-1"));
            int len;
            while ((len = is.read(bytes)) != -1) {
                sb.append(new String(bytes, 0, len, "ISO-8859-1"));
            }
            return sb.toString();
        } finally {
            try {
                is.close();
            } catch (final Exception e) {
                LogUtils.processException(logger, e);
            }
        }
    }

    private static int readBytes(InputStream is, byte[] buffer, int count) throws IOException {
        int read = 0, i;
        while (count > 0 && (i = is.read(buffer, 0, count)) != -1) {
            count -= i;
            read += i;
        }
        return read;
    }

}