package cz.vity.freerapid.plugins.services.turbobit;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.PostMethod;

import java.awt.image.BufferedImage;
import java.util.logging.Logger;
import java.util.regex.Matcher;


/**
 * Class which contains main code
 *
 * @author Arthur Gunawan RickCL
 */
class TurboBitFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(TurboBitFileRunner.class.getName());
    private final static String mRef = "http://www.turbobit.net/en";

    @Override
    public void runCheck() throws Exception { //this method validates file
        super.runCheck();
        client.setReferer(mRef);

        HttpMethod httpMethod = getMethodBuilder().setReferer(mRef).setAction(fileURL).toGetMethod();
        client.setReferer(mRef);
        if (makeRequest(httpMethod)) {
            checkProblems();
            checkNameAndSize(getContentAsString());//ok let's extract file name and size from the page
        } else
            throw new PluginImplementationException();
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        logger.info(content);
        if (!(content == null)) {
            if (content.contains("&nbsp;</span><b>") && content.contains("</b></h1>") && content.contains(":</b>") && content.contains("</div>")) {
                PlugUtils.checkName(httpFile, content, "&nbsp;</span><b>", "</b></h1>");
                PlugUtils.checkFileSize(httpFile, content, "\u0420\u0430\u0437\u043c\u0435\u0440 \u0444\u0430\u0439\u043b\u0430:</b>", "</div>");
                httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
            }
        }
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        checkNameAndSize(getContentAsString());//ok let's extract file name and size from the page
        HttpMethod httpMethod = getMethodBuilder().setReferer(mRef).setAction(parseURL(fileURL)).toGetMethod();

        if (!tryDownloadAndSaveFile(httpMethod)) {
            checkProblems();//if downloading failed
            logger.warning(getContentAsString());//log the info
            throw new PluginImplementationException();//some unknown problem
        }
    }

    private void checkFileProblems() throws ErrorDuringDownloadingException {
        Matcher err404Matcher = PlugUtils.matcher("<div class=\"text-404\">(.*?)</div", getContentAsString());
        if (err404Matcher.find()) {
            if (err404Matcher.group(1).contains("\u00d0\u2014\u00d0\u00b0\u00d0\u00bf\u00d1\u20ac\u00d0\u00be\u00d1?\u00d0\u00b5\u00d0\u00bd\u00d0\u00bd\u00d1\u2039\u00d0\u00b9 \u00d0\u00b4\u00d0\u00be\u00d0\u00ba\u00d1?\u00d0\u00bc\u00d0\u00b5\u00d0\u00bd\u00d1\u201a \u00d0\u00bd\u00d0\u00b5 \u00d0\u00bd\u00d0\u00b0\u00d0\u00b9\u00d0\u00b4\u00d0\u00b5\u00d0\u00bd"))
                throw new URLNotAvailableAnymoreException(err404Matcher.group(1));
        }
        if (getContentAsString().contains("\u0424\u0430\u0439\u043B \u043D\u0435 \u043D\u0430\u0439\u0434\u0435\u043D."))
            throw new URLNotAvailableAnymoreException();
    }

    private void checkDownloadProblems() throws ErrorDuringDownloadingException {
        try {
            Matcher waitMatcher = PlugUtils.matcher("\u00d0\u0178\u00d0\u00be\u00d0\u00bf\u00d1\u20ac\u00d0\u00be\u00d0\u00b1\u00d1?\u00d0\u00b9\u00d1\u201a\u00d0\u00b5\\s+\u00d0\u00bf\u00d0\u00be\u00d0\u00b2\u00d1\u201a\u00d0\u00be\u00d1\u20ac\u00d0\u00b8\u00d1\u201a\u00d1\u0152.*<span id='timeout'>([^>]*)<", getContentAsString());
            if (waitMatcher.find()) {
                throw new YouHaveToWaitException("You have to wait", Integer.valueOf(waitMatcher.group(1)));
            }
            Matcher errMatcher = PlugUtils.matcher("<div[^>]*class='error'[^>]*>([^<]*)<", getContentAsString());
            if (errMatcher.find() && !errMatcher.group(1).isEmpty()) {
                if (errMatcher.group(1).contains("\u00d0?\u00d0\u00b5\u00d0\u00b2\u00d0\u00b5\u00d1\u20ac\u00d0\u00bd\u00d1\u2039\u00d0\u00b9 \u00d0\u00be\u00d1\u201a\u00d0\u00b2\u00d0\u00b5\u00d1\u201a")
                 || errMatcher.group(1).contains("\u041d\u0435\u0432\u0435\u0440\u043d\u044b\u0439 \u043e\u0442\u0432\u0435\u0442!") )
                    throw new CaptchaEntryInputMismatchException();
                throw new PluginImplementationException();
            }
            if (getContentAsString().contains("\u00d0\u00a1\u00d1?\u00d1\u2039\u00d0\u00bb\u00d0\u00ba\u00d0\u00b0 \u00d0\u00bf\u00d1\u20ac\u00d0\u00be\u00d1?\u00d1\u20ac\u00d0\u00be\u00d1\u2021\u00d0\u00b5\u00d0\u00bd\u00d0\u00b0")) // it's unlikely we get this...
                throw new YouHaveToWaitException("Trying again...", 10);
        } catch (NumberFormatException e) {
            throw new PluginImplementationException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        checkFileProblems();
        checkDownloadProblems();
    }

    private String parseURL(String myURL) throws Exception {

        Matcher matcher = PlugUtils.matcher("http://(www\\.)?turbobit\\.net/([a-z0-9]+)\\.html", myURL);
        if (!matcher.find()) {
            checkProblems();
            throw new PluginImplementationException();
        }

        String urlCode = matcher.group(2);
        String myAction = "http://www.turbobit.net/download/free/" + urlCode + "/";
        HttpMethod httpMethod = getMethodBuilder().setReferer(mRef).setAction(myAction).toGetMethod();
        client.setReferer(mRef);
        String getRef = client.getReferer();
        logger.info("Get Referer : " + getRef);
        if (!makeRedirectedRequest(httpMethod)) {
            checkProblems();
            throw new PluginImplementationException();
        }
        //<img alt="Captcha" src="http://turbobit.net/captcha/securimg_1/1264152974"  />
        matcher = getMatcherAgainstContent("<img alt=\"Captcha\"[^>]+src\\s*=\\s*['\"]([^'\"]+)['\"][^>]*>" );
        if (matcher.find()) {
            String s = PlugUtils.replaceEntities(matcher.group(1));
            logger.info("Captcha - image " + s);
            String captcha;
            final BufferedImage captchaImage = getCaptchaSupport().getCaptchaImage(s);
            //logger.info("Read captcha:" + CaptchaReader.read(captchaImage));
            captcha = getCaptchaSupport().askForCaptcha(captchaImage);

            client.setReferer(mRef);
            final PostMethod postMethod = getPostMethod( myAction );
            //PlugUtils.addParameters(postMethod, getContentAsString(), new String[]{"icid"});

            postMethod.addParameter("captcha_response", captcha);

            if (!makeRequest(postMethod)) {
                logger.info(getContentAsString());
                throw new PluginImplementationException();
            }
        }

        matcher = getMatcherAgainstContent("limit: ([0-9]+),");
        if (!matcher.find()) {
            checkProblems();
            throw new ServiceConnectionProblemException("Problem with a connection to service.\nCannot find requested page content");
        }
        String t = matcher.group(1);
        int seconds = new Integer(t);
        logger.info("wait - " + t);

        logger.info("Download URL: " + t);
        downloadTask.sleep(seconds + 1);

        myAction = "http://www.turbobit.net/download/timeout/" + urlCode + "/";
        httpMethod = getMethodBuilder().setReferer(mRef).setAction(myAction).toGetMethod();
        client.setReferer(mRef);
        getRef = client.getReferer();
        logger.info("Get Referer : " + getRef);

        // <a href='/download/redirect/c8ca1469cb893d8acbb17305bf01035b/045888zyivux' onclick='mg_switch(this,event);'>
        //matcher = getMatcherAgainstContent("<a href\\s*=\\s*['\"]([^'\"]+)['\"][^>]*['\"]" );

        if (!makeRedirectedRequest(httpMethod)) {
            checkProblems();
            throw new PluginImplementationException();
        }

        String contentAsString = getContentAsString();
        if (!contentAsString.contains("download/redirect/")) {
            checkProblems();
            throw new PluginImplementationException();
        }/**/

        String finURL = "http://turbobit.net/download/redirect/" + PlugUtils.getStringBetween(contentAsString, "/download/redirect/", "'");
        logger.info("Final URL: " + finURL);

        return finURL;
    }

}