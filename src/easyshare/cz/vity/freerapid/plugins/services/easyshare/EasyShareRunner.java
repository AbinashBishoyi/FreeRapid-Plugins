package cz.vity.freerapid.plugins.services.easyshare;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.HttpFileDownloader;
import cz.vity.freerapid.plugins.webclient.PlugUtils;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.apache.commons.httpclient.params.HttpMethodParams;

import java.io.IOException;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * @author Ladislav Vitasek, Ludek Zika
 */

class EasyShareRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(EasyShareRunner.class.getName());
    private String httpSite;
    private String baseURL;

    public void runCheck(HttpFileDownloader downloader) throws Exception {
        super.runCheck(downloader);
        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRequest(getMethod)) {
            checkName(getContentAsString());
        } else
            throw new PluginImplementationException("Problem with a connection to service.\nCannot find requested page content");
    }

    private void checkName(String contentAsString) throws Exception {

        if (!contentAsString.contains("easy-share")) {
            logger.warning(getContentAsString());
            throw new InvalidURLOrServiceProblemException("Invalid URL or unindentified service");
        }

        if (contentAsString.contains("File not found")) {
            throw new URLNotAvailableAnymoreException(String.format("<b>File not found</b><br>"));

        }
        Matcher matcher = PlugUtils.matcher("Download ([^,]+), upload", contentAsString);
        if (matcher.find()) {
            final String fn = new String(matcher.group(1).getBytes("windows-1252"), "UTF-8");
            logger.info("File name " + fn);
            httpFile.setFileName(fn);
        } else logger.warning("File name was not found" + getContentAsString());
    }


    public void run(HttpFileDownloader downloader) throws Exception {
        client.getHTTPClient().getParams().setBooleanParameter(HttpClientParams.ALLOW_CIRCULAR_REDIRECTS, true);
        baseURL = fileURL;
        httpSite = fileURL.substring(0, fileURL.lastIndexOf('/'));
        logger.info("Starting download in TASK " + fileURL);
        GetMethod getMethod = getGetMethod(fileURL);
        getMethod.setFollowRedirects(true);
        if (makeRequest(getMethod)) {
            checkName(getContentAsString());
            if (!(getContentAsString().contains("Please enter") || getContentAsString().contains("w="))) {
                checkProblems();
                logger.warning(getContentAsString());
                throw new PluginImplementationException("Plugin implementation problem");

            }

            while (getContentAsString().contains("Please enter") || getContentAsString().contains("w=")) {
                Matcher matcher;

                if (getContentAsString().contains("w=")) {
                    matcher = getMatcherAgainstContent("w='([0-9]+?)';");
                    if (matcher.find()) {
                        downloader.sleep(Integer.parseInt(matcher.group(1)));
                    } else {
                        logger.warning(getContentAsString());
                        throw new PluginImplementationException("Plugin implementation problem");
                    }

                    matcher = getMatcherAgainstContent("u='(/.*?)';");
                    if (matcher.find()) {
                        final String link = matcher.group(1);
                        getMethod = getGetMethod(httpSite + link);
                        if (!makeRequest(getMethod)) {
                            logger.warning(getContentAsString());
                            throw new ServiceConnectionProblemException("Unknown error");
                        }
                    }

                } else if (!getContentAsString().contains("Please enter")) {
                    checkProblems();
                    logger.warning(getContentAsString());
                    throw new PluginImplementationException("Plugin implementation problem");
                }
                matcher = getMatcherAgainstContent("File size: ([0-9.]+( )?.B).?</div>");
                if (matcher.find()) {
                    logger.info("File size " + matcher.group(1));
                    httpFile.setFileSize(PlugUtils.getFileSizeFromString(matcher.group(1)));
                }
                if (stepCaptcha(getContentAsString())) break;
            }


        } else
            throw new PluginImplementationException("Problem with a connection to service.\nCannot find requested page content");
    }


    private void checkProblems() throws ServiceConnectionProblemException, URLNotAvailableAnymoreException {
        if (getContentAsString().contains("File not found")) {
            throw new URLNotAvailableAnymoreException(String.format("<b>File not found</b><br>"));

        }

    }

    private boolean stepCaptcha(final String contentAsString) throws Exception {
        if (contentAsString.contains("Please enter")) {

            final Matcher m = PlugUtils.matcher("type=\"hidden\" name=\"id\" value=\"(.*?)\"", contentAsString);
            String id;
            if (m.find()) {
                id = m.group(1);
                logger.info("ESRunner - file id is " + id);
            } else throw new PluginImplementationException("ID was not found");

            Matcher matcher = PlugUtils.matcher("src=\"(/kaptchacluster[^\"]*)\"", contentAsString);
            if (matcher.find()) {
                String s = matcher.group(1);
                logger.info(httpSite + s);
                client.setReferer(baseURL);
                String captcha = getCaptchaSupport().getCaptcha(httpSite + s);

                if (captcha == null) {
                    throw new CaptchaEntryInputMismatchException();
                } else {
                    matcher = PlugUtils.matcher("<form action=\"([^\"]*file_contents[^\"]*)\"", contentAsString);
                    if (matcher.find()) {
                        s = matcher.group(1);
                        logger.info(s);

                        final PostMethod method = getPostMethod(s);
                        client.getHTTPClient().getParams().setParameter(HttpMethodParams.SINGLE_COOKIE_HEADER, true);
                        method.addParameter("id", id);
                        method.addParameter("captcha", captcha);
                        if (tryDownload(method)) return true;
                        else {
                            checkProblems();
                            if (getContentAsString().contains("Please enter") || getContentAsString().contains("w="))
                                return false;
                            logger.warning(getContentAsString());
                            throw new IOException("File input stream is empty.");
                        }
                    } else {
                        logger.warning(getContentAsString());
                        throw new PluginImplementationException("Action was not found");
                    }
                }

            } else throw new PluginImplementationException("Captcha picture was not found");
        }
        return false;
    }

}