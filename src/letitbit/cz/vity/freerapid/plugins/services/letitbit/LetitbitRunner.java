package cz.vity.freerapid.plugins.services.letitbit;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.*;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;

import java.io.IOException;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Ladislav Vitasek, Ludek Zika
 */
class LetitbitRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(LetitbitRunner.class.getName());


    public void runCheck(HttpFileDownloader downloader) throws Exception {
        super.runCheck(downloader);
        final GetMethod getMethod = client.getGetMethod(fileURL);
        if (makeRequest(getMethod)) {
            checkNameAndSize(client.getContentAsString());
            httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
        } else
            throw new PluginImplementationException("Problem with a connection to service.\nCannot find requested page content");
    }

    private void checkNameAndSize(String contentAsString) throws Exception {
        if (!contentAsString.contains("letitbit")) {
            logger.warning(client.getContentAsString());
            throw new InvalidURLOrServiceProblemException("Invalid URL or unindentified service");
        }
        if (contentAsString.contains("file was not found")) {
            throw new URLNotAvailableAnymoreException(String.format("The requested file was not found"));

        }
        Matcher matcher = PlugUtils.matcher("span> (.*? .b)</h1>", contentAsString);
        if (matcher.find()) {
            logger.info("File size " + matcher.group(1));
            httpFile.setFileSize(PlugUtils.getFileSizeFromString(matcher.group(1)));
        }
        matcher = PlugUtils.matcher("File::</span>\\s*([^<]*)", contentAsString);
        if (matcher.find()) {
            final String fn = matcher.group(1);
            logger.info("File name " + fn);
            httpFile.setFileName(fn);
        } else logger.warning("File name was not found" + contentAsString);
    }

    public void run(HttpFileDownloader downloader) throws Exception {
        super.run(downloader);
        logger.info("Starting download in TASK " + fileURL);

        final GetMethod getMethod = client.getGetMethod(fileURL);
        getMethod.setFollowRedirects(true);
        if (makeRequest(getMethod)) {
            checkNameAndSize(client.getContentAsString());
            Matcher matcher = Pattern.compile("form action=\"([^\"]*download[^\"]*)\"(.*)</form>", Pattern.MULTILINE | Pattern.DOTALL).matcher(client.getContentAsString());
            if (!matcher.find()) {
                checkProblems();
                logger.warning(client.getContentAsString());
                throw new InvalidURLOrServiceProblemException("Invalid URL or unindentified service");
            }
            String s = matcher.group(1);
            String form = matcher.group(2);

            String uid = PlugUtils.getParameter("uid", form);
            String frameset = PlugUtils.getParameter("frameset", form);

            logger.info("Submit form to - " + s);
            client.setReferer(fileURL);
            final PostMethod postMethod = client.getPostMethod(s);

            postMethod.addParameter("uid", uid);
            postMethod.addParameter("frameset", frameset);
            postMethod.addParameter("fix", "1");

            if (!makeRequest(postMethod)) {
                logger.info(client.getContentAsString());
                throw new PluginImplementationException("Problem with a connection to service.\nCannot find requested page content");
            }
            matcher = PlugUtils.matcher("src=\"([^?]*)\\?link=([^\"]*)\"", client.getContentAsString());
            if (matcher.find()) {
                String t = matcher.group(2);
                logger.info("Download URL: " + t);
                downloader.sleep(4);
                if (downloader.isTerminated())
                    throw new InterruptedException();
                httpFile.setState(DownloadState.GETTING);
                client.setReferer(matcher.group(1) + "?link=" + t);
                client.getHTTPClient().getParams().setParameter(HttpMethodParams.SINGLE_COOKIE_HEADER, true);
                final GetMethod method = client.getGetMethod(t);
                method.setFollowRedirects(true);
                if (!tryDownload(method)) {
                    checkProblems();
                    logger.info(client.getContentAsString());
                    throw new IOException("File input stream is empty.");
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
        String content = client.getContentAsString();
        matcher = Pattern.compile("The page is temporarily unavailable", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE).matcher(client.getContentAsString());
        if (matcher.find()) {
            throw new YouHaveToWaitException("The page is temporarily unavailable!", 60 * 2);
        }
        matcher = Pattern.compile("You must have static IP", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE).matcher(client.getContentAsString());
        if (matcher.find()) {
            throw new YouHaveToWaitException("You must have static IP! Try again", 60 * 2);
        }
        if (content.contains("file was not found")) {
            throw new URLNotAvailableAnymoreException(String.format("The requested file was not found"));

        }

    }


}