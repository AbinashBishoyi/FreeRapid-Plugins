package cz.vity.freerapid.plugins.services.sharpfile;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class SharpFileServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "SharpFile";
    }

    @Override
    public String getName() {
        return "sharpfile.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new SharpFileFileRunner();
    }

}