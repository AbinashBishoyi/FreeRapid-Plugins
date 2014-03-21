package cz.vity.freerapid.plugins.services.filespace;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.NotRecoverableDownloadException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingRunner;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class FileSpaceFileRunner extends XFileSharingRunner {

    @Override
    protected void setLanguageCookie() throws Exception {
        if (fileURL.contains("spaceforfiles.com/"))
            fileURL = fileURL.replace("spaceforfiles.com/", "filespace.com/");
        super.setLanguageCookie();
    }

    @Override
    protected MethodBuilder getXFSMethodBuilder() throws Exception {
        final MethodBuilder methodBuilder = super.getXFSMethodBuilder();
        if (methodBuilder.getParameters().get("method_free") != null)
            return methodBuilder.setParameter("method_free", "Free Download");
        return methodBuilder;
    }

    @Override
    protected void checkFileProblems() throws ErrorDuringDownloadingException {
        final String content = getContentAsString();
        if (content.contains("No such file with this filename")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
        super.checkFileProblems();
    }

    @Override
    protected void checkDownloadProblems() throws ErrorDuringDownloadingException {
        final String content = getContentAsString();
        if (content.contains("file is available for Premium Users only")) {
            throw new NotRecoverableDownloadException("This file is only available to premium users");
        }
        super.checkDownloadProblems();
    }
}