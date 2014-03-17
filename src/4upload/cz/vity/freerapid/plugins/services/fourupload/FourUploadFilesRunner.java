package cz.vity.freerapid.plugins.services.fourupload;

import java.awt.image.BufferedImage;
import java.io.FileOutputStream;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;

import cz.vity.freerapid.plugins.exceptions.BuildMethodException;
import cz.vity.freerapid.plugins.exceptions.CaptchaEntryInputMismatchException;
import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.exceptions.YouHaveToWaitException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;

/**
 * @author RickCL
 */
class FourUploadFilesRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(FourUploadFilesRunner.class.getName());
    private static final String URI_BASE = "http://4upload.ru";


    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {
            checkNameAndSize(getContentAsString());
        } else
            throw new PluginImplementationException();
    }

    @Override
    public void run() throws Exception {
        super.run();

        GetMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {
            checkNameAndSize(getContentAsString());

            Matcher matcher = getMatcherAgainstContent("<a\\shref=\"([\\w/\\.]*)\"\\sclass=\"button-download\">");
            if( !matcher.find() ) {
                checkProblems(20);
            }

            getMethod = getGetMethod(URI_BASE + matcher.group(1));
            if( !makeRequest(getMethod)) {
                checkProblems(21);
            }

            /** It's not necessary wait this
            matcher = getMatcherAgainstContent("<p\\sid=\"counter\">(\\d*)</p>");
            if( !matcher.find() ) {
                checkProblems();
            }
            String t = matcher.group(1);
            int seconds = new Integer(t);
            logger.info("wait - " + seconds);
            /**/

            matcher = getMatcherAgainstContent("Ajax.Request\\('([/\\w]*)'\\);");
            if( !matcher.find() ) {
                checkProblems(22);
            }

            String postAction = getMethodBuilder().setActionFromFormByName("downloadForm", false).getAction();
            logger.info( "Getting POST action: " +  postAction );

            String recaptcha = getActionFromScriptSrcWhereTagContains("recaptcha");
            logger.info( "Captcha URL: " + recaptcha );
            getMethod = getGetMethod(recaptcha);
            if( !makeRedirectedRequest(getMethod)) {
                checkProblems(23);
            }
            //System.out.println( getContentAsString() );
            matcher = getMatcherAgainstContent("challenge\\s?:\\s?'([\\w-]*)'");
            if( !matcher.find() ) {
                checkProblems(24);
            }
            String recaptcha_challenge_field = matcher.group(1);
            String captcha="http://www.google.com/recaptcha/api/image?c=" + recaptcha_challenge_field;

            logger.info("Captcha URL: " + captcha);

            //URL=http://api.recaptcha.net/image?c=03AHJ_VutklA4_ONVFIp9E8Ib8Oo30wpR4txlnv6R1kJJhfBQ65YRDj4E1w5hf2Q39b4FZB8wwqVCy1S-Ma_KhsgzMztLm9sNrBblLXrL7Ltl19RHat_0QiP_HshdPVECmCHTRQ8JBixFm3GosUWbKeGbTeue4bk729Q
            //URL=http://www.google.com/recaptcha/api/image?c=03AHJ_VutklA4_ONVFIp9E8Ib8Oo30wpR4txlnv6R1kJJhfBQ65YRDj4E1w5hf2Q39b4FZB8wwqVCy1S-Ma_KhsgzMztLm9sNrBblLXrL7Ltl19RHat_0QiP_HshdPVECmCHTRQ8JBixFm3GosUWbKeGbTeue4bk729Q
            final BufferedImage captchaImage = getCaptchaSupport().getCaptchaImage(captcha);
            //logger.info("Read captcha:" + CaptchaReader.read(captchaImage));
            captcha = getCaptchaSupport().askForCaptcha(captchaImage);
            if( captcha == null || captcha.isEmpty() ) {
                throw new CaptchaEntryInputMismatchException("Can't be null");
            }

            // It's not necessary wait this
            //downloadTask.sleep(seconds + 1);

            //final HttpMethod httpMethod = getMethodBuilder().setAction(URI_BASE + matcher.group(1)).setReferer(fileURL).toPostMethod();
            PostMethod postMethod = getPostMethod( URI_BASE + postAction );
            postMethod.setParameter("recaptcha_challenge_field", recaptcha_challenge_field );
            postMethod.setParameter("recaptcha_response_field", captcha);
            postMethod.setParameter("download_start", "true");
            //postMethod.setFollowRedirects(true);

            if (!tryDownloadAndSaveFile(postMethod)) {
                checkProblems(25);//if downloading failed
                //logger.warning(getContentAsString());//log the info
                throw new CaptchaEntryInputMismatchException();
            }

        } else
            throw new PluginImplementationException();
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        logger.fine(content);
        if (!(content == null)) {
            if ( content.contains("<div class=\"fileName\">") && content.contains("<div class=\"fileSize\">") ) {
                content = content.replaceAll("\n", "").replaceAll("\r", "");
                String name = PlugUtils.getStringBetween(content, "<div class=\"fileName\">", "</div>");
                name = name.replaceAll("</?\\w+\\s*[^>]*>", "").trim();
                final String size = PlugUtils.getStringBetween(content, "<div class=\"fileSize\">", "</div>");
                final long lsize = PlugUtils.getFileSizeFromString( size.replaceAll("</?\\w+\\s*[^>]*>", "").trim() );

                httpFile.setFileName( name );
                httpFile.setFileSize(lsize);

                httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
                return;
            } else {
                checkProblems(1);
            }
        }
    }

    private void checkProblems(int step) throws ServiceConnectionProblemException, YouHaveToWaitException, URLNotAvailableAnymoreException, PluginImplementationException {
        String content = getContentAsString();
        if (content.contains("\u0421 \u0432\u0430\u0448\u0435\u0433\u043e IP \u0430\u0434\u0440\u0435\u0441\u0430")) {
            throw new ServiceConnectionProblemException(String.format("<b>Your IP is already downloading a file from our system.</b><br>You cannot download more than one file in parallel."));
        }
        if (content.contains("\u041e\u0448\u0438\u0431\u043a\u0430 404")) {
            throw new URLNotAvailableAnymoreException(String.format("The address is incorrectly collected or this file no longer exists"));
        }
        /** For debug
        try {
            FileOutputStream f = new FileOutputStream("error-content.html");
            f.write( content.getBytes() );
            f.close();
        } catch(Exception e) {}
        /**/
        throw new PluginImplementationException("Step " + step);
    }

    public String getActionFromScriptSrcWhereTagContains(final String text) throws BuildMethodException {
        Pattern scriptPattern = Pattern.compile("(<script(?:.*?)src\\s?=\\s?(?:\"|')(.+?)(?:\"|')(?:.*?)>)", Pattern.DOTALL | Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
        final Matcher matcher = scriptPattern.matcher( getContentAsString() );
        int start = 0;
        final String lower = text.toLowerCase();
        while (matcher.find(start)) {
            final String content = matcher.group(1);
            if (content.toLowerCase().contains(lower)) {
                return matcher.group(2);
            }
            start = matcher.end();
        }
        throw new BuildMethodException("Tag <script> with containing '" + text + "' was not found!");
    }


}
