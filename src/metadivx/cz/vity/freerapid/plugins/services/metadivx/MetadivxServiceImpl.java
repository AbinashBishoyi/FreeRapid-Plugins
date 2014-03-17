package cz.vity.freerapid.plugins.services.metadivx;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 * @author RickCL
 */
public class MetadivxServiceImpl extends AbstractFileShareService {

    public String getName() {
        return "metadivx.com";
    }

    public int getMaxDownloadsFromOneIP() {
        return 2;
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new MetadivxFileRunner();
    }

}