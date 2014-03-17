package cz.vity.freerapid.plugins.services.bezvadata;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author Vity
 */
public class BezvaDataServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "bezvadata.cz";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new BezvaDataFileRunner();
    }

}