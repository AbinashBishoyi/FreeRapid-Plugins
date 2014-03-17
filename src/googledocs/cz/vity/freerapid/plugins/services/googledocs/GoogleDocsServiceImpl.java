package cz.vity.freerapid.plugins.services.googledocs;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author tong2shot
 */
public class GoogleDocsServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "docs.google.com";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new GoogleDocsFileRunner();
    }

}