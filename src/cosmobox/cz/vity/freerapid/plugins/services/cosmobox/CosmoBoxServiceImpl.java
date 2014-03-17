package cz.vity.freerapid.plugins.services.cosmobox;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author tong2shot
 */
public class CosmoBoxServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "CosmoBox";
    }

    @Override
    public String getName() {
        return "cosmobox.org";
    }

    @Override
    public PluginRunner getPluginRunnerInstance() {
        return new CosmoBoxFileRunner();
    }

}