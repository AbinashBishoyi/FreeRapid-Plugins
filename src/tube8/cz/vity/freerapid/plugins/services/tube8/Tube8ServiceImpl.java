package cz.vity.freerapid.plugins.services.tube8;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 * @author TommyTom
 */
public class Tube8ServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "tube8.com";
    }

    @Override
    public int getMaxDownloadsFromOneIP() {
        //TODO don't forget to update this value, in plugin.xml don't forget to update this value too
        return 9;
    }

    @Override
    public boolean supportsRunCheck() {
        return true;//ok
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new Tube8FileRunner();
    }

}