package cz.vity.freerapid.plugins.services.files2share;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class Files2ShareServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "Files2Share";
    }

    @Override
    public String getName() {
        return "files2share.ch";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new Files2ShareFileRunner();
    }

}