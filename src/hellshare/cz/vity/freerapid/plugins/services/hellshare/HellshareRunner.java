package cz.vity.freerapid.plugins.services.hellshare;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadState;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.io.*;

/**
 * @author Ladislav Vitasek, Ludek Zika
 */
class HellshareRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(HellshareRunner.class.getName());
    private final static Map<String, PostMethod> methodsMap = new HashMap<String, PostMethod>();
    private final static int WAIT_TIME = 30;

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {
            checkNameAndSize(getContentAsString());
            checkCaptcha();
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
                    throw new YouHaveToWaitException("Na serveru jsou využity všechny free download sloty", WAIT_TIME);
            }
            client.setReferer(fileURL);

            if (checkInQueue())
                return;

            matcher = getMatcherAgainstContent("<input type=\"button\" value=\"FREE DOWNLOAD\" onclick=\"document[.]getElementById[(]\'FreeDownProgress\'[)][.]style.display=\'block\'; document[.]getElementById[(]\'FreeDownProgress\'[)][.]src=\'([^\']+)\'\" />");
            if(matcher.find())
            {
                String downURL = matcher.group(1);
                final GetMethod getmethod = getGetMethod(downURL);
                if (makeRequest(getmethod)) {
                    PostMethod method = stepCaptcha();
                    httpFile.setState(DownloadState.GETTING);
                    if (!tryDownloadAndSaveFile(method)) {
                        matcher = getMatcherAgainstContent("Omlouv.me se, ale tento soubor nen. pro danou zemi v tuto chv.li dostupn.. Pracujeme na odstran.n. probl.mu.|We are sorry, but this file is not accessible at this time for this country. We are currenlty working on fixing this problem");
                        if(matcher.find())
                            throw new PluginImplementationException("This file is not available for this country for this moment");
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
            Matcher matcher = PlugUtils.matcher("<table class=\"download-properties\">[^<]+<tr><th scope=\"row\" class=\"download-properties-label\">[^<]+</th><td><h2>([^<]+)</h2></td></tr>[^<]+<tr><th scope=\"row\" class=\"download-properties-label\">[^<]+</th><td>([^<]+ .B)</td></tr>", content);
            if (matcher.find()) {
                String fn = matcher.group(1);
                logger.info("File name " + fn);
                httpFile.setFileName(fn);
                Long a = PlugUtils.getFileSizeFromString(matcher.group(2));
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

    private void checkCaptcha() throws Exception {
        Matcher matcher = getMatcherAgainstContent("([0-9.]+)%");
        if (matcher.find()) {
            if (matcher.group(1).equals("100"))
                return;
        }
        client.setReferer(fileURL);

        matcher = getMatcherAgainstContent("<input type=\"button\" value=\"FREE DOWNLOAD\" onclick=\"document[.]getElementById[(]\'FreeDownProgress\'[)][.]style.display=\'block\'; document[.]getElementById[(]\'FreeDownProgress\'[)][.]src=\'([^\']+)\'\" />");
        if(matcher.find())
        {
            String downURL = matcher.group(1);
            final GetMethod getmethod = getGetMethod(downURL);
            if (makeRequest(getmethod)) {
                if ("".equals(getContentAsString()))
                    return;
                matcher = getMatcherAgainstContent("<img src=\"([^\"]*)\" border=\"0\" align=\"antispam\" align=\"middle\" id=\"captcha-img\" ");
                if (!matcher.find())
                    return;
                String img = PlugUtils.replaceEntities(matcher.group(1));
                boolean emptyCaptcha;
                String captcha;
                do {
                    logger.info("Captcha image " + img);
                    captcha = getCaptchaSupport().getCaptcha(img);
                    if (captcha == null)
                        return;
                    if (captcha.equals("")) {
                        emptyCaptcha = true;
                        img = img + "1";
                    } else emptyCaptcha = false;
                } while (emptyCaptcha);
                matcher = getMatcherAgainstContent("<form method=\"post\" action=\"([^\"]*)\"");
                if (!matcher.find())
                    return;

                String finalURL = matcher.group(1);

                final PostMethod method = getPostMethod(finalURL);

                PlugUtils.addParameters(method, getContentAsString(), new String[]{"submit"});
                method.addParameter("captcha", captcha);

                methodsMap.put(fileURL, method);
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
    }

    private boolean checkInQueue() throws Exception {
        if (!methodsMap.containsKey(fileURL))
            return false;

        final PostMethod met = methodsMap.get(fileURL);
        final PostMethod method = getPostMethod(met.getURI().toString());
        method.addParameters(met.getParameters());

        httpFile.setState(DownloadState.GETTING);
        if(tryDownloadAndSaveFile(method))
            methodsMap.remove(fileURL);
        else {
            checkProblems();
            Matcher matcher = getMatcherAgainstContent("Omlouv.me se, ale tento soubor nen. pro danou zemi v tuto chv.li dostupn.. Pracujeme na odstran.n. probl.mu.|We are sorry, but this file is not accessible at this time for this country. We are currenlty working on fixing this problem");
            if(matcher.find())
                throw new PluginImplementationException("This file is not available for this country for this moment");

            matcher = getMatcherAgainstContent("<img src=\"([^\"]*)\" border=\"0\" align=\"antispam\" align=\"middle\" id=\"captcha-img\" ");
            if (matcher.find())
                stepCaptcha();

            throw new YouHaveToWaitException("Na serveru jsou využity všechny free download sloty", WAIT_TIME);
        }

        return true;
    }

    private PostMethod stepCaptcha() throws Exception {
        if ("".equals(getContentAsString())) {
            throw new YouHaveToWaitException("Neurèité omezení", 4 * WAIT_TIME);
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

        methodsMap.put(fileURL, method);
        return method;
    }

    private void checkProblems() throws ServiceConnectionProblemException, YouHaveToWaitException, URLNotAvailableAnymoreException {
        Matcher matcher;
        matcher = getMatcherAgainstContent("Soubor nenalezen|S.bor nen.jden.|A f.jl nem volt megtal.lhat.");
        if (matcher.find()) {
            throw new URLNotAvailableAnymoreException(String.format("<b>Soubor nenalezen</b><br>"));
        }
        matcher = getMatcherAgainstContent("Na serveru jsou .* free download|Na serveri s. vyu.it. v.etky free download sloty|A szerveren az .sszes free download slot ki van haszn.lva");
        if (matcher.find()) {
            throw new YouHaveToWaitException("Na serveru jsou využity všechny free download sloty", WAIT_TIME);
        }
    }
}