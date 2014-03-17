package cz.vity.freerapid.plugins.services.movzap;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.Charset;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author tong2shot
 */
class MovZapFileRunner extends XFileSharingRunner {
    private final static Logger logger = Logger.getLogger(MovZapFileRunner.class.getName());
    private final static byte[] SECRET_KEY = "N%66=]H6".getBytes(Charset.forName("UTF-8"));

    @Override
    public void run() throws Exception {
        logger.info("Starting download in TASK " + fileURL);
        GetMethod method = getGetMethod(fileURL);
        if (!makeRedirectedRequest(method)) {
            checkFileProblems();
            throw new ServiceConnectionProblemException();
        }
        checkFileProblems();
        checkNameAndSize();

        final String downloadFormContent = getContentAsString();
        final String id = PlugUtils.getStringBetween(getContentAsString(), "\"id\" value=\"", "\"");
        HttpMethod httpMethod = getMethodBuilder()
                .setReferer(fileURL)
                .setAction("/xml/" + id + ".xml")
                .toGetMethod();
        if (!makeRedirectedRequest(httpMethod)) {
            checkDownloadProblems();
            throw new ServiceConnectionProblemException();
        }
        checkDownloadProblems();

        httpFile.setFileName(PlugUtils.getStringBetween(getContentAsString(), "<title><![CDATA[", "]]></title>"));
        httpMethod = getMethodBuilder()
                .setReferer(fileURL)
                .setAction("/xml2/" + id + ".xml")
                .toGetMethod();
        if (!makeRedirectedRequest(httpMethod)) {
            checkDownloadProblems();
            throw new ServiceConnectionProblemException();
        }
        checkDownloadProblems();

        String cipherDownloadURL;
        if (getContentAsString().contains("hd.file")) { //HD as default
            cipherDownloadURL = PlugUtils.getStringBetween(getContentAsString(), "<hd.file><![CDATA[", "]]></hd.file>");
        } else if (getContentAsString().contains("file")) {
            cipherDownloadURL = PlugUtils.getStringBetween(getContentAsString(), "<file><![CDATA[", "]]></file>");
        } else {
            throw new PluginImplementationException("Download link not found");
        }
        cipherDownloadURL = cipherDownloadURL.substring(0, cipherDownloadURL.length() - 6);
        Cipher cipher = Cipher.getInstance("DES/ECB/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(SECRET_KEY, "DES"));
        final String downloadURL = new String(cipher.doFinal(Base64.decodeBase64(cipherDownloadURL)));

        final String waitTimeRule = "id=\"countdown_str\".*?<span id=\".*?\">.*?(\\d+).*?</span";
        final Matcher waitTimematcher = PlugUtils.matcher(waitTimeRule, downloadFormContent);
        if (waitTimematcher.find()) {
            downloadTask.sleep(Integer.parseInt(waitTimematcher.group(1)) + 1);
        } else {
            downloadTask.sleep(10);
        }

        httpMethod = getMethodBuilder()
                .setReferer(fileURL)
                .setAction(downloadURL)
                .setParameter("start", "0")
                .toGetMethod();

        setFileStreamContentTypes("text/plain");
        if (!tryDownloadAndSaveFile(httpMethod)) {
            checkDownloadProblems();
            throw new ServiceConnectionProblemException("Error starting download");
        }
    }

    @Override
    protected void checkNameAndSize() throws ErrorDuringDownloadingException {
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

}