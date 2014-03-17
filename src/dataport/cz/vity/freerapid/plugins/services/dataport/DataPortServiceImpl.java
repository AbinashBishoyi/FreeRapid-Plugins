package cz.vity.freerapid.plugins.services.dataport;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 * @author Vity
 */
public class DataPortServiceImpl extends AbstractFileShareService {

    public String getName() {
        return "dataport.cz";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;//ok
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new DataPortFileRunner();
    }

}