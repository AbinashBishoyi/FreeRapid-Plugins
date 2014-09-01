package cz.vity.freerapid.plugins.services.clicknupload;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class ClicknUploadServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "ClicknUpload";
    }

    @Override
    public String getName() {
        return "clicknupload.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new ClicknUploadFileRunner();
    }

}