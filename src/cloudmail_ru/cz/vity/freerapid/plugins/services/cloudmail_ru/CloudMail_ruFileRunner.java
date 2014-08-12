package cz.vity.freerapid.plugins.services.cloudmail_ru;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.JsonMapper;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.UnsupportedEncodingException;
import java.net.*;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author tong2shot
 * @since 0.9u4
 */
class CloudMail_ruFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(CloudMail_ruFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {
            fileURL = getMethod.getURI().toString(); // /weblink/ redirected to /public/
            checkProblems();
            checkNameAndSize(getListNode(getContentAsString(), getFileId(fileURL)));
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize(JsonNode listNode) throws ErrorDuringDownloadingException {
        String filename = listNode.findPath("name").getTextValue();
        if (filename == null) {
            throw new PluginImplementationException("File name not found");
        }
        if (!isFolder(listNode)) {
            httpFile.setFileName(filename);
            long filesize = listNode.findPath("size").getLongValue();
            if (filesize == 0) {
                throw new PluginImplementationException("File size not found");
            }
            httpFile.setFileSize(filesize);
        } else {
            httpFile.setFileName(filename + " >> Ready to extract");
        }
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        HttpMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            fileURL = method.getURI().toString(); // /weblink/ redirected to /public/
            checkProblems();
            JsonNode listNode = getListNode(getContentAsString(), getFileId(fileURL));
            checkNameAndSize(listNode);
            if (isFolder(listNode)) {
                parseFolder(listNode);
            } else {
                String downloadUrl = listNode.findPath("url").findPath("get").getTextValue();
                if (downloadUrl == null) {
                    throw new PluginImplementationException("Error getting download URL");
                }
                method = getMethodBuilder().setReferer(fileURL).setAction(downloadUrl).toHttpMethod();
                if (!tryDownloadAndSaveFile(method)) {
                    checkProblems();
                    throw new ServiceConnectionProblemException("Error starting download");
                }
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("\"error\":\"not_exists\"")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

    private void checkFileProblemsApi(String fileId) throws Exception {
        String apiUrl = "https://cloud.mail.ru/api/v1/folder/recursive?storage=public&id=" + URLEncoder.encode(fileId, "UTF-8")
                + "&sort=%7B%22type%22%3A%22name%22%2C%22order%22%3A%22asc%22%7D&api=1&htmlencoded=false&build=hotfix-21-11.201408051855";
        GetMethod method = getGetMethod(apiUrl);
        if (!makeRedirectedRequest(method)) {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
        checkProblems();
    }

    private String getFileId(String fileUrl) throws MalformedURLException, UnsupportedEncodingException {
        URL url = new URL(fileUrl);
        String fileId = URLDecoder.decode(url.getPath().replaceFirst("^/public/", ""), "UTF-8");
        logger.info("File ID: " + fileId);
        return fileId;
    }

    private JsonNode getListNode(String content, String fileId) throws Exception {
        Matcher matcher = PlugUtils.matcher("(?s)cloudBuilder\\((.+?)\\);", content);
        if (!matcher.find()) {
            throw new PluginImplementationException("Error getting JSON content");
        }
        String jsonContent = matcher.group(1);

        ObjectMapper mapper = new JsonMapper().getObjectMapper();
        JsonNode rootNode;
        try {
            rootNode = mapper.readTree(jsonContent);
        } catch (Exception e) {
            throw new PluginImplementationException("Error parsing JSON");
        }

        JsonNode foldersNodes = rootNode.findPath("folders");
        if (foldersNodes.isMissingNode()) {
            checkFileProblemsApi(fileId);
            throw new PluginImplementationException("Error getting folders node");
        }
        JsonNode selectedListNode = null;
        for (JsonNode foldersNode : foldersNodes) {
            JsonNode listNodes = foldersNode.findPath("list");
            for (JsonNode listNode : listNodes) {
                if (listNode.get("id").getTextValue().equals(fileId)) {
                    selectedListNode = listNode;
                    break;
                }
            }
        }
        if (selectedListNode == null) {
            throw new PluginImplementationException("Unable to select list node");
        }
        return selectedListNode;
    }

    private boolean isFolder(JsonNode listNode) {
        return listNode.findPath("kind").getTextValue().equals("folder");
    }

    private void parseFolder(JsonNode listNode) throws Exception {
        List<URI> list = new LinkedList<URI>();
        JsonNode itemsNode = listNode.findPath("items");
        for (JsonNode item : itemsNode) {
            list.add(new URI("https://cloud.mail.ru/public/" + item.getTextValue()));
        }
        if (list.isEmpty()) {
            throw new PluginImplementationException("No links found");
        }
        getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(httpFile, list);
        logger.info(list.size() + " links added");
        httpFile.getProperties().put("removeCompleted", true);
    }
}
