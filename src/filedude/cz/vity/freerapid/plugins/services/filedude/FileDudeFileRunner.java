package cz.vity.freerapid.plugins.services.filedude;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author tong2shot
 */
class FileDudeFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(FileDudeFileRunner.class.getName());


    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL); //create GET request
        if (makeRedirectedRequest(method)) { //we make the main request
            checkProblems();//check problems

            HttpMethod httpMethod;
            String tempURL = fileURL;
            while (PlugUtils.matcher("<img src=\"(http://.+?captcha.+?)\" />", getContentAsString()).find()) { //while page content contains captcha
                String captcha = getCaptcha();
                httpMethod = getMethodBuilder()
                        .setReferer(fileURL)
                        .setActionFromFormWhereActionContains("confirm", true)
                        .setParameter("captcha", captcha)
                        .toPostMethod();
                if (!makeRedirectedRequest(httpMethod)) {
                    checkProblems();
                    throw new PluginImplementationException();
                }
                checkProblems();
                tempURL = httpMethod.getURI().toString();
            }

            httpMethod = getMethodBuilder()
                    .setReferer(tempURL)
                    .setActionFromAHrefWhereATagContains("Download!")
                    .toHttpMethod();

            //here is the download link extraction
            if (!tryDownloadAndSaveFile(httpMethod)) {
                checkProblems();//if downloading failed
                throw new ServiceConnectionProblemException("Error starting download");//some unknown problem
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private String getCaptcha() throws Exception {
        final Matcher matcher = PlugUtils.matcher("<img src=\"(http://.+?captcha.+?)\" />", getContentAsString());
        final String captchaURL;
        if (matcher.find()) {
            captchaURL = matcher.group(1);
        } else {
            throw new PluginImplementationException("Captcha URL not found");
        }

        final String captcha = getCaptchaSupport().getCaptcha(captchaURL);
        if (captcha == null) {
            throw new CaptchaEntryInputMismatchException();
        }
        return captcha;
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("The file you've requested doesn't exist")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
        if (contentAsString.contains("Internal Server Error")) {
            throw new PluginImplementationException("Internal Server Error");
        }
    }

}