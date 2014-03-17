package cz.vity.freerapid.plugins.services.xtraupload;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.*;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Ladislav Vitasek, Ludek Zika
 */
class XtrauploadRunner {
    private final static Logger logger = Logger.getLogger(XtrauploadRunner.class.getName());
    private HttpDownloadClient client;
    private HttpFileDownloader downloader;
    private String initURL;

    public void run(HttpFileDownloader downloader) throws Exception {
        this.downloader = downloader;
        HttpFile httpFile = downloader.getDownloadFile();
        client = downloader.getClient();
        final String fileURL = httpFile.getFileUrl().toString();
        initURL = fileURL;
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod getMethod = client.getGetMethod(fileURL);
        getMethod.setFollowRedirects(true);
        if (client.makeRequest(getMethod) == HttpStatus.SC_OK) {

            Matcher matcher = Pattern.compile("\\(([0-9.]* .B)\\)", Pattern.MULTILINE).matcher(client.getContentAsString());
            if (matcher.find()) {
                Long a = PlugUtils.getFileSizeFromString(matcher.group(1));
                logger.info("File size " + a);
                 httpFile.setFileSize(a);
            }
            matcher = Pattern.compile("File name:((<[^>]*>)|\\s)*([^<]+)<", Pattern.MULTILINE).matcher(client.getContentAsString());
            if (matcher.find()) {
                final String fn = matcher.group(matcher.groupCount());
                logger.info("File name " + fn);
                httpFile.setFileName(fn);
            } else logger.warning("File name was not found" + client.getContentAsString());

            do {
                checkProblems();
                if (!client.getContentAsString().contains("captchacode")) {
                    logger.info(client.getContentAsString());
                    throw new PluginImplementationException("No captcha.\nCannot find requested page content");
                }
                stepCaptcha(client.getContentAsString());

            } while (client.getContentAsString().contains("Captcha number error or expired"));

            matcher = Pattern.compile("document.location=\"([^\"]*)\"", Pattern.MULTILINE).matcher(client.getContentAsString());
            if (matcher.find()) {
                String s = matcher.group(1);
                logger.info("Found File URL - " + s);
                if (downloader.isTerminated())
                    throw new InterruptedException();
                httpFile.setState(DownloadState.GETTING);
                final GetMethod method = client.getGetMethod(s);
                try {
                    final InputStream inputStream = client.makeFinalRequestForFile(method, httpFile);
                    if (inputStream != null) {
                        downloader.saveToFile(inputStream);
                    } else {
                        checkProblems();
                        throw new IOException("File input stream is empty.");
                    }
                } finally {
                    method.abort();
                    method.releaseConnection();
                }
            } else {
                checkProblems();
                logger.info(client.getContentAsString());
                throw new PluginImplementationException("Problem with a connection to service.\nCannot find requested page content");
            }

        } else
            throw new PluginImplementationException("Problem with a connection to service.\nCannot find requested page content");
    }

    private boolean stepCaptcha(String contentAsString) throws Exception {
        if (contentAsString.contains("captchacode")) {

            Matcher matcher = Pattern.compile("captcha", Pattern.MULTILINE).matcher(contentAsString);
            if (matcher.find()) {
                String s = "http://www.xtraupload.de/captcha.php";
                String captcha = downloader.getCaptcha(s);
                if (captcha == null) {
                    throw new CaptchaEntryInputMismatchException();
                } else {

                    matcher = Pattern.compile("name=myform action\\=\"([^\"]*)\"", Pattern.MULTILINE).matcher(contentAsString);
                    if (!matcher.find()) {
                        throw new PluginImplementationException("Captcha form action was not found");
                    }
                    s = matcher.group(1);
                    client.setReferer(initURL);
                    final PostMethod postMethod = client.getPostMethod(s);
                    postMethod.addParameter("captchacode", captcha);

                    client.getHTTPClient().getParams().setParameter(HttpMethodParams.SINGLE_COOKIE_HEADER, true);
                     if (client.makeRequest(postMethod) == HttpStatus.SC_OK) {
                          return true;
                    }
                }
            } else {
                logger.warning(contentAsString);
                throw new PluginImplementationException("Captcha picture was not found");
            }

        }
        return false;
    }

    private void checkProblems() throws ServiceConnectionProblemException, YouHaveToWaitException, URLNotAvailableAnymoreException {
        Matcher matcher;
        matcher = Pattern.compile("You have got max allowed download sessions from the same IP", Pattern.MULTILINE).matcher(client.getContentAsString());
        if (matcher.find()) {
            throw new YouHaveToWaitException("You have got max allowed download sessions from the same IP!", 5 * 60);
        }
        matcher = Pattern.compile("max allowed bandwidth size per hour", Pattern.MULTILINE).matcher(client.getContentAsString());
        if (matcher.find()) {

            throw new YouHaveToWaitException("You have got max allowed bandwidth size per hour", 10 * 60);
        }
        matcher = Pattern.compile("Your requested file is not found", Pattern.MULTILINE).matcher(client.getContentAsString());
        if (matcher.find()) {
            throw new URLNotAvailableAnymoreException("The requested file is not available");
        }

    }

}