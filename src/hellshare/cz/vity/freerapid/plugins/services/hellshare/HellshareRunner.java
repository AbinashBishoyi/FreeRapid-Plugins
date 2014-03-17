package cz.vity.freerapid.plugins.services.hellshare;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadState;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;

import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * @author Ladislav Vitasek, Ludek Zika
 */
class HellshareRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(HellshareRunner.class.getName());

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
        logger.info("Starting download in TASK " + fileURL);

        final GetMethod getMethod = getGetMethod(fileURL);
        getMethod.setFollowRedirects(true);
        if (makeRequest(getMethod)) {
            checkNameAndSize(getContentAsString());
            Matcher matcher = getMatcherAgainstContent("([0-9.]+)%");
            if (matcher.find()) {
                if (matcher.group(1).equals("100"))
                    throw new YouHaveToWaitException("Na serveru jsou vyu�ity v�echny free download sloty", 30);
            }
            client.setReferer(fileURL);

            matcher = getMatcherAgainstContent("<input type=\"button\" value=\"FREE DOWNLOAD\" onclick=\"document[.]getElementById[(]\'FreeDownProgress\'[)][.]style.display=\'block\'; document[.]getElementById[(]\'FreeDownProgress\'[)][.]src=\'([^\']+)\'\" />");
            if(matcher.find())
            {
                String downURL = matcher.group(1);
                final GetMethod getmethod = getGetMethod(downURL);
                if (makeRequest(getmethod)) {
                    PostMethod method = stepCaptcha();
                    httpFile.setState(DownloadState.GETTING);
                    if (!tryDownloadAndSaveFile(method)) {
                        boolean finish = false;
                        while (!finish) {
                            method = stepCaptcha();
                            finish = tryDownloadAndSaveFile(method);
                        }
                    }
                } else {
                    checkProblems();
                    logger.info(getContentAsString());
                    throw new PluginImplementationException();
                }
            } else {
                checkProblems();
                logger.info(getContentAsString());
                throw new PluginImplementationException();
            }

        } else
            throw new PluginImplementationException();
    }

    private void checkNameAndSize(String content) throws Exception {
        if (getContentAsString().contains("FreeDownProgress")) {
            Matcher matcher = PlugUtils.matcher("<tr><th scope=\"row\" class=\"download-properties-label\">N.zev:</th><td><h2>([^<]+)</h2></td></tr>", content);
            if (matcher.find()) {
                String fn = matcher.group(matcher.groupCount());
                logger.info("File name " + fn);
                httpFile.setFileName(fn);
            }
            matcher = PlugUtils.matcher("<tr><th scope=\"row\" class=\"download-properties-label\">Velikost:</th><td>([^<]+ .B)</td></tr>", content);
            if (matcher.find()) {
                Long a = PlugUtils.getFileSizeFromString(matcher.group(1));
                logger.info("File size " + a);
                httpFile.setFileSize(a);
            }
            httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
        } else {
            checkProblems();
            logger.info(getContentAsString());
            throw new PluginImplementationException();
        }
    }

    private PostMethod stepCaptcha() throws Exception {
        if ("".equals(getContentAsString())) {
            throw new YouHaveToWaitException("Neur�it� omezen�", 120);
        }
        Matcher matcher;
        matcher = getMatcherAgainstContent("<img src=\"([^\"]*)\" border=\"0\" align=\"antispam\" align=\"middle\" id=\"captcha-img\" ");
        if (!matcher.find()) {
            checkProblems();
            throw new PluginImplementationException();
        }
        String img = PlugUtils.replaceEntities(matcher.group(1));
        boolean emptyCaptcha;
        String captcha;
        do {
            logger.info("Captcha image " + img);
            captcha = getCaptchaSupport().getCaptcha(img);
            if (captcha == null) {
                throw new CaptchaEntryInputMismatchException();
            }
            if (captcha.equals("")) {
                emptyCaptcha = true;
                img = img + "1";
            } else emptyCaptcha = false;
        } while (emptyCaptcha);
        matcher = getMatcherAgainstContent("<form method=\"post\" action=\"([^\"]*)\"");
        if (!matcher.find()) {
            throw new PluginImplementationException();
        }

        String finalURL = matcher.group(1);

        final PostMethod method = getPostMethod(finalURL);
        
        PlugUtils.addParameters(method, getContentAsString(), new String[]{"submit"});
        method.addParameter("captcha", captcha);
        return method;
    }

    private void checkProblems() throws ServiceConnectionProblemException, YouHaveToWaitException, URLNotAvailableAnymoreException {
        Matcher matcher;
        matcher = getMatcherAgainstContent("Soubor nenalezen");
        if (matcher.find()) {
            throw new URLNotAvailableAnymoreException(String.format("<b>Soubor nenalezen</b><br>"));
        }
        matcher = getMatcherAgainstContent("Na serveru jsou .* free download");
        if (matcher.find()) {
            throw new YouHaveToWaitException("Na serveru jsou vyu�ity v�echny free download sloty", 30);
        }


    }
}