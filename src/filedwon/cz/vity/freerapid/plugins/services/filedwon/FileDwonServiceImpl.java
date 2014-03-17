package cz.vity.freerapid.plugins.services.filedwon;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class FileDwonServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "FileDwon";
    }

    @Override
    public String getName() {
        return "filedwon.info";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new FileDwonFileRunner();
    }

}