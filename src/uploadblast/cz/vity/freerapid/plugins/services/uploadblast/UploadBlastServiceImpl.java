package cz.vity.freerapid.plugins.services.uploadblast;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class UploadBlastServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "UploadBlast";
    }

    @Override
    public String getName() {
        return "uploadblast.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new UploadBlastFileRunner();
    }

}