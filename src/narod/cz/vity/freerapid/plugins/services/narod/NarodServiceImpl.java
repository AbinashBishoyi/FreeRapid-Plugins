package cz.vity.freerapid.plugins.services.narod;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 * @author Thumb
 */
public class NarodServiceImpl extends AbstractFileShareService {

    public String getName() {
        return "narod.ru";
    }

    public int getMaxDownloadsFromOneIP() {
        //TODO don't forget to update this value, in plugin.xml don't forget to update this value too
        return 999; // ???
    }

    @Override
    public boolean supportsRunCheck() {
        return true;//ok
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new NarodFileRunner();
    }

}