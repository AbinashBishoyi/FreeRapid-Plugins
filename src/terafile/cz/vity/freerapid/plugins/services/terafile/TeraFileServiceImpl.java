package cz.vity.freerapid.plugins.services.terafile;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class TeraFileServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "TeraFile";
    }

    @Override
    public String getName() {
        return "terafile.co";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new TeraFileFileRunner();
    }

}