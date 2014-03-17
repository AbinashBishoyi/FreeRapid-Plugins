package cz.vity.freerapid.plugins.services.cosmobox;

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
class CosmoBoxFileRunner extends XFileSharingCommonFileRunner {
    private final static Logger logger = Logger.getLogger(CosmoBoxFileRunner.class.getName());
    private final static String SERVICE_TITLE = "CosmoBox";
    private final static String SERVICE_COOKIE_DOMAIN = ".cosmobox.org";
    private final static String SERVICE_LOGIN_URL = "http://www.cosmobox.org/login.html";
    private final static String SERVICE_LOGIN_ACTION = "http://www.cosmobox.org/";

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
    protected Class getRunnerClass() {
        return CosmoBoxFileRunner.class;
    }

    @Override
    protected Class getImplClass() {
        return CosmoBoxServiceImpl.class;
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
    public void runCheck() throws Exception {
        super.runCheck();
    }

    @Override
    protected void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        PlugUtils.checkName(httpFile, content, "<h2>Download File", "</h2>");
        PlugUtils.checkFileSize(httpFile, content, "</font> (", ")</font>");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
    }

}