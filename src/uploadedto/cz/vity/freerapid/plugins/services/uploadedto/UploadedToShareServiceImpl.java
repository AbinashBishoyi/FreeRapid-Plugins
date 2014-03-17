package cz.vity.freerapid.plugins.services.uploadedto;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * @author Ladislav Vitasek
 */
public class UploadedToShareServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "uploaded.net";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new UploadedToRunner();
    }

}
