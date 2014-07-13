package cz.vity.freerapid.plugins.services.onetwothreevideo;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.JsonMapper;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author tong2shot
 * @since 0.9u4
 */
class OneTwoThreeVideoFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(OneTwoThreeVideoFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkNameAndSize(getContentAsString());
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        PlugUtils.checkName(httpFile, content, "<div class=\"top\" style=\"margin: 0px;width:320px; float: left; line-height:20px; overflow:hidden;\">", "</");
        httpFile.setFileName(httpFile.getFileName() + ".flv");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkNameAndSize(getContentAsString());

            String movieId = getMovieId(fileURL);
            Crypto crypto = new Crypto(movieId);
            JsonMapper jsonMapper = new JsonMapper();
            HttpMethod httpMethod = getInitPlayerMethod(crypto, jsonMapper);
            if (!makeRedirectedRequest(httpMethod)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            checkProblems();

            String decrypted = crypto.decryptWithKey(getContentAsString());
            ObjectMapper om = jsonMapper.getObjectMapper();
            JsonNode rootNode = om.readTree(decrypted);
            String hash = null;
            String location = null;
            try {
                hash = rootNode.path("Hashes").get(0).getTextValue();
                location = rootNode.path("Locations").get(0).getTextValue();
            } catch (Exception e) {
                //
            }
            if ((hash == null) || (location == null)) {
                throw new PluginImplementationException("Error getting video URL");
            }
            String videoUrl = String.format("http://%s/%s/%s/%d/%d.flv?%s", location, crypto.getPublicKey(), DigestUtils.md5Hex(hash), Integer.parseInt(movieId) / 1000
                    , Integer.parseInt(movieId), crypto.encryptWithKey(String.format("{\"Salt\":\"%s\"}", crypto.getSalt())));
            httpMethod = getGetMethod(videoUrl);
            if (!tryDownloadAndSaveFile(httpMethod)) {
                checkProblems();
                throw new ServiceConnectionProblemException("Error starting download");
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("Video niet gevonden")
                || contentAsString.contains("video is recentelijk verwijderd of bestaat niet")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

    private String getMovieId(String fileUrl) throws PluginImplementationException {
        Matcher matcher = PlugUtils.matcher("MovieID=(\\d+)", fileUrl);
        if (!matcher.find()) {
            throw new PluginImplementationException("Movie ID not found");
        }
        return matcher.group(1);
    }

    private HttpMethod getInitPlayerMethod(Crypto crypto, JsonMapper jsonMapper) throws Exception {
        String jsonStr = getJson(crypto, jsonMapper);
        String requestContent = crypto.encryptWithKey(jsonStr);
        PostMethod method = (PostMethod) getMethodBuilder()
                .setReferer(fileURL)
                .setAction("http://www.123video.nl/initialize_player_v4.aspx")
                .toPostMethod();
        method.setRequestHeader("123videoPlayer", crypto.getPublicKey());
        method.setRequestEntity(new StringRequestEntity(requestContent, "application/x-www-form-urlencoded", "UTF-8"));
        return method;
    }

    private String getJson(Crypto crypto, JsonMapper jsonMapper) throws IOException {
        Map<String, Object> jsonMap = new HashMap<String, Object>();
        jsonMap.put("Random", (int) (Math.random() * 1.0E10));
        jsonMap.put("MovieID", Integer.parseInt(crypto.getMovieId()));
        jsonMap.put("MemberID", 0);
        jsonMap.put("Password", "");
        jsonMap.put("PublicKey", crypto.getPublicKey());
        jsonMap.put("IsEmbedded", false);
        jsonMap.put("EmbedUrl", null);
        jsonMap.put("AdWanted", true);
        jsonMap.put("ExternalInterfaceAvailable", true);
        jsonMap.put("Salt", crypto.getSalt());

        ObjectMapper om = jsonMapper.getObjectMapper();
        return om.writeValueAsString(jsonMap);
    }

}
