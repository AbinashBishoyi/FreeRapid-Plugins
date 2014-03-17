package cz.vity.freerapid.plugins.services.sanshare;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class SanShareServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "SanShare";
    }

    @Override
    public String getName() {
        return "sanshare.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new SanShareFileRunner();
    }

}