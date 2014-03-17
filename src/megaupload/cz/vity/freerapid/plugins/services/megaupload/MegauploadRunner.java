package cz.vity.freerapid.plugins.services.megaupload;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.*;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.params.HttpClientParams;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * @author Ladislav Vitasek, Ludek Zika
 *        
 *
 */

class MegauploadRunner {
    private final static Logger logger = Logger.getLogger(MegauploadRunner.class.getName());
    private HttpDownloadClient client;
    private HttpFileDownloader downloader;
    private String HTTP_SITE = "http://www.megaupload.com";
    private int captchaCount;

    public void run(HttpFileDownloader downloader) throws Exception {
        this.downloader = downloader;
        HttpFile httpFile = downloader.getDownloadFile();
        client = downloader.getClient();
        client.getHTTPClient().getParams().setBooleanParameter(HttpClientParams.ALLOW_CIRCULAR_REDIRECTS, true);
        final String fileURL = httpFile.getFileUrl().toString();
        if (httpFile.getFileUrl().getHost().contains("megarotic") || httpFile.getFileUrl().getHost().contains("sexuploader"))
            HTTP_SITE = "http://www.megarotic.com";
        logger.info("Starting download in TASK " + fileURL);

        final GetMethod getMethod = client.getGetMethod(fileURL);
        getMethod.setFollowRedirects(true);
        if (client.makeRequest(getMethod) == HttpStatus.SC_OK) {
            Matcher matcher = PlugUtils.matcher(" ([0-9.]+ .B).?</div>", client.getContentAsString());
            if (matcher.find()) {
                logger.info("File size " + matcher.group(1));
                httpFile.setFileSize(PlugUtils.getFileSizeFromString(matcher.group(1)));
            } else {
                if (client.getContentAsString().contains("trying to access is temporarily unavailable"))
                    throw new YouHaveToWaitException("The file you are trying to access is temporarily unavailable.", 2 * 60);
            }
            matcher = PlugUtils.matcher("Filename:(</font>)?</b> ([^<]*)", client.getContentAsString());
            if (matcher.find()) {
                final String fn = PlugUtils.unescapeHtml(matcher.group(2));
                logger.info("File name " + fn);
                httpFile.setFileName(fn);
            } else logger.warning("File name was not found" + client.getContentAsString());
            captchaCount = 0;
            while (client.getContentAsString().contains("Please enter")) {
                stepCaptcha(client.getContentAsString());
            }

            if (client.getContentAsString().contains("Click here to download")) {
                matcher = PlugUtils.matcher("=([0-9]+);[^/w]*function countdown", client.getContentAsString());
                if (!matcher.find()) {
                    throw new InvalidURLOrServiceProblemException("Invalid URL or unindentified service");
                }
                String s = matcher.group(1);
                int seconds = new Integer(s);
                s = new LinkInJSResolver(logger).findUrl(client.getContentAsString());

                if ("".equals(s)) logger.warning("Link was not found" + client.getContentAsString());
                logger.info("Found File URL - " + s);

                downloader.sleep(seconds + 1);
                if (downloader.isTerminated())
                    throw new InterruptedException();

                httpFile.setState(DownloadState.GETTING);
                final GetMethod method = client.getGetMethod(encodeURL(s));

                try {
                    final InputStream inputStream = client.makeFinalRequestForFile(method, httpFile);
                    if (inputStream != null) {
                        downloader.saveToFile(inputStream);
                    } else {
                        checkProblems();
                        logger.warning(client.getContentAsString());
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


    private void checkProblems() throws ServiceConnectionProblemException, URLNotAvailableAnymoreException {
        Matcher matcher;
        matcher = PlugUtils.matcher("Download limit exceeded", client.getContentAsString());
        if (matcher.find()) {

            throw new ServiceConnectionProblemException(String.format("Download limit exceeded."));
        }
        matcher = PlugUtils.matcher("All download slots", client.getContentAsString());
        if (matcher.find()) {

            throw new ServiceConnectionProblemException(String.format("No free slot for your country."));
        }
        matcher = PlugUtils.matcher("Unfortunately, the link you have clicked is not available", client.getContentAsString());
        if (matcher.find()) {
            throw new URLNotAvailableAnymoreException(String.format("<b>The file is not available</b><br>"));

        }

    }

    private boolean stepCaptcha(String contentAsString) throws Exception {
        if (contentAsString.contains("Please enter")) {

            Matcher matcher = PlugUtils.matcher("src=\"(/capgen[^\"]*)\"", contentAsString);
            if (matcher.find()) {
                String s = replaceEntities(matcher.group(1));
                logger.info("Captcha - image " + HTTP_SITE + s);
                String captcha;
                if (captchaCount < 2) {
                    EditImage ei = new EditImage(downloader.getCaptchaImage(HTTP_SITE + s));
                    captcha = PlugUtils.recognize(ei.separate(), "");
                    captchaCount++;
                    logger.info("Captcha - OCR recognized " + captcha + " attempts " + captchaCount);
                    matcher = PlugUtils.matcher("[A-Za-z0-9]{3}", captcha);
                    if (!matcher.find()) {
                        captcha = downloader.getCaptcha(HTTP_SITE + s);
                    }
                } else captcha = downloader.getCaptcha(HTTP_SITE + s);

                if (captcha == null) {
                    throw new CaptchaEntryInputMismatchException();
                } else {
                    //  client.setReferer(baseURL);
                    String d = getParameter("d", contentAsString);
                    String imagecode = getParameter("imagecode", contentAsString);
                    String megavar = getParameter("megavar", contentAsString);

                    final PostMethod postMethod = client.getPostMethod(HTTP_SITE);

                    postMethod.addParameter("d", d);
                    postMethod.addParameter("imagecode", imagecode);
                    postMethod.addParameter("megavar", megavar);
                    postMethod.addParameter("imagestring", captcha);

                    if (client.makeRequest(postMethod) == HttpStatus.SC_OK) {

                        return true;
                    }
                }
            } else throw new PluginImplementationException("Captcha picture was not found");
        }
        return false;
    }


    private String replaceEntities(String s) {
        s = s.replaceAll("\\&amp;", "&");
        return s;
    }

    private String encodeURL(String s) throws UnsupportedEncodingException {
        Matcher matcher = PlugUtils.matcher("(.*/)([^/]*)$", s);
        if (matcher.find()) {
            return matcher.group(1) + URLEncoder.encode(matcher.group(2), "UTF-8");
        }
        return s;
    }

    private String getParameter(String s, String contentAsString) throws PluginImplementationException {
        Matcher matcher = PlugUtils.matcher("name=\"" + s + "\" value=\"([^\"]*)\"", contentAsString);
        if (matcher.find()) {
            return matcher.group(1);
        } else
            throw new PluginImplementationException("Parameter " + s + " was not found");
    }
}