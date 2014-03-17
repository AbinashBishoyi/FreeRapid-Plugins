package cz.vity.freerapid.plugins.services.bebasupload;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author Arthur Gunawan, tong2shot
 */
public class BebasUploadServiceImpl extends XFileSharingServiceImpl {
    @Override
    public String getServiceTitle() {
        return "BebasUpload";
    }

    @Override
    public String getName() {
        return "bebasupload.com";
    }

    @Override
    public PluginRunner getPluginRunnerInstance() {
        return new BebasUploadFileRunner();
    }
}
