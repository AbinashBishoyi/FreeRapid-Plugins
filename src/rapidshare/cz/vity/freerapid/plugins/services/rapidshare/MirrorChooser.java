package cz.vity.freerapid.plugins.services.rapidshare;

import cz.vity.freerapid.plugins.webclient.interfaces.ConfigurationStorageSupport;
import cz.vity.freerapid.plugins.webclient.interfaces.DialogSupport;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginContext;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;

import java.util.logging.Logger;
import java.util.regex.Matcher;

class MirrorChooser {
    final static String CONFIGFILE = "rsMirror.xml";
    private final static Logger logger = Logger.getLogger(MirrorChooser.class.getName());
    private RapidShareMirrorConfig mirrorConfig;
    private ConfigurationStorageSupport storage;

    public void setContent(String content) {
        this.content = content;
    }

    private String content;
    private DialogSupport dialogSupport;

    MirrorChooser(PluginContext context, RapidShareMirrorConfig mirrorConfig) {

        storage = context.getConfigurationStorageSupport();
        dialogSupport = context.getDialogSupport();
        this.mirrorConfig = mirrorConfig;

    }

    private RapidShareMirrorConfig getMirrorConfig() {
        return mirrorConfig;
    }

    MirrorBean getChosen() {
        return mirrorConfig.getChosen();
    }


    private void setPreffered(Object object) {
        if (object instanceof MirrorBean) {
            MirrorBean mirror = (MirrorBean) object;
            mirrorConfig.setChosen(mirror);
        }

    }

    Object[] getArray() {
        return mirrorConfig.getAr().toArray();
    }

    private void makeMirrorList() throws Exception {
        logger.info("Making list of mirrors ");
        initPreferred(mirrorConfig);
        Matcher matcher = PlugUtils.matcher("<input (checked)? type=\"radio\" name=\"mirror\" onclick=\"document.dlf.action=.'http://rs[0-9]+([^.]+)[^']*.';\" /> ([^<]*)<br", content);
        while (matcher.find()) {

            String mirrorName = matcher.group(3);
            String ident = matcher.group(2);

            logger.info("Found mirror " + mirrorName + " ident " + ident);
            mirrorConfig.getAr().add(new MirrorBean(mirrorName, ident));
        }
        logger.info("Saving config ");
        storage.storeConfigToFile(getMirrorConfig(), CONFIGFILE);
        // <input checked type="radio" name="mirror" onclick="document.dlf.action=\'http://rs332gc.rapidshare.com/files/168531395/2434660/rkdr.part3.rar\';" /> GlobalCrossing<br />
    }

    void chooseFromList() throws Exception {
        MirrorChooserUI ms = new MirrorChooserUI(this);
        if (dialogSupport.showOKCancelDialog(ms, "Choose mirror")) {
            setPreffered(ms.getChoosen());
            logger.info("Setting chosen to " + getChosen());
            storage.storeConfigToFile(getMirrorConfig(), CONFIGFILE);
        }
    }

    private String findURL(String ident) {
        if (ident.equals("default")) {
            logger.info("Chosen is default, returning ");
            return "";
        }
        Matcher matcher = PlugUtils.matcher("<input (checked)? type=\"radio\" name=\"mirror\" onclick=\"document.dlf.action=.'(http://rs[0-9]+" + ident + "[^']*).';\" />", content);
        if (matcher.find()) {
            String url = matcher.group(2);
            logger.info("Found preferred url for ident " + ident + " " + url);
            return url;
        } else {
            logger.info("URL for preferred mirror not found, returning default ");
            return "";
        }
    }

    public String getPreferredURL(String content) throws Exception {
        this.content = content;
        logger.info("Checking existing RapidShareMirrorConfig: " + storage.configFileExists(CONFIGFILE));
        if (!storage.configFileExists(CONFIGFILE) || mirrorConfig.getAr().size() < 2) {
            makeMirrorList();
        }

        return findURL(getChosen().getIdent());

    }

    static void initPreferred(RapidShareMirrorConfig mirrorConfig) {
        logger.info("Initializing preferred mirror as default ");

        MirrorBean defaultBean = MirrorBean.createDefault();
        if (!mirrorConfig.getAr().contains(defaultBean)) 
            mirrorConfig.getAr().add(defaultBean);
        mirrorConfig.setChosen(defaultBean);
    }

}
