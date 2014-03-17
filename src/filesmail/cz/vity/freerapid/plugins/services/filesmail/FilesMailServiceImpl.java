package cz.vity.freerapid.plugins.services.filesmail;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class FilesMailServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "files.mail.ru";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new FilesMailFileRunner();
    }

}