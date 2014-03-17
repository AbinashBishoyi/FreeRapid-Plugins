package cz.vity.freerapid.plugins.services.tvuoluolcom;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author tong2shot
 */
public class TvuolUolComServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "tvuol.uol.com.br";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new TvuolUolComFileRunner();
    }

}