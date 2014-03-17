package cz.vity.freerapid.plugins.services.bego;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class BegoServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "bego.cc";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new BegoFileRunner();
    }

}