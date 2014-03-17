package cz.vity.freerapid.plugins.services.linkblur;

import java.net.URL;
import java.util.logging.Logger;
import java.util.regex.Matcher;

import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import cz.vity.freerapid.plugins.exceptions.CaptchaEntryInputMismatchException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.services.recaptcha.ReCaptcha;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadState;
import cz.vity.freerapid.plugins.webclient.hoster.CaptchaSupport;

/**
 * @author RickCL
 */
class LinkblurRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(LinkblurRunner.class.getName());
    public boolean result;
    public boolean fin;
    public String capt;

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting run task " + fileURL);
        final GetMethod method = getGetMethod(fileURL);
        logger.info(fileURL);
        makeRequest(method);
        String action = null;
        if( fileURL.contains("prot/") ) {
            HttpMethod hMethod = getMethodBuilder().setActionFromFormByIndex(1, true)
                .setBaseURL("http://linkblur.com")
                .setReferer(fileURL)
                .setParameter("submit","continue")
                .toHttpMethod();
            makeRequest( hMethod );
            action = getMethodBuilder().setActionFromIFrameSrcWhereTagContains("protected").getAction();
        } else if( fileURL.contains("captcha/") ) {
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

            HttpMethod hMethod = r.modifyResponseMethod(getMethodBuilder(content).setReferer(fileURL).setActionFromFormByIndex(1, true).setBaseURL("http://linkblur.com").setAction(fileURL)).toPostMethod();
            makeRequest( hMethod );
            action = getMethodBuilder().setActionFromIFrameSrcWhereTagContains("protected").getAction();
        } else {
            action = getMethodBuilder().setActionFromIFrameSrcWhereTagContains("miniclip").getAction();
        }
        if( action != null ) {
            httpFile.setNewURL(new URL(action));
            httpFile.setPluginID("");
            httpFile.setState(DownloadState.QUEUED);
        } else {
            throw new ServiceConnectionProblemException();
        }
    }

}
