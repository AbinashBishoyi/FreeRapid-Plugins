package cz.vity.freerapid.plugins.services.vidxden;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingRunner;
import cz.vity.freerapid.plugins.services.xfilesharing.nameandsize.FileNameHandler;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.net.URL;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class which contains main code
 *
 * @author tong2shot
 */
class VidXDenFileRunner extends XFileSharingRunner {

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
    protected void correctURL() throws Exception {
        if (fileURL.matches("http://(?:www\\.)?divxden\\.com/.+")) {
            httpFile.setNewURL(new URL(fileURL.replaceFirst("divxden\\.com", "vidxden.com")));
        }
    }

    @Override
    protected List<String> getDownloadPageMarkers() {
        final List<String> downloadPageMarkers = super.getDownloadPageMarkers();
        downloadPageMarkers.add(0, "eval(function(p,a,c,k,e,d)");
        return downloadPageMarkers;
    }

    @Override
    //not the real download link, will be modified in doDownload()
    protected List<String> getDownloadLinkRegexes() {
        final List<String> downloadLinkRegexes = super.getDownloadLinkRegexes();
        downloadLinkRegexes.add(0, "(http://(?:www\\.)?vidxden\\.com)");
        return downloadLinkRegexes;
    }

    @Override
    protected void doDownload(HttpMethod method) throws Exception {
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
        method = getMethodBuilder()
                .setReferer(fileURL)
                .setAction(downloadURL)
                .toGetMethod();
        super.doDownload(method);
    }
}