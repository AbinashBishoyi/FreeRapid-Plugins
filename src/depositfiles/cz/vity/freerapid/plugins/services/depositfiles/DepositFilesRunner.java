package cz.vity.freerapid.plugins.services.depositfiles;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.*;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Ladislav Vitasek, Ludek Zika
 */
class DepositFilesRunner {
    private final static Logger logger = Logger.getLogger(cz.vity.freerapid.plugins.services.depositfiles.DepositFilesRunner.class.getName());
    private HttpDownloadClient client;


    private static final String HTTP_DEPOSITFILES = "http://www.depositfiles.com";


    public void run(HttpFileDownloader downloader) throws Exception {

        HttpFile httpFile = downloader.getDownloadFile();
        client = downloader.getClient();
        String fileURL = httpFile.getFileUrl().toString();
        fileURL = CheckURL(fileURL);
        logger.info("Starting download in TASK " + fileURL);

        final GetMethod getMethod = client.getGetMethod(fileURL);
        getMethod.setFollowRedirects(true);
        if (client.makeRequest(getMethod) == HttpStatus.SC_OK) {

            Matcher matcher = PlugUtils.matcher("<b>([0-9.]+&nbsp;.B)</b>", client.getContentAsString());
            if (matcher.find()) {
                logger.info("File size " + matcher.group(1));
                httpFile.setFileSize(PlugUtils.getFileSizeFromString(matcher.group(1).replaceAll("&nbsp;", "")));
            }
            matcher = PlugUtils.matcher("class\\=\"info[^=]*\\=\"([^\"]*)\"", client.getContentAsString());
            if (matcher.find()) {
                final String fn = matcher.group(1);
                logger.info("File name " + fn);
                httpFile.setFileName(fn);
            } else logger.warning("File name was not found" + client.getContentAsString());

            if (!client.getContentAsString().contains("Free downloading mode")) {

                matcher = PlugUtils.matcher("form action=\\\"([^h\\\"][^t\\\"][^t\\\"][^p\\\"][^\\\"]*)\\\"", client.getContentAsString());
                if (!matcher.find()) {
                    checkProblems();
                    logger.warning(client.getContentAsString());
                    throw new InvalidURLOrServiceProblemException("Invalid URL or unindentified service");
                }
                String s = matcher.group(1);

                logger.info("Submit form to - " + s);
                client.setReferer(fileURL);
                final PostMethod postMethod = client.getPostMethod(HTTP_DEPOSITFILES + s);
                postMethod.addParameter("gateway_result", "1");

                if (client.makeRequest(postMethod) != HttpStatus.SC_OK) {
                    logger.info(client.getContentAsString());
                    throw new PluginImplementationException("Problem with a connection to service.\nCannot find requested page content");
                }

            }
            //        <span id="download_waiter_remain">60</span>
            matcher = PlugUtils.matcher("download_waiter_remain\">([0-9]+)", client.getContentAsString());
            if (!matcher.find()) {
                checkProblems();
                throw new ServiceConnectionProblemException("Problem with a connection to service.\nCannot find requested page content");
            }
            String t = matcher.group(1);
            int seconds = new Integer(t);
            logger.info("wait - " + t);

            if (downloader.isTerminated())
                throw new InterruptedException();


            matcher = PlugUtils.matcher("form action=\"([^\"]*)\" method=\"get\"", client.getContentAsString());
            if (matcher.find()) {
                t = matcher.group(1);
                logger.info("Download URL: " + t);
                downloader.sleep(seconds + 1);
                if (downloader.isTerminated())
                    throw new InterruptedException();
                httpFile.setState(DownloadState.GETTING);
                final GetMethod method = client.getGetMethod(t);

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

    private String CheckURL(String fileURL) {
        return fileURL.replaceFirst("/../files", "/en/files");

    }

    private void checkProblems() throws ServiceConnectionProblemException, YouHaveToWaitException, URLNotAvailableAnymoreException {
        Matcher matcher;
        String content = client.getContentAsString();
        if (content.contains("already downloading")) {
            throw new ServiceConnectionProblemException(String.format("<b>Your IP is already downloading a file from our system.</b><br>You cannot download more than one file in parallel."));
        }
        matcher = Pattern.compile("Please try in\\s*([0-9]+) minute", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE).matcher(content);
        if (matcher.find()) {
            throw new YouHaveToWaitException("You used up your limit for file downloading!", Integer.parseInt(matcher.group(1)) * 60 + 20);
        }
        matcher = Pattern.compile("Please try in\\s*([0-9]+) hour", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE).matcher(content);
        if (matcher.find()) {
            throw new YouHaveToWaitException("You used up your limit for file downloading!", Integer.parseInt(matcher.group(1)) * 60 * 60 + 20);
        }

        matcher = PlugUtils.matcher("slots[^<]*busy", content);
        if (matcher.find()) {
            throw new ServiceConnectionProblemException(String.format("<b>All downloading slots for your country are busy</b><br>"));

        }
        if (content.contains("file does not exist")) {
            throw new URLNotAvailableAnymoreException(String.format("<b>Such file does not exist or it has been removed for infringement of copyrights.</b><br>"));

        }

    }


}
