package cz.vity.freerapid.plugins.services.data;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author Javi
 */
public class DataServiceImpl extends AbstractFileShareService {

    public String getName() {
        return "data.hu";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;//ok
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new DataFileRunner();
    }

}