package cz.vity.freerapid.plugins.services.#shortsmall#;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author #author#
 */
public class #fullWithoutDot#ServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "#fulllower#";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new #fullWithoutDot#FileRunner();
    }

}