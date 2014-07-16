package cz.vity.freerapid.plugins.services.ultrafile;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class UltraFileServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "UltraFile";
    }

    @Override
    public String getName() {
        return "ultrafile.me";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new UltraFileFileRunner();
    }

}