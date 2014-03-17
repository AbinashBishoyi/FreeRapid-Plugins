package cz.vity.freerapid.plugins.services.rtmp;

import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadClient;
import cz.vity.freerapid.plugins.webclient.DownloadClientConsts;
import cz.vity.freerapid.plugins.webclient.DownloadState;
import cz.vity.freerapid.plugins.webclient.utils.HttpUtils;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import cz.vity.freerapid.utilities.LogUtils;
import cz.vity.freerapid.utilities.Utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.util.logging.Logger;

/**
 * Subclass this for support for RTMP downloads.
 *
 * @author ntoskrnl
 * @see #tryDownloadAndSaveFile(RtmpSession)
 */
public abstract class AbstractRtmpRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(AbstractRtmpRunner.class.getName());

    /**
     * Method uses given RtmpSession parameter to connect to the server and tries to download.<br />
     * Download state of HttpFile is updated automatically - sets <code>DownloadState.GETTING</code> and then <code>DownloadState.DOWNLOADING</code>.
     * The DownloadClient parameter {@link DownloadClientConsts#NO_CONTENT_LENGTH_AVAILABLE NO_CONTENT_LENGTH_AVAILABLE} is also set.
     *
     * @param rtmpSession RtmpSession to use for downloading
     * @return true if file was successfully downloaded, false otherwise
     * @throws Exception if something goes horribly wrong
     * @see RtmpSession
     */
    protected boolean tryDownloadAndSaveFile(final RtmpSession rtmpSession) throws Exception {
        httpFile.setState(DownloadState.GETTING);
        logger.info("Starting RTMP download");

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
