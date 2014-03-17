package cz.vity.freerapid.plugins.services.asixfiles;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.services.xfilesharingcommon.XFileSharingCommonFileRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;

import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author tong2shot
 */
class AsixFilesFileRunner extends XFileSharingCommonFileRunner {
    private final static Logger logger = Logger.getLogger(AsixFilesFileRunner.class.getName());
    private final static String SERVICE_COOKIE_DOMAIN = ".asixfiles.com";
    private final static String SERVICE_TITLE = "AsixFiles";
    private final static String SERVICE_LOGIN_REFERER = "http://www.asixfiles.com/login.html";
    private final static String SERVICE_LOGIN_ACTION = "http://www.asixfiles.com";

    @Override
    protected Class getRunnerClass() {
        return AsixFilesFileRunner.class;
    }

    @Override
    protected Class getImplClass() {
        return AsixFilesServiceImpl.class;
    }


    @Override
    protected String getCookieDomain() {
        return SERVICE_COOKIE_DOMAIN;
    }

    @Override
    protected String getServiceTitle() {
        return SERVICE_TITLE;
    }

    @Override
    protected boolean isRegisteredUserImplemented() {
        return true;
    }

    @Override
    protected String getLoginURL() {
        return SERVICE_LOGIN_REFERER;
    }

    @Override
    protected String getLoginActionURL() {
        return SERVICE_LOGIN_ACTION;
    }

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
    }

    @Override
    public void run() throws Exception {
        super.run();
    }

    @Override
    protected void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        final String nameAndSizeRule = "You have requested.*?http://(?:www\\.)?" + "asixfiles.com" + "/[a-z0-9]{12}/(.*)</font> \\((.*?)\\)</font>$";
        final Matcher matcher = PlugUtils.matcher(nameAndSizeRule, content);
        if (!matcher.find()) {
            throw new PluginImplementationException("File name and size not found");
        }
        httpFile.setFileName(matcher.group(1).trim());
        httpFile.setFileSize(PlugUtils.getFileSizeFromString(matcher.group(2).trim()));
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

}