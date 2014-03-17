package cz.vity.freerapid.plugins.services.cosmobox;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.services.xfilesharingcommon.RegisteredUserRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;

import java.util.logging.Logger;

/**
 * Class which contains main code
 *
 * @author tong2shot
 */
class CosmoBoxFileRunner extends RegisteredUserRunner {
    private final static Logger logger = Logger.getLogger(CosmoBoxFileRunner.class.getName());
    private final static String SERVICE_TITLE = "CosmoBox";
    private final static String SERVICE_COOKIE_DOMAIN = ".cosmobox.org";
    private final static String SERVICE_LOGIN_URL = "http://www.cosmobox.org/login.html";
    private final static String SERVICE_LOGIN_ACTION = "http://www.cosmobox.org/";

    public CosmoBoxFileRunner() {
        super(SERVICE_COOKIE_DOMAIN, SERVICE_TITLE, SERVICE_LOGIN_URL, SERVICE_LOGIN_ACTION, CosmoBoxFileRunner.class, CosmoBoxServiceImpl.class);
    }

    @Override
    protected void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        PlugUtils.checkName(httpFile, content, "<h2>Download File", "</h2>");
        PlugUtils.checkFileSize(httpFile, content, "</font> (", ")</font>");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

}