package cz.vity.freerapid.plugins.services.jalurcepat;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;

import java.util.logging.Logger;

/**
 * Class which contains main code
 *
 * @author tong2shot
 */
class JalurCepatFileRunner extends XFileSharingRunner {
    private final static Logger logger = Logger.getLogger(JalurCepatFileRunner.class.getName());
    private static final String SERVICE_TITLE = "JalurCepat";

    public JalurCepatFileRunner() {
        super(SERVICE_TITLE);
    }

    @Override
    public void checkNameAndSize() throws ErrorDuringDownloadingException {
        PlugUtils.checkName(httpFile, getContentAsString(), "File Name:</b></td><td nowrap>", "</b>");
        PlugUtils.checkFileSize(httpFile, getContentAsString(), "File Size:</b></td><td>", "<small>");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    protected void checkFileProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("File Not Found") || contentAsString.contains("No such file") || contentAsString.contains("<font class=\"err\">")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
        super.checkFileProblems();
    }

}