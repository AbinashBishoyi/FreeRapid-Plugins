package cz.vity.freerapid.plugins.services.ryushare;

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
class RyuShareFileRunner extends RegisteredUserRunner {
    private final static Logger logger = Logger.getLogger(RyuShareFileRunner.class.getName());
    private final static String SERVICE_TITLE = "RyuShare";
    private final static String SERVICE_LOGIN_URL = "http://www.ryushare.com/login.python";
    private final static String SERVICE_LOGIN_ACTION = "http://www.ryushare.com";

    public RyuShareFileRunner() {
        super(SERVICE_TITLE, SERVICE_LOGIN_URL, SERVICE_LOGIN_ACTION, RyuShareFileRunner.class, RyuShareServiceImpl.class);
    }

    @Override
    protected void checkNameAndSize() throws ErrorDuringDownloadingException {
        PlugUtils.checkName(httpFile, getContentAsString(), "Download File", "</h2>");
        PlugUtils.checkFileSize(httpFile, getContentAsString(), "</font> (", ")</font>");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

}