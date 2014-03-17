package cz.vity.freerapid.plugins.services.filebaseto;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.hoster.CaptchaSupport;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;

import java.io.IOException;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * @author Ladislav Vitasek, Ludek Zika
 */
class FilebaseToRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(FilebaseToRunner.class.getName());
    private final static String WEB = "http://filebase.to";

    public FilebaseToRunner() {
        super();
    }

    public void runCheck() throws Exception {
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkNameAndSize(getContentAsString());
        } else
            throw new ServiceConnectionProblemException();
    }

    public void run() throws Exception {
        super.run();
        client.setReferer(fileURL);
        GetMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkNameAndSize(getContentAsString());

            while (getContentAsString().contains("/captcha")) {

                PostMethod method = stepCaptcha(getContentAsString());
                if (!makeRedirectedRequest(method))
                    throw new ServiceConnectionProblemException();
                if (getContentAsString().contains("Der Code wurde falsch eingegeben"))
                    if (!makeRedirectedRequest(getMethod)) {
                        throw new ServiceConnectionProblemException();
                    }
            }

            final Matcher matcher1 = getMatcherAgainstContent("<center><form action=\"(http.+?)\"");
            final Matcher matcher2 = getMatcherAgainstContent("a href=\"(.*?download/ticket.*?)\"");
            if (matcher1.find()) {
                PostMethod postMethod = getPostMethod(matcher1.group(1));
                PlugUtils.addParameters(postMethod, getContentAsString(), new String[]{"wait"});
                //  logger.warning(getContentAsString());
                if (!tryDownloadAndSaveFile(postMethod)) {
                    checkProblems();
                    logger.warning(getContentAsString());
                    throw new IOException("File input stream is empty.");
                }

            } else if (matcher2.find()) {
                String fn = matcher2.group(1);
                logger.info("Alternative final URL " + fn);
                GetMethod gMethod = getGetMethod(matcher2.group(1));
                if (!tryDownloadAndSaveFile(gMethod)) {
                    checkProblems();
                    logger.warning(getContentAsString());
                    throw new IOException("File input stream is empty.");
                }

            } else throw new PluginImplementationException("Download link or captcha not found");


        } else
            throw new ServiceConnectionProblemException();
    }


    private void checkNameAndSize(String content) throws Exception {

        if (!content.contains("FileBase.to")) {
            logger.warning(getContentAsString());
            throw new InvalidURLOrServiceProblemException("Invalid URL or unindentified service");
        }

        Matcher matcher = getMatcherAgainstContent("#666666;\">\"(.+?)\"</span>");
        if (matcher.find()) {
            String fn = matcher.group(1).trim();
            logger.info("File name " + fn);
            httpFile.setFileName(fn);
            httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
        } else logger.warning("File name not found");

        matcher = getMatcherAgainstContent("e:</td>\\s*<td width=\"50%\" style=\"font-weight:bold;\">(.+?)<");
        if (matcher.find()) {
            Long a = PlugUtils.getFileSizeFromString(matcher.group(1));
            logger.info("File size " + a);
            httpFile.setFileSize(a);
        } else logger.warning("File size not found");
    }


    private PostMethod stepCaptcha(String contentAsString) throws Exception {
        if (contentAsString.contains("/captcha")) {
            CaptchaSupport captchaSupport = getCaptchaSupport();
            Matcher matcher = PlugUtils.matcher("<img src=\"(/captcha.+?)\"", contentAsString);
            if (matcher.find()) {
                String s = matcher.group(1);
                logger.info("Captcha URL " + s);
                String captcha = captchaSupport.getCaptcha(WEB + s);

                if (captcha == null) {
                    throw new CaptchaEntryInputMismatchException();
                } else {
                    client.setReferer(fileURL);
                    final PostMethod postMethod = getPostMethod(fileURL);
                    PlugUtils.addParameters(postMethod, contentAsString, new String[]{"cid"});
                    postMethod.addParameter("uid", captcha);
                    postMethod.addParameter("session_code", "");
                    return postMethod;
                }
            } else {
                logger.warning(contentAsString);
                throw new PluginImplementationException("Captcha picture was not found");
            }
        }
        return null;
    }

    private void checkProblems() throws ServiceConnectionProblemException, YouHaveToWaitException, URLNotAvailableAnymoreException {
        if (getContentAsString().contains("Fehler 404")) {
            throw new URLNotAvailableAnymoreException("Fehler 404 - Dieses Datei wurde leider nicht gefunden");
        }
        if (getContentAsString().contains("<h1>Not Found</h1>")) {
            throw new ServiceConnectionProblemException("File was not found on the server - service problem?");
        }
//        if (getContentAsString().contains("Please finish download and try")) {
//            throw new ServiceConnectionProblemException("You already download some file. Please finish download and try again.");
//        }
    }

}
