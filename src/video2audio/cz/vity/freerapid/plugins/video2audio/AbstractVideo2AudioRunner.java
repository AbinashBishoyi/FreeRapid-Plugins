package cz.vity.freerapid.plugins.video2audio;

import cz.vity.freerapid.plugins.services.rtmp.AbstractRtmpRunner;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpFile;

import java.io.*;
import java.util.logging.Logger;

/**
 * @author ntoskrnl
 * @author tong2shot
 */
public abstract class AbstractVideo2AudioRunner extends AbstractRtmpRunner {

    private static final Logger logger = Logger.getLogger(AbstractVideo2AudioRunner.class.getName());

    public void convertToAudio(final int bitrate, final boolean mp4) throws IOException {
        if (downloadTask.isTerminated()) {
            logger.info("Download task was terminated");
            return;
        }
        logger.info("Converting to audio");
        final HttpFile downloadFile = downloadTask.getDownloadFile();
        final File inputFile = downloadFile.getStoreFile();
        if (!inputFile.exists()) {
            logger.warning("Input file not found, conversion aborted");
            return;
        }
        logger.info("Input file: " + inputFile);

        InputStream is = null;
        FileOutputStream fos = null;
        String fnameNoExt = downloadFile.getFileName().replaceFirst("\\..{3,4}$", "");
        String fname = fnameNoExt + ".mp3";
        File outputFile = new File(downloadFile.getSaveToDirectory(), fname);
        int outputFileCounter = 1;
        try {
            is = (mp4 ? new Mp4ToMp3InputStream(new FileInputStream(inputFile), bitrate) : new FlvToMp3InputStream(new FileInputStream(inputFile), bitrate));
            while (outputFile.exists()) {
                fname = fnameNoExt + "-" + outputFileCounter++ + ".mp3";
                outputFile = new File(downloadFile.getSaveToDirectory(), fname);
            }
            fos = new FileOutputStream(outputFile);
            logger.info("Output name: " + fname);
            byte[] buffer = new byte[64 * 1024];
            int len;
            int size = 0;
            while ((len = is.read(buffer)) != -1) {
                fos.write(buffer, 0, len);
                size += len;
            }
            inputFile.delete();
            downloadFile.setFileName(fname);
            downloadFile.setDownloaded(size);
            downloadFile.setFileSize(size);
        } finally {
            if (is != null) {
                is.close();
            }
            if (fos != null) {
                fos.close();
            }
        }
    }
}
