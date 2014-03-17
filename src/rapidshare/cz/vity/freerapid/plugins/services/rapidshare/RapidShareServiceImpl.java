package cz.vity.freerapid.plugins.services.rapidshare;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.ConfigurationStorageSupport;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

import java.util.ArrayList;
import java.util.logging.Logger;

/**
 * @author Ladislav Vitasek
 */
public class RapidShareServiceImpl extends AbstractFileShareService {
    private final static Logger logger = Logger.getLogger(RapidShareServiceImpl.class.getName());
    private static final String SERVICE_NAME = "RapidShare.com";


    @Override
    public String getName() {
        return SERVICE_NAME;
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new RapidShareRunner();
    }

    @Override
    public void showOptions() throws Exception {
        MirrorChooser chooser = new MirrorChooser(getPluginContext(), getConfig());
        chooser.chooseFromList();
    }

    RapidShareMirrorConfig getConfig() throws Exception {

        ConfigurationStorageSupport storage = getPluginContext().getConfigurationStorageSupport();
        if (mirrorConfig == null)
            if (!storage.configFileExists(MirrorChooser.CONFIGFILE)) {
                logger.info("Initializing new mirrorConfig ");
                mirrorConfig = new RapidShareMirrorConfig();
                mirrorConfig.setAr(new ArrayList<MirrorBean>());
                MirrorChooser.initPreferred(mirrorConfig);
            } else {
                logger.info("Loading mirrorConfig from config file");
                mirrorConfig = storage.loadConfigFromFile(MirrorChooser.CONFIGFILE, RapidShareMirrorConfig.class);

            }

        return mirrorConfig;
    }

    private volatile RapidShareMirrorConfig mirrorConfig;

}
