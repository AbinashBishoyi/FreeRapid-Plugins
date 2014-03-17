package cz.vity.freerapid.plugins.services.serienjunkies;

import cz.vity.freerapid.plugins.exceptions.CaptchaEntryInputMismatchException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;

import java.net.URI;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * @author benpicco
 */
class SerienjunkiesFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(SerienjunkiesFileRunner.class.getName());

    private final String HTTP_SERIENJUNKIES = "http://download.serienjunkies.org";

    @Override
    public void run() throws Exception {
        super.run();
        //checkURL(fileURL);
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {

            checkProblems();
            stepCaptcha(getContentAsString());

            while (getContentAsString().contains("IMG SRC=\"/secure/")) {
                logger.info("Captcha wrong - retry");
                try {   // We must not reload the page to quick
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                }
                makeRedirectedRequest(getMethod); // reload the page if captcha is wrong
                stepCaptcha(getContentAsString());
            }

            String content = getContentAsString();
            Matcher matcher = PlugUtils.matcher("ACTION\\=\"http://download.serienjunkies.org/([^\"]*)\"", content);

            List<URI> uriList = new LinkedList<URI>();

            while (matcher.find()) {

                String url = PlugUtils.getStringBetween(matcher.group(), "ACTION=\"", "\"");

                // We will get to a frameset that encapsulates the real page
                if (!makeRedirectedRequest(getGetMethod(url)))
                    logger.warning("Request Failed");

                // escape the frameset
                try {
                    url = PlugUtils.getStringBetween(getContentAsString(), "<FRAME SRC=\"", "\"");
                } catch (PluginImplementationException e) {
                    logger.warning(getContentAsString());
                }

                // We are getting redirected to the real site now
                HttpMethod method = this.getGetMethod(url);
                this.makeRedirectedRequest(method);

                url = method.getURI().toString();

                logger.info("New URL : " + url);

                uriList.add(new URI(url));

                // We have to wait again, otherwise we will be detected as a bot
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ie) {
                }
            }

            // We converti this download task in order to keep the list clean
            String url = uriList.remove(0).toString();
            getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(httpFile, uriList);
            this.httpFile.setNewURL(new URL(url));
            this.httpFile.setPluginID("");
            this.httpFile.setState(DownloadState.QUEUED);
        } else
            checkProblems();

    }

    private boolean stepCaptcha(String contentAsString) throws Exception {

        logger.info("Starting Captcha recognition");
        Matcher matcher = PlugUtils.matcher("IMG SRC=\"(\\/secure\\/([^\"]*))\"", contentAsString);
        if (matcher.find()) {
            String s = PlugUtils.replaceEntities(matcher.group(1));
            String captcha = getCaptchaSupport().getCaptcha(HTTP_SERIENJUNKIES + s);
            if (captcha == null) {
                throw new CaptchaEntryInputMismatchException();
            } else {

                String file_id = PlugUtils.getParameter("s", contentAsString);

                matcher = PlugUtils.matcher("FORM ACTION\\=\"([^\"]*)\" METHOD\\=\"post\"", contentAsString);
                if (!matcher.find()) {
                    throw new PluginImplementationException("Captcha form action was not found");
                }
                s = matcher.group(1);

                logger.info("Captcha entered - preparing request, file_id=" + file_id);

                final PostMethod postMethod = getPostMethod(HTTP_SERIENJUNKIES + s);
                postMethod.addParameter("s", file_id);
                postMethod.addParameter("c", captcha);
                postMethod.addParameter("action", "");

                if (makeRequest(postMethod)) {
                    logger.info("Request succseeded");
                    return true;
                } else {
                    logger.warning("Request to " + HTTP_SERIENJUNKIES + s + " failed!");
                    return false;
                }
            }
        } else {
            //logger.warning(contentAsString);
            throw new PluginImplementationException("Captcha picture was not found");
        }
    }

    private void checkProblems() throws ServiceConnectionProblemException {
        if (getContentAsString().contains("404 Not Found")) {
            throw new ServiceConnectionProblemException("Serienjunkies Error : Link was not found on this server!");
        }
    }
}
