package cz.vity.freerapid.plugins.services.jalurcepat;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.exceptions.YouHaveToWaitException;
import cz.vity.freerapid.plugins.services.xfilesharingcommon.XFileSharingCommonFileRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;

import java.util.logging.Logger;

/**
 * Class which contains main code
 *
 * @author tong2shot
 */
class JalurCepatFileRunner extends XFileSharingCommonFileRunner {
    private final static Logger logger = Logger.getLogger(JalurCepatFileRunner.class.getName());
    private static final String SERVICE_TITLE = "JalurCepat";
    private static final String SERVICE_COOKIE_DOMAIN = ".jalurcepat.com";

    @Override
    public String getCookieDomain() {
        return SERVICE_COOKIE_DOMAIN;
    }

    @Override
    public String getServiceTitle() {
        return SERVICE_TITLE;
    }

    @Override
    protected byte getNumberOfPages() {
        return 1;
    }

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
    }

    @Override
    public void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        PlugUtils.checkName(httpFile, content, "File Name:</b></td><td nowrap>", "</b>");
        PlugUtils.checkFileSize(httpFile, content, "File Size:</b></td><td>", "<small>");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
    }

    @Override
    protected void checkFileProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("File Not Found") || contentAsString.contains("No such file") || contentAsString.contains("<font class=\"err\">")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
        super.checkFileProblems();
    }

    @Override
    protected void checkDownloadProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("Wrong captcha")) {
            throw new YouHaveToWaitException("Wrong captcha", 1);
        }
        if (contentAsString.contains("this file reached max downloads limit")) {
            throw new PluginImplementationException("Sorry but this file reached max downloads limit");
        }
        super.checkDownloadProblems();
    }

}