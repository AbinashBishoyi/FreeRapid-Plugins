package com.googlecode.mp4parser.authoring.builder;

import com.coremedia.iso.boxes.*;
import com.coremedia.iso.boxes.apple.AppleItemListBox;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.boxes.apple.AppleEncoderBox;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

/**
 * @author tong2shot
 */
public class ItunesBuilder extends DefaultMp4Builder {
    private String text;

    public ItunesBuilder() {
        this("FreeRapid Downloader - mp4parser");
    }

    public ItunesBuilder(String text) {
        this.text = text;
    }

    @Override
    public Container build(Movie movie) throws IOException {
        Track audioTrack = null;
        for (Track track : movie.getTracks()) {
            if (track.getHandler().equals("soun")) {
                audioTrack = track;
                break;
            }
        }
        if (audioTrack == null) {
            throw new IOException("Audio track not found");
        }
        Movie m = new Movie();
        m.addTrack(audioTrack);
        return super.build(m);
    }

    @Override
    protected Box createUdta(Movie movie) {
        UserDataBox userDataBox = new UserDataBox();
        MetaBox metaBox = new MetaBox();
        HandlerBox handlerBox = new HandlerBox();
        AppleItemListBox appleItemListBox = new AppleItemListBox();
        AppleEncoderBox appleEncoderBox = new AppleEncoderBox();

        handlerBox.setHandlerType("mdir");
        handlerBox.setA(0x6170706c); //appl
        metaBox.addBox(handlerBox);

        appleEncoderBox.setValue(text);
        appleItemListBox.addBox(appleEncoderBox);
        metaBox.addBox(appleItemListBox);
        userDataBox.addBox(metaBox);
        return userDataBox;
    }

    @Override
    protected FileTypeBox createFileTypeBox(Movie movie) {
        List<String> minorBrands = new LinkedList<String>();
        minorBrands.add("isom");
        minorBrands.add("iso2");
        return new FileTypeBox("M4A ", 512, minorBrands); //brands is four-character code
    }
}
