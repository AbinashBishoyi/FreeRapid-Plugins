package cz.vity.freerapid.plugins.services.uptobox;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.NotRecoverableDownloadException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingRunner;
import cz.vity.freerapid.plugins.services.xfilesharing.nameandsize.FileNameHandler;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;

import java.util.List;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author tong2shot
 */
class UptoBoxFileRunner extends XFileSharingRunner {

    @Override
    protected List<FileNameHandler> getFileNameHandlers() {
        final List<FileNameHandler> fileNameHandlers = super.getFileNameHandlers();
        fileNameHandlers.add(0, new UptoBoxFileNameHandler());
        return fileNameHandlers;
    }

    @Override
    protected int getWaitTime() throws Exception {
        final Matcher matcher = getMatcherAgainstContent("Wait.*?<.+?\">.*?(\\d+).*?</span");
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1)) + 1;
        }
        return 0;
    }

    @Override
    protected void checkFileProblems() throws ErrorDuringDownloadingException {
        final String content = getContentAsString();
        if (content.contains("the file you want is not available")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
        super.checkFileProblems();
    }

    @Override
    protected void checkDownloadProblems() throws ErrorDuringDownloadingException {
        final String content = getContentAsString();
        if (content.contains("Vous ne pouvez pas t&eacute;l&eacute;charger des fichiers de taille sup&eacute;rieur &agrave")) {
            throw new NotRecoverableDownloadException(PlugUtils.getStringBetween(content, " class=\"err\">", "<br").replace("Vous ne pouvez pas t&eacute;l&eacute;charger des fichiers de taille sup&eacute;rieur &agrave", "You can not download file sizes greater than"));
        }
        super.checkDownloadProblems();
    }

    @Override
    protected List<String> getDownloadPageMarkers() {
        final List<String> downloadPageMarkers = super.getDownloadPageMarkers();
        downloadPageMarkers.add("Click here to start your download");
        return downloadPageMarkers;
    }

}