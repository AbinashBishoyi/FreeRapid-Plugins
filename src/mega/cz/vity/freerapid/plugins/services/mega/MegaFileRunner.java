package cz.vity.freerapid.plugins.services.mega;

import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.webclient.*;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import cz.vity.freerapid.utilities.LogUtils;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class which contains main code
 *
 * @author ntoskrnl
 */
class MegaFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(MegaFileRunner.class.getName());

    private static enum LinkType {
        P, N, FOLDER;

        public String parameter() {
            return toString().toLowerCase(Locale.ENGLISH);
        }
    }

    private MegaApi api;
    private String id;
    private byte[] key;
    private LinkType type = LinkType.P;

    private void init() throws Exception {
        if (id == null) {
            Matcher matcher = PlugUtils.matcher("#(N)?!([a-zA-Z\\d]{8})!([a-zA-Z\\d\\-_]{43})$", fileURL);
            if (matcher.find()) {
                if (matcher.group(1) != null) {
                    type = LinkType.N;
                }
                id = matcher.group(2);
                api = new MegaApi(client, matcher.group(3));
            } else {
                matcher = PlugUtils.matcher("#F!([a-zA-Z\\d]{8})!([a-zA-Z\\d\\-_]{22})$", fileURL);
                if (!matcher.find()) {
                    throw new PluginImplementationException("Error parsing file URL");
                }
                id = matcher.group(1);
                key = Base64.decodeBase64(matcher.group(2));
                type = LinkType.FOLDER;
            }
        }
    }

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        init();
        if (type != LinkType.FOLDER) {
            final String content = api.request("[{\"a\":\"g\",\"" + type.parameter() + "\":\"" + id + "\",\"ssl\":\"1\"}]");
            checkNameAndSize(content);
        }
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
        if (type == LinkType.FOLDER) {
            final List<URI> list = getFolderLinks();
            getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(httpFile, list);
            httpFile.getProperties().put("removeCompleted", true);
            return;
        }
        final String content = api.request("[{\"a\":\"g\",\"g\":1,\"ssl\":1,\"" + type.parameter() + "\":\"" + id + "\"}]");
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

    public List<URI> getFolderLinks() throws Exception {
        final HttpMethod method = new MethodBuilder(client)
                .setAction("https://g.api.mega.co.nz/cs?id=" + new Random().nextInt(0x10000000) + "&n=" + id)
                .toPostMethod();
        ((PostMethod) method).setRequestEntity(new StringRequestEntity("[{\"a\":\"f\",\"c\":\"1\",\"r\":\"1\"}]", "text/plain", "UTF-8"));
        if (!makeRedirectedRequest(method)) {
            throw new ServiceConnectionProblemException();
        }
        MegaApi.checkProblems(getContentAsString());
        final List<URI> list = parseFolderContent(getContentAsString());
        if (list.isEmpty()) {
            throw new PluginImplementationException("No links found");
        }
        return list;
    }

    private List<URI> parseFolderContent(final String content) throws Exception {
        final List<URI> list = new LinkedList<URI>();
        final Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"));
        final Matcher matcher = PlugUtils.matcher("\\{\\s*(\"h\".+?)\\s*\\}", content);
        while (matcher.find()) {
            final NodeData data = new NodeData(matcher.group(1));
            if (data.getField("s") != null) {
                final String nodeId = data.getField("h");
                final String nodeKey = data.getField("k");
                if (nodeId == null || nodeKey == null) {
                    throw new PluginImplementationException("Error parsing server response");
                }
                final String[] keyParts = nodeKey.split(":");
                if (keyParts.length != 2) {
                    throw new PluginImplementationException("Error parsing server response");
                }
                final String key = Base64.encodeBase64URLSafeString(cipher.doFinal(Base64.decodeBase64(keyParts[1])));
                try {
                    list.add(new URI("https://mega.co.nz/#N!" + nodeId + "!" + key));
                } catch (final URISyntaxException e) {
                    LogUtils.processException(logger, e);
                }
            }
        }
        return list;
    }

    private static class NodeData {
        private String content;

        public NodeData(final String content) {
            this.content = content;
        }

        public String getField(final String field) {
            final Matcher matcher = PlugUtils.matcher("\"" + Pattern.quote(field) + "\":\\s*?\"?(.+?)[\",]", content);
            if (matcher.find()) {
                return matcher.group(1);
            } else {
                return null;
            }
        }
    }

}