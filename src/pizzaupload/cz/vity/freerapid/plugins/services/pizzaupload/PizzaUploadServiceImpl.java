package cz.vity.freerapid.plugins.services.pizzaupload;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class PizzaUploadServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "PizzaUpload";
    }

    @Override
    public String getName() {
        return "pizzaupload.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new PizzaUploadFileRunner();
    }

}