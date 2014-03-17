package cz.vity.freerapid.plugins.services.filebaseto;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.*;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * @author Ladislav Vitasek, Ludek Zika
 */
class FilebaseToRunner {
    private final static Logger logger = Logger.getLogger(FilebaseToRunner.class.getName());
    private HttpDownloadClient client;
    private HttpFileDownloader downloader;
    private String fileURL;

    public void run(HttpFileDownloader downloader) throws Exception {
        this.downloader = downloader;
        HttpFile httpFile = downloader.getDownloadFile();
        client = downloader.getClient();
        client.getHTTPClient().getParams().setParameter(HttpMethodParams.SINGLE_COOKIE_HEADER, true);
        fileURL = httpFile.getFileUrl().toString() + "&dl=1";
        logger.info("Starting download in TASK " + fileURL);

        final GetMethod getMethod = client.getGetMethod(fileURL);
        getMethod.setFollowRedirects(true);
        if (client.makeRequest(getMethod) == HttpStatus.SC_OK) {

            while (client.getContentAsString().contains("Captcha-Code:")) {
                stepCaptcha(client.getContentAsString());
            }

            Matcher matcher = PlugUtils.matcher("form name=\"waitform\" action=\"([^\"]*)\"", client.getContentAsString());
            if (matcher.find()) {
                String t = matcher.group(1);
                logger.info("Submit form to: " + t);

                matcher = PlugUtils.matcher("Filesize:((<[^>]*>)|\\s)*([0-9.]+ .B)<", client.getContentAsString());
                if (matcher.find()) {
                    logger.info("File size " + matcher.group(matcher.groupCount()));
                    httpFile.setFileSize(PlugUtils.getFileSizeFromString(matcher.group(matcher.groupCount())));
                }
                matcher = PlugUtils.matcher("\"Download ([^\"]+)\"", client.getContentAsString());
                if (matcher.find()) {
                    final String fn = matcher.group(1);
                    logger.info("File name " + fn);
                    httpFile.setFileName(fn);
                } else logger.warning("File name was not found" + client.getContentAsString());


                matcher = PlugUtils.matcher("Please wait ([0-9]+) seconds", client.getContentAsString());
                if (matcher.find()) {
                    String s = matcher.group(1);
                    int seconds = new Integer(s);
                    logger.info("wait - " + s);
                    downloader.sleep(seconds + 1);
                }

                if (downloader.isTerminated())
                    throw new InterruptedException();
                client.setReferer(fileURL);
                String code = getParameter("code", client.getContentAsString());
                String cid = getParameter("cid", client.getContentAsString());
                String userid = getParameter("userid", client.getContentAsString());
                String usermd5 = getParameter("usermd5", client.getContentAsString());


                httpFile.setState(DownloadState.GETTING);
                final PostMethod method = client.getPostMethod(t);

                method.addParameter("code", code);
                method.addParameter("cid", cid);
                method.addParameter("userid", userid);
                method.addParameter("usermd5", usermd5);
                method.addParameter("wait", ("Download " + httpFile.getFileName()));

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

    private String getParameter(String s, String contentAsString) throws PluginImplementationException {
        Matcher matcher = PlugUtils.matcher("name=\"" + s + "\"[^v>]*value=\"([^\"]*)\"", contentAsString);
        if (matcher.find()) {
            return matcher.group(1);
        } else
            throw new PluginImplementationException("Parameter " + s + " was not found");
    }

    private boolean stepCaptcha(String contentAsString) throws Exception {
        if (contentAsString.contains("Captcha-Code:")) {

            Matcher matcher = PlugUtils.matcher("src=\"([^\"]*captcha[^\"]*)\"", contentAsString);
            if (matcher.find()) {
                String s = matcher.group(1);
                if (!s.contains("filebase.to")) s= "http://filebase.to/" +s;
                logger.info("Captcha URL " + s);
                String captcha = downloader.getCaptcha(s);
                if (captcha == null) {
                    throw new CaptchaEntryInputMismatchException();
                } else {

                    String cid = getParameter("cid", contentAsString);


                    client.setReferer(fileURL);
                    final PostMethod postMethod = client.getPostMethod(fileURL);
                    postMethod.addParameter("cid", cid);
                    postMethod.addParameter("uid", captcha);
                    postMethod.addParameter("go", "Ok!");

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

        matcher = PlugUtils.matcher("Du bist keinem g.ltigen Link gefolgt", client.getContentAsString());
        if (matcher.find()) {
            throw new URLNotAvailableAnymoreException(String.format("<b>Such file does not exist or it has been removed.</b><br>"));

        }

    }


}