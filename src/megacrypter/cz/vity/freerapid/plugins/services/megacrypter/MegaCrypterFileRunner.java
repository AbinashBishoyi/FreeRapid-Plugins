package cz.vity.freerapid.plugins.services.megacrypter;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadClientConsts;
import cz.vity.freerapid.plugins.webclient.DownloadState;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author ntoskrnl
 */
class MegaCrypterFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(MegaCrypterFileRunner.class.getName());

    private byte[] key;
    private byte[] nonce;

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        runCheck(false);
    }

    private void runCheck(final boolean askPassword) throws Exception {
        final PostMethod method = getPostMethod("http://megacrypter.com/api");
        final String entity = "{\"m\": \"info\", \"link\": \"" + fileURL + "\"}";
        method.setRequestEntity(new StringRequestEntity(entity, "application/json", "UTF-8"));
        if (!makeRedirectedRequest(method)) {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
        checkProblems();

        Matcher matcher = getMatcherAgainstContent("\"size\"\\s*:\\s*(\\d+)");
        if (!matcher.find()) {
            throw new PluginImplementationException("File size not found");
        }
        httpFile.setFileSize(PlugUtils.getFileSizeFromString(matcher.group(1)));
        matcher = getMatcherAgainstContent("\"name\"\\s*:\\s*\"(.+?)\"");
        if (!matcher.find()) {
            throw new PluginImplementationException("File name not found");
        }
        String name = matcher.group(1);
        matcher = getMatcherAgainstContent("\"key\"\\s*:\\s*\"(.+?)\"");
        if (!matcher.find()) {
            throw new PluginImplementationException("File name not found");
        }
        String keyStr = matcher.group(1);
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);

        matcher = getMatcherAgainstContent("\"pass\"\\s*:\\s*\"(.+?)#(.+?)\"");
        if (matcher.find()) {
            if (!askPassword) {
                return;
            }
            final byte[] key = getKey(matcher.group(1), matcher.group(2));
            final Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new IvParameterSpec(new byte[16]));
            name = new String(cipher.doFinal(Base64.decodeBase64(name)), "UTF-8");
            keyStr = Base64.encodeBase64String(cipher.doFinal(Base64.decodeBase64(keyStr)));
        }
        httpFile.setFileName(name);
        key = prepareKey(keyStr);
    }

    private byte[] getKey(final String verification, final String salt) throws Exception {
        String key;
        do {
            final String password = getDialogSupport().askForPassword("MegaCrypter");
            if (password == null) {
                throw new NotRecoverableDownloadException("This file is secured with a password");
            }
            key = DigestUtils.md5Hex(password);
        } while (!verification.equals(DigestUtils.md5Hex(salt + key + salt)));
        return Hex.decodeHex(key.toCharArray());
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        if (key == null) {
            runCheck(true);
        }
        final PostMethod method = getPostMethod("http://megacrypter.com/api");
        final String entity = "{\"m\": \"dl\", \"link\": \"" + fileURL + "\"}";
        method.setRequestEntity(new StringRequestEntity(entity, "application/json", "UTF-8"));
        if (!makeRedirectedRequest(method)) {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
        checkProblems();
        final Matcher matcher = getMatcherAgainstContent("\"url\"\\s*:\\s*\"(.+?)\"");
        if (!matcher.find()) {
            throw new PluginImplementationException("Download URL not found");
        }
        final String url = matcher.group(1).replace("\\/", "/");
        //the server doesn't send Accept-Ranges, but supports resume
        setClientParameter(DownloadClientConsts.IGNORE_ACCEPT_RANGES, true);
        final HttpMethod dlMethod = getMethodBuilder()
                .setBaseURL(null)
                .removeParameter("Referer")
                .setAction(url.replaceFirst("https", "http"))
                .toPostMethod();
        if (!tryDownloadAndSaveFile(dlMethod, getDownloadCipher(getStartPosition()))) {
            throw new ServiceConnectionProblemException("Error starting download");
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        if (getContentAsString().contains("\"error\"")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

    /*
     * The following methods are copy-pasted from the Mega plugin.
     */

    private byte[] prepareKey(final String key) throws Exception {
        final byte[] b = Base64.decodeBase64(key);
        final int L = b.length / 2;
        final byte[] result = new byte[L];
        for (int i = 0; i < L; i++) {
            result[i] = (byte) (b[i] ^ b[L + i]);
        }
        nonce = Arrays.copyOfRange(b, L, L + 8);
        return result;
    }

    private Cipher getDownloadCipher(final long startPosition) throws Exception {
        final Cipher cipher = Cipher.getInstance("AES/CTR/NoPadding");
        final ByteBuffer buffer = ByteBuffer.allocate(16).put(nonce);
        buffer.asLongBuffer().put(startPosition / 16);
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new IvParameterSpec(buffer.array()));
        final int skip = (int) (startPosition % 16);
        if (skip != 0) {
            if (cipher.update(new byte[skip]).length != skip) {
                //that should always work with a CTR mode cipher
                throw new IOException("Failed to skip bytes from cipher");
            }
        }
        return cipher;
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