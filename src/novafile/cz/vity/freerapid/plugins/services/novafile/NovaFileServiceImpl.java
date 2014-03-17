package cz.vity.freerapid.plugins.services.novafile;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author tong2shot
 */
public class NovaFileServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "NovaFile";
    }

    @Override
    public String getName() {
        return "novafile.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new NovaFileFileRunner();
    }

}