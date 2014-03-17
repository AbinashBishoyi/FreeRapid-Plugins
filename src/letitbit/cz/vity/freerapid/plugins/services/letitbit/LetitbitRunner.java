package cz.vity.freerapid.plugins.services.letitbit;

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
import java.util.regex.Pattern;

/**
 * @author Ladislav Vitasek, Ludek Zika
 */
class LetitbitRunner {
    private final static Logger logger = Logger.getLogger(LetitbitRunner.class.getName());
    private HttpDownloadClient client;

    public void run(HttpFileDownloader downloader) throws Exception {
        HttpFile httpFile = downloader.getDownloadFile();
        client = downloader.getClient();
        String fileURL = httpFile.getFileUrl().toString();
        logger.info("Starting download in TASK " + fileURL);

        final GetMethod getMethod = client.getGetMethod(fileURL);
        getMethod.setFollowRedirects(true);
        if (client.makeRequest(getMethod) == HttpStatus.SC_OK) {
            Matcher matcher = Pattern.compile("form action=\"([^\"]*download[^\"]*)\"(.*)</form>", Pattern.MULTILINE | Pattern.DOTALL).matcher(client.getContentAsString());
            if (!matcher.find()) {
                checkProblems();
                logger.warning(client.getContentAsString());
                throw new InvalidURLOrServiceProblemException("Invalid URL or unindentified service");
            }
            String s = matcher.group(1);
            String form = matcher.group(2);
            matcher = Pattern.compile("span> (.*? .b)</h1>", Pattern.MULTILINE).matcher(client.getContentAsString());
            if (matcher.find()) {
                logger.info("File size " + matcher.group(1));
                httpFile.setFileSize(PlugUtils.getFileSizeFromString(matcher.group(1)));
            }
            matcher = Pattern.compile("File::</span> ([^<]*)", Pattern.MULTILINE).matcher(client.getContentAsString());
            if (matcher.find()) {
                final String fn = matcher.group(1);
                logger.info("File name " + fn);
                httpFile.setFileName(fn);
            } else logger.warning("File name was not found" + client.getContentAsString());

            String uid = getParameter("uid", form);
            String frameset = getParameter("frameset", form);

            logger.info("Submit form to - " + s);
            client.setReferer(fileURL);
            final PostMethod postMethod = client.getPostMethod(s);
            postMethod.addParameter("uid", uid);
            postMethod.addParameter("frameset", frameset);
            postMethod.addParameter("fix", "1");

            if (client.makeRequest(postMethod) != HttpStatus.SC_OK) {
                logger.info(client.getContentAsString());
                throw new PluginImplementationException("Problem with a connection to service.\nCannot find requested page content");
            }
            matcher = Pattern.compile("src=\"([^?]*)\\?link=([^\"]*)\"", Pattern.MULTILINE).matcher(client.getContentAsString());
            if (matcher.find()) {
                String t = matcher.group(2);
                logger.info("Download URL: " + t);
                //downloader.sleep(10);
                if (downloader.isTerminated())
                    throw new InterruptedException();
                httpFile.setState(DownloadState.GETTING);
                client.setReferer(matcher.group(1) + "?link=" + t);
                client.getHTTPClient().getParams().setParameter(HttpMethodParams.SINGLE_COOKIE_HEADER, true);
                final GetMethod method = client.getGetMethod(t);
                method.setFollowRedirects(true);
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


    private String getParameter(String s, String contentAsString) throws PluginImplementationException {
        Matcher matcher = Pattern.compile("name=\"" + s + "\"[^>]*value=\"([^\"]*)\"", Pattern.MULTILINE).matcher(contentAsString);
        if (matcher.find()) {
            return matcher.group(1);
        } else
            logger.info(contentAsString);
        throw new PluginImplementationException("Parameter " + s + " was not found");
    }

    private void checkProblems() throws ServiceConnectionProblemException, YouHaveToWaitException, URLNotAvailableAnymoreException {
        Matcher matcher;
        matcher = Pattern.compile("The page is temporarily unavailable", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE).matcher(client.getContentAsString());
        if (matcher.find()) {
            throw new YouHaveToWaitException("The page is temporarily unavailable!", 60 * 2);
        }

        matcher = Pattern.compile("file was not found", Pattern.MULTILINE).matcher(client.getContentAsString());
        if (matcher.find()) {
            throw new URLNotAvailableAnymoreException(String.format("The requested file was not found"));

        }

    }


}