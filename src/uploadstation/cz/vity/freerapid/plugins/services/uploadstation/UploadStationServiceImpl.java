package cz.vity.freerapid.plugins.services.uploadstation;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author ntoskrnl
 */
public class UploadStationServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "uploadstation.com";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new UploadStationFileRunner();
    }

}