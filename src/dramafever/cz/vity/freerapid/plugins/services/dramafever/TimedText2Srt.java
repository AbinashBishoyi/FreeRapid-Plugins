package cz.vity.freerapid.plugins.services.dramafever;

import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import cz.vity.freerapid.utilities.LogUtils;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.util.logging.Logger;

/**
 * @author tong2shot
 */
public class TimedText2Srt {
    private final static Logger logger = Logger.getLogger(TimedText2Srt.class.getName());

    private TimedText2Srt() {
    }

    public static String convert(String timedTextXml) {
        StringBuilder subtitleSb = new StringBuilder();
        try {
            Element body = (Element) DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new ByteArrayInputStream(timedTextXml.getBytes("UTF-8"))).getElementsByTagName("body").item(0);
            NodeList pElements = body.getElementsByTagName("p");
            for (int i = 0, pElementsLength = pElements.getLength(); i < pElementsLength; i++) {
                Element pElement = (Element) pElements.item(i);
                subtitleSb.append(i + 1).append("\n");
                subtitleSb.append(toTimeFormat(pElement.getAttribute("begin")));
                subtitleSb.append(" --> ");
                subtitleSb.append(toTimeFormat(pElement.getAttribute("end")));
                subtitleSb.append("\n");
                addSubtitleElement(subtitleSb, pElement.getChildNodes(), pElement.getChildNodes().getLength(), 0);
                subtitleSb.append("\n\n");
            }
        } catch (Exception e) {
            LogUtils.processException(logger, e);
        }
        return subtitleSb.toString();
    }

    private static void addSubtitleElement(StringBuilder sb, NodeList childNodes, int childNodesLength, int childNodesCounter) throws PluginImplementationException {
        if (childNodesCounter < childNodesLength) {
            Node childNode = childNodes.item(childNodesCounter);
            if (childNode.getNodeName().equals("br")) {
                sb.append("\n");
            } else if (childNode.getNodeName().equals("#text")) {
                sb.append(PlugUtils.unescapeUnicode(childNode.getNodeValue().trim()));
            } else if (childNode.getNodeName().equals("span")) {
                addSubtitleElement(sb, childNode.getChildNodes(), childNode.getChildNodes().getLength(), 0);
            }
            addSubtitleElement(sb, childNodes, childNodesLength, childNodesCounter + 1);
        }
    }

    private static String toTimeFormat(String value) {
        String[] split1 = value.replace(".", ",").split(",");
        String hms = split1[0];
        int millis = (split1.length == 1 ? 0 : Integer.parseInt(split1[1]));
        int h, m, s;
        String[] split2 = hms.split(":");
        h = Integer.parseInt(split2[0]);
        m = Integer.parseInt(split2[1]);
        s = Integer.parseInt(split2[2]);
        return String.format("%02d:%02d:%02d,%03d", h, m, s, millis);
    }
}
