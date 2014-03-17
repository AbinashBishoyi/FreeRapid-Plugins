package cz.vity.freerapid.plugins.services.keepfile;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.services.recaptcha.ReCaptcha;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.hoster.CaptchaSupport;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;

import java.net.URLDecoder;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author RickCL
 */
class KeepFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(KeepFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        HttpMethod httpMethod = getMethodBuilder()
            .setAction(fileURL)
            .toGetMethod();
        if (!makeRedirectedRequest(httpMethod)) {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
        String content = getContentAsString();

        try {
            String fileName = PlugUtils.getStringBetween(content, "<font style=\"font-size:16px;\"><b>", "<b>");

            String fileSize = PlugUtils.getStringBetween(content, "<b> (", ")");
            final long lsize = PlugUtils.getFileSizeFromString( fileSize );

            httpFile.setFileName( URLDecoder.decode(fileName,"UTF-8") );
            httpFile.setFileSize( lsize );
        } catch(PluginImplementationException e) {
            checkProblems();
            throw new ServiceConnectionProblemException(e);
        }

    }

    private void checkProblems() throws Exception {
        if (getContentAsString().contains("File Not Found")) {
            throw new URLNotAvailableAnymoreException("File Not Found");
        }
    }

    @Override
    public void run() throws Exception {
        super.run();

        HttpMethod httpMethod = getMethodBuilder()
            .setAction(fileURL)
            .toGetMethod();
        if (!makeRedirectedRequest(httpMethod)) {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
        httpMethod = getMethodBuilder().setActionFromFormWhereTagContains("method_free", true).toPostMethod();
        logger.info( httpMethod.getURI().toString() );
        if (!makeRedirectedRequest(httpMethod)) {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
        String content = getContentAsString();

        int waitTime = 0;
        Matcher matcher = PlugUtils.matcher("Wait <span[^>]*>\\s*([0-9]+?)\\s*</span>", content);
        if(!matcher.find()) {
            matcher = PlugUtils.matcher("You have to wait till next download (\\d*) (?:minutes|seconds),{0,1}\\s{0,1}(?:(\\d*) seconds)?", content);
            if(matcher.find()) {
                for(int i=1; i<=matcher.groupCount(); i++) {
                    if( matcher.groupCount() == 2 && i == 1) {
                        waitTime += new Long(TimeUnit.MINUTES.toSeconds(Long.parseLong(matcher.group(i)))).intValue();
                    } else {
                        waitTime += new Long(TimeUnit.SECONDS.toSeconds(Long.parseLong(matcher.group(i)))).intValue();
                    }
                }
                throw new YouHaveToWaitException( matcher.group(0), waitTime );
            }
            throw new ServiceConnectionProblemException();
        } else {
            waitTime = new Long(TimeUnit.SECONDS.toSeconds(Long.parseLong(matcher.group(1)))).intValue();
        }
        downloadTask.sleep(waitTime);

        while (getContentAsString().contains("captcha")) {
            if (!tryDownloadAndSaveFile(stepCaptcha(fileURL))) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
        }

    }

    private HttpMethod stepCaptcha(final String action) throws Exception {
        logger.info("Handling ReCaptcha");

        final Matcher m = getMatcherAgainstContent("api.recaptcha.net/noscript\\?k=([^\"]+)\"");
        if (!m.find()) throw new PluginImplementationException("ReCaptcha key not found");
        final String reCaptchaKey = m.group(1);

        final String content = getContentAsString();
        final ReCaptcha r = new ReCaptcha(reCaptchaKey, client);
        final CaptchaSupport captchaSupport = getCaptchaSupport();

        final String captchaURL = r.getImageURL();
        logger.info("Captcha URL " + captchaURL);

        final String captcha = captchaSupport.getCaptcha(captchaURL);
        if (captcha == null) throw new CaptchaEntryInputMismatchException();
        r.setRecognized(captcha);

        return r.modifyResponseMethod(
                getMethodBuilder(content)
                        .setReferer(action)
                        .setActionFromFormWhereTagContains("recaptcha", true)
                        .setAction(action)
        ).toPostMethod();
    }

}