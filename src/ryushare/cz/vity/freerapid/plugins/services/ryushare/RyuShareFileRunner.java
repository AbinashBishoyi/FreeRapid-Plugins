package cz.vity.freerapid.plugins.services.ryushare;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.services.xfilesharingcommon.XFileSharingCommonFileRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;

import java.util.logging.Logger;

/**
 * Class which contains main code
 *
 * @author tong2shot
 */
class RyuShareFileRunner extends XFileSharingCommonFileRunner {
    private final static Logger logger = Logger.getLogger(RyuShareFileRunner.class.getName());
    private static final String SERVICE_TITLE = "RyuShare";
    private static final String SERVICE_COOKIE_DOMAIN = ".ryushare.com";
    private final static String SERVICE_LOGIN_URL = "http://www.ryushare.com/login.python";
    private final static String SERVICE_LOGIN_ACTION = "http://www.ryushare.com";

    @Override
    protected String getCookieDomain() {
        return SERVICE_COOKIE_DOMAIN;
    }

    @Override
    protected String getServiceTitle() {
        return SERVICE_TITLE;
    }

    @Override
    protected Class getRunnerClass() {
        return RyuShareFileRunner.class;
    }

    @Override
    protected Class getImplClass() {
        return RyuShareServiceImpl.class;
    }

    @Override
    protected String getLoginURL() {
        return SERVICE_LOGIN_URL;
    }

    @Override
    protected String getLoginActionURL() {
        return SERVICE_LOGIN_ACTION;
    }

    @Override
    protected boolean isRegisteredUserImplemented() {
        return true;
    }

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
    }

    @Override
    protected void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        PlugUtils.checkName(httpFile, content, "Download File", "</h2>");
        PlugUtils.checkFileSize(httpFile, content, "</font> (", ")</font>");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }


    @Override
    public void run() throws Exception {
        super.run();
    }

}