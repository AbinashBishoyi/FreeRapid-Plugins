package cz.vity.freerapid.plugins.services.uploadil;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author Frishrash
 */
public class UploadIlServiceImpl extends AbstractFileShareService {
    private static final String SERVICE_NAME = "upload-il.com";

    @Override
    public String getName() {
        return SERVICE_NAME;
    }

    @Override
    public boolean supportsRunCheck() {
        return true;//ok
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new UploadIlFileRunner();
    }

}