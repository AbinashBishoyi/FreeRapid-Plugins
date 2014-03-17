package cz.vity.freerapid.plugins.services.mega;

import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadClientConsts;
import cz.vity.freerapid.plugins.webclient.DownloadState;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author ntoskrnl
 */
class MegaFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(MegaFileRunner.class.getName());

    private MegaApi api;
    private String id;

    private void init() throws Exception {
        if (api == null) {
            final Matcher matcher = PlugUtils.matcher("#!([a-zA-Z\\d]{8})!([a-zA-Z\\d\\-_]{43})", fileURL);
            if (!matcher.find()) {
                throw new PluginImplementationException("Error parsing file URL");
            }
            id = matcher.group(1);
            api = new MegaApi(client, matcher.group(2));
        }
    }

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        init();
        final String content = api.request("[{\"a\":\"g\",\"p\":\"" + id + "\",\"ssl\":\"1\"}]");
        checkNameAndSize(content);
    }

    private void checkNameAndSize(final String content) throws Exception {
        try {
            httpFile.setFileSize(PlugUtils.getFileSizeFromString(PlugUtils.getStringBetween(content, "\"s\":", ",")));
            httpFile.setFileName(PlugUtils.getStringBetween(content, "\"n\":\"", "\""));
            httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
        } catch (final Exception e) {
            logger.warning("Content from API request:\n" + content);
            throw e;
        }
    }

    @Override
    public void run() throws Exception {
        super.run();
        init();
        logger.info("Starting download in TASK " + fileURL);
        final String content = api.request("[{\"a\":\"g\",\"g\":1,\"ssl\":1,\"p\":\"" + id + "\"}]");
        checkNameAndSize(content);
        final String url;
        try {
            url = PlugUtils.getStringBetween(content, "\"g\":\"", "\"");
        } catch (final Exception e) {
            logger.warning("Content from API request:\n" + content);
            throw e;
        }
        //the server doesn't send Accept-Ranges, but supports resume
        setClientParameter(DownloadClientConsts.IGNORE_ACCEPT_RANGES, true);
        final HttpMethod method = getMethodBuilder()
                .setAction(url.replaceFirst("https", "http"))
                .toPostMethod();
        if (!tryDownloadAndSaveFile(method, api.getDownloadCipher(getStartPosition()))) {
            throw new ServiceConnectionProblemException("Error starting download");
        }
    }

    private long getStartPosition() throws Exception {
        long position = 0;
        final File storeFile = httpFile.getStoreFile();
        if (storeFile != null && storeFile.exists()) {
            position = Math.max(httpFile.getRealDownload(), 0);
            if (position != 0) {
                logger.info("Download start position: " + position);
            }
        }
        return position;
    }

    private boolean tryDownloadAndSaveFile(final HttpMethod method, final Cipher cipher) throws Exception {
        if (httpFile.getState() == DownloadState.PAUSED || httpFile.getState() == DownloadState.CANCELLED)
            return false;
        else
            httpFile.setState(DownloadState.GETTING);
        if (logger.isLoggable(Level.INFO)) {
            logger.info("Download link URI: " + method.getURI().toString());
            logger.info("Making final request for file");
        }

        try {
            InputStream inputStream = client.makeFinalRequestForFile(method, httpFile, true);
            if (inputStream != null) {
                inputStream = new CipherInputStream(inputStream, cipher);
                logger.info("Saving to file");
                downloadTask.saveToFile(inputStream);
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

}