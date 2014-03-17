package cz.vity.freerapid.plugins.services.rtmp;

import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadClient;
import cz.vity.freerapid.plugins.webclient.DownloadState;
import cz.vity.freerapid.plugins.webclient.utils.HttpUtils;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import cz.vity.freerapid.utilities.LogUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.util.logging.Logger;

/**
 * @author ntoskrnl
 */
public abstract class AbstractRtmpRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(AbstractRtmpRunner.class.getName());

    /**
     * Method uses given RtmpSession parameter to connect to the server and tries to download.<br />
     * Download state of HttpFile is updated automatically - sets <code>DownloadState.GETTING</code> and then <code>DownloadState.DOWNLOADING</code>.
     * The HttpClient parameter <code>noContentLengthAvailable</code> is also set.
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

        final String fn = httpFile.getFileName();
        if (fn == null || fn.isEmpty())
            throw new IOException("No defined file name");
        httpFile.setFileName(HttpUtils.replaceInvalidCharsForFileSystem(PlugUtils.unescapeHtml(fn), "_"));

        client.getHTTPClient().getParams().setBooleanParameter("noContentLengthAvailable", true);

        rtmpSession.setHttpFile(httpFile);//for size estimation

        RtmpClient rtmpClient = null;
        try {
            rtmpClient = new RtmpClient(rtmpSession);
            rtmpClient.connect();

            InputStream in = rtmpClient.getStream();

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
        } catch (Throwable e) {
            LogUtils.processException(logger, e);
            while (e.getCause() != null) {
                e = e.getCause();
            }
            throw new PluginImplementationException("RTMP error - " + getThrowableDescription(e));
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

    private static String getThrowableDescription(Throwable t) {
        final String s = t.getClass().getSimpleName();
        final String l = t.getLocalizedMessage();
        if (l == null) {
            return s;
        } else {
            return s + ": " + l;
        }
    }

}
