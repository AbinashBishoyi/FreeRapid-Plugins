package cz.vity.freerapid.plugins.services.fileshare;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * @author Saikek
 *         Class that provides basic info about plugin
 */
public class FileShareServiceImpl extends AbstractFileShareService {
    private static final String SERVICE_NAME = "fileshare.in.ua";

    public String getName() {
        return SERVICE_NAME;
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new FileShareFileRunner();
    }

}
