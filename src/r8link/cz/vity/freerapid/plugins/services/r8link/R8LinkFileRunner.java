package cz.vity.freerapid.plugins.services.r8link;

import cz.vity.freerapid.plugins.exceptions.CaptchaEntryInputMismatchException;
import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.services.recaptcha.ReCaptcha;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadState;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.net.URL;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class R8LinkFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(R8LinkFileRunner.class.getName());

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL); //create GET request
        if (makeRedirectedRequest(method)) { //we make the main request
            checkProblems();//check problems
            Matcher match;
            do {
                final HttpMethod httpMethod = stepCaptcha(getMethodBuilder()
                        .setActionFromFormWhereTagContains("Continue Download", true)
                        .setAction(fileURL).setReferer(fileURL)
                ).toPostMethod();
                if (!makeRedirectedRequest(httpMethod)) {
                    checkProblems();
                    throw new ServiceConnectionProblemException();
                }
                match = PlugUtils.matcher("<meta.+?HTTP-EQUIV='Refresh'.+?URL=(.+?)'>", getContentAsString());
            } while (!match.find());
            this.httpFile.setNewURL(new URL(match.group(1))); //to setup new URL
            this.httpFile.setFileState(FileState.NOT_CHECKED);
            this.httpFile.setPluginID(""); //to run detection what plugin should be used for new URL, when file is in QUEUED state
            this.httpFile.setState(DownloadState.QUEUED);
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("<h3>NOT FOUND</h3>")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
    }

    private MethodBuilder stepCaptcha(MethodBuilder builder) throws Exception {
        String key;
        if (getContentAsString().contains("incorrect-captcha"))
            key = PlugUtils.getStringBetween(getContentAsString(), "recaptcha/api/noscript?k=", "&amp;");
        else
            key = PlugUtils.getStringBetween(getContentAsString(), "recaptcha/api/noscript?k=", "\"");
        final ReCaptcha reCaptcha = new ReCaptcha(key, client);
        final String captcha = getCaptchaSupport().getCaptcha(reCaptcha.getImageURL());
        if (captcha == null) {
            throw new CaptchaEntryInputMismatchException();
        }
        reCaptcha.setRecognized(captcha);
        return reCaptcha.modifyResponseMethod(builder);
    }
}