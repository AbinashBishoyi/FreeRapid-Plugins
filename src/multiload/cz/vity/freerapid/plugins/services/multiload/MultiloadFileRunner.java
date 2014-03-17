package cz.vity.freerapid.plugins.services.multiload;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import org.apache.commons.httpclient.methods.GetMethod;

import java.net.URI;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpMethod;

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
        String server=MultiloadSettingsPanel.serverNames[config.getServerSetting()];
        if(!getContentAsString().contains(server)) server=MultiloadSettingsPanel.serverNames[3]; //rapidshare is default
        HttpMethod method=getMethodBuilder().setActionFromAHrefWhereATagContains(server).setReferer(fileURL).setBaseURL("http://www.multiload.cz/").toGetMethod();
        method.setFollowRedirects(false);
        client.getHTTPClient().executeMethod(method);
        Header h=method.getResponseHeader("location");
        String targetLink="";
        if(h!=null) targetLink=h.getValue();
        if("".equals(targetLink))
            throw new ServiceConnectionProblemException("Cannot find link");
        method.releaseConnection();
        final List<URI> uriList = new LinkedList<URI>();
        uriList.add(new URI(targetLink));
        getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(httpFile, uriList);
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