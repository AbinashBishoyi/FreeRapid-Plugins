package cz.vity.freerapid.plugins.services.mp4parser.test;

import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.authoring.builder.DefaultMp4Builder;
import com.googlecode.mp4parser.authoring.container.mp4.MovieCreator;

import java.io.File;
import java.io.FileOutputStream;

/**
 * @author tong2shot
 */
public class testMux {
    public static void main(String[] args) throws Exception {
        //To test : make sure your IDE load 'isoparser-default.properties' as resource.
        String dir = "/media/LNXDEV/frd/frd-test/dash test2/";
        String fnameNoExt = "α7S Unparalleled 4K image quality (from Sony: Official Video Release)";
        String audio = dir + fnameNoExt + ".m4a";
        String video = dir + fnameNoExt + ".m4v";
        Movie countVideo = MovieCreator.build(video);
        Movie countAudioEnglish = MovieCreator.build(audio);
        Track audioTrackEnglish = countAudioEnglish.getTracks().get(0);
        audioTrackEnglish.getTrackMetaData().setLanguage("eng");
        countVideo.addTrack(audioTrackEnglish);
        com.coremedia.iso.boxes.Container out = new DefaultMp4Builder()
                .build(countVideo);
        FileOutputStream fos = new FileOutputStream(new File(dir + fnameNoExt
                + ".mp4"));
        out.writeContainer(fos.getChannel());
        fos.close();


        //-------------------------------------------------------
        fnameNoExt = "The Hunger Games: Catching Fire - EXCLUSIVE Final Trailer";
        audio = dir + fnameNoExt + ".m4a";
        video = dir + fnameNoExt + ".m4v";
        countVideo = MovieCreator.build(video);
        countAudioEnglish = MovieCreator.build(audio);
        audioTrackEnglish = countAudioEnglish.getTracks().get(0);
        audioTrackEnglish.getTrackMetaData().setLanguage("eng");
        countVideo.addTrack(audioTrackEnglish);
        out = new DefaultMp4Builder()
                .build(countVideo);
        fos = new FileOutputStream(new File(dir + fnameNoExt
                + ".mp4"));
        out.writeContainer(fos.getChannel());
        fos.close();


        //-------------------------------------------------------
        fnameNoExt = "fashiontv | FTV.com - FULL SHOW JEREMY SCOTT FEM PE 99";
        audio = dir + fnameNoExt + ".m4a";
        video = dir + fnameNoExt + ".m4v";
        countVideo = MovieCreator.build(video);
        countAudioEnglish = MovieCreator.build(audio);
        audioTrackEnglish = countAudioEnglish.getTracks().get(0);
        audioTrackEnglish.getTrackMetaData().setLanguage("eng");
        countVideo.addTrack(audioTrackEnglish);
        out = new DefaultMp4Builder()
                .build(countVideo);
        fos = new FileOutputStream(new File(dir + fnameNoExt
                + ".mp4"));
        out.writeContainer(fos.getChannel());
        fos.close();


    }
}
