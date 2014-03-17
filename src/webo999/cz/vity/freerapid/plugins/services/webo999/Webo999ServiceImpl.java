package cz.vity.freerapid.plugins.services.webo999;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class Webo999ServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "webo999.com";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new Webo999FileRunner();
    }

}