package cz.vity.freerapid.plugins.services.filebitnet;

import cz.vity.freerapid.plugins.exceptions.CaptchaEntryInputMismatchException;
import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * @author RickCL
 */
class FilebitNetFilesRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(FilebitNetFilesRunner.class.getName());

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
        if (!makeRedirectedRequest(getMethod)) {
            throw new PluginImplementationException();
        }
        checkNameAndSize(getContentAsString());
        HttpMethod method = getMethodBuilder().setActionFromFormByIndex(1, true)
                .setAction(fileURL)
                .removeParameter("method_premium")
                .toPostMethod();
        if (!makeRedirectedRequest(method)) {
            throw new PluginImplementationException();
        }
        Matcher matcher = getMatcherAgainstContent("<span\\sid=\"countdown\"[^>]*>(\\d*)</span>");
        int sleepTime = 1;
        if (matcher.find()) {
            sleepTime += Integer.parseInt(matcher.group(1));
        }

        logger.info("Processing captcha");

        String contentAsString = getContentAsString();
        String captchaRule = "<span style=\\'position:absolute;padding\\-left:(\\d+)px;padding\\-top:\\d+px;\\'>(\\d+)</span>";
        Matcher captchaMatcher = PlugUtils.matcher(captchaRule, PlugUtils.unescapeHtml(contentAsString));
        StringBuffer strbuffCaptcha = new StringBuffer(4);
        SortedMap<Integer, String> captchaMap = new TreeMap<Integer, String>();

        while (captchaMatcher.find()) {
            captchaMap.put(Integer.parseInt(captchaMatcher.group(1)), captchaMatcher.group(2));
        }
        for (String value : captchaMap.values()) {
            strbuffCaptcha.append(value);
        }
        String strCaptcha = Integer.toString(Integer.parseInt(strbuffCaptcha.toString()));
        logger.info("Captcha : " + strCaptcha);

        downloadTask.sleep(sleepTime);

        method = getMethodBuilder().setActionFromFormByName("F1", true)
                .setAction(fileURL)
                .setParameter("code", strCaptcha)
                .removeParameter("btn_download")
                .toPostMethod();
        if (!makeRedirectedRequest(method)) {
            throw new PluginImplementationException();
        }

        method = getMethodBuilder().setActionFromAHrefWhereATagContains(httpFile.getFileName()).toGetMethod();
        if (!tryDownloadAndSaveFile(method)) {
            throw new CaptchaEntryInputMismatchException();
        }

    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        PlugUtils.checkName(httpFile, content, "name=\"fname\" value=\"", "\"");
        PlugUtils.checkFileSize(httpFile, content, "</font> (", ")</font>");
    }

}
