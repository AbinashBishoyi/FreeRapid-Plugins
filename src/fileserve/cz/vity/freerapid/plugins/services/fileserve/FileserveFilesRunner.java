package cz.vity.freerapid.plugins.services.fileserve;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpMethod;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.exceptions.YouHaveToWaitException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;

/**
 * @author RickCL
 */
class FileserveFilesRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(FileserveFilesRunner.class.getName());
    private static final String URI_BASE = "http://fileserve.com";

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        fileURL = fileURL.replaceFirst("www.fileserve.com", "fileserve.com");
        final HttpMethod getMethod = getMethodBuilder().setAction(fileURL).toGetMethod();
        if (makeRedirectedRequest(getMethod)) {
            checkNameAndSize(getContentAsString());
        } else
            throw new PluginImplementationException();
    }

    @Override
    public void run() throws Exception {
        super.run();

        fileURL = fileURL.replaceFirst("www.fileserve.com", "fileserve.com");

        HttpMethod getMethod = getMethodBuilder().setAction(fileURL).toGetMethod();
        if (makeRedirectedRequest(getMethod)) {
            checkNameAndSize(getContentAsString());

            String fileKey = fileURL.substring(fileURL.lastIndexOf('/')+1);
            Pattern p = Pattern.compile("http://fileserve.com/[^/]*/([\\w]*)");
            Matcher m = p.matcher(fileURL);
            if( m.find() ) {
                fileKey = m.group(1);
            }

            Matcher matcher = getMatcherAgainstContent("reCAPTCHA_publickey='([\\d\\w-]*)'");
            if( !matcher.find() ) {
                checkProblems("Read CaptchaKey");
            }
            String recaptcha = "http://www.google.com/recaptcha/api/challenge?k="+matcher.group(1)+"&ajax=1&cachestop=0."+System.currentTimeMillis();

            String captcha;
            do {
                logger.info( "Captcha URL: " + recaptcha );
                getMethod = getGetMethod(recaptcha);
                if( !makeRedirectedRequest(getMethod)) {
                    checkProblems("Get Captcha Challenge");
                }

                //var RecaptchaState = {site:'6LdSvrkSAAAAAOIwNj-IY-Q-p90hQrLinRIpZBPi',challenge:'03AHJ_VuuJMcPAWa2rH-CzfLfoAmJfSgx3d0UyaU7r51_A-8-OiQmN_tMtpuZS8Cv-KaQ9wobLil_oxs2Ou399l3goQU8SfJQ3yM3r4ErIDf6XgEIFCHf67mB5Lt44qzuTlIqxOtOPi_Om4VJxZTiv6wooz7rzJcYgoQ',is_incorrect:false,programming_error:'',error_message:'',server:'http://www.google.com/recaptcha/api/',timeout:18000}; Recaptcha.challenge_callback();
                matcher = getMatcherAgainstContent("challenge\\s?:\\s?'([\\w-]*)',.*,server:'([^']*)'");
                if( !matcher.find() ) {
                    checkProblems("Read Challenge Key");
                }
                String recaptcha_challenge_field = matcher.group(1);
                String captchaImg = matcher.group(2) + "image?c=" + recaptcha_challenge_field;
                logger.info("Captcha URL: " + captchaImg);

                final BufferedImage captchaImage = getCaptchaSupport().getCaptchaImage(captchaImg);
                //logger.info("Read captcha:" + CaptchaReader.read(captchaImage));
                captcha = getCaptchaSupport().askForCaptcha(captchaImage);
                if( captcha == null || captcha.isEmpty() ) {
                    //throw new CaptchaEntryInputMismatchException("Can't be null");
                    continue;
                }

                HttpMethod postMethod = getMethodBuilder().setAction(URI_BASE + "/checkReCaptcha.php").setReferer(fileURL)
                    .setParameter("recaptcha_challenge_field", recaptcha_challenge_field )
                    .setParameter("recaptcha_response_field", captcha)
                    .setParameter("recaptcha_shortencode_field", fileKey)
                    .toPostMethod();
                if( !makeRedirectedRequest(postMethod) ) {
                    throw new PluginImplementationException();
                }
            } while( !getContentAsString().equalsIgnoreCase("success") );

            HttpMethod pMethod = getMethodBuilder().setAction(fileURL).setReferer(fileURL)
                .setParameter("downloadLink","wait")
                .toPostMethod();
            if( !makeRedirectedRequest(pMethod) ) {
                throw new PluginImplementationException();
            }
            try {
                downloadTask.sleep( Integer.parseInt(getContentAsString()) + 1 );
            } catch(NumberFormatException e) {
                throw new PluginImplementationException();
            }
            pMethod = getMethodBuilder().setAction(fileURL).setReferer(fileURL)
                .setParameter("downloadLink","show")
                .toPostMethod();
            if( !makeRedirectedRequest(pMethod) ) {
                throw new PluginImplementationException();
            }

            pMethod = getMethodBuilder().setAction(fileURL).setReferer(fileURL)
                .setParameter("download","normal")
                .toPostMethod();
            String downloadURL=null;
            if( !makeRequest(pMethod) ) {
                Header h = pMethod.getResponseHeader("Location");
                downloadURL = h.getValue();
                pMethod = getMethodBuilder().setAction(downloadURL).setReferer(fileURL)
                    .toGetMethod();
            } else {
                checkProblems("Expeted Redirect");
            }
            /**
                Not Work using default: Content-Type is not allow in method checkContentTypeStream

            [Content-Type: X-LIGHTTPD-send-file
             , Accept-Ranges: bytes
             , ETag: "317160400"
             , Last-Modified: Fri, 30 Apr 2010 16:29:06 GMT
             , Content-Length: 18348495
             , Date: Sat, 08 May 2010 16:58:05 GMT
             , Server: lighttpd/1.4.25
             ]
             */
            if (!tryDownloadAndSaveFile(pMethod)) {
                logger.severe(getContentAsString());
                checkProblems("Download File");
            }
        } else
            throw new PluginImplementationException();
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        PlugUtils.checkName(httpFile, content, "<h1>", "</h1>");
        PlugUtils.checkFileSize(httpFile, content, "<span><strong>", "</strong>");
    }

    private void checkProblems(String step) throws ServiceConnectionProblemException, YouHaveToWaitException, URLNotAvailableAnymoreException, PluginImplementationException {
        //String content = getContentAsString();
        Matcher matcher = getMatcherAgainstContent("You have to wait (\\d*) seconds to start another download");
        if( matcher.find() ) {
            httpFile.setState(DownloadState.QUEUED);
            throw new YouHaveToWaitException(matcher.group(), Integer.parseInt(matcher.group(1)) );
        }
        throw new PluginImplementationException("Step " + step);
    }

    /**
    Not Work using default: Content-Type is not allow in method checkContentTypeStream

    [Content-Type: X-LIGHTTPD-send-file
     , Accept-Ranges: bytes
     , ETag: "317160400"
     , Last-Modified: Fri, 30 Apr 2010 16:29:06 GMT
     , Content-Length: 18348495
     , Date: Sat, 08 May 2010 16:58:05 GMT
     , Server: lighttpd/1.4.25
    ]
     */
    protected boolean tryDownloadAndSaveFile(HttpMethod method) throws Exception {
        logger.info("Download link URI: " + method.getURI().toString());
        httpFile.setState(DownloadState.GETTING);
        logger.info("Making final request for file");
        try {
            final InputStream inputStream = client.makeRequestForFile(method);
            if (inputStream != null) {
                final Header contentLength = method.getResponseHeader("Content-Length");

                final Long contentResponseLength = Long.valueOf(contentLength.getValue());
                final Header contentRange = method.getResponseHeader("Content-Range");
                if (contentRange != null) {
                    final String val = contentRange.getValue();
                    final Matcher matcher = Pattern.compile("(\\d+)-\\d+/(\\d+)").matcher(val);
                    if (matcher.find()) {
                        httpFile.getProperties().put("startPosition", Long.valueOf(matcher.group(1)));
                        httpFile.setFileSize(Long.valueOf(matcher.group(2)));
                    } else
                        httpFile.getProperties().put("startPosition", 0L);
                    httpFile.setResumeSupported(true);
                } else {
                    if (!client.getHTTPClient().getParams().isParameterTrue("ignoreAcceptRanges")) {
                        final Header acceptRangesHeader = method.getResponseHeader("Accept-Ranges");
                        if (httpFile.isResumeSupported())
                            httpFile.setResumeSupported(acceptRangesHeader != null && "bytes".equals(acceptRangesHeader.getValue()));
                    }
                    httpFile.setFileSize(contentResponseLength);
                }
                httpFile.getProperties().put("supposeToDownload", contentResponseLength);

                logger.info("Saving to file");
                downloadTask.saveToFile(inputStream);
                return true;
            } else {
                logger.info("Saving file failed");
                return false;
            }
        } finally {
            method.abort();
            method.releaseConnection();
        }
    }

}
