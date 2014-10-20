package cz.vity.freerapid.plugins.services.chayfile;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class ChayFileServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "ChayFile";
    }

    @Override
    public String getName() {
        return "chayfile.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new ChayFileFileRunner();
    }

}