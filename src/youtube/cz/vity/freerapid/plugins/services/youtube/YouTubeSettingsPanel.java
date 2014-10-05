package cz.vity.freerapid.plugins.services.youtube;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Kajda, JPEXS, ntoskrnl, tong2shot
 */
class YouTubeSettingsPanel extends JPanel {
    private final static Logger logger = Logger.getLogger(YouTubeSettingsPanel.class.getName());
    private YouTubeSettingsConfig config;

    public YouTubeSettingsPanel(YouTubeServiceImpl service) throws Exception {
        super();
        config = service.getConfig();
        initPanel();
    }

    private void initPanel() {
        final JLabel videoQualityLabel = new JLabel("Preferred quality level:");
        final JComboBox<VideoQuality> videoQualityList = new JComboBox<VideoQuality>(VideoQuality.getItems());
        final JLabel containerLabel = new JLabel("Preferred container:");
        final JComboBox<Container> containerList = new JComboBox<Container>(Container.getItems());
        final JLabel convertAudioQualityLabel = new JLabel("Audio bitrate:");
        final JComboBox<AudioQuality> convertAudioQualityList = new JComboBox<AudioQuality>(AudioQuality.getItems());
        final JLabel extractAudioQualityLabel = new JLabel("Audio bitrate:");
        final JComboBox<AudioQuality> extractAudioQualityList = new JComboBox<AudioQuality>(AudioQuality.getItems());
        final ButtonGroup buttonGroup = new ButtonGroup();
        final JRadioButton downloadVideoRb = new JRadioButton(DownloadMode.downloadVideo.toString());
        final JRadioButton convertToAudioRb = new JRadioButton(DownloadMode.convertToAudio.toString());
        final JRadioButton extractAudioRb = new JRadioButton(DownloadMode.extractAudio.toString());
        final JPanel downloadVideoPanel = new JPanel();
        final JPanel convertToAudioPanel = new JPanel();
        final JPanel extractAudioPanel = new JPanel();
        final JCheckBox orderCheckBox = new JCheckBox("Sort by newest first when adding links from user pages");
        final JCheckBox subtitlesCheckBox = new JCheckBox("Download subtitles whenever available");
        final JCheckBox enableDashCheckBox = new JCheckBox("<html>Enable DASH streams <small><sup>*)</sup></small></html>");
        final JCheckBox enableInternalMultiplexerCheckBox = new JCheckBox("Enable internal DASH multiplexer/merger");
        final JLabel dashNoteLabel = new JLabel("<html><small>*) For 480p or 1080p or above, DASH streams need to be enabled.</small></html>");
        final JLabel ffmpegLinkLabel = new JLabel("<html><small>If you're not using the internal DASH multiplexer :" +
                "<br><a href=\"\">How to multiplex/merge DASH files using FFmpeg</a></small></html>");

        videoQualityLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        videoQualityList.setAlignmentX(Component.LEFT_ALIGNMENT);
        containerLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        containerList.setAlignmentX(Component.LEFT_ALIGNMENT);
        convertAudioQualityLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        convertAudioQualityList.setAlignmentX(Component.LEFT_ALIGNMENT);
        extractAudioQualityLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        extractAudioQualityList.setAlignmentX(Component.LEFT_ALIGNMENT);
        orderCheckBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        subtitlesCheckBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        enableDashCheckBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        enableInternalMultiplexerCheckBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        dashNoteLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        ffmpegLinkLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        ffmpegLinkLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));

        downloadVideoRb.setActionCommand(DownloadMode.downloadVideo.toString());
        convertToAudioRb.setActionCommand(DownloadMode.convertToAudio.toString());
        extractAudioRb.setActionCommand(DownloadMode.extractAudio.toString());
        buttonGroup.add(downloadVideoRb);
        buttonGroup.add(convertToAudioRb);
        buttonGroup.add(extractAudioRb);

        videoQualityList.setSelectedItem(config.getVideoQuality());
        containerList.setSelectedItem(config.getContainer());
        convertAudioQualityList.setSelectedItem(config.getConvertAudioQuality());
        extractAudioQualityList.setSelectedItem(config.getExtractAudioQuality());

        orderCheckBox.setSelected(config.isReversePlaylistOrder());
        subtitlesCheckBox.setSelected(config.isDownloadSubtitles());
        enableDashCheckBox.setSelected(config.isEnableDash());
        enableInternalMultiplexerCheckBox.setSelected(config.isEnableInternalMultiplexer());

        downloadVideoRb.setSelected(config.getDownloadMode() == DownloadMode.downloadVideo);
        convertToAudioRb.setSelected(config.getDownloadMode() == DownloadMode.convertToAudio);
        extractAudioRb.setSelected(config.getDownloadMode() == DownloadMode.extractAudio);

        videoQualityList.setEnabled(config.getDownloadMode() == DownloadMode.downloadVideo);
        containerList.setEnabled(config.getDownloadMode() == DownloadMode.downloadVideo);
        convertAudioQualityList.setEnabled(config.getDownloadMode() == DownloadMode.convertToAudio);
        extractAudioQualityList.setEnabled(config.getDownloadMode() == DownloadMode.extractAudio);

        ActionListener rbListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                boolean isDownloadVideo = e.getActionCommand().equals(DownloadMode.downloadVideo.toString());
                boolean isConvertAudio = e.getActionCommand().equals(DownloadMode.convertToAudio.toString());
                boolean isExtractAudio = e.getActionCommand().equals(DownloadMode.extractAudio.toString());
                videoQualityList.setEnabled(isDownloadVideo);
                containerList.setEnabled(isDownloadVideo);
                convertAudioQualityList.setEnabled(isConvertAudio);
                extractAudioQualityList.setEnabled(isExtractAudio);
                DownloadMode downloadMode = (isDownloadVideo ? DownloadMode.downloadVideo :
                        (isConvertAudio ? DownloadMode.convertToAudio : DownloadMode.extractAudio));
                config.setDownloadMode(downloadMode);
            }
        };
        downloadVideoRb.addActionListener(rbListener);
        convertToAudioRb.addActionListener(rbListener);
        extractAudioRb.addActionListener(rbListener);

        videoQualityList.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                config.setVideoQuality((VideoQuality) videoQualityList.getSelectedItem());
            }
        });
        containerList.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                config.setContainer((Container) containerList.getSelectedItem());
            }
        });
        convertAudioQualityList.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                config.setConvertAudioQuality((AudioQuality) convertAudioQualityList.getSelectedItem());
            }
        });
        extractAudioQualityList.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                config.setExtractAudioQuality((AudioQuality) extractAudioQualityList.getSelectedItem());
            }
        });

        orderCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                config.setReversePlaylistOrder(orderCheckBox.isSelected());
            }
        });
        subtitlesCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                config.setDownloadSubtitles(subtitlesCheckBox.isSelected());
            }
        });
        enableDashCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                config.setEnableDash(enableDashCheckBox.isSelected());
            }
        });
        enableInternalMultiplexerCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                config.setEnableInternalMultiplexer(enableInternalMultiplexerCheckBox.isSelected());
            }
        });
        ffmpegLinkLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.BROWSE))
                    return;
                try {
                    Desktop.getDesktop().browse(new URI("http://wordrider.net/forum/10/13123/14222/re___updated__about_youtube__1080p_and_480p____#msg-14222"));
                } catch (Exception ex) {
                    logger.log(Level.SEVERE, "Opening browser failed", ex);
                }
            }
        });

        downloadVideoPanel.setLayout(new BoxLayout(downloadVideoPanel, BoxLayout.Y_AXIS));
        downloadVideoPanel.add(videoQualityLabel);
        downloadVideoPanel.add(videoQualityList);
        downloadVideoPanel.add(containerLabel);
        downloadVideoPanel.add(containerList);
        convertToAudioPanel.setLayout(new BoxLayout(convertToAudioPanel, BoxLayout.Y_AXIS));
        convertToAudioPanel.add(convertAudioQualityLabel);
        convertToAudioPanel.add(convertAudioQualityList);
        extractAudioPanel.setLayout(new BoxLayout(extractAudioPanel, BoxLayout.Y_AXIS));
        extractAudioPanel.add(extractAudioQualityLabel);
        extractAudioPanel.add(extractAudioQualityList);
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        add(downloadVideoRb);
        add(downloadVideoPanel);
        add(Box.createRigidArea(new Dimension(0, 5)));
        add(convertToAudioRb);
        add(convertToAudioPanel);
        add(Box.createRigidArea(new Dimension(0, 5)));
        add(extractAudioRb);
        add(extractAudioPanel);
        add(Box.createRigidArea(new Dimension(0, 10)));
        add(orderCheckBox);
        add(subtitlesCheckBox);
        add(enableDashCheckBox);
        add(enableInternalMultiplexerCheckBox);
        add(Box.createRigidArea(new Dimension(0, 10)));
        add(dashNoteLabel);
        add(Box.createRigidArea(new Dimension(0, 5)));
        add(ffmpegLinkLabel);
        setBorder(BorderFactory.createEmptyBorder(0, 10, 5, 10));
    }

}