package cz.vity.freerapid.plugins.services.biggerupload;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author Vity
 */
public class BiggerUploadServiceImpl extends AbstractFileShareService {

    public String getName() {
        return "biggerupload.com";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;//ok
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new BiggerUploadFileRunner();
    }

}
