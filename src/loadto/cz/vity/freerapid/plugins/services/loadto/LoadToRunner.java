package cz.vity.freerapid.plugins.services.loadto;

import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.exceptions.YouHaveToWaitException;
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
class LoadToRunner {
    private final static Logger logger = Logger.getLogger(cz.vity.freerapid.plugins.services.loadto.LoadToRunner.class.getName());
    private HttpDownloadClient client;

    public void run(HttpFileDownloader downloader) throws Exception {
        HttpFile httpFile = downloader.getDownloadFile();
        client = downloader.getClient();
        String fileURL = httpFile.getFileUrl().toString();
        logger.info("Starting download in TASK " + fileURL);

        final GetMethod getMethod = client.getGetMethod(fileURL);
        getMethod.setFollowRedirects(true);
        if (client.makeRequest(getMethod) == HttpStatus.SC_OK) {
            if (client.getContentAsString().contains("Load.to")) {
                Matcher matcher = PlugUtils.matcher("<title>([^/]*) //", client.getContentAsString());
                if (matcher.find()) {
                    String fn = matcher.group(1);
                    logger.info("File name " + fn);
                    httpFile.setFileName(fn);
                }
                matcher = PlugUtils.matcher("([0-9.]+ Bytes)", client.getContentAsString());
                if (matcher.find()) {
                    Long a = PlugUtils.getFileSizeFromString(matcher.group(1));
                    logger.info("File size " + a);
                    httpFile.setFileSize(a);
                }
                downloader.sleep(5);
                matcher = PlugUtils.matcher("<form method=\"post\" action=\"(http[^\"]*)\"", client.getContentAsString());
                if (!matcher.find()) {
                    checkProblems();
                    throw new ServiceConnectionProblemException("Problem with a connection to service.\nCannot find requested page content");
                }
                final PostMethod method = client.getPostMethod(matcher.group(1));

                client.getHTTPClient().getParams().setParameter(HttpMethodParams.SINGLE_COOKIE_HEADER, true);
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
        } else
            throw new PluginImplementationException("Problem with a connection to service.\nCannot find requested page content");
    }

    private void checkProblems() throws ServiceConnectionProblemException, YouHaveToWaitException, URLNotAvailableAnymoreException {
        Matcher matcher;
        matcher = PlugUtils.matcher("Can't find file.", client.getContentAsString());
        if (matcher.find()) {
            throw new URLNotAvailableAnymoreException(String.format("<b>Can't find file. Please check URL.</b><br>"));
        }
    }

}