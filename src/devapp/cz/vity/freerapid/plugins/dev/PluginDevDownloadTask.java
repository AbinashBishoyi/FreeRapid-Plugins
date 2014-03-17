package cz.vity.freerapid.plugins.dev;

import cz.vity.freerapid.plugins.webclient.ConnectionSettings;
import cz.vity.freerapid.plugins.webclient.DownloadClient;
import cz.vity.freerapid.plugins.webclient.DownloadState;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpDownloadClient;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpFile;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpFileDownloadTask;
import cz.vity.freerapid.utilities.LogUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.logging.Logger;

/**
 * @author Vity
 */
class PluginDevDownloadTask implements HttpFileDownloadTask {
    /**
     * Field logger instance
     */
    private final static Logger logger = Logger.getLogger(PluginDevDownloadTask.class.getName());

    /**
     * Field file - file that is being downloaded
     */
    private HttpFile file;
    /**
     * download client/robot that is used for downloading
     */
    private DownloadClient downloadClient;
    private boolean useTempFiles;


    /**
     * Constructor
     *
     * @param file     file that is being downloaded
     * @param settings internet connection settings
     */
    PluginDevDownloadTask(HttpFile file, ConnectionSettings settings) {
        this.file = file;
        downloadClient = new DownloadClient();
        downloadClient.initClient(settings);
    }

    /**
     * Returns file that is given to be downloaded
     *
     * @return file for downloading
     */
    public HttpFile getDownloadFile() {
        return file;
    }

    /**
     * Client associated with current file - its HTTP connections are used to grab a file
     *
     * @return actual instance of HttpDownloadClient
     */
    public HttpDownloadClient getClient() {
        return downloadClient;
    }

    /**
     * Method that handles direct saving file onto physical disc.
     * File download state is set to COMPLETED automatically <br>
     * Checks if it is possible to write file on disk with current given name.
     * If <code>useTempFiles</code> is defined to true, the physical temp file is created.
     *
     * @param inputStream stream which contains data to be saved on the disk - should not be null
     * @throws Exception error during reading or if inputStream is null
     */
    public void saveToFile(final InputStream inputStream) throws Exception {
        if (useTempFiles) {
            final File tempFile = File.createTempFile("pluginDev", file.getFileName());
            logger.info("Simulating saving file from a stream to temp file " + tempFile.getAbsolutePath());
            final PluginDevHttpFile httpFile = (PluginDevHttpFile) file;
            httpFile.setSaveToDirectory(tempFile.getParentFile());
            httpFile.setFileName(tempFile.getName());
            tempFile.deleteOnExit();
            FileOutputStream outputStream = null;
            try {
                outputStream = new FileOutputStream(tempFile);
                final byte[] buffer = new byte[2048];
                int read;
                while ((read = inputStream.read(buffer)) > -1) {
                    outputStream.write(buffer, 0, read);
                }
                logger.info("File was successfully read from the stream");
            } finally {
                logger.info("Closing stream");
                try {
                    inputStream.close();
                } catch (Exception e) {
                    LogUtils.processException(logger, e);
                }
                if (outputStream != null) {
                    outputStream.close();
                }

            }
        } else {
            logger.info("Simulating saving file from a stream - trying to read 2KB of data");
            try {
                final byte[] buffer = new byte[2048];
                int done = 0;
                int toRead = 2048;
                int read;
                while (done < 2048 && (read = inputStream.read(buffer, 0, toRead)) > -1) {
                    done += read;
                    toRead -= read;
                }
                logger.info(done + " bytes were successfully read from the stream");
            } finally {
                logger.info("Closing stream");
                try {
                    inputStream.close();
                } catch (Exception e) {
                    LogUtils.processException(logger, e);
                }
            }
        }
    }

    /**
     * Sets file download state to WAITING.
     * Stops download thread for given time.
     *
     * @param seconds time to sleep
     * @throws InterruptedException if the thread was interrupted during sleeping
     */
    public void sleep(int seconds) throws InterruptedException {
        file.setState(DownloadState.WAITING);

        logger.info("Going to sleep for " + (seconds) + " seconds");
        for (int i = seconds; i > 0; i--) {
            if (isTerminated())
                break;
            Thread.sleep(1000);
        }

    }

    /**
     * Checks whether current downloading process has been stopped
     *
     * @return true if downloading of the file was interrupted
     */
    public boolean isTerminated() {
        return false;
    }

    public void setUseTempFiles(boolean useTempFiles) {
        this.useTempFiles = useTempFiles;
    }
}

