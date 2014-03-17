package cz.vity.freerapid.plugins.services.miroriii;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadState;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.methods.GetMethod;

import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Class which contains main code
 *
 * @author ntoskrnl
 */
class MiroriiiFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(MiroriiiFileRunner.class.getName());
    private final HashMap<String, String> servicePattern = new HashMap<String, String>();
    private final HashMap<String, String[]> serviceErrorMessages = new HashMap<String, String[]>();
    private MiroriiiSettingsConfig config;

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        fileURL = fileURL.replace("miroriii", "miroriii");
        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkNameAndSize();
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    @Override
    public void run() throws Exception {
        super.run();
        fileURL = fileURL.replace("miroriii", "miroriii");
        logger.info("Starting download in TASK " + fileURL);
        setConfig();
        prepareMaps();
        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkNameAndSize();
            if (parseWebsite()) {
                return;
            }
            throw new URLNotAvailableAnymoreException("File not available anymore; All links expired");
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String content = getContentAsString();
        if (content.contains("Désolé ce fichier est introuvable.")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

    private void checkNameAndSize() throws ErrorDuringDownloadingException {
        final String content = getContentAsString().replace(" go", " gb").replace(" mo", " mb").replace(" ko", " kb");
        PlugUtils.checkName(httpFile, content, "Nom du fichier :</td>\n" +
                "    <td class=\"right\">", "</td>");
        PlugUtils.checkFileSize(httpFile, content, "Taille du fichier :</td>\n" +
                "    <td  class=\"right\">\n" +
                "      ", "    </td>");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    private void setConfig() throws Exception {
        MiroriiiServiceImpl service = (MiroriiiServiceImpl) getPluginService();
        config = service.getConfig();
    }

    private void prepareMaps() {
        servicePattern.put("MegaUpload.com", "http://(www\\.)?(megaupload|megarotic|megaporn|sexuploader)\\.com/\\?[df]=.+");
        servicePattern.put("RapidShare.com", "https?://((www\\.)|(rs[0-9]+\\.))?rapidshare\\.com/files/[0-9]+/.*");
        servicePattern.put("HotFile.com", "https?://.*?hotfile\\.com/(dl|links)/.+");
        servicePattern.put("Uploaded.to", "http://(www\\.)?(uploaded|ul)\\.to/.+");
        servicePattern.put("Free.fr", "http://dl\\.free\\.fr/.+");
        servicePattern.put("Sharehoster", "http://www.sharehoster.com/dl/.+");

        serviceErrorMessages.put("RapidShare.com", new String[]{"has removed file", "file could not be found", "illegal content", "file has been removed", "limit is reached", "Currently a lot of users", "no more download slots", "our servers are overloaded", "You have reached the", "is already", "you either need a Premium Account", "momentarily not available", "This file is larger than"});
        serviceErrorMessages.put("MegaUpload.com", new String[]{"trying to access is temporarily unavailable", "Download limit exceeded", "All download slots", "to download is larger than", "the link you have clicked is not available", "We have detected an elevated number of requests"});
        serviceErrorMessages.put("HotFile.com", new String[]{"404 - Not Found", "File not found", "removed due to copyright", "document.getElementById('dwltmr"});
        serviceErrorMessages.put("Uploaded.to", new String[]{"The file status can only be queried by premium users"});
        serviceErrorMessages.put("Free.fr", new String[]{"Fichier inexistant."});
        serviceErrorMessages.put("Sharehoster", new String[]{"Votre téléchargement n'est malheureusement pas disponible pour le moment!", "? ?????????, ???? ?????? ? ????????? ????? ?? ????????.", "Your download is currently unavailable!"});

    }

    private boolean checkService(final String service, final String url) throws Exception {

        /*final HttpMethod httpMethod = getMethodBuilder().setReferer(fileURL).setAction(turl).toGetMethod();
        if (client.makeRequest(httpMethod, false) / 100 != 3)
            throw new ServiceConnectionProblemException("Problem with redirection");
        final Header locationHeader = httpMethod.getResponseHeader("Location");
        if (locationHeader == null)
            throw new ServiceConnectionProblemException("Could not find redirect location");
        final String url = locationHeader.getValue();
        if (url.equals("http://www.miroriii.com/"))
            return false;*/

        if (config.getCheckDownloadService()) {
            logger.info("Checking for errors on download service");
            if (!checkDownloadService(service, url))
                return false;
        } else {
            logger.info("Skipping check of download service");
        }

        logger.info("URL: " + url + " : OK");
        return true;
    }

    private boolean checkDownloadService(final String service, final String url) throws Exception {
        if (!makeRedirectedRequest(getGetMethod(url)))
            return false;
        final String content = getContentAsString();

        for (final String errorMessage : serviceErrorMessages.get(service)) {
            if (content.contains(errorMessage))
                return false;
        }
        return true;
    }

    //Add
    private boolean parseWebsite() throws Exception {
        final String codehtml = getContentAsString();
        final List<URI> links = new LinkedList<URI>();
        final List<String> matchList = new ArrayList<String>();
        boolean found = false;
        try {
            links.clear();
            matchList.clear();
            Pattern regex = Pattern.compile("\\b(https?|ftp)://([-A-Z0-9.]+)(/[-A-Z0-9+&@#/%=~_|!:,.;]*)?(\\?[A-Z0-9+&@#/%=~_|!:,.;]*)?", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
            Matcher regexMatcher = regex.matcher(codehtml);
            while (regexMatcher.find()) {
                matchList.add(regexMatcher.group());
            }

            for (final String service : config.getServices()) {
                regex = Pattern.compile(servicePattern.get(service), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
                for (final String test : matchList) {
                    regexMatcher = regex.matcher(test);
                    while (regexMatcher.find()) {
                        if (checkService(service, test)) {
                            logger.info("New URL: " + test);
                            if (config.getAddLink() || service.contains("RapidShare.com")) {
                                links.add(new URI(test));
                            } else {
                                httpFile.setNewURL(new URL(test));
                                httpFile.setPluginID("");
                                httpFile.setState(DownloadState.QUEUED);
                                return true;
                            }
                            found = true;
                        }
                    }
                }
                if (found) break;
            }
        } catch (PatternSyntaxException ex) {
            // Syntax error in the regular expression
        }
        if (found) {
            getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(httpFile, links);
            links.clear();
            return true;
        }
        return false;
    }
    //End Add

}