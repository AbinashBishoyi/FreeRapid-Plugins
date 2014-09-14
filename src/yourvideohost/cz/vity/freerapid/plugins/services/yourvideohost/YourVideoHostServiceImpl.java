package cz.vity.freerapid.plugins.services.yourvideohost;

import cz.vity.freerapid.plugins.services.xfileplayer.XFilePlayerServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class YourVideoHostServiceImpl extends XFilePlayerServiceImpl {

    @Override
    public String getServiceTitle() {
        return "YourVideoHost";
    }

    @Override
    public String getName() {
        return "yourvideohost.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new YourVideoHostFileRunner();
    }

}