package cz.vity.freerapid.plugins.services.storage;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author Vity
 */
public class StorageServiceImpl extends AbstractFileShareService {

    public String getName() {
        return "storage.to";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;//ok
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new StorageFileRunner();
    }

}
