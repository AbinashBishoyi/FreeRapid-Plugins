package cz.vity.freerapid.plugins.services.multiload;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.methods.GetMethod;

import java.net.URI;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Class which contains main code
 *
 * @author Vity+JPEXS
 */
class MultiloadFileRunner extends AbstractRunner {

    private final static Logger logger = Logger.getLogger(MultiloadFileRunner.class.getName());
    private MultiloadSettingsConfig config;

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL); //create GET request
        if (makeRedirectedRequest(method)) { //we make the main request
            checkProblems();//check problems
            parseWebsite();
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void parseWebsite() throws Exception {
        setConfig();
        String server = MultiloadSettingsPanel.serverNames[(config.getServerSetting()+1)%7];
        String linksGroup = "";
        if (server.equals("multishare.cz")) {
            linksGroup = PlugUtils.getStringBetween(getContentAsString(), "<p class=\"manager-linky multishare-kod\">", "</p>");
        } else {
            linksGroup = PlugUtils.getStringBetween(getContentAsString(), "<p class=\"manager-server\"><strong>" + server + "</strong></p><p class=\"manager-linky\">", "</p>");
        }
        final List<URI> uriList = new LinkedList<URI>();
        boolean found = false;
        if (linksGroup.contains("<br>")) {
            String links[] = linksGroup.split("<br>");
            for (String link : links) {
                if (link.matches("http[^ ]+")) {
                    uriList.add(new URI(link));
                    found = true;
                }
            }
        } else {
            if (linksGroup.matches("http[^ ]+")) {
                uriList.add(new URI(linksGroup));
                found = true;
            }
        }
        if (found) {
            getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(httpFile, uriList);
        } else {
            if (linksGroup.contains("(chyba serveru)")) {
                throw new NotSupportedDownloadByServiceException("Chyba serveru " + server);
            } else {
                throw new PluginImplementationException("Cannot parse links");
            }
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("soubor neexistuje")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
    }

    private void setConfig() throws Exception {
        MultiloadServiceImpl service = (MultiloadServiceImpl) getPluginService();
        config = service.getConfig();
    }
}
