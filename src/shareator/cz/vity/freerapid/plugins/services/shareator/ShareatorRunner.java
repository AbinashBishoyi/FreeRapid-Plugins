package cz.vity.freerapid.plugins.services.shareator;

import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.exceptions.YouHaveToWaitException;
import cz.vity.freerapid.plugins.webclient.*;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * @author Ladislav Vitasek, Ludek Zika
 */
class ShareatorRunner {
    private final static Logger logger = Logger.getLogger(cz.vity.freerapid.plugins.services.shareator.ShareatorRunner.class.getName());
    private HttpDownloadClient client;

    public void run(HttpFileDownloader downloader) throws Exception {
        HttpFile httpFile = downloader.getDownloadFile();
        client = downloader.getClient();
        String fileURL = httpFile.getFileUrl().toString();
        logger.info("Starting download in TASK " + fileURL);

        final GetMethod getMethod = client.getGetMethod(fileURL);
        getMethod.setFollowRedirects(true);
        if (client.makeRequest(getMethod) == HttpStatus.SC_OK) {
            if (client.getContentAsString().contains("btn_download")) {
                Matcher matcher = PlugUtils.matcher("Filename:((<[^>]*>)|\\s)+([^<]+)<", client.getContentAsString());
                if (matcher.find()) {
                    String fn = matcher.group(matcher.groupCount());
                    logger.info("File name " + fn);
                    httpFile.setFileName(fn);
                }
                matcher = PlugUtils.matcher("([0-9.]+ bytes)", client.getContentAsString());
                if (matcher.find()) {
                    Long a = PlugUtils.getFileSizeFromString(matcher.group(1));
                    logger.info("File size " + a);
                    httpFile.setFileSize(a);
                }
                downloader.sleep(5);
                String id = getParameter("id", client.getContentAsString());
                String rand = getParameter("rand", client.getContentAsString());

                client.setReferer(fileURL);
                final PostMethod postMethod = client.getPostMethod(fileURL);
                postMethod.addParameter("op", "download2");
                postMethod.addParameter("id", id);
                postMethod.addParameter("rand", rand);
                postMethod.addParameter("referer", "");
                postMethod.addParameter("method_free", "");
                postMethod.addParameter("method_premium", "");

                if (client.makeRequest(postMethod) == HttpStatus.SC_OK) {
                    matcher = PlugUtils.matcher("((<[^>]*>)|\\s)+(http://shareator.com[^<]+)<", client.getContentAsString());
                    if (!matcher.find()) {
                        checkProblems();
                        throw new PluginImplementationException("Problem with a connection to service.\nCannot find requested page content");
                    }
                    final String fn = encodeURL(matcher.group(matcher.groupCount()));
                    logger.info("Found file URL " + fn);
                    final GetMethod method = client.getGetMethod(fn);
                    httpFile.setState(DownloadState.GETTING);
                    try {
                        final InputStream inputStream = client.makeFinalRequestForFile(method, httpFile);
                        if (inputStream != null) {
                            downloader.saveToFile(inputStream);

                        } else {
                            checkProblems();
                            logger.info(client.getContentAsString());
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

    private String encodeURL(String s) throws UnsupportedEncodingException {
        Matcher matcher = PlugUtils.matcher("(.*/)([^/]*)$", s);
        if (matcher.find()) {
            return matcher.group(1) + URLEncoder.encode(matcher.group(2), "UTF-8");
        }
        return s;
    }

    private void checkProblems() throws ServiceConnectionProblemException, YouHaveToWaitException, URLNotAvailableAnymoreException {
        Matcher matcher;
        matcher = PlugUtils.matcher("No such file", client.getContentAsString());
        if (matcher.find()) {
            throw new URLNotAvailableAnymoreException(String.format("<b>No such file with this filename</b><br>"));
        }

        matcher = PlugUtils.matcher("using all download slots for IP ([0-9.]+)", client.getContentAsString());
        if (matcher.find()) {
            final String ip = matcher.group(1);
            throw new ServiceConnectionProblemException(String.format("You're using all download slots for IP %s<br>Please wait till current downloads finish.", ip));
        }
    }
}