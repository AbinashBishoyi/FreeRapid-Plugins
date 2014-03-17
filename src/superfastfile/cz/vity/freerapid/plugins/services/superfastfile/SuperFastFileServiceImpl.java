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


    @Override
    public boolean supportsRunCheck() {
        return true;//ok
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new SuperFastFileFileRunner();
    }

}