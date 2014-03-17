package cz.vity.freerapid.plugins.services.netloadin;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.DownloadState;
import cz.vity.freerapid.plugins.webclient.HttpDownloadClient;
import cz.vity.freerapid.plugins.webclient.HttpFile;
import cz.vity.freerapid.plugins.webclient.HttpFileDownloader;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.params.HttpClientParams;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Ladislav Vitasek, Ludek Zika
 */
class NetloadInRunner {
    private final static Logger logger = Logger.getLogger(NetloadInRunner.class.getName());
    private HttpDownloadClient client;
    private HttpFileDownloader downloader;
    private static final String HTTP_NETLOAD = "http://netload.in";

    private String initURL;
    private String enterURL;

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
            do {
                stepEnterPage(client.getContentAsString());
                if (!client.getContentAsString().contains("Please enter the Securitycode")) {
                    logger.info(client.getContentAsString());
                    throw new PluginImplementationException("No captcha.\nCannot find requested page content");

                }
                stepCaptcha(client.getContentAsString());

            } while (client.getContentAsString().contains("You may forgot the security code or it might be wrong"));

            Matcher matcher = Pattern.compile(">([0-9.]*) MB</div>", Pattern.MULTILINE).matcher(client.getContentAsString());
            if (matcher.find()) {
                logger.info("File size " + matcher.group(1));
                Double a = new Double(matcher.group(1).replaceAll(" ", ""));
                a = (a * 1024 * 1024);
                httpFile.setFileSize(a.longValue());
            }
            // download: JFC107.part1.rar
            matcher = Pattern.compile("download:([^<]*)", Pattern.MULTILINE).matcher(client.getContentAsString());
            if (matcher.find()) {
                final String fn = matcher.group(1);
                logger.info("File name " + fn);
                httpFile.setFileName(fn);
            } else logger.warning("File name was not found" + client.getContentAsString());

            matcher = Pattern.compile("please wait.*countdown\\(([0-9]+)", Pattern.MULTILINE).matcher(client.getContentAsString());
            if (matcher.find()) {
                int time = Integer.parseInt(matcher.group(1)) / 100;
                downloader.sleep(time + 1);
                if (downloader.isTerminated())
                    throw new InterruptedException();
            }
            matcher = Pattern.compile("href=\"([^\"]*)\" >Click here for the download", Pattern.MULTILINE).matcher(client.getContentAsString());
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

    private String replaceEntities(String s) {
        s = s.replaceAll("\\&amp;", "&");
        return s;
    }

    private boolean stepEnterPage(String contentAsString) throws Exception {
        Matcher matcher = Pattern.compile("class=\"Free_dl\"><a href=\"([^\"]*)\"", Pattern.MULTILINE).matcher(client.getContentAsString());
        if (!matcher.find()) {
            checkProblems();
            throw new InvalidURLOrServiceProblemException("Invalid URL or unindentified service");
        }
        String s = "/" + replaceEntities(matcher.group(1));
        client.setReferer(initURL);

        logger.info("Go to URL - " + s);
        GetMethod method1 = client.getGetMethod(HTTP_NETLOAD + s);
        enterURL = HTTP_NETLOAD + s;
        method1.setFollowRedirects(true);
        client.getHTTPClient().getParams().setBooleanParameter(HttpClientParams.ALLOW_CIRCULAR_REDIRECTS, true);
        client.getHTTPClient().getParams().setParameter("http.protocol.single-cookie-header", true);

        if (client.makeRequest(method1) != HttpStatus.SC_OK) {
            throw new PluginImplementationException("Problem with a connection to service.\nCannot find requested page content");
        }

        return true;
    }


    private boolean stepCaptcha(String contentAsString) throws Exception {
        if (contentAsString.contains("Please enter the Securitycode")) {

            Matcher matcher = Pattern.compile("src=\"(share\\/includes\\/captcha.*?)\"", Pattern.MULTILINE).matcher(contentAsString);
            if (matcher.find()) {
                String s = "/" + replaceEntities(matcher.group(1));
                String captcha = downloader.getCaptcha(HTTP_NETLOAD + s);
                if (captcha == null) {
                    throw new CaptchaEntryInputMismatchException();
                } else {

                    String file_id = getParameter("file_id", contentAsString);
                    matcher = Pattern.compile("form method\\=\"post\" action\\=\"([^\"]*)\"", Pattern.MULTILINE).matcher(contentAsString);
                    if (!matcher.find()) {
                        throw new PluginImplementationException("Captcha form action was not found");
                    }
                    s = "/" + matcher.group(1);
                    client.setReferer(enterURL);
                    final PostMethod postMethod = client.getPostMethod(HTTP_NETLOAD + s);
                    postMethod.addParameter("file_id", file_id);
                    postMethod.addParameter("captcha_check", captcha);
                    postMethod.addParameter("start", "");

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

    private String getParameter(String s, String contentAsString) throws PluginImplementationException {
        Matcher matcher = Pattern.compile("name=\"" + s + "\"[^v>]*value=\"([^\"]*)\"", Pattern.MULTILINE).matcher(contentAsString);
        if (matcher.find()) {
            return matcher.group(1);
        } else
            throw new PluginImplementationException("Parameter " + s + " was not found");
    }

    private void checkProblems() throws ServiceConnectionProblemException, YouHaveToWaitException, URLNotAvailableAnymoreException {
        Matcher matcher;
        matcher = Pattern.compile("You could download your next file in.*countdown\\(([0-9]+)", Pattern.MULTILINE).matcher(client.getContentAsString());
        if (matcher.find()) {
            final int time = Integer.parseInt(matcher.group(1)) / 6000;
            throw new YouHaveToWaitException(String.format("<b> You could download your next file in %s minutes", time), time * 60);
        }
        matcher = Pattern.compile("Sorry, we don't host the requested file", Pattern.MULTILINE).matcher(client.getContentAsString());
        if (matcher.find()) {
            throw new URLNotAvailableAnymoreException(String.format("<b>Requested file isn't hosted. Probably was deleted.</b>"));
        }

    }

}