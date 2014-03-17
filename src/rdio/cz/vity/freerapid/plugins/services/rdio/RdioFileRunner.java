package cz.vity.freerapid.plugins.services.rdio;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.services.tunlr.Tunlr;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.hoster.PremiumAccount;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import cz.vity.freerapid.utilities.LogUtils;
import org.apache.commons.httpclient.HttpMethod;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author ntoskrnl
 */
class RdioFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(RdioFileRunner.class.getName());

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        login();
        final String authorizationKey = PlugUtils.getStringBetween(getContentAsString(), "\"authorizationKey\": \"", "\"");
        HttpMethod method = getMethodBuilder()
                .setReferer(fileURL)
                .setAction("http://www.rdio.com/api/1/")
                .setParameter("url", new URI(fileURL).getRawPath().replaceAll("%28", "(").replaceAll("%29", ")"))
                .setParameter("extras", "tracks")
                .setParameter("method", "getObjectFromUrl")
                .setParameter("_authorization_key", authorizationKey)
                .setHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8") //httpclient-3 messes up non-ascii characters in the url parameter without this
                .toPostMethod();
        if (!makeRedirectedRequest(method)) {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
        logger.info(getContentAsString());
        checkProblems();
        if (getContentAsString().contains("\"type\": \"a\"")) {
            parseAlbum();
            return;
        }
        checkNameAndSize();
        Matcher matcher = getMatcherAgainstContent("\"key\": \"(\\w+?)\",");
        if (!matcher.find()) {
            throw new PluginImplementationException("Unable to find key");
        }
        final String id = matcher.group(1);
        method = getMethodBuilder()
                .setReferer(fileURL)
                .setAction("http://www.rdio.com/api/1/")
                .setParameter("key", id)
                .setParameter("manualPlay", "true")
                .setParameter("type", "mp3-high")
                .setParameter("playerName", "_web_" + new Random().nextInt(10000000))
                .setParameter("requiresUnlimited", "false")
                .setParameter("method", "getPlaybackInfo")
                .setParameter("_authorization_key", authorizationKey)
                .toPostMethod();
        if (!client.getSettings().isProxySet()) {
            Tunlr.setupMethod(method);
        }
        if (!makeRedirectedRequest(method)) {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
        logger.info(getContentAsString());
        checkProblems();
        method = getMethodBuilder()
                .setReferer(fileURL)
                .setActionFromTextBetween("\"surl\": \"", "\"")
                .toGetMethod();
        if (!tryDownloadAndSaveFile(method)) {
            checkProblems();
            throw new ServiceConnectionProblemException("Error starting download");
        }
    }

    private void checkNameAndSize() throws ErrorDuringDownloadingException {
        final String artist = PlugUtils.getStringBetween(getContentAsString(), "\"artist\": \"", "\"");
        final String name = PlugUtils.getStringBetween(getContentAsString(), "\"name\": \"", "\"");
        httpFile.setFileName(PlugUtils.unescapeUnicode(artist + " - " + name + ".mp3"));
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        if (getContentAsString().contains("Object not found")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
        if (getContentAsString().contains("No permissions to stream")) {
            throw new PluginImplementationException("No permissions to stream");
        }
    }

    private void parseAlbum() throws Exception {
        final List<URI> list = new LinkedList<URI>();
        final Matcher matcher = getMatcherAgainstContent("\"url\": \"(.+?)\"");
        final URI baseUrl = new URI("http://www.rdio.com");
        while (matcher.find()) {
            final String url = matcher.group(1);
            if (url.contains("/track/")) {
                try {
                    list.add(baseUrl.resolve(new URI(url)));
                } catch (final URISyntaxException e) {
                    LogUtils.processException(logger, e);
                }
            }
        }
        if (list.isEmpty()) {
            throw new PluginImplementationException("No links found");
        }
        getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(httpFile, list);
        httpFile.getProperties().put("removeCompleted", true);
    }

    private void login() throws Exception {
        synchronized (RdioFileRunner.class) {
            RdioServiceImpl service = (RdioServiceImpl) getPluginService();
            PremiumAccount pa = service.getConfig();
            if (!pa.isSet()) {
                pa = new PremiumAccount();
                //test account details - feel free to use
                pa.setUsername("coolbub");
                pa.setPassword(".");
            }
            HttpMethod method = getMethodBuilder()
                    .setReferer(fileURL)
                    .setAction("https://www.rdio.com/account/signin/")
                    .toGetMethod();
            if (!makeRedirectedRequest(method)) {
                throw new ServiceConnectionProblemException();
            }
            final String authorizationKey = PlugUtils.getStringBetween(getContentAsString(), "\"authorizationKey\": \"", "\"");
            method = getMethodBuilder()
                    .setReferer(fileURL)
                    .setAction("https://www.rdio.com/api/1/")
                    .setParameter("username", pa.getUsername())
                    .setParameter("password", pa.getPassword())
                    .setParameter("remember", "1")
                    .setParameter("method", "signIn")
                    .setParameter("_authorization_key", authorizationKey)
                    .toPostMethod();
            if (!makeRedirectedRequest(method)) {
                throw new ServiceConnectionProblemException();
            }
            if (!getContentAsString().contains("\"status\": \"ok\"")) {
                throw new BadLoginException("Invalid Rdio account login information!");
            }
            method = getMethodBuilder()
                    .setReferer(fileURL)
                    .setActionFromTextBetween("\"redirect_url\": \"", "\"")
                    .toGetMethod();
            if (!makeRedirectedRequest(method)) {
                throw new ServiceConnectionProblemException();
            }
        }
    }

}