package cz.vity.freerapid.plugins.services.adobehds;

import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpDownloadClient;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpStatus;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

/**
 * @author ntoskrnl
 */
class HdsManifest {

    private static final Logger logger = Logger.getLogger(HdsManifest.class.getName());

    private final List<HdsMedia> medias;

    public HdsManifest(final HttpDownloadClient client, final String manifestUrl) throws IOException {
        try {
            final XPath xpath = XPathFactory.newInstance().newXPath();
            final F4mManifest manifest = F4mManifest.fetch(client, manifestUrl, xpath);
            final List<HdsMedia> medias = new ArrayList<HdsMedia>();
            final MediaNode firstMediaNode = manifest.getFirstMediaNode();
            if (!firstMediaNode.getHref().isEmpty()) {
                for (final MediaNode media : manifest.getMediaNodes()) {
                    final String href = getUrl(manifestUrl, manifest.getBaseUrl(), media.getHref());
                    final F4mManifest childManifest = F4mManifest.fetch(client, href, xpath);
                    final MediaNode childMedia = childManifest.getFirstMediaNode();
                    final BootstrapInfo info = childManifest.getBootstrapInfo(childMedia.getBootstrapInfoId());
                    final String url = getUrl(href, childManifest.getBaseUrl(), childMedia.getUrl());
                    medias.add(new HdsMedia(url, manifest.getUrlQuery(), media.getBitrate(), info.getFragmentCount()));
                }
            } else {
                for (final MediaNode media : manifest.getMediaNodes()) {
                    final BootstrapInfo info = manifest.getBootstrapInfo(media.getBootstrapInfoId());
                    final String url = getUrl(manifestUrl, manifest.getBaseUrl(), media.getUrl());
                    medias.add(new HdsMedia(url, manifest.getUrlQuery(), media.getBitrate(), info.getFragmentCount()));
                }
            }
            logger.info("Found medias: " + medias.toString());
            this.medias = Collections.unmodifiableList(medias);
        } catch (final Exception e) {
            throw new IOException("Failed to parse manifest", e);
        }
    }

    public List<HdsMedia> getMedias() {
        return medias;
    }

    private String getUrl(final String manifestUrl, String baseUrl, final String url) throws Exception {
        if (baseUrl.isEmpty()) {
            baseUrl = manifestUrl;
        }
        return new URI(baseUrl).resolve(new URI(url)).toString();
    }

    private static class F4mManifest {
        private final Document document;
        private final XPath xpath;

        private F4mManifest(final Document document, final XPath xpath) {
            this.document = document;
            this.xpath = xpath;
        }

        public String getBaseUrl() throws Exception {
            return xpath.evaluate("/manifest/baseURL/text()", document);
        }

        public String getUrlQuery() throws Exception {
            String ret = xpath.evaluate("/manifest/pv-2.0", document);
            return ret.isEmpty() ? null : ret.replaceFirst("^;", "");
        }

        public List<MediaNode> getMediaNodes() throws Exception {
            final NodeList mediaNodeList = (NodeList) xpath.evaluate("/manifest/media", document, XPathConstants.NODESET);
            final List<MediaNode> mediaNodes = new ArrayList<MediaNode>();
            for (int i = 0; i < mediaNodeList.getLength(); i++) {
                mediaNodes.add(MediaNode.create(mediaNodeList.item(i), xpath));
            }
            return mediaNodes;
        }

        public MediaNode getFirstMediaNode() throws Exception {
            return MediaNode.create((Node) xpath.evaluate("/manifest/media", document, XPathConstants.NODE), xpath);
        }

        public BootstrapInfo getBootstrapInfo(final String bootstrapInfoId) throws Exception {
            if (!bootstrapInfoId.matches("[A-Za-z0-9_]*")) {
                throw new IOException("Invalid bootstrap info id: " + bootstrapInfoId);
            }
            final String bootstrapInfo = xpath.evaluate("/manifest/bootstrapInfo[@id='" + bootstrapInfoId + "']/text()", document);
            if (bootstrapInfo.isEmpty()) {
                throw new IOException("Bootstrap info not found: " + bootstrapInfoId);
            }
            return new BootstrapInfo(new DataInputStream(new ByteArrayInputStream(Base64.decodeBase64(bootstrapInfo))));
        }

        public static F4mManifest fetch(final HttpDownloadClient client, final String url, final XPath xpath) throws Exception {
            logger.info("Manifest URL: " + url);
            final HttpMethod method = client.getGetMethod(url);
            if (client.makeRequest(method, true) != HttpStatus.SC_OK) {
                throw new ServiceConnectionProblemException();
            }
            final InputStream in = new ByteArrayInputStream(client.getContentAsString().getBytes("UTF-8"));
            final Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(in);
            return new F4mManifest(document, xpath);
        }
    }

    private static class MediaNode {
        private final String url;
        private final int bitrate;
        private final String bootstrapInfoId;
        private final String href;

        private MediaNode(final String url, final int bitrate, final String bootstrapInfoId, final String href) {
            this.url = url;
            this.bitrate = bitrate;
            this.bootstrapInfoId = bootstrapInfoId;
            this.href = href;
        }

        public String getUrl() {
            return url;
        }

        public int getBitrate() {
            return bitrate;
        }

        public String getBootstrapInfoId() {
            return bootstrapInfoId;
        }

        public String getHref() {
            return href;
        }

        public static MediaNode create(final Node media, final XPath xpath) throws Exception {
            final String url = xpath.evaluate("./@url", media);
            final int bitrate = getBitrate(media, xpath);
            final String bootstrapInfoId = xpath.evaluate("./@bootstrapInfoId", media);
            final String href = xpath.evaluate("./@href", media);
            return new MediaNode(url, bitrate, bootstrapInfoId, href);
        }

        private static int getBitrate(final Node media, final XPath xpath) throws Exception {
            final String str = xpath.evaluate("./@bitrate", media);
            if (str.isEmpty()) {
                return 0;
            }
            return Integer.parseInt(str);
        }
    }

}
