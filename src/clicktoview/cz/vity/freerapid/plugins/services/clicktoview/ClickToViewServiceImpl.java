package cz.vity.freerapid.plugins.services.clicktoview;

import cz.vity.freerapid.plugins.services.xfileplayer.XFilePlayerServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class ClickToViewServiceImpl extends XFilePlayerServiceImpl {

    @Override
    public String getServiceTitle() {
        return "ClickToView";
    }

    @Override
    public String getName() {
        return "clicktoview.org";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new ClickToViewFileRunner();
    }

}