package cz.vity.freerapid.plugins.services.megaupload;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.params.HttpClientParams;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * @author Ladislav Vitasek, Ludek Zika
 */

class MegauploadRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(MegauploadRunner.class.getName());
    private String HTTP_SITE = "http://www.megaupload.com";
    //private int captchaCount;

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        if (httpFile.getFileUrl().getHost().contains("megarotic") || httpFile.getFileUrl().getHost().contains("sexuploader"))
            HTTP_SITE = "http://www.megarotic.com";
        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {
            checkNameAndSize(getContentAsString());
        } else
            throw new PluginImplementationException();
    }

    @Override
    public void run() throws Exception {
        super.run();
        client.getHTTPClient().getParams().setBooleanParameter(HttpClientParams.ALLOW_CIRCULAR_REDIRECTS, true);
        if (httpFile.getFileUrl().getHost().contains("megarotic") || httpFile.getFileUrl().getHost().contains("sexuploader"))
            HTTP_SITE = "http://www.megarotic.com";
        logger.info("Starting download in TASK " + fileURL);

        final GetMethod getMethod = getGetMethod(fileURL);
        getMethod.setFollowRedirects(true);
        if (makeRequest(getMethod)) {
            checkNameAndSize(getContentAsString());
            Matcher matcher;
            //       captchaCount = 0;
            while (getContentAsString().contains("Enter this")) {
                stepCaptcha(getContentAsString());
            }

            if (getContentAsString().contains("downloadlink")) {
                matcher = getMatcherAgainstContent("id=\"downloadlink\"><a href=\"(http.+?)\"");
                if (!matcher.find()) {
                    throw new PluginImplementationException();
                }

                final String downloadURL = matcher.group(1);
                final int i = downloadURL.lastIndexOf('/');
                if (i > 0) {
                    httpFile.setFileName(downloadURL.substring(i + 1));
                }
                final GetMethod method = getGetMethod(downloadURL);
                if (!tryDownloadAndSaveFile(method)) {
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
            throw new PluginImplementationException("Problem with a connection to service.\nCannot find requested page content");
    }

    private void checkNameAndSize(String content) throws Exception {

        if (content.contains("link you have clicked is not available")) {
            throw new URLNotAvailableAnymoreException("<b>The file is not available</b><br>");

        }
        Matcher matcher = PlugUtils.matcher("font-size:13px;\">([0-9.]+ .B).?</font>", content);
        if (matcher.find()) {
            logger.info("File size " + matcher.group(1));
            httpFile.setFileSize(PlugUtils.getFileSizeFromString(matcher.group(1)));
        }
        //Filename:</font> <font style="font-family: arial; color: rgb(255, 103, 0); font-size: 15px; font-weight: bold;">
        matcher = PlugUtils.matcher("Filename:</font> <font .+?>(.+?)</font><br>", content);
        if (matcher.find()) {
            final String fn = PlugUtils.unescapeHtml(matcher.group(1));
            logger.info("File name " + fn);
            httpFile.setFileName(fn);
            httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
        } else logger.warning("File name was not found" + getContentAsString());

    }


    private void checkProblems() throws ServiceConnectionProblemException, URLNotAvailableAnymoreException, IOException, YouHaveToWaitException {

        final String contentAsString = getContentAsString();
        if (contentAsString.contains("trying to access is temporarily unavailable"))
            throw new YouHaveToWaitException("The file you are trying to access is temporarily unavailable.", 2 * 60);


        if (contentAsString.contains("Download limit exceeded")) {
            final GetMethod getMethod = getGetMethod(HTTP_SITE + "/premium/???????????????");
            if (makeRequest(getMethod)) {
                Matcher matcher = getMatcherAgainstContent("Please wait ([0-9]+)");
                if (matcher.find()) {
                    throw new YouHaveToWaitException("You used up your limit for file downloading!", 1 + 60 * Integer.parseInt(matcher.group(1)));
                }
            }
            throw new ServiceConnectionProblemException("Download limit exceeded.");
        }

        if (contentAsString.contains("All download slots")) {

            throw new ServiceConnectionProblemException("No free slot for your country.");
        }

        if (contentAsString.contains("Unfortunately, the link you have clicked is not available")) {
            throw new URLNotAvailableAnymoreException("<b>The file is not available</b><br>");

        }

    }

    private boolean stepCaptcha(String contentAsString) throws Exception {
        if (contentAsString.contains("Enter this")) {

            Matcher matcher = PlugUtils.matcher("src=\"(.*?/gencap.php[^\"]*)\"", contentAsString);
            if (matcher.find()) {
                String s = PlugUtils.replaceEntities(matcher.group(1));
                logger.info("Captcha - image " + HTTP_SITE + s);
                String captcha;
                final BufferedImage captchaImage = getCaptchaSupport().getCaptchaImage(s);
//                if (captchaCount++ < 3) {
//                    EditImage ei = new EditImage(captchaImage);
//                    captcha = PlugUtils.recognize(ei.separate(), "-C A-z");
//                    if (captcha != null) {
//                        logger.info("Captcha - OCR recognized " + captcha + " attempts " + captchaCount);
//                        matcher = PlugUtils.matcher("[A-Z-a-z-0-9]{3}", captcha);
//                        if (!matcher.find()) {
//                            captcha = null;
//                        }
//                    }
//                }

//                if (captcha == null) {
                captcha = getCaptchaSupport().askForCaptcha(captchaImage);
//                } else captchaImage.flush();//askForCaptcha uvolnuje ten obrazek, takze tady to udelame rucne
                if (captcha == null)
                    throw new CaptchaEntryInputMismatchException();

                final PostMethod postMethod = getPostMethod(fileURL);
                PlugUtils.addParameters(postMethod, contentAsString, new String[]{"megavar"});

                PlugUtils.addParameters(postMethod, contentAsString, new String[]{"captchacode", "megavar"});

                postMethod.addParameter("captcha", captcha);

                if (makeRequest(postMethod)) {

                    return true;
                }
            } else throw new PluginImplementationException("Captcha picture was not found");
        }
        return false;
    }

//    private String encodeURL(String s) throws UnsupportedEncodingException {
//        Matcher matcher = PlugUtils.matcher("(.*/)([^/]*)$", s);
//        if (matcher.find()) {
//            return matcher.group(1) + URLEncoder.encode(matcher.group(2), "UTF-8");
//        }
//        return s;
//    }
}