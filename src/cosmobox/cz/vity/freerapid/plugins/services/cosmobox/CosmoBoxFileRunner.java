package cz.vity.freerapid.plugins.services.cosmobox;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.services.xfilesharing.RegisteredUserRunner;
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
    private final static String SERVICE_LOGIN_URL = "http://www.cosmobox.org/login.html";
    private final static String SERVICE_LOGIN_ACTION = "http://www.cosmobox.org/";

    public CosmoBoxFileRunner() {
        super(SERVICE_TITLE, SERVICE_LOGIN_URL, SERVICE_LOGIN_ACTION, CosmoBoxFileRunner.class, CosmoBoxServiceImpl.class);
    }

    @Override
    protected void checkNameAndSize() throws ErrorDuringDownloadingException {
        PlugUtils.checkName(httpFile, getContentAsString(), "<h2>Download File", "</h2>");
        PlugUtils.checkFileSize(httpFile, getContentAsString(), "</font> (", ")</font>");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

}