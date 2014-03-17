package cz.vity.freerapid.plugins.services.vidxden;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingRunner;
import cz.vity.freerapid.plugins.services.xfilesharing.nameandsize.FileNameHandler;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.net.URL;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class which contains main code
 *
 * @author tong2shot
 */
class VidXDenFileRunner extends XFileSharingRunner {
    private final static Logger logger = Logger.getLogger(VidXDenFileRunner.class.getName());

    @Override
    protected List<FileNameHandler> getFileNameHandlers() {
        final List<FileNameHandler> fileNameHandlers = super.getFileNameHandlers();
        fileNameHandlers.add(0, new VidXDenFileNameHandler());
        return fileNameHandlers;
    }

    @Override
    protected void checkFileSize() throws ErrorDuringDownloadingException {

    }

    @Override
    public void runCheck() throws Exception {
        if (fileURL.matches("http://(?:www\\.)?divxden\\.com/.+")) {
            httpFile.setNewURL(new URL(fileURL.replaceFirst("divxden\\.com", "vidxden.com")));
        }
        super.runCheck();
    }

    @Override
    public void run() throws Exception {
        setLanguageCookie();
        login();
        logger.info("Starting download in TASK " + fileURL);
        GetMethod method = getGetMethod(fileURL);
        if (!makeRedirectedRequest(method)) {
            checkFileProblems();
            throw new ServiceConnectionProblemException();
        }
        checkFileProblems();
        checkNameAndSize();

        HttpMethod httpMethod = getMethodBuilder()
                .setReferer(fileURL)
                .setActionFromFormWhereTagContains("method_free", true)
                .setAction(fileURL)
                .removeParameter("method_premium")
                .toPostMethod();
        if (!makeRedirectedRequest(httpMethod)) {
            checkDownloadProblems();
            throw new ServiceConnectionProblemException();
        }
        checkDownloadProblems();

        final String waitTimeRule = "id=\"countdown_str\".*?<span id=\".*?\">.*?(\\d+).*?</span";
        final Matcher waitTimematcher = PlugUtils.matcher(waitTimeRule, getContentAsString());
        if (waitTimematcher.find()) {
            downloadTask.sleep(Integer.parseInt(waitTimematcher.group(1)) + 1);
        } else {
            downloadTask.sleep(10);
        }

        final Matcher jsMatcher = getMatcherAgainstContent("<script type='text/javascript'>\\s*(" + Pattern.quote("eval(function(p,a,c,k,e,d)") + ".+?)\\s*</script>");
        if (!jsMatcher.find()) {
            throw new PluginImplementationException("Content generator javascript not found");
        }
        final String jsString = jsMatcher.group(1).replaceFirst(Pattern.quote("eval(function(p,a,c,k,e,d)"), "function test(p,a,c,k,e,d)")
                .replaceFirst(Pattern.quote("return p}"), "return p};test")
                .replaceFirst(Pattern.quote(".split('|')))"), ".split('|'));");
        final ScriptEngineManager manager = new ScriptEngineManager();
        final ScriptEngine engine = manager.getEngineByName("javascript");
        String downloadURL;
        try {
            final String jsEval = (String) engine.eval(jsString);
            if (jsEval.contains("DivXBrowserPlugin")) {
                downloadURL = PlugUtils.getStringBetween(jsEval, "\"src\"value=\"", "\"");
            } else if (jsEval.contains("SWFObject")) {
                downloadURL = PlugUtils.getStringBetween(jsEval, "'file','", "'");
            } else {
                throw new PluginImplementationException("Download link not found");
            }
        } catch (ScriptException e) {
            throw new PluginImplementationException("JavaScript eval failed");
        }

        httpMethod = getMethodBuilder()
                .setReferer(fileURL)
                .setAction(downloadURL)
                .toGetMethod();

        setFileStreamContentTypes("text/plain");
        if (!tryDownloadAndSaveFile(httpMethod)) {
            checkDownloadProblems();
            throw new ServiceConnectionProblemException("Error starting download");
        }
    }


}