package cz.vity.freerapid.plugins.services.hexupload;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class HexUploadServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "HexUpload";
    }

    @Override
    public String getName() {
        return "hexupload.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new HexUploadFileRunner();
    }

}