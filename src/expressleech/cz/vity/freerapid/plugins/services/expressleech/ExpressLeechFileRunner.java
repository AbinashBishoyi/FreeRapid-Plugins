package cz.vity.freerapid.plugins.services.expressleech;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.NotRecoverableDownloadException;
import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingRunner;
import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.services.xfilesharing.nameandsize.FileSizeHandler;
import cz.vity.freerapid.plugins.services.xfilesharing.nameandsize.FileSizeHandlerNoSize;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;

import java.util.List;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class ExpressLeechFileRunner extends XFileSharingRunner {
    @Override
    protected List<FileSizeHandler> getFileSizeHandlers() {
        final List<FileSizeHandler> fileSizeHandlers = super.getFileSizeHandlers();
        fileSizeHandlers.add(0, new ExpressLeechFileSizeHandler());
        fileSizeHandlers.add(new FileSizeHandlerNoSize());
        return fileSizeHandlers;
    }

    @Override
    protected String getDownloadLinkFromRegexes() throws ErrorDuringDownloadingException {
        final Matcher match = getMatcherAgainstContent("<a href=\"http://dl.srv.+?/([^/]+?)(?:\"|')");
        if (match.find())
            httpFile.setFileName(match.group(1).trim());
        return super.getDownloadLinkFromRegexes();
    }

    @Override
    protected List<String> getDownloadPageMarkers() {
        final List<String> downloadPageMarkers = super.getDownloadPageMarkers();
        downloadPageMarkers.add("final_download.png");
        return downloadPageMarkers;
    }

    @Override
    protected List<String> getDownloadLinkRegexes() {
        final List<String> downloadLinkRegexes = super.getDownloadLinkRegexes();
        downloadLinkRegexes.add(0, "<a href=['\"](http://dl\\..+?)['\"]");
        return downloadLinkRegexes;
    }

    @Override
    protected void stepPassword(final MethodBuilder methodBuilder) throws Exception {
        if (getContentAsString().contains("<input type=\"password\" id=\"password\" name=\"password\" class=\"myForm\">")) {  // ONLY changed this line
            final String serviceTitle = ((XFileSharingServiceImpl) getPluginService()).getServiceTitle();
            final String password = getDialogSupport().askForPassword(serviceTitle);
            if (password == null) {
                throw new NotRecoverableDownloadException("This file is secured with a password");
            }
            methodBuilder.setParameter("password", password);
        }
    }

    @Override
    protected void checkFileProblems() throws ErrorDuringDownloadingException {
        if (getContentAsString().contains("Access to this resource on the server is denied")) {
            throw new NotRecoverableDownloadException("Access to this resource on the server is denied.");
        }
        super.checkFileProblems();
    }
}