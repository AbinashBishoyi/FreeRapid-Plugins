package cz.vity.freerapid.plugins.services.pandamemo;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class PandaMemoServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "PandaMemo";
    }

    @Override
    public String getName() {
        return "pandamemo.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new PandaMemoFileRunner();
    }

}