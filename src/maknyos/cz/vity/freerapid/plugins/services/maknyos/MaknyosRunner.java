package cz.vity.freerapid.plugins.services.maknyos;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.NotRecoverableDownloadException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingRunner;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;

/**
 * @author Alex,zid,tong2shot
 */
class MaknyosRunner extends XFileSharingRunner {

    @Override
    protected void checkFileProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("tidak ditemukan")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
        super.checkFileProblems();
    }

    @Override
    protected void checkDownloadProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("already downloading")) {
            throw new ServiceConnectionProblemException(String.format("<b>maknyos Error:</b><br>Your IP address is already downloading a file. <br>Please wait until the download is completed."));
        }
        if (contentAsString.contains("Currently a lot of users")) {
            throw new ServiceConnectionProblemException(String.format("<b>maknyos Error:</b><br>Currently a lot of users are downloading files."));
        }
        if (contentAsString.contains("You can download files up to")) {
            throw new NotRecoverableDownloadException(PlugUtils.getStringBetween(contentAsString, "<br><p class=\"err\">", "<br>"));
        }
        super.checkDownloadProblems();
    }

}