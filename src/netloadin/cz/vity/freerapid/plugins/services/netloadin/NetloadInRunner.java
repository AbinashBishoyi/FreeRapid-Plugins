package cz.vity.freerapid.plugins.services.netloadin;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
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
class NetloadInRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(NetloadInRunner.class.getName());
    private String HTTP_NETLOAD = "http://netload.in";

    private String initURL;
    private String enterURL;

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        checkURL(fileURL);
        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            if (!getContentAsString().contains("Netload")) {
                logger.warning(getContentAsString());
                throw new InvalidURLOrServiceProblemException("Invalid URL or unindentified service");
            }
            Matcher matcher = getMatcherAgainstContent("we don't host the requested file");
            if (matcher.find()) {
                throw new URLNotAvailableAnymoreException(String.format("<b>Requested file isn't hosted. Probably was deleted.</b>"));
            }
            httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
        } else {
            checkProblems();
            throw new PluginImplementationException();
        }
    }

    public void run() throws Exception {
        super.run();
        checkURL(fileURL);
        initURL = fileURL;
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {
            do {
                stepEnterPage(getContentAsString());
                if (!getContentAsString().contains("Please enter the Securitycode")) {
                    logger.info(getContentAsString());
                    throw new PluginImplementationException("No captcha.\nCannot find requested page content");

                }
                stepCaptcha(getContentAsString());

            } while (getContentAsString().contains("You may forgot the security code or it might be wrong"));

            Matcher matcher = getMatcherAgainstContent(">([0-9.]+ .B)</div>");
            if (matcher.find()) {
                logger.info("File size " + matcher.group(1));
                httpFile.setFileSize(PlugUtils.getFileSizeFromString(matcher.group(1)));
            }
            // download: JFC107.part1.rar
            matcher = getMatcherAgainstContent("download:\\s*([^<]*)");
            if (matcher.find()) {
                final String fn = matcher.group(1);
                logger.info("File name " + fn);
                httpFile.setFileName(fn);
            } else logger.warning("File name was not found" + getContentAsString());

            matcher = getMatcherAgainstContent("please wait.*countdown\\(([0-9]+)");
            if (matcher.find()) {
                int time = Integer.parseInt(matcher.group(1)) / 100;
                downloadTask.sleep(time + 1);
            }
            matcher = PlugUtils.matcher("href=\"([^\"]*)\" >Click here for the download", getContentAsString());
            if (matcher.find()) {
                String s = matcher.group(1);
                logger.info("Found File URL - " + s);
                final GetMethod method = getGetMethod(s);
                if (!tryDownloadAndSaveFile(method)) {
                    checkProblems();
                    throw new IOException("File input stream is empty.");
                }
            } else {
                checkProblems();
                logger.info(getContentAsString());
                throw new PluginImplementationException();
            }

        } else
            throw new PluginImplementationException();
    }

    private boolean stepEnterPage(String contentAsString) throws Exception {
        if (contentAsString.contains("This file is secured with a password")) {
            stepPasswordPage();
            contentAsString = getContentAsString();
        }
        Matcher matcher = PlugUtils.matcher("class=\"Free_dl\">(.|\\W)*?<a href=\"([^\"]*)\"", contentAsString);
        //logger.info(contentAsString);       
        if (!matcher.find()) {
            checkProblems();
            throw new InvalidURLOrServiceProblemException("Invalid URL or unindentified service");
        }
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
        String s = "/" + PlugUtils.replaceEntities(matcher.group(2));
        client.setReferer(initURL);

        logger.info("Go to URL - " + s);
        GetMethod method1 = getGetMethod(HTTP_NETLOAD + s);
        enterURL = HTTP_NETLOAD + s;
        client.getHTTPClient().getParams().setBooleanParameter(HttpClientParams.ALLOW_CIRCULAR_REDIRECTS, true);

        if (!makeRedirectedRequest(method1)) {
            throw new PluginImplementationException();
        }

        return true;
    }


    private boolean stepCaptcha(String contentAsString) throws Exception {
        if (contentAsString.contains("Please enter the Securitycode")) {

            Matcher matcher = PlugUtils.matcher("src=\"(share\\/includes\\/captcha.*?)\"", contentAsString);
            if (matcher.find()) {
                String s = "/" + PlugUtils.replaceEntities(matcher.group(1));
                String captcha = getCaptchaSupport().getCaptcha(HTTP_NETLOAD + s);
                if (captcha == null) {
                    throw new CaptchaEntryInputMismatchException();
                } else {

                    String file_id = PlugUtils.getParameter("file_id", contentAsString);
                    matcher = PlugUtils.matcher("form method\\=\"post\" action\\=\"([^\"]*)\"", contentAsString);
                    if (!matcher.find()) {
                        throw new PluginImplementationException("Captcha form action was not found");
                    }
                    s = "/" + matcher.group(1);
                    client.setReferer(enterURL);
                    final PostMethod postMethod = getPostMethod(HTTP_NETLOAD + s);
                    postMethod.addParameter("file_id", file_id);
                    postMethod.addParameter("captcha_check", captcha);
                    postMethod.addParameter("start", "");

                    if (makeRequest(postMethod)) {

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

    private void checkURL(String fileURL) {
        if (fileURL.toLowerCase().contains("www.netload.in")) HTTP_NETLOAD = "http://www.netload.in";
    }

    private void stepPasswordPage() throws Exception {
        while (getContentAsString().contains("This file is secured with a password")) {

            Matcher matcher = getMatcherAgainstContent("name=\"form\" method=\"post\" action=\"([^\"]*)\"");
            if (!matcher.find()) {
                throw new PluginImplementationException("Invalid URL or unindentified service");
            }
            String tar = HTTP_NETLOAD + "/" + matcher.group(1);
            logger.info("Post url to - " + tar);
            PostMethod post1 = getPostMethod(tar);
            matcher = getMatcherAgainstContent("value=\"([^\"]*)\" name=\"file_id\"");
            if (!matcher.find()) {
                throw new PluginImplementationException("Invalid URL or unindentified service");
            }
            String file_id = matcher.group(1);
            post1.addParameter("file_id", file_id);
            post1.addParameter("password", getPassword());

            if (!makeRedirectedRequest(post1)) {
                throw new PluginImplementationException();
            }

        }

    }

    private String getPassword() throws Exception {
        NetloadPasswordUI ps = new NetloadPasswordUI();
        if (getDialogSupport().showOKCancelDialog(ps, "Secured file on Netload.in")) {
            return (ps.getPassword());
        } else throw new NotRecoverableDownloadException("This file is secured with a password!");

    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        Matcher matcher;
        matcher = getMatcherAgainstContent("You could download your next file in.*countdown\\(([0-9]+)");
        if (matcher.find()) {
            final int time = Integer.parseInt(matcher.group(1)) / 6000;
            throw new YouHaveToWaitException(String.format("<b> You could download your next file in %s minutes", time), time * 60);
        }
        if (getContentAsString().contains("Sorry, we don't host the requested file")) {
            throw new URLNotAvailableAnymoreException(String.format("<b>Requested file isn't hosted. Probably was deleted.</b>"));
        }
        if (getContentAsString().contains("unknown_file_data")) {
            throw new URLNotAvailableAnymoreException("Unknown file data");
        }
        if (getContentAsString().contains("currently in maintenance work")) {
            throw new ServiceConnectionProblemException("This Server is currently in maintenance work. Please try it in a few hours again.");
        }
        if (getContentAsString().contains("This file was damaged")) {
            throw new URLNotAvailableAnymoreException("This file was damaged by a hard-disc crash.");
        }
    }

}