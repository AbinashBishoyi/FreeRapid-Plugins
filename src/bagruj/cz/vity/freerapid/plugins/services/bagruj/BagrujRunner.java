package cz.vity.freerapid.plugins.services.bagruj;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.hoster.CaptchaSupport;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * @author Ladislav Vitasek, Ludek Zika
 */
class BagrujRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(BagrujRunner.class.getName());

    public BagrujRunner() {
        super();
    }

    public void runCheck() throws Exception {
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {
            checkNameAndSize(getContentAsString());
        } else
            throw new ServiceConnectionProblemException("Problem with a connection to service.\nCannot find requested page content");
    }

    public void run() throws Exception {
        super.run();
        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {
            if (getContentAsString().contains("captcha")) {
                checkNameAndSize(getContentAsString());
                while (getContentAsString().contains("captcha")) {

                    PostMethod method = stepCaptcha(getContentAsString());
                    Matcher matcher = getMatcherAgainstContent("<span id=\"countdown\">([0-9]+)</span>");
                    if (matcher.find()) {
                        int time = Integer.parseInt(matcher.group(1));
                        downloadTask.sleep(time - 1);
                    }
                    if (!makeRedirectedRequest(method))
                        throw new ServiceConnectionProblemException("Problem with a connection to service.\nCannot find requested page content");
                }
                if (!getContentAsString().contains("pro tvoji IP adresu n")) {
                    checkProblems();
                    logger.info(getContentAsString());
                    throw new PluginImplementationException("Cannot find requested page content");
                }

                Matcher matcher = getMatcherAgainstContent("(http://[^\"]+)\">\\1");

                if (!matcher.find()) {
                    checkProblems();
                    logger.info(getContentAsString());
                    throw new PluginImplementationException("Cannot find requested page content");
                }

                String finalURL = matcher.group(1);
                GetMethod finalMethod = getGetMethod(finalURL);

                if (!tryDownloadAndSaveFile(finalMethod)) {
                    checkProblems();
                    logger.warning(getContentAsString());
                    throw new IOException("File input stream is empty.");
                }

            } else {
                checkProblems();
                logger.info(getContentAsString());
                throw new PluginImplementationException("Problem with a connection to service.\nCannot find requested page content");
            }
        } else
            throw new ServiceConnectionProblemException("Problem with a connection to service.\nCannot find requested page content");
    }


    private void checkNameAndSize(String content) throws Exception {

        if (!content.contains("bagruj.cz")) {
            logger.warning(getContentAsString());
            throw new InvalidURLOrServiceProblemException("Invalid URL or unindentified service");
        }
        if (content.contains("No such file with this filename")) {
            throw new URLNotAvailableAnymoreException(String.format("<b>No such file with this filename.</b><br>"));
        }

        Matcher matcher = getMatcherAgainstContent("Soubor:((<[^>]*>)|\\s)*([^<]*)<");
        if (matcher.find()) {
            String fn = matcher.group(matcher.groupCount());
            logger.info("File name " + fn);
            httpFile.setFileName(fn);
            httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
        }

        matcher = getMatcherAgainstContent("\\(([0-9.]+ bytes)\\)");
        if (matcher.find()) {
            Long a = PlugUtils.getFileSizeFromString(matcher.group(1));
            logger.info("File size " + a);
            httpFile.setFileSize(a);
            httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
        }
        // konec odebirani jmena


    }


    private PostMethod stepCaptcha(String contentAsString) throws Exception {
        if (contentAsString.contains("captcha")) {
            CaptchaSupport captchaSupport = getCaptchaSupport();
            Matcher matcher = PlugUtils.matcher("(http://bagruj.cz/captchas/[^\"]+)", contentAsString);
            if (matcher.find()) {
                String s = matcher.group(1);
                logger.info("Captcha URL " + s);
                String captcha = captchaSupport.getCaptcha(s);

                if (captcha == null) {
                    throw new CaptchaEntryInputMismatchException();
                } else {

                    client.setReferer(fileURL);
                    final PostMethod postMethod = getPostMethod(fileURL);
                    String[] parameters = new String[]{"op", "id", "rand", "method_free", "method_free", "down_direct"};
                    PlugUtils.addParameters(postMethod, contentAsString, parameters);
                    postMethod.addParameter("code", captcha);
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
        if (getContentAsString().contains("No such file with this filename")) {
            throw new URLNotAvailableAnymoreException(String.format("<b>No such file with this filename.</b><br>"));
        }
        if (getContentAsString().contains("The page you are looking for is temporarily unavailable")) {
            throw new ServiceConnectionProblemException(String.format("<b>The page you are looking for is temporarily unavailable</b><br>"));

        }


    }

}