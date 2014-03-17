package cz.vity.freerapid.plugins.services.ryushare;

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
class RyuShareFileRunner extends RegisteredUserRunner {
    private final static Logger logger = Logger.getLogger(RyuShareFileRunner.class.getName());
    private static final String SERVICE_TITLE = "RyuShare";
    private static final String SERVICE_COOKIE_DOMAIN = ".ryushare.com";
    private final static String SERVICE_LOGIN_URL = "http://www.ryushare.com/login.python";
    private final static String SERVICE_LOGIN_ACTION = "http://www.ryushare.com";

    public RyuShareFileRunner() {
        super(SERVICE_COOKIE_DOMAIN, SERVICE_TITLE, SERVICE_LOGIN_URL, SERVICE_LOGIN_ACTION, RyuShareFileRunner.class, RyuShareServiceImpl.class);
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