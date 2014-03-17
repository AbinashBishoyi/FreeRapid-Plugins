package cz.vity.freerapid.plugins.services.#shortsmall#;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 * @author #author#
 */
public class #fullWithoutDot#ServiceImpl extends AbstractFileShareService {

    public String getName() {
        return "#fulllower#";
    }

    public int getMaxDownloadsFromOneIP() {
        return 1;
    }

    @Override
    public boolean supportsRunCheck() {
        return true;//ok
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new #fullWithoutDot#FileRunner();
    }

}
