package cz.vity.freerapid.plugins.services.superfastfile;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author Vity
 */
public class SuperFastFileServiceImpl extends AbstractFileShareService {

    public String getName() {
        return "superfastfile.com";
    }

    public int getMaxDownloadsFromOneIP() {
        //don't forget to update this value,don't forget to update this value  in plugin.xml too
        return 3;
    }

    @Override
    public boolean supportsRunCheck() {
        return true;//ok
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new SuperFastFileFileRunner();
    }

}