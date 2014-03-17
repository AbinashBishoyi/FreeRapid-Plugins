package cz.vity.freerapid.plugins.services.uploadedto;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.services.recaptcha.ReCaptcha;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Ladislav Vitasek, ntoskrnl
 */
class UploadedToRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(UploadedToRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        addCookie(new Cookie(".uploaded.to", "lang", "en", "/", 86400, false));
        addCookie(new Cookie(".ul.to", "lang", "en", "/", 86400, false));
        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {
            checkSizeAndName(getContentAsString());
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        addCookie(new Cookie(".uploaded.to", "lang", "en", "/", 86400, false));
        addCookie(new Cookie(".ul.to", "lang", "en", "/", 86400, false));
        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            final String contentAsString = getContentAsString();
            checkSizeAndName(contentAsString);

            //they usually redirect
            fileURL = getMethod.getURI().toString();

            Matcher matcher = PlugUtils.matcher("var secs = ([0-9]+);", contentAsString);
            if (!matcher.find()) {
                if (contentAsString.contains("is exceeded")) {
                    matcher = PlugUtils.matcher("wait ([0-9]+) minute", contentAsString);
                    if (matcher.find()) {
                        Integer waitMinutes = Integer.valueOf(matcher.group(1));
                        if (waitMinutes == 0)
                            waitMinutes = 1;
                        throw new YouHaveToWaitException("<b>Uploaded.to error:</b><br>Your Free-Traffic is exceeded!", (waitMinutes * 60));
                    }
                    throw new YouHaveToWaitException("<b>Uploaded.to error:</b><br>Your Free-Traffic is exceeded!", 60);
                } else if (contentAsString.contains("File doesn")) {
                    throw new URLNotAvailableAnymoreException("<b>Uploaded.to error:</b><br>File doesn't exist");
                }
                throw new InvalidURLOrServiceProblemException("Invalid URL or unindentified service");
            }
            downloadTask.sleep(Integer.parseInt(matcher.group(1)) + 1);

            while (!tryDownloadAndSaveFile(stepCaptcha())) {
                checkProblems();
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkSizeAndName(String content) throws Exception {
        if (!content.contains("uploaded.to")) {
            throw new InvalidURLOrServiceProblemException("Invalid URL or unindentified service");
        }
        if (content.contains("File doesn")) {
            throw new URLNotAvailableAnymoreException("<b>Uploaded.to error:</b><br>File doesn't exist");
        }

        Matcher matcher = PlugUtils.matcher("([0-9.]+ .B)", content);
        if (matcher.find()) {
            final String fileSize = matcher.group(1);
            logger.info("File size " + fileSize);
            httpFile.setFileSize(PlugUtils.getFileSizeFromString(fileSize));
        }

        matcher = Pattern.compile("Filename: &nbsp;</td><td><b>(.*?)</b></td></tr>", Pattern.DOTALL).matcher(content);
        if (matcher.find()) {
            String fn = matcher.group(1).trim();
            matcher = PlugUtils.matcher("Filetype: &nbsp;</td><td>(\\S*)</td></tr>", content);
            if (matcher.find()) {
                fn = fn + matcher.group(1);
            }
            logger.info("File name '" + fn + "'");
            httpFile.setFileName(fn);
        } else logger.warning("File name was not found" + content);

        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);

    }


    private void checkProblems() throws ServiceConnectionProblemException {
        if (getContentAsString().contains("already downloading")) {
            throw new ServiceConnectionProblemException(String.format("<b>Uploaded.to Error:</b><br>Your IP address is already downloading a file. <br>Please wait until the download is completed."));
        }
        if (getContentAsString().contains("The internal connection has failed")) {
            throw new ServiceConnectionProblemException(String.format("<b>Uploaded.to Error:</b><br>Your IP address is already downloading a file. <br>Please wait until the download is completed."));
        }
        if (getContentAsString().contains("Currently a lot of users")) {
            throw new ServiceConnectionProblemException(String.format("<b>Uploaded to Error:</b><br>Currently a lot of users are downloading files."));
        }
        if (getContentAsString().contains("can only be queried by premium users")) {
            throw new ServiceConnectionProblemException(String.format("The file status can only be queried by premium users"));
        }
    }

    private HttpMethod stepCaptcha() throws Exception {
        MethodBuilder request = getMethodBuilder()
                .setReferer(fileURL)
                .setBaseURL(fileURL)
                .setActionFromFormByName("download_form", true);

        Matcher m = getMatcherAgainstContent("/noscript\\?k=([^\"]+)\"");
        if (!m.find()) throw new PluginImplementationException("ReCaptcha key not found");

        ReCaptcha r = new ReCaptcha(m.group(1), client);
        String imageURL = r.getImageURL();
        String captcha = getCaptchaSupport().getCaptcha(imageURL);
        if (captcha == null) throw new CaptchaEntryInputMismatchException();
        r.setRecognized(captcha);

        return r.modifyResponseMethod(request).toPostMethod();
    }

}