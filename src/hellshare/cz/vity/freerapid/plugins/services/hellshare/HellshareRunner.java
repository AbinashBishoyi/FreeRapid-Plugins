package cz.vity.freerapid.plugins.services.hellshare;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.*;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;

import java.io.InputStream;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * @author Ladislav Vitasek, Ludek Zika
 */
class HellshareRunner {
    private final static Logger logger = Logger.getLogger(cz.vity.freerapid.plugins.services.hellshare.HellshareRunner.class.getName());
    private HttpDownloadClient client;
    private HttpFileDownloader downloader;
    private HttpFile httpFile;


    public void run(HttpFileDownloader downloader) throws Exception {
        this.downloader = downloader;
        this.httpFile = downloader.getDownloadFile();
        client = downloader.getClient();
        String fileURL = httpFile.getFileUrl().toString();
        logger.info("Starting download in TASK " + fileURL);

        final GetMethod getMethod = client.getGetMethod(fileURL);
        getMethod.setFollowRedirects(true);
        if (client.makeRequest(getMethod) == HttpStatus.SC_OK) {
            if (client.getContentAsString().contains("free_download_iframe")) {
                Matcher matcher = PlugUtils.matcher("<div class=\"download-filename\">([^<]*)</div>", client.getContentAsString());
                if (matcher.find()) {
                    String fn = matcher.group(matcher.groupCount());
                    logger.info("File name " + fn);
                    httpFile.setFileName(fn);
                }
                matcher = PlugUtils.matcher("<td>([0-9.]+ .B)</td>", client.getContentAsString());
                if (matcher.find()) {
                    Long a = PlugUtils.getFileSizeFromString(matcher.group(1));
                    logger.info("File size " + a);
                    httpFile.setFileSize(a);
                }

                matcher = PlugUtils.matcher("([0-9.]+)%", client.getContentAsString());
                if (matcher.find()) {
                    if (matcher.group(1).equals("100"))
                        throw new YouHaveToWaitException("Na serveru jsou využity všechny free download sloty", 30);
                }
                client.setReferer(fileURL);

                final PostMethod postmethod = client.getPostMethod(fileURL);
                postmethod.addParameter("free_download_iframe", "FREE DOWNLOAD");
                if (client.makeRequest(postmethod) == HttpStatus.SC_OK) {
                    PostMethod method = stepCaptcha();
                    httpFile.setState(DownloadState.GETTING);
                    if (!trydownload(method)) {
                        boolean finish = false;
                        while (!finish) {
                            method = stepCaptcha();
                            finish = trydownload(method);
                        }
                    }
                } else {
                    checkProblems();
                    logger.info(client.getContentAsString());
                    throw new PluginImplementationException("Problem with a connection to service.\nCannot find requested page content");
                }
            } else {
                checkProblems();
                logger.info(client.getContentAsString());
                throw new PluginImplementationException("Problem with a connection to service.\nCannot find requested page content");
            }
        } else
            throw new PluginImplementationException("Problem with a connection to service.\nCannot find requested page content");
    }

    private PostMethod stepCaptcha() throws Exception {

        if ("".equals(client.getContentAsString())) {
            throw new YouHaveToWaitException("Neurèité omezení", 120);

        }

        Matcher matcher;
        matcher = PlugUtils.matcher("<img id=\"captcha-img\" src=\"([^\"]*)\"", client.getContentAsString());
        if (!matcher.find()) {
            throw new PluginImplementationException("Problem with a connection to service.\nCannot find requested page content");
        }

        String img = replaceEntities(matcher.group(1));
        logger.info("Captcha image " + img);
        final String captcha = downloader.getCaptcha(img);
        if (captcha == null) {
            throw new CaptchaEntryInputMismatchException();
        }
        matcher = PlugUtils.matcher("form action=\"([^\"]*)\"", client.getContentAsString());
        if (!matcher.find()) {
            throw new PluginImplementationException("Problem with a connection to service.\nCannot find requested page content");
        }

        String finalURL = matcher.group(1);
        String free_download_uri = getParameter("free_download_uri", client.getContentAsString());

        final PostMethod method = client.getPostMethod(finalURL);

        method.addParameter("free_download_uri", free_download_uri);
        method.addParameter("captcha", captcha);
        method.addParameter("free_download_button", "St%E1hnout");
        return method;
    }


    private boolean trydownload(PostMethod method) throws Exception {
        httpFile.setState(DownloadState.GETTING);
        try {
            final InputStream inputStream2 = client.makeFinalRequestForFile(method, httpFile);
            if (inputStream2 != null) {
                downloader.saveToFile(inputStream2);
                return true;
            } else {
                return false;
            }
        } finally {
            method.abort();
            method.releaseConnection();
        }
    }

    private String getParameter(String s, String contentAsString) throws PluginImplementationException {
        Matcher matcher = PlugUtils.matcher("name=\"" + s + "\" value=\"([^\"]*)\"", contentAsString);
        if (matcher.find()) {
            return matcher.group(1);
        } else
            throw new PluginImplementationException("Parameter " + s + " was not found");
    }


    private String replaceEntities(String s) {
        s = s.replaceAll("\\&amp;", "&");
        return s;
    }

    private void checkProblems() throws ServiceConnectionProblemException, YouHaveToWaitException, URLNotAvailableAnymoreException {
        Matcher matcher;
        matcher = PlugUtils.matcher("Soubor nenalezen", client.getContentAsString());
        if (matcher.find()) {
            throw new URLNotAvailableAnymoreException(String.format("<b>Soubor nenalezen</b><br>"));
        }


    }
}