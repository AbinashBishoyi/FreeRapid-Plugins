package cz.vity.freerapid.plugins.services.mrfile;

import cz.vity.freerapid.plugins.services.xfileplayer.XFilePlayerServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class MrFileServiceImpl extends XFilePlayerServiceImpl {

    @Override
    public String getServiceTitle() {
        return "MrFile";
    }

    @Override
    public String getName() {
        return "mrfile.me";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new MrFileFileRunner();
    }

}