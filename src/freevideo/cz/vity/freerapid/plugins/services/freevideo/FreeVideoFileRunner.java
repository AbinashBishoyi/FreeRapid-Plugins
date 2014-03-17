package cz.vity.freerapid.plugins.services.freevideo;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author JPEXS
 */
class FreeVideoFileRunner extends AbstractRunner {

    private final static Logger logger = Logger.getLogger(FreeVideoFileRunner.class.getName());

    private static String md5(String data) {
        String ret = "";
        try {
            MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
            digest.update(data.getBytes());
            byte[] hash = digest.digest();
            for (byte aHash : hash) {
                String hex = Integer.toHexString(aHash & 0xff);
                if (hex.length() == 1) {
                    hex = "0" + hex;
                }
                ret += hex;
            }
        } catch (NoSuchAlgorithmException ex) {
        }
        return ret;
    }

    @Override
    public void runCheck() throws Exception { //this method validates file
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);//make first request
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkNameAndSize(getContentAsString());//ok let's extract file name and size from the page
        } else {
            throw new PluginImplementationException();
        }
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        Matcher matcher=PlugUtils.matcher(".*/([^/]+)-[0-9]+\\.html$", fileURL);
        String url=PlugUtils.getStringBetween(content, "url: \"", "\",");

        if(matcher.matches()){
            httpFile.setFileName(matcher.group(1)+url.substring(url.lastIndexOf(".")));
        }
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL); //create GET request
        if (makeRedirectedRequest(method)) { //we make the main request
            final String contentAsString = getContentAsString();//check for response
            checkProblems();//check problems
            checkNameAndSize(contentAsString);//extract file name and size from the page
            String url=PlugUtils.getStringBetween(contentAsString, "url: \"", "\",");
            //String baseUrl=PlugUtils.getStringBetween(contentAsString, "baseUrl: \"", "\",");
            //String timeStamp=PlugUtils.getStringBetween(contentAsString, "timestamp: \"", "\"");
            //String downloadUrl=baseUrl+"/"+generateProtection(timeStamp, url)+"/"+url;

            final HttpMethod httpMethod = getMethodBuilder().setReferer(fileURL).setAction(url).toHttpMethod();

            //here is the download link extraction
            if (!tryDownloadAndSaveFile(httpMethod)) {
                checkProblems();//if downloading failed
                logger.warning(getContentAsString());//log the info
                throw new PluginImplementationException();//some unknown problem
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.matches(".*V.mi po.adovan. str.nka bohuel nebyla nalezena.*")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }
}
