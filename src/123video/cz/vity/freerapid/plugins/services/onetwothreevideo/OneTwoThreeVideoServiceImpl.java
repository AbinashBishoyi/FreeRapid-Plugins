package cz.vity.freerapid.plugins.services.onetwothreevideo;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author tong2shot
 */
public class OneTwoThreeVideoServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "123Video.nl";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new OneTwoThreeVideoFileRunner();
    }

}