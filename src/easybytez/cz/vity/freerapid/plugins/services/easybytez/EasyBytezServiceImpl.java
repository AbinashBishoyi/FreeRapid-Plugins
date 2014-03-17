package cz.vity.freerapid.plugins.services.easybytez;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author tong2shot
 */
public class EasyBytezServiceImpl extends XFileSharingServiceImpl {
    @Override
    public String getServiceTitle() {
        return "EasyBytez";
    }

    @Override
    public String getName() {
        return "easybytez.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new EasyBytezFileRunner();
    }
}