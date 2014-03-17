package cz.vity.freerapid.plugins.services.shareator;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * @author Ladislav Vitasek, Ludek Zika
 */
class ShareatorRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(ShareatorRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRequest(getMethod)) {
            checkNameAndSize(getContentAsString());
        } else
            throw new PluginImplementationException("Problem with a connection to service.\nCannot find requested page content");
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);

        final GetMethod getMethod = getGetMethod(fileURL);
        getMethod.setFollowRedirects(true);
        if (makeRequest(getMethod)) {
            if (getContentAsString().contains("btn_download")) {
                checkNameAndSize(getContentAsString());
                downloadTask.sleep(5);
                String id = PlugUtils.getParameter("id", getContentAsString());
                String rand = PlugUtils.getParameter("rand", getContentAsString());

                client.setReferer(fileURL);
                final PostMethod postMethod = getPostMethod(fileURL);
                postMethod.addParameter("op", "download2");
                postMethod.addParameter("id", id);
                postMethod.addParameter("rand", rand);
                postMethod.addParameter("referer", "");
                postMethod.addParameter("method_free", "");
                postMethod.addParameter("method_premium", "");

                if (makeRequest(postMethod)) {
                    Matcher matcher = getMatcherAgainstContent("((<[^>]*>)|\\s)+(http://shareator.com[^<]+)<");
                    if (!matcher.find()) {
                        checkProblems();
                        throw new PluginImplementationException("Problem with a connection to service.\nCannot find requested page content");
                    }
                    final String fn = encodeURL(matcher.group(matcher.groupCount()));
                    logger.info("Found file URL " + fn);
                    final GetMethod method = getGetMethod(fn);
                    if (!tryDownloadAndSaveFile(method)) {
                        checkProblems();
                        logger.info(getContentAsString());
                        throw new IOException("File input stream is empty.");
                    }
                } else {
                    checkProblems();
                    logger.info(getContentAsString());
                    throw new PluginImplementationException("Problem with a connection to service.\nCannot find requested page content");
                }
            } else {
                checkProblems();
                logger.info(getContentAsString());
                throw new PluginImplementationException("Problem with a connection to service.\nCannot find requested page content");
            }
        } else
            throw new PluginImplementationException("Problem with a connection to service.\nCannot find requested page content");
    }

    private void checkNameAndSize(String content) throws Exception {

        if (!content.contains("shareator.com")) {
            logger.warning(getContentAsString());
            throw new InvalidURLOrServiceProblemException("Invalid URL or unindentified service");
        }

        if (getContentAsString().contains("No such file")) {
            throw new URLNotAvailableAnymoreException(String.format("<b>No such file with this filename</b><br>"));
        }

        Matcher matcher = PlugUtils.matcher("Filename:((<[^>]*>)|\\s)+([^<]+)<", content);
        if (matcher.find()) {
            String fn = matcher.group(matcher.groupCount());
            logger.info("File name " + fn);
            httpFile.setFileName(fn);
        } else logger.warning("File name was not found" + getContentAsString());
        matcher = PlugUtils.matcher("([0-9.]+ bytes)", content);
        if (matcher.find()) {
            Long a = PlugUtils.getFileSizeFromString(matcher.group(1));
            logger.info("File size " + a);
            httpFile.setFileSize(a);
            httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
        }
    }

    private String encodeURL(String s) throws UnsupportedEncodingException {
        Matcher matcher = PlugUtils.matcher("(.*/)([^/]*)$", s);
        if (matcher.find()) {
            return matcher.group(1) + URLEncoder.encode(matcher.group(2), "UTF-8");
        }
        return s;
    }

    private void checkProblems() throws ServiceConnectionProblemException, YouHaveToWaitException, URLNotAvailableAnymoreException {
        if (getContentAsString().contains("No such file")) {
            throw new URLNotAvailableAnymoreException(String.format("<b>No such file with this filename</b><br>"));
        }

        Matcher matcher = getMatcherAgainstContent("using all download slots for IP ([0-9.]+)");
        if (matcher.find()) {
            final String ip = matcher.group(1);
            throw new ServiceConnectionProblemException(String.format("You're using all download slots for IP %s<br>Please wait till current downloads finish.", ip));
        }
    }
}