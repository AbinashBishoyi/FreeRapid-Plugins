package cz.vity.freerapid.plugins.services.bebasupload;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 * @author Arthur Gunawan
 */
public class BebasUploadServiceImpl extends AbstractFileShareService {

    public String getName() {
        return "bebasupload.com";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;//ok
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new BebasUploadFileRunner();
    }

}
