package cz.vity.freerapid.plugins.video2audio;

import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.services.rtmp.AbstractRtmpRunner;
import cz.vity.freerapid.plugins.services.rtmp.RtmpClient;
import cz.vity.freerapid.plugins.services.rtmp.RtmpSession;
import cz.vity.freerapid.plugins.webclient.DownloadClient;
import cz.vity.freerapid.plugins.webclient.DownloadClientConsts;
import cz.vity.freerapid.plugins.webclient.DownloadState;
import cz.vity.freerapid.plugins.webclient.utils.HttpUtils;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import cz.vity.freerapid.utilities.LogUtils;
import cz.vity.freerapid.utilities.Utils;
import org.apache.commons.httpclient.HttpMethod;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author ntoskrnl
 */
public abstract class AbstractVideo2AudioRunner extends AbstractRtmpRunner {

    private static final Logger logger = Logger.getLogger(AbstractVideo2AudioRunner.class.getName());

    protected boolean tryDownloadAndSaveFile(final HttpMethod method, final int bitrate, final boolean mp4) throws Exception {
        if (httpFile.getState() == DownloadState.PAUSED || httpFile.getState() == DownloadState.CANCELLED)
            return false;
        else
            httpFile.setState(DownloadState.GETTING);
        if (logger.isLoggable(Level.INFO)) {
            logger.info("Download link URI: " + method.getURI().toString());
            logger.info("Starting video2audio HTTP download");
        }

        httpFile.setResumeSupported(false);
        setClientParameter(DownloadClientConsts.NO_CONTENT_LENGTH_AVAILABLE, true);

        try {
            InputStream in = client.makeFinalRequestForFile(method, httpFile, true);
            if (in != null) {
                in = mp4 ? new Mp4ToMp3InputStream(in, bitrate) : new FlvToMp3InputStream(in, bitrate);
                logger.info("Saving to file");
                downloadTask.saveToFile(in);
                return true;
            } else {
                logger.info("Saving file failed");
                return false;
            }
        } finally {
            method.abort();
            method.releaseConnection();
        }
    }

    protected boolean tryDownloadAndSaveFile(final RtmpSession rtmpSession, final int bitrate) throws Exception {
        if (httpFile.getState() == DownloadState.PAUSED || httpFile.getState() == DownloadState.CANCELLED)
            return false;
        else
            httpFile.setState(DownloadState.GETTING);
        logger.info("Starting video2audio RTMP download");

        httpFile.getProperties().remove(DownloadClient.START_POSITION);
        httpFile.getProperties().remove(DownloadClient.SUPPOSE_TO_DOWNLOAD);
        httpFile.setResumeSupported(false);

        final String fn = httpFile.getFileName();
        if (fn == null || fn.isEmpty())
            throw new IOException("No defined file name");
        httpFile.setFileName(HttpUtils.replaceInvalidCharsForFileSystem(PlugUtils.unescapeHtml(fn), "_"));

        setClientParameter(DownloadClientConsts.NO_CONTENT_LENGTH_AVAILABLE, true);

        rtmpSession.setConnectionSettings(client.getSettings());//for proxy
        rtmpSession.setHttpFile(httpFile);//for size estimation

        RtmpClient rtmpClient = null;
        try {
            rtmpClient = new RtmpClient(rtmpSession);
            rtmpClient.connect();

            InputStream in = rtmpSession.getOutputWriter().getStream();

            if (in != null) {
                in = new FlvToMp3InputStream(in, bitrate);
                logger.info("Saving to file");
                downloadTask.saveToFile(in);
                return true;
            } else {
                logger.info("Saving file failed");
                return false;
            }
        } catch (InterruptedException e) {
            //ignore
        } catch (InterruptedIOException e) {
            //ignore
        } catch (Exception e) {
            LogUtils.processException(logger, e);
            Throwable t = e;
            while (t.getCause() != null) {
                t = t.getCause();
            }
            throw new PluginImplementationException("RTMP error - " + Utils.getThrowableDescription(t));
        } finally {
            if (rtmpClient != null) {
                try {
                    rtmpClient.disconnect();
                } catch (Exception e) {
                    LogUtils.processException(logger, e);
                }
            }
        }
        return true;
    }

}
