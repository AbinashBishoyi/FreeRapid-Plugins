package cz.vity.freerapid.plugins.services.xfileplayer;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingRunner;
import cz.vity.freerapid.plugins.services.xfilesharing.nameandsize.FileSizeHandler;
import cz.vity.freerapid.plugins.services.xfilesharing.nameandsize.FileSizeHandlerNoSize;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class which contains main code
 *
 * @author birchie
 */
public abstract class XFilePlayerRunner extends XFileSharingRunner {

    @Override
    protected List<FileSizeHandler> getFileSizeHandlers() {
        final List<FileSizeHandler> fileSizeHandlers = super.getFileSizeHandlers();
        fileSizeHandlers.add(new FileSizeHandlerNoSize());
        return fileSizeHandlers;
    }

    @Override
    protected MethodBuilder getXFSMethodBuilder() throws Exception {
        if (checkDownloadPageMarker())
            return getMethodBuilder().setAction(fileURL);
        return super.getXFSMethodBuilder();
    }

    @Override
    protected MethodBuilder getXFSMethodBuilder(final String content) throws Exception {
        try {
            return super.getXFSMethodBuilder(content);
        } catch (Exception e) {
            return getXFSMethodBuilder(content, "download1");
        }
    }

    @Override
    protected List<String> getDownloadPageMarkers() {
        final List<String> downloadPageMarkers = super.getDownloadPageMarkers();
        downloadPageMarkers.add("jwplayer('vplayer').setup");
        downloadPageMarkers.add("jwplayer(\"vplayer\").setup");
        downloadPageMarkers.add("jwplayer('container').setup");
        downloadPageMarkers.add("jwplayer(\"container\").setup");
        downloadPageMarkers.add("jwplayer('flvplayer').setup");
        downloadPageMarkers.add("jwplayer(\"flvplayer\").setup");
        downloadPageMarkers.add("jwplayer('mediaplayer').setup");
        downloadPageMarkers.add("jwplayer(\"mediaplayer\").setup");
        downloadPageMarkers.add("jwplayer('jwPlayerContainer').setup");
        downloadPageMarkers.add("jwplayer(\"jwPlayerContainer\").setup");
        downloadPageMarkers.add("eval(function(p,a,c,k,e,d)");
        return downloadPageMarkers;
    }

    @Override
    protected List<String> getDownloadLinkRegexes() {
        final List<String> downloadLinkRegexes = super.getDownloadLinkRegexes();
        downloadLinkRegexes.add(0, "['\"]?file['\"]?\\s*?:\\s*?['\"]([^'\"]+?)['\"],");
        downloadLinkRegexes.add(0, "['\"]?file['\"]?\\s*?:\\s*?['\"](http[^'\"]+?)['\"],");
        return downloadLinkRegexes;
    }

    @Override
    protected String getDownloadLinkFromRegexes() throws ErrorDuringDownloadingException {
        try {
            return super.getDownloadLinkFromRegexes();
        } catch (Exception ee) {
            if (getContentAsString().contains("eval(function(p,a,c,k,e,d)")) {
                final String jsText = unPackJavaScript();
                Logger.getLogger(XFilePlayerRunner.class.getName()).info("Text from JavaScript: " + jsText);
                for (final String downloadLinkRegex : getDownloadLinkRegexes()) {
                    final Matcher matcher = PlugUtils.matcher(downloadLinkRegex, jsText);
                    if (matcher.find()) {
                        return matcher.group(1);
                    }
                }
            }
            throw new PluginImplementationException("Download link not found");
        }
    }

    private String unPackJavaScript() throws ErrorDuringDownloadingException {
        final Matcher jsMatcher = getMatcherAgainstContent("<script type='text/javascript'>\\s*?(" + Pattern.quote("eval(function(p,a,c,k,e,d)") + ".+?)\\s*?</script>");
        if (!jsMatcher.find()) {
            throw new PluginImplementationException("javascript not found");
        }
        final String jsString = jsMatcher.group(1).replaceFirst(Pattern.quote("eval(function(p,a,c,k,e,d)"), "function test(p,a,c,k,e,d)")
                .replaceFirst(Pattern.quote("return p}"), "return p};test").replaceFirst(Pattern.quote(".split('|')))"), ".split('|'));");
        final ScriptEngineManager manager = new ScriptEngineManager();
        final ScriptEngine engine = manager.getEngineByName("javascript");
        try {
            return (String) engine.eval(jsString);
        } catch (ScriptException e) {
            throw new PluginImplementationException("JavaScript eval failed");
        }
    }
}