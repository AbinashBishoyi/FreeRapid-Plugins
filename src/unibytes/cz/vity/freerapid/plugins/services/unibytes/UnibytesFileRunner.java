package cz.vity.freerapid.plugins.services.unibytes;

import java.awt.image.BufferedImage;
import java.net.URLDecoder;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;

import cz.vity.freerapid.plugins.exceptions.CaptchaEntryInputMismatchException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.exceptions.YouHaveToWaitException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;

/**
 * Class which contains main code
 *
 * @author RickCL
 */
class UnibytesFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(UnibytesFileRunner.class.getName());

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

        try {
            //Matcher matcher = getMatcherAgainstContent("You are trying to download file:(?:<[^>]*>)*([^<]*)");
            String content = getContentAsString();
            String fileName = PlugUtils.getStringBetween(content, "You are trying to download file:", "</span><br/>").replaceAll("<[^>]*>", "");
            String fileSize=null;// = PlugUtils.getStringBetween(content, "You are trying to download file:[^\\(]*\\(", "\\)", 1);
            Matcher matcher = Pattern.compile("You are trying to download file:[^\n]*[^\\(]*\\(([^\\)]*)\\)", Pattern.MULTILINE).matcher(content);
            if(matcher.find()) {
                fileSize=matcher.group(1);
            } else {
                checkProblems();
            }

            final long lsize = PlugUtils.getFileSizeFromString( fileSize );

            httpFile.setFileName( URLDecoder.decode(fileName,"UTF-8") );
            httpFile.setFileSize( lsize );

            httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
        } catch(PluginImplementationException e) {
            checkProblems();
        }

    }

    private void checkProblems() throws Exception {
        if (getContentAsString().contains("File not found")) {
            throw new URLNotAvailableAnymoreException("File not found or removed");
        }
        if (getContentAsString().contains("Somebody else is already downloading")) {
            throw new ServiceConnectionProblemException("Somebody else is already downloading using your IP-address");
        }
        if (getContentAsString().contains("Try to download file later")) {
            int waitTime = PlugUtils.getWaitTimeBetween(getContentAsString(), "<span id=\"guestDownloadDelayValue\">", "</span>", TimeUnit.MINUTES);
            throw new YouHaveToWaitException("Try to download file later or get the VIP-account on our service. Wait for " + waitTime, waitTime );
        }

    }

    @Override
    public void run() throws Exception {
        super.run();

        HttpMethod httpMethod = getMethodBuilder()
            .setAction(fileURL)
            .toGetMethod();
        logger.info( httpMethod.getURI().toString() );
        if (!makeRedirectedRequest(httpMethod)) {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }

        httpMethod = getMethodBuilder().setActionFromFormWhereTagContains("startForm", true).toPostMethod();
        logger.info( httpMethod.getURI().toString() );
        if (!makeRedirectedRequest(httpMethod)) {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }

        checkProblems();

        int waitTime = PlugUtils.getWaitTimeBetween(getContentAsString(), "var timerRest = ", ";", TimeUnit.SECONDS);
        downloadTask.sleep(waitTime);

        httpMethod = getMethodBuilder().setActionFromFormWhereTagContains("stepForm", true).toPostMethod();
        logger.info( httpMethod.getURI().toString() );
        if (!makeRedirectedRequest(httpMethod)) {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }

        httpMethod = getMethodBuilder().setActionFromFormWhereTagContains("stepForm", true).toPostMethod();
        logger.info( httpMethod.getURI().toString() );
        if (!makeRedirectedRequest(httpMethod)) {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }

        while( getContentAsString().contains("name=\"captcha\"") ) {
            String captcha="http://www.unibytes.com/captcha.jpg";
            final BufferedImage captchaImage = getCaptchaSupport().getCaptchaImage(captcha);
            captcha = getCaptchaSupport().askForCaptcha(captchaImage);
            if( captcha == null || captcha.isEmpty() ) {
                throw new CaptchaEntryInputMismatchException("Can't be null");
            }

            logger.info( getMethodBuilder().setActionFromFormWhereTagContains("stepForm", true).getParameters().toString() );

            httpMethod = getMethodBuilder().setActionFromFormWhereTagContains("stepForm", true)
                .removeParameter("captcha")
                .setParameter("captcha", captcha)
                .toPostMethod();
            logger.info( httpMethod.getURI().toString() );
            if (!makeRedirectedRequest(httpMethod)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
        }
        httpMethod = getMethodBuilder().setActionFromTextBetween("<div style=\"width: 650px; margin: 40px auto; text-align: center; font-size: 2em;\"><a href=\"", "\">Download</a>").toGetMethod();
        logger.info( httpMethod.getURI().toString() );
        if (!tryDownloadAndSaveFile(httpMethod)) {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }

   }

}