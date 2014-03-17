package cz.vity.freerapid.plugins.services.filebitnet;

import java.util.TreeMap;
import java.util.logging.Logger;
import java.util.regex.Matcher;

import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import cz.vity.freerapid.plugins.exceptions.CaptchaEntryInputMismatchException;
import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;

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
        if( !makeRedirectedRequest(getMethod) ) {
            throw new PluginImplementationException();
        }
        checkNameAndSize(getContentAsString());
        HttpMethod method = getMethodBuilder().setActionFromFormByIndex(1, true)
            .setAction(fileURL)
            .removeParameter("method_premium")
            .toPostMethod();
        if( !makeRedirectedRequest(method) ) {
            throw new PluginImplementationException();
        }
        Matcher matcher = getMatcherAgainstContent("<span\\sid=\"countdown\"[^>]*>(\\d*)</span>");
        int sleepTime=1;
        if( matcher.find() ) {
            sleepTime += Integer.parseInt( matcher.group(1) );
        }
        Matcher matcherCaptcha = getMatcherAgainstContent("left:(\\d+)px[^&]*&#(\\d{2,3})");
        String captcha = "";
        TreeMap<Integer, String> t = new TreeMap<Integer, String>();
        while( matcherCaptcha.find() ) {
            t.put(Integer.parseInt(matcherCaptcha.group(1)), ""+( (char) Integer.parseInt(matcherCaptcha.group(2))) );
        }
        for(String s : t.values() )
            captcha += s;
        logger.info("Try Captcha TreeMap : " + t);
        logger.info("Try Captcha : " + captcha);
        downloadTask.sleep(sleepTime);

        method = getMethodBuilder().setActionFromFormByName("F1", true)
            .setAction(fileURL)
            .setParameter("code", captcha)
            .removeParameter("btn_download")
            .toPostMethod();
        if( !makeRedirectedRequest(method) ) {
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
