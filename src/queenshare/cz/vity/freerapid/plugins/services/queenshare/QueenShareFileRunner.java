package cz.vity.freerapid.plugins.services.queenshare;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.NotRecoverableDownloadException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingRunner;
import cz.vity.freerapid.plugins.services.xfilesharing.nameandsize.FileSizeHandler;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;

import java.util.List;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class QueenShareFileRunner extends XFileSharingRunner {

    @Override
    protected MethodBuilder getXFSMethodBuilder() throws Exception {
        return getXFSMethodBuilder(getContentAsString() + "</Form>");     //# missing end form tag
    }

    @Override
    protected List<String> getDownloadPageMarkers() {
        final List<String> downloadPageMarkers = super.getDownloadPageMarkers();
        downloadPageMarkers.add("alt=\"Get File\"");
        return downloadPageMarkers;
    }

    @Override
    protected List<String> getDownloadLinkRegexes() {
        final List<String> downloadLinkRegexes = super.getDownloadLinkRegexes();
        downloadLinkRegexes.add(0, "<a href\\s*=\\s*\"(?:http://adf\\.ly.+?)?(http://.*)\">.+?alt=\"Get File\".+?</a>");
        return downloadLinkRegexes;
    }

    @Override
    protected List<FileSizeHandler> getFileSizeHandlers() {
        final List<FileSizeHandler> fileSizeHandlers = super.getFileSizeHandlers();
        fileSizeHandlers.add(0, new QueenShareFileSizeHandler());
        return fileSizeHandlers;
    }

    @Override
    protected void checkFileProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("<h2>File Not Found") ||
                contentAsString.contains("The file you were looking for could not be found") ||
                contentAsString.contains("The file was removed")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
    }

    @Override
    protected void checkDownloadProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("You can download files up to")) {
            throw new NotRecoverableDownloadException(PlugUtils.getStringBetween(contentAsString, "<p class=\"err\">", "<br>"));
        }
        if (contentAsString.contains("may wish to try again at a later time")) {
            throw new ServiceConnectionProblemException("The Web Server may be down, too busy, or experiencing other problems preventing it from responding to requests. You may wish to try again at a later time");
        }
        super.checkDownloadProblems();
    }
}