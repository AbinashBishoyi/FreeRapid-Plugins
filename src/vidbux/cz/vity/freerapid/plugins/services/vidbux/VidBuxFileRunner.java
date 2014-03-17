package cz.vity.freerapid.plugins.services.vidbux;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingRunner;
import cz.vity.freerapid.plugins.services.xfilesharing.nameandsize.FileNameHandler;
import cz.vity.freerapid.plugins.services.xfilesharing.nameandsize.FileSizeHandler;
import cz.vity.freerapid.plugins.services.xfilesharing.nameandsize.FileSizeHandlerNoSize;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class VidBuxFileRunner extends XFileSharingRunner {
    @Override
    protected List<FileNameHandler> getFileNameHandlers() {
        final List<FileNameHandler> fileNameHandlers = super.getFileNameHandlers();
        fileNameHandlers.add(0, new VidBuxFileNameHandler());
        return fileNameHandlers;
    }

    @Override
    protected List<FileSizeHandler> getFileSizeHandlers() {
        final List<FileSizeHandler> fileSizeHandlers = super.getFileSizeHandlers();
        fileSizeHandlers.add(new FileSizeHandlerNoSize());
        return fileSizeHandlers;
    }

    @Override
    //similar with vidxden
    protected String getDownloadLinkFromRegexes() throws ErrorDuringDownloadingException {
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
        return downloadURL;
    }

    @Override
    protected List<String> getDownloadPageMarkers() {
        final List<String> downloadPageMarkers = super.getDownloadPageMarkers();
        downloadPageMarkers.add(0, "<h3>Now playing:");
        return downloadPageMarkers;
    }

}