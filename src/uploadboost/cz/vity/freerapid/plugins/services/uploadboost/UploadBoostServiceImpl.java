package cz.vity.freerapid.plugins.services.uploadboost;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author tong2shot
 */
public class UploadBoostServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "UploadBoost";
    }

    @Override
    public String getName() {
        return "uploadboost.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new UploadBoostFileRunner();
    }

}