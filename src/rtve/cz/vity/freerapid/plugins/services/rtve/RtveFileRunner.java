package cz.vity.freerapid.plugins.services.rtve;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.httpclient.HttpMethod;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.Charset;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author ntoskrnl
 */
class RtveFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(RtveFileRunner.class.getName());

    private final static String LAST_PROVIDER = "7e4PR7HoDQw%3D";
    private final static byte[] BLOWFISH_KEY = "hul#Lost".getBytes(Charset.forName("UTF-8"));

    private final SecretKeySpec spec;
    private final Cipher cipher;

    public RtveFileRunner() {
        try {
            spec = new SecretKeySpec(BLOWFISH_KEY, "Blowfish");
            cipher = Cipher.getInstance("Blowfish/ECB/PKCS5Padding");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final HttpMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkNameAndSize();
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize() throws ErrorDuringDownloadingException {
        PlugUtils.checkName(httpFile, getContentAsString(), "<title>", "- RTVE.es</title>");
        httpFile.setFileName(PlugUtils.unescapeHtml(httpFile.getFileName().replace('/', '.')) + ".flv");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    private void checkFileExt(final String url) throws ErrorDuringDownloadingException {
        final int i = url.lastIndexOf('.');
        if (i > -1) {
            final String ext = url.substring(i);
            if (!ext.equals(".flv")) {
                httpFile.setFileName(httpFile.getFileName().substring(0, httpFile.getFileName().length() - 4) + ext);
            }
        }
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        HttpMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkNameAndSize();

            final String assetID = PlugUtils.getStringBetween(getContentAsString(), ".addVariable(\"assetID\",\"", "\");");
            final String location = PlugUtils.getStringBetween(getContentAsString(), ".addVariable(\"location\",\"", "\");");
            final String[] assetIdSplit = assetID.split("_");
            final String xmlUrl = "http://www.rtve.es/swf/data/" + assetIdSplit[1] + "/" + assetIdSplit[2] + "/" + location + "/" + hashing(assetIdSplit[0]) + "/" + assetIdSplit[0] + ".xml";
            logger.info("xmlUrl = " + xmlUrl);

            method = getGetMethod(xmlUrl);
            if (!makeRedirectedRequest(method)) {
                throw new ServiceConnectionProblemException();
            }

            final Matcher matcher = getMatcherAgainstContent("name=\"multicdn\".+?params=\"(.+?)\"");
            if (!matcher.find()) {
                throw new PluginImplementationException("Error parsing multicdn XML");
            }
            final String[] paramsSplit = matcher.group(1).split(",");
            String assetDataId = null;
            for (final String param : paramsSplit) {
                if (param.startsWith("assetDataId")) {
                    assetDataId = param.split("::")[1];
                    break;
                }
            }
            if (assetDataId == null) {
                throw new PluginImplementationException("assetDataId not found");
            }

            final String assetType;
            if (assetID.contains("video")) {
                assetType = "VIDEO";
            } else if (assetID.contains("audio")) {
                assetType = "AUDIO";
            } else {
                throw new PluginImplementationException("Error parsing assetID");
            }

            final String assetData = "ASSET_DATA_" + assetType + "-" + assetDataId;
            logger.info("assetData = " + assetData);
            final String resourceUrl = "http://www.rtve.es/scdweb/dataservices/getResource?id=" + new String(Base64.encodeBase64(encrypt(assetData.getBytes("UTF-8"))), "UTF-8") + "&oprovider=" + LAST_PROVIDER;
            logger.info("resourceUrl = " + resourceUrl);

            method = getGetMethod(resourceUrl);
            if (!makeRedirectedRequest(method)) {
                throw new ServiceConnectionProblemException();
            }

            try {
                final String code = PlugUtils.getStringBetween(getContentAsString(), "<code>", "</code>");
                throw new NotRecoverableDownloadException("Error fetching resource XML, code '" + code + "'");
            } catch (PluginImplementationException e) {
                //ignore
            }

            final String url = PlugUtils.getStringBetween(getContentAsString(), "<url>", "</url>");
            final String decryptedUrl = new String(decrypt(Base64.decodeBase64(url.getBytes("UTF-8"))), "UTF-8");
            logger.info("decryptedUrl = " + decryptedUrl);

            checkFileExt(decryptedUrl);

            method = getGetMethod(decryptedUrl);
            if (!tryDownloadAndSaveFile(method)) {
                checkProblems();
                throw new ServiceConnectionProblemException("Error starting download");
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        if (getContentAsString().contains("the page you're trying to view is not available")) {
            throw new URLNotAvailableAnymoreException("Page not found");
        }
    }

    private byte[] decrypt(final byte[] toDecrypt) throws ErrorDuringDownloadingException {
        try {
            cipher.init(Cipher.DECRYPT_MODE, spec);
            return cipher.doFinal(toDecrypt);
        } catch (Exception e) {
            throw new PluginImplementationException(e);
        }
    }

    private byte[] encrypt(final byte[] toEncrypt) throws ErrorDuringDownloadingException {
        try {
            cipher.init(Cipher.ENCRYPT_MODE, spec);
            return cipher.doFinal(toEncrypt);
        } catch (Exception e) {
            throw new PluginImplementationException(e);
        }
    }

    private static String hashing(final String s) throws ErrorDuringDownloadingException {
        try {
            char c1 = s.charAt(s.length() - 1);
            char c2 = s.charAt(s.length() - 2);
            char c3 = s.charAt(s.length() - 3);
            char c4 = s.charAt(s.length() - 4);
            return c1 + "/" + c2 + "/" + c3 + "/" + c4;
        } catch (StringIndexOutOfBoundsException e) {
            throw new PluginImplementationException("String index out of bounds", e);
        }
    }

}