package cz.vity.freerapid.plugins.services.egoshare;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadState;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * @author Ladislav Vitasek, Ludek Zika
 */
class EgoshareRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(EgoshareRunner.class.getName());
    private ServicePluginContext context;
    private String initURL;

    public EgoshareRunner(ServicePluginContext context) {
        super();
        this.context = context;
    }

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRequest(getMethod)) {
            checkNameAndSize(getContentAsString());
        } else
            throw new PluginImplementationException("Problem with a connection to service.\nCannot find requested page content");
    }

    @Override
    public void run() throws Exception {
        initURL = fileURL;
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod getMethod = getGetMethod(fileURL);
        getMethod.setFollowRedirects(true);
        if (makeRequest(getMethod)) {

            checkNameAndSize(getContentAsString());
            Matcher matcher;

            do {
                checkProblems();
                if (!getContentAsString().contains("Please enter the number")) {
                    logger.info(getContentAsString());
                    throw new PluginImplementationException("No captcha.\nCannot find requested page content");
                }
                stepCaptcha(getContentAsString());

            } while (getContentAsString().contains("Please enter the number"));

            matcher = getMatcherAgainstContent("decode\\(\"([^\"]*)\"");
            if (matcher.find()) {
                String s = decode(matcher.group(1));
                logger.info("Found File URL - " + s);
                httpFile.setState(DownloadState.GETTING);
                final GetMethod method = getGetMethod(s);
                Date newDate = new Date();
                if (tryDownloadAndSaveFile(method)) setTicket(newDate);
                else {
                    checkProblems();
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

        if (content.contains("Your requested file could not be found")) {
            throw new URLNotAvailableAnymoreException("Your requested file could not be found");
        }

        Matcher matcher = PlugUtils.matcher("\\(([0-9.]* .B)\\)", content);
        if (matcher.find()) {
            Long a = PlugUtils.getFileSizeFromString(matcher.group(1));
            logger.info("File size " + a);
            httpFile.setFileSize(a);
            httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
        }
        matcher = PlugUtils.matcher("File name: </b></td>\\s*<td align=left><b> ([^<]*)<", content);
        if (matcher.find()) {
            final String fn = matcher.group(1);
            logger.info("File name " + fn);
            httpFile.setFileName(fn);
        } else logger.warning("File name was not found" + getContentAsString());
    }

    private String decode(String input) {

        final StringBuilder output = new StringBuilder();
        int chr1;
        int chr2;
        int chr3;
        int enc1;
        int enc2;
        int enc3;
        int enc4;
        int i = 0;
        final String _keyStr = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=";

        input = input.replaceAll("[^A-Za-z0-9+/=]", "");

        while (i < input.length()) {

            enc1 = _keyStr.indexOf(input.charAt(i++));
            enc2 = _keyStr.indexOf(input.charAt(i++));
            enc3 = _keyStr.indexOf(input.charAt(i++));
            enc4 = _keyStr.indexOf(input.charAt(i++));

            chr1 = (enc1 << 2) | (enc2 >> 4);
            chr2 = ((enc2 & 15) << 4) | (enc3 >> 2);
            chr3 = ((enc3 & 3) << 6) | enc4;

            output.append((char) chr1);

            if (enc3 != 64) {
                output.append((char) chr2);
            }
            if (enc4 != 64) {
                output.append((char) chr3);
            }
        }


        try {
            return (new String(output.toString().getBytes(), "UTF-8"));

        } catch (UnsupportedEncodingException ex) {
            logger.warning("Unsupported encoding" + ex);
        }
        return "";

    }


    private boolean stepCaptcha(String contentAsString) throws Exception {
        if (contentAsString.contains("Please enter the number")) {

            if (contentAsString.contains("captcha")) {
                String s = "http://www.egoshare.com/captcha.php";
                String captcha = getCaptchaSupport().getCaptcha(s);
                if (captcha == null) {
                    throw new CaptchaEntryInputMismatchException();
                } else {

                    String ndpage = PlugUtils.getParameter("2ndpage", contentAsString);
                    final Matcher matcher = PlugUtils.matcher("name=myform action\\=\"([^\"]*)\"", contentAsString);
                    if (!matcher.find()) {
                        throw new PluginImplementationException("Captcha form action was not found");
                    }
                    s = matcher.group(1);
                    client.setReferer(initURL);
                    final PostMethod postMethod = getPostMethod(s);

                    postMethod.addParameter("captchacode", captcha);
                    postMethod.addParameter("2ndpage", ndpage);

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

    private int getTimeToWait() {
        long startOfTicket = context.getStartOfTicket().getTime();
        long now = new Date().getTime();
        if ((now - startOfTicket) < 60 * 60 * 1000)
            return new Long(((startOfTicket + 1000 * 60 * 60) - now) / 1000).intValue();
        return 20 * 60;
    }

    private void setTicket(Date newTime) {
        long oldTime = context.getStartOfTicket().getTime();

        if ((newTime.getTime() - oldTime) > 60 * 60 * 1000)
            context.setStartOfTicket(newTime);
    }

    private void checkProblems() throws ServiceConnectionProblemException, YouHaveToWaitException, URLNotAvailableAnymoreException {
        Matcher matcher;
        matcher = getMatcherAgainstContent("You have got max allowed download sessions from the same IP");
        if (matcher.find()) {
            throw new YouHaveToWaitException("You have got max allowed download sessions from the same IP!", 5 * 60);
        }
        matcher = getMatcherAgainstContent("this download is too big for your");
        if (matcher.find()) {

            throw new YouHaveToWaitException("Sorry, this download is too big for your remaining download volume per hour!!", getTimeToWait());
        }
        matcher = getMatcherAgainstContent("Your requested file could not be found");
        if (matcher.find()) {
            throw new URLNotAvailableAnymoreException("Your requested file could not be found");
        }

    }

}