package cz.vity.freerapid.plugins.services.easyshare;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.params.HttpClientParams;

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

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRequest(getMethod)) {
            checkNameAndSize(getContentAsString());
        } else
            throw new ServiceConnectionProblemException("Problem with a connection to service.\nCannot find requested page content");
    }

    private void checkNameAndSize(String contentAsString) throws Exception {

        if (!contentAsString.contains("Share")) {
            logger.warning(getContentAsString());
            throw new InvalidURLOrServiceProblemException("Invalid URL or unindentified service");
        }

        checkProblems();
        Matcher matcher = PlugUtils.matcher("Download ([^,]+), upload", contentAsString);
        if (matcher.find()) {
            final String fn = new String(matcher.group(1).getBytes("windows-1252"), "UTF-8");
            logger.info("File name " + fn);
            httpFile.setFileName(fn);
        } else logger.warning("File name was not found" + getContentAsString());

        matcher = getMatcherAgainstContent("\\(([0-9.]+ .B)\\)");
        if (matcher.find()) {
            logger.info("File size " + matcher.group(1));
            httpFile.setFileSize(PlugUtils.getFileSizeFromString(matcher.group(1)));
        }

    }

    @Override
    public void run() throws Exception {
        super.run();
        client.getHTTPClient().getParams().setBooleanParameter(HttpClientParams.ALLOW_CIRCULAR_REDIRECTS, true);
        baseURL = fileURL;
        httpSite = fileURL.substring(0, fileURL.indexOf('/', 10));
        logger.info("httpSite set to " + httpSite);
        logger.info("Starting download in TASK " + fileURL);
        GetMethod getMethod = getGetMethod(fileURL);
        getMethod.setFollowRedirects(true);
        if (makeRequest(getMethod)) {
            checkNameAndSize(getContentAsString());
            if (!(getContentAsString().contains("Type characters") || getContentAsString().contains("Free Download"))) {
                checkProblems();
                logger.warning(getContentAsString());
                throw new PluginImplementationException("Plugin implementation problem");

            }
            if (getContentAsString().contains("Free Download")) {

                if (getContentAsString().contains(" w=")) {
                    Matcher matcher = getMatcherAgainstContent("w='([0-9]+?)';");
                    if (matcher.find()) {
                        downloadTask.sleep(Integer.parseInt(matcher.group(1)));
                    } else {
                        logger.warning(getContentAsString());
                        throw new PluginImplementationException("Plugin implementation problem");
                    }
                }

                getMethod = getGetMethod(skipEnterPageUrl());
                if (!makeRequest(getMethod)) {
                    logger.warning(getContentAsString());
                    throw new ServiceConnectionProblemException("Unknown error");
                }
            }
            if (!getContentAsString().contains("Type characters") && getContentAsString().contains("Download the file")) {
                stepNoCaptcha(getContentAsString());
            } else while (true) {
                if (!getContentAsString().contains("Type characters")) {
                    checkProblems();
                    logger.warning(getContentAsString());
                    throw new PluginImplementationException("Plugin implementation problem");
                }

                if (stepCaptcha(getContentAsString())) break;
            }


        } else
            throw new ServiceConnectionProblemException("Problem with a connection to service.\nCannot find requested page content");
    }

    private String skipEnterPageUrl() throws PluginImplementationException {
        Matcher matcher = PlugUtils.matcher("com/([0-9]+)", fileURL);
        if (matcher.find()) {
            return "http://www.easy-share.com/c/" + matcher.group(1);
        } else {
            logger.warning("Cannot get number from url " + fileURL);
            throw new PluginImplementationException("Plugin implementation problem");
        }
    }


    private void checkProblems() throws Exception {
        if (getContentAsString().contains("File not found")) {
            throw new URLNotAvailableAnymoreException(String.format("<b>File not found</b><br>"));
        }
        if (getContentAsString().contains("Requested file is deleted")) {
            throw new URLNotAvailableAnymoreException(String.format("<b>Requested file is deleted</b><br>"));
        }
        if (getContentAsString().contains("Error 404: Page not found")) {
            throw new InvalidURLOrServiceProblemException(String.format("<b>Error 404: Page not found</b><br>"));
        }
        if (getContentAsString().contains("You have downloaded ")) {
            throw new YouHaveToWaitException(String.format("<b>You have downloaded to much during last hour. You have to wait</b><br>"), 20 * 60);
        }

    }

    private boolean stepCaptcha(final String contentAsString) throws Exception {
        if (contentAsString.contains("Type characters")) {

            final Matcher m = PlugUtils.matcher("type=\"hidden\" name=\"id\" value=\"(.*?)\"", contentAsString);
            String id;
            if (m.find()) {
                id = m.group(1);
                logger.info("ESRunner - file id is " + id);
            } else throw new PluginImplementationException("ID was not found");

            Matcher matcher = PlugUtils.matcher("src=\"(/kaptchacluster[^\"]*)\"", contentAsString);
            if (matcher.find()) {
                String s = matcher.group(1);
                logger.info("Captcha image url: " + httpSite + s);
                client.setReferer(baseURL);
                String captcha = getCaptchaSupport().getCaptcha(httpSite + s);

                if (captcha == null) {
                    throw new CaptchaEntryInputMismatchException();
                } else {
                    logger.info("Entered captcha: " + captcha);
                    return finalAction(contentAsString, id, captcha);
                }

            } else throw new PluginImplementationException("Captcha picture was not found");
        }
        return false;
    }


    private boolean stepNoCaptcha(final String contentAsString) throws Exception {
        if (contentAsString.contains("Download the file")) {
            logger.info("Captcha not needed ");
            String id = PlugUtils.getParameter( "id", contentAsString);
            String captcha = PlugUtils.getParameter( "captcha", contentAsString);

            return finalAction(contentAsString, id, captcha);
        }


        return false;
    }

    private boolean finalAction(String contentAsString, String id, String captcha) throws Exception {
        Matcher matcher;
        String s;
        matcher = PlugUtils.matcher("<form action=\"([^\"]*file_contents[^\"]*)\"", contentAsString);
        if (matcher.find()) {
            s = matcher.group(1);
            logger.info("Captcha action from form: " + s);
            final PostMethod method = getPostMethod(s);
            method.addParameter("id", id);
            method.addParameter("captcha", captcha);
            if (tryDownloadAndSaveFile(method)) return true;
            else {
                checkProblems();
                if (getContentAsString().contains("Type characters"))
                    return false;
                logger.warning(getContentAsString());
                throw new IOException("File input stream is empty.");
            }
        } else {
            logger.warning(getContentAsString());
            throw new PluginImplementationException("Action was not found");
        }
    }

}