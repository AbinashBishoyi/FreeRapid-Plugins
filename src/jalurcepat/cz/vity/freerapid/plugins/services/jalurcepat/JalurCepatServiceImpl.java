package cz.vity.freerapid.plugins.services.jalurcepat;

import cz.vity.freerapid.plugins.services.xfilesharingcommon.XFileSharingCommonServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author tong2shot
 */
//public class JalurCepatServiceImpl extends AbstractFileShareService { //for non-supported registered users, this can also be used
public class JalurCepatServiceImpl extends XFileSharingCommonServiceImpl {
    @Override
    public String getName() {
        return "jalurcepat.com";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    public PluginRunner getPluginRunnerInstance() {
        return new JalurCepatFileRunner();
    }

}