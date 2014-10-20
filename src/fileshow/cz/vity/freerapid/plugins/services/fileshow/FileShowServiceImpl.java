package cz.vity.freerapid.plugins.services.fileshow;

import cz.vity.freerapid.plugins.services.xfileplayer.XFilePlayerServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class FileShowServiceImpl extends XFilePlayerServiceImpl {

    @Override
    public String getServiceTitle() {
        return "FileShow";
    }

    @Override
    public String getName() {
        return "fileshow.tv";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new FileShowFileRunner();
    }

}