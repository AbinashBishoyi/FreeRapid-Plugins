package cz.vity.freerapid.plugins.services.sapopt;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author tong2shot
 */
public class SapoPtServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "sapo.pt";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new SapoPtFileRunner();
    }

}