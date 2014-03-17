package cz.vity.freerapid.plugins.services.ezzfile;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class EzzFileServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "EzzFile";
    }

    @Override
    public String getName() {
        return "ezzfile.it";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new EzzFileFileRunner();
    }

}