package cz.vity.freerapid.plugins.services.flickr;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadClientConsts;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import cz.vity.freerapid.utilities.LogUtils;
import org.apache.commons.httpclient.HttpMethod;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;


/**
 * Class which contains main code
 *
 * @author Arthur
 * @author tong2shot
 */
class FlickrFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(FlickrFileRunner.class.getName());
    public static final String API_KEY = "f2949bc8f2e7d566279784478033e72a";
    public static final String METHOD_PHOTOS_GET_SIZES = "flickr.photos.getSizes";
    public static final String METHOD_PHOTOS_GET_INFO = "flickr.photos.getInfo";
    public static final String METHOD_PHOTOSETS_GET_PHOTOS = "flickr.photosets.getPhotos";
    public static final String METHOD_GALLERIES_GET_PHOTOS = "flickr.galleries.getPhotos";
    public static final String METHOD_URLS_LOOKUP_GALLERY = "flickr.urls.lookupGallery";
    public static final String METHOD_URLS_LOOKUP_USER = "flickr.urls.lookupUser";
    public static final String METHOD_FAVORITES_GET_PUBLIC_LIST = "flickr.favorites.getPublicList";

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        setFileStreamContentTypes(new String[0], new String[]{"application/json"});
        if (isPhotoSets()) {
            parsePhotoSets();
        } else if (isGalleries()) {
            parseGalleries();
        } else if (isFavorites()) {
            parseFavorites();
        } else {
            downloadContent();
        }
    }

    private void downloadContent() throws Exception {
        final String photoId = getPhotoIdFromURL();
        //Step #1 Get title and media type
        //reference : http://www.flickr.com/services/api/flickr.photos.getInfo.html
        HttpMethod method = getFlickrMethodBuilder(METHOD_PHOTOS_GET_INFO)
                .setParameter("photo_id", photoId)
                .toGetMethod();
        if (!makeRedirectedRequest(method)) {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
        checkProblems();
        String title = PlugUtils.unescapeUnicode(PlugUtils.getStringBetween(getContentAsString(), "\"title\":{\"_content\":\"", "\""));
        final String media = PlugUtils.getStringBetween(getContentAsString(), "\"media\":\"", "\"");
        if (title.length() > 200) {
            title = title.substring(0, 199);
        }

        //Step #2 Get the best quality content
        //reference : http://www.flickr.com/services/api/flickr.photos.getSizes.html
        method = getFlickrMethodBuilder(METHOD_PHOTOS_GET_SIZES)
                .setParameter("photo_id", getPhotoIdFromURL())
                .toGetMethod();
        if (!makeRedirectedRequest(method)) {
            checkProblems();
            throw new ServiceConnectionProblemException("Connection Error");
        }
        checkProblems();
        final Matcher matcher = getMatcherAgainstContent("\"source\":\"(.+?)\", ?\"url\":\"(.+?)\"");
        String source = null;
        String url = null;
        //the best quality is the last one
        while (matcher.find()) {
            source = matcher.group(1).replace("\\/", "/");
            url = matcher.group(2).replace("\\/", "/");
        }
        if (source == null) {
            throw new PluginImplementationException("Unable to find download url");
        }
        final String fileExtension = media.equals("video") ? ".mp4" : source.substring(source.lastIndexOf("."));
        httpFile.setFileName(title + fileExtension);
        method = getMethodBuilder()
                .setReferer(url)
                .setAction(source)
                .toGetMethod();
        setClientParameter(DownloadClientConsts.DONT_USE_HEADER_FILENAME, true);
        if (!tryDownloadAndSaveFile(method)) {
            checkProblems();
            if (method.getURI().toString().contains("/photo_unavailable")) {
                throw new URLNotAvailableAnymoreException("File not found");
            }
            throw new ServiceConnectionProblemException("Error starting download");
        }
    }

    private MethodBuilder getFlickrMethodBuilder(final String method) throws BuildMethodException {
        return getMethodBuilder()
                .setAction("https://api.flickr.com/services/rest/")
                .setParameter("method", method)
                .setParameter("api_key", API_KEY)
                .setParameter("format", "json")
                .setParameter("nojsoncallback", "1");
    }

    //Do not confuse with getUserId()
    private String getUserIdFromURL() throws PluginImplementationException {
        final Matcher matcher = PlugUtils.matcher("/photos/([^/]+)/?.*", fileURL);
        if (!matcher.find()) {
            throw new PluginImplementationException("Can't get user ID");
        }
        return matcher.group(1);
    }

    private LinkedList<URI> getURIList(final String action, final String URIRegex, final boolean isOwnerInRegex) throws Exception {
        final LinkedList<URI> uriList = new LinkedList<URI>();
        final String userIdFromURL = getUserIdFromURL();
        int page = 1;
        int numberOfPages = 0;
        do {
            final HttpMethod method = getMethodBuilder()
                    .setReferer(fileURL)
                    .setAction(action)
                    .setParameter("page", String.valueOf(page))
                    .toGetMethod();
            if (!makeRedirectedRequest(method)) {
                checkProblems();
                throw new ServiceConnectionProblemException("Connection Error");
            }
            if (0 == numberOfPages) {
                numberOfPages = PlugUtils.getNumberBetween(getContentAsString(), "\"pages\":", ",");
            }
            final Matcher matcher = getMatcherAgainstContent(URIRegex);
            while (matcher.find()) {
                try {
                    final String owner = isOwnerInRegex ? matcher.group(2) : userIdFromURL;
                    uriList.add(new URI(String.format("http://www.flickr.com/photos/%s/%s/", owner, matcher.group(1))));
                } catch (final URISyntaxException e) {
                    LogUtils.processException(logger, e);
                }
            }
        } while (page++ < numberOfPages);
        return uriList;
    }

    private void queueLinks(List<URI> uriList) throws PluginImplementationException {
        if (uriList.isEmpty()) {
            throw new PluginImplementationException("No links found");
        }
        getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(httpFile, uriList);
        httpFile.getProperties().put("removeCompleted", true);
        logger.info(uriList.size() + " photos added");
    }

    private String getPhotoIdFromURL() throws Exception {
        final Matcher matcher = PlugUtils.matcher("/photos/.+/([0-9]+)/?", fileURL);
        if (!matcher.find()) {
            throw new PluginImplementationException("Can't get photo ID");
        }
        return matcher.group(1);
    }


    private boolean isPhotoSets() {
        return fileURL.contains("/sets/");
    }

    private String getPhotoSetIdFromURL() throws PluginImplementationException {
        final Matcher matcher = PlugUtils.matcher("/sets/([0-9]+)/?", fileURL);
        if (!matcher.find()) {
            throw new PluginImplementationException("Can't get photosets ID");
        }
        return matcher.group(1);
    }

    //reference : http://www.flickr.com/services/api/flickr.photosets.getPhotos.html
    private void parsePhotoSets() throws Exception {
        final String action = getFlickrMethodBuilder(METHOD_PHOTOSETS_GET_PHOTOS)
                .setParameter("photoset_id", getPhotoSetIdFromURL())
                .getEscapedURI();
        final List<URI> uriList = getURIList(action, "\"id\":\"(\\d+)\", \"secret\"", false);
        queueLinks(uriList);
    }

    private boolean isGalleries() {
        return fileURL.contains("/galleries/");
    }

    //reference : http://www.flickr.com/services/api/flickr.urls.lookupGallery.html
    private String getGalleryId() throws Exception {
        final HttpMethod method = getFlickrMethodBuilder(METHOD_URLS_LOOKUP_GALLERY)
                .setParameter("url", fileURL)
                .toGetMethod();
        if (!makeRedirectedRequest(method)) {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
        return PlugUtils.getStringBetween(getContentAsString(), "\"id\":\"", "\"");
    }

    //reference : http://www.flickr.com/services/api/flickr.galleries.getPhotos.html
    private void parseGalleries() throws Exception {
        final String action = getFlickrMethodBuilder(METHOD_GALLERIES_GET_PHOTOS)
                .setParameter("gallery_id", getGalleryId())
                .getEscapedURI();
        final List<URI> uriList = getURIList(action, "\"id\":\"(\\d+)\", \"owner\":\"(.+?)\", \"secret\"", true);
        queueLinks(uriList);
    }

    private boolean isFavorites() {
        return fileURL.endsWith("/favorites/") || fileURL.endsWith("/favorites");
    }

    //reference : http://www.flickr.com/services/api/flickr.urls.lookupUser.html
    //Do not confuse with getUserIdFromURL()
    private String getUserId() throws Exception {
        final HttpMethod method = getFlickrMethodBuilder(METHOD_URLS_LOOKUP_USER)
                .setParameter("url", fileURL)
                .toGetMethod();
        if (!makeRedirectedRequest(method)) {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
        return PlugUtils.getStringBetween(getContentAsString(), "\"id\":\"", "\"");
    }

    //reference : http://www.flickr.com/services/api/flickr.favorites.getPublicList.html
    private void parseFavorites() throws Exception {
        final String action = getFlickrMethodBuilder(METHOD_FAVORITES_GET_PUBLIC_LIST)
                .setParameter("user_id", getUserId())
                .getEscapedURI();
        final List<URI> uriList = getURIList(action, "\"id\":\"(\\d+)\", \"owner\":\"(.+?)\", \"secret\"", true);
        queueLinks(uriList);
    }


    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("File Not Found")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
        if (contentAsString.contains("Invalid gallery ID")) {
            throw new PluginImplementationException("Invalid gallery ID");
        }
    }

}