package cz.vity.freerapid.plugins.services.uploadjocketredirect;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 * @author Arthur Gunawan
 */
public class UploadJocketRedirectServiceImpl extends AbstractFileShareService {

    public String getName() {
        return "uploadjocketredirect.com";
    }

    @Override
    public boolean supportsRunCheck() {
        return false;//ok
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new UploadJocketRedirectFileRunner();
    }

}