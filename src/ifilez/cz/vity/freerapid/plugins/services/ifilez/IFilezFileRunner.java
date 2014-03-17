package cz.vity.freerapid.plugins.services.ifilez;

import java.awt.image.BufferedImage;
import java.net.URLDecoder;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.regex.Matcher;

import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.PostMethod;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;

/**
 * @author RickCL
 * @since 0.85
 */
class IFilezFileRunner extends AbstractRunner {

    private final static Logger logger = Logger.getLogger(IFilezFileRunner.class.getName());
    private static final String HTTP_IFILEZ = "http://i-filez.com";

    @Override
    public void runCheck() throws Exception {
        super.runCheck();

        HttpMethod httpMethod = getMethodBuilder()
            .setAction(fileURL)
            .toGetMethod();
        if (!makeRedirectedRequest(httpMethod)) {
            throw new ServiceConnectionProblemException();
        }
        
        checkNameAndSize();
    }
    
    @Override
    public void run() throws Exception {
        super.run();

        HttpMethod httpMethod = getMethodBuilder()
            .setAction(fileURL)
            .toGetMethod();
        if (!makeRedirectedRequest(httpMethod)) {
            throw new ServiceConnectionProblemException();
        }
        String content = getContentAsString();
        
        while( content.contains("verifycode") ) {
            final PostMethod postMethod = (PostMethod) getMethodBuilder().setActionFromFormByIndex(4, true).removeParameter("verifycode").toPostMethod();
            logger.info( httpMethod.getURI().toString() );
            
            Matcher matcher = getMatcherAgainstContent("src=\"(/includes/vvc.php[^\"]*)\"");
            if (matcher.find()) {
                String s = HTTP_IFILEZ + PlugUtils.replaceEntities(matcher.group(1));
                logger.info("Captcha - image " + s);
                String captcha;
                final BufferedImage captchaImage = getCaptchaSupport().getCaptchaImage(s);
                //logger.info("Read captcha:" + CaptchaReader.read(captchaImage));
                captcha = getCaptchaSupport().askForCaptcha(captchaImage);

                postMethod.addParameter("verifycode", captcha);

                if (!makeRedirectedRequest(postMethod)) {
                    logger.info(getContentAsString());
                    throw new PluginImplementationException();
                }
            }
            content = getContentAsString();
        }
        
        String url = PlugUtils.getStringBetween(content, "document.getElementById(\"wait_input\").value= unescape('", "');");
        url = URLDecoder.decode(url,"UTF-8");

        int waitTime = PlugUtils.getWaitTimeBetween(content, "var sec=", ";", TimeUnit.SECONDS);
        downloadTask.sleep(waitTime);
        
        httpMethod = getMethodBuilder()
            .setAction( url )
            .setReferer(fileURL)
            .toGetMethod();
        if (!tryDownloadAndSaveFile(httpMethod)) {
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize() throws ErrorDuringDownloadingException,Exception {
        String content = getContentAsString();
        
        String fileName = PlugUtils.getStringBetween(content, "<th>Nome do arquivo:</th>", "</td>").replaceAll("<[^>]*>", "").trim();
        String fileSize = PlugUtils.getStringBetween(content, "<th>Tamanho:</th>", "</td>").replaceAll("<[^>]*>", "").trim();
        final long lsize = PlugUtils.getFileSizeFromString( fileSize );

        httpFile.setFileName( URLDecoder.decode(fileName,"UTF-8") );
        httpFile.setFileSize( lsize );
    }

}
