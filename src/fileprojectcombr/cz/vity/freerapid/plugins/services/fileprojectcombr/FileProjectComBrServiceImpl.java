package cz.vity.freerapid.plugins.services.fileprojectcombr;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author tong2shot
 */
public class FileProjectComBrServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "fileproject.com.br";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new FileProjectComBrFileRunner();
    }

}