package cz.vity.freerapid.plugins.services.uptobox;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author tong2shot
 */
public class UptoBoxServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "UptoBox";
    }

    @Override
    public String getName() {
        return "uptobox.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new UptoBoxFileRunner();
    }

}