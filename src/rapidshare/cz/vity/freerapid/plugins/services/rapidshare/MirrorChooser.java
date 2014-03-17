package cz.vity.freerapid.plugins.services.rapidshare;

import cz.vity.freerapid.plugins.webclient.interfaces.ConfigurationStorageSupport;
import cz.vity.freerapid.plugins.webclient.interfaces.DialogSupport;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginContext;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;

import java.util.ArrayList;
import java.util.logging.Logger;
import java.util.regex.Matcher;

class MirrorChooser {
    private final static String CONFIGFILE = "rapidMirror.xml";
    private final static Logger logger = Logger.getLogger(MirrorChooser.class.getName());
    private RapidShareMirrorConfig mirrorConfig;
    private ConfigurationStorageSupport storage;
    private String content;
    private DialogSupport dialogSupport;

    MirrorChooser(PluginContext context, String content) {

        storage = context.getConfigurationStorageSupport();
        dialogSupport = context.getDialogSupport();
        this.content = content;

    }

    private RapidShareMirrorConfig getMirrorConfig() {
        return mirrorConfig;
    }

    String getChoosen() {
        return mirrorConfig.getChoosen();
    }


    private void setPreffered(Object object) {
        if (object instanceof MirrorBean) {
            MirrorBean mirror = (MirrorBean) object;
            mirrorConfig.setChoosen(mirror.getIdent());
        }

    }

    Object[] getArray() {
        return mirrorConfig.getAr().toArray();
    }

    private void add(String name, String ident) {
        MirrorBean m = new MirrorBean();
        m.setName(name);
        m.setIdent(ident);
        mirrorConfig.getAr().add(m);
    }

    private void makeMirrorList() throws Exception {
        mirrorConfig = new RapidShareMirrorConfig();
        mirrorConfig.setAr(new ArrayList<MirrorBean>());
        add("default", "default");
        Matcher matcher = PlugUtils.matcher("<input (checked)? type=\"radio\" name=\"mirror\" onclick=\"document.dlf.action=.'http://rs[0-9]+([^.]+)[^']*.';\" /> ([^<]*)<br", content);
        while (matcher.find()) {

            String mirrorName = matcher.group(3);
            String ident = matcher.group(2);

            logger.info("Mirror " + mirrorName + " ident " + ident);
            add(mirrorName, ident);

        }
           chooseFromList();

        // <input checked type="radio" name="mirror" onclick="document.dlf.action=\'http://rs332gc.rapidshare.com/files/168531395/2434660/rkdr.part3.rar\';" /> GlobalCrossing<br />
    }

    void chooseFromList() throws Exception {
       if(mirrorConfig==null)  mirrorConfig = storage.loadConfigFromFile(CONFIGFILE, RapidShareMirrorConfig.class);
                MirrorChooserUI ms = new MirrorChooserUI(this);
        if (dialogSupport.showOKCancelDialog(ms, "Vyber")) {
            setPreffered(ms.getChoosen());
            storage.storeConfigToFile(getMirrorConfig(), CONFIGFILE);
        }
    }

    private String getMirrorURL(String ident) {
        if (ident.equals("default")) return "";
        Matcher matcher = PlugUtils.matcher("<input (checked)? type=\"radio\" name=\"mirror\" onclick=\"document.dlf.action=.'(http://rs[0-9]+" + ident + "[^']*).';\" />", content);
        if (matcher.find()) {
            String url = matcher.group(2);
            logger.info("Found preferred url for ident " + ident + " " + url);
            return url;
        } else return "";
    }

    public String getPreferredURL() throws Exception {

        logger.info("Checking existing RapidShareMirrorConfig: " + storage.configFileExists(CONFIGFILE));
        if (!storage.configFileExists(CONFIGFILE)) makeMirrorList();

        return readConfig();

    }

    private String readConfig() throws Exception {

        mirrorConfig = storage.loadConfigFromFile(CONFIGFILE, RapidShareMirrorConfig.class);
        return getMirrorURL(getChoosen());

    }

}
