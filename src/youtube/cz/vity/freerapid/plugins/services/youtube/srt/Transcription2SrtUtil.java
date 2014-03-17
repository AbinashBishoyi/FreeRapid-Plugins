package cz.vity.freerapid.plugins.services.youtube.srt;

import jlibs.xml.sax.binding.BindingHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

/**
 * @author Vity
 */
public class Transcription2SrtUtil {
    /**
     * Converts XML transcription file into Srt file
     * @param transcriptionXml data
     * @return converted string
     */
    public static String convert(String transcriptionXml) throws IOException, SAXException, ParserConfigurationException {
        BindingHandler handler = new BindingHandler(TranscriptionBinding.class);
        final List<SrtItem> list = (List<SrtItem>) handler.parse(new InputSource(new ByteArrayInputStream(transcriptionXml.getBytes("UTF-8"))));
        StringBuilder result = new StringBuilder();

        for (SrtItem item : list) {
            result.append(item);
        }
        return result.toString();
    }
}
