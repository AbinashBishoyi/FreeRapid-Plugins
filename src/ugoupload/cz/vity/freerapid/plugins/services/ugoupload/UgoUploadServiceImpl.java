package cz.vity.freerapid.plugins.services.ugoupload;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class UgoUploadServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "ugoupload.net";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new UgoUploadFileRunner();
    }

}