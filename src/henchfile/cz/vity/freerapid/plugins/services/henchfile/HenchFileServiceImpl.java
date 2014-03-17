package cz.vity.freerapid.plugins.services.henchfile;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class HenchFileServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "HenchFile";
    }

    @Override
    public String getName() {
        return "henchfile.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new HenchFileFileRunner();
    }

}