package cz.vity.freerapid.plugins.services.jumbofiles;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.exceptions.YouHaveToWaitException;
import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingRunner;
import cz.vity.freerapid.plugins.services.xfilesharing.nameandsize.FileNameHandler;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class which contains main code
 *
 * @author tong2shot, birchie
 */
class JumboFilesFileRunner extends XFileSharingRunner {

    @Override
    protected List<FileNameHandler> getFileNameHandlers() {
        final List<FileNameHandler> fileNameHandlers = super.getFileNameHandlers();
        fileNameHandlers.add(0, new JumboFilesFileNameHandler());
        return fileNameHandlers;
    }

    @Override
    protected boolean checkDownloadPageMarker() {
        if (super.checkDownloadPageMarker()) {
            return true;
        } else {
            for (final String downloadLinkRegex : getDownloadLinkRegexes()) {
                final Matcher matcher = getMatcherAgainstContent(downloadLinkRegex);
                if (matcher.find()) {
                    return true;
                }
            }
            return false;
        }
    }

    @Override
    protected List<String> getDownloadLinkRegexes() {
        final List<String> downloadLinkRegexes = super.getDownloadLinkRegexes();
        downloadLinkRegexes.add(0, "<FORM METHOD\\s*=\\s*\"LINK\" ACTION\\s*=\\s*\"".toLowerCase() + "(http.+?" + Pattern.quote(httpFile.getFileName()) + ")\">");
        downloadLinkRegexes.add(0, "<FORM METHOD\\s*=\\s*\"LINK\" ACTION\\s*=\\s*\"(http.+?" + Pattern.quote(httpFile.getFileName()) + ")\">");
        downloadLinkRegexes.add(0, "<A HREF\\s*=\\s*(?:\"|')" + "(http.+?" + Pattern.quote(httpFile.getFileName()) + ")(?:\"|')");
        return downloadLinkRegexes;
    }

    @Override
    protected MethodBuilder getXFSMethodBuilder() throws Exception {
        final MethodBuilder methodBuilder;
        String content = getContentAsString();
        try {
            final String formData = PlugUtils.getStringBetween(content, "getElementById(\"dl\").innerHTML = '", "';");
            content = content.replaceAll("<div id=\"dl\"", "<div id=\"dl\">" + formData);
        } catch (Exception e) { /**/ }
        if (content.contains("method_free")) {
            methodBuilder = getMethodBuilder(content)
                    .setReferer(fileURL)
                    .setActionFromFormWhereTagContains("method_free", true)
                    .setAction(fileURL);
        } else {
            methodBuilder = getMethodBuilder(content)
                    .setReferer(fileURL)
                    .setActionFromFormWhereTagContains("F1", true)
                    .setAction(fileURL);
        }
        if ((methodBuilder.getParameters().get("method_free") != null) && (!methodBuilder.getParameters().get("method_free").isEmpty())) {
            methodBuilder.removeParameter("method_premium");
        }
        return methodBuilder;
    }

    @Override
    protected void checkFileProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("Not Found or Deleted") || contentAsString.contains("file was removed") || contentAsString.contains("File is deleted or not found")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
        if (contentAsString.contains("server is in maintenance mode") || contentAsString.contains("we are performing maintenance on this server")) {
            throw new PluginImplementationException("This server is in maintenance mode. Please try again later.");
        }
        // calling super.checkFileProblems() will catch "File Not Found", which is not the case
    }

    @Override
    protected void checkDownloadProblems() throws ErrorDuringDownloadingException {
        final String content = getContentAsString();
        if (content.contains("Delay between downloads must be not less than")) {
            throw new YouHaveToWaitException("You have reached the download limit", 10 * 60);
        }
        super.checkDownloadProblems();
    }
}