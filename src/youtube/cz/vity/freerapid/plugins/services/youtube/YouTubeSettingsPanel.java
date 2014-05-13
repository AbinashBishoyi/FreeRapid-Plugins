package cz.vity.freerapid.plugins.services.youtube;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * @author Kajda, JPEXS, ntoskrnl, tong2shot
 */
public class YouTubeSettingsPanel extends JPanel {
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
        final JLabel audioQualityLabel = new JLabel("Audio bitrate:");
        final JComboBox<AudioQuality> audioQualityList = new JComboBox<AudioQuality>(AudioQuality.getItems());
        final ButtonGroup buttonGroup = new ButtonGroup();
        final String videoStr = "Download video";
        final String audioStr = "Convert to audio";
        final JRadioButton videoRb = new JRadioButton(videoStr);
        final JRadioButton audioRb = new JRadioButton(audioStr);
        final JPanel videoPanel = new JPanel();
        final JPanel audioPanel = new JPanel();
        final JCheckBox orderCheckBox = new JCheckBox("Sort by newest first when adding links from user pages");
        final JCheckBox subtitlesCheckBox = new JCheckBox("Download subtitles whenever available");
        final JCheckBox enableDashCheckBox = new JCheckBox("Enable DASH streams");
        videoQualityLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        videoQualityList.setAlignmentX(Component.LEFT_ALIGNMENT);
        containerLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        containerList.setAlignmentX(Component.LEFT_ALIGNMENT);
        audioQualityLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        audioQualityList.setAlignmentX(Component.LEFT_ALIGNMENT);
        orderCheckBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        subtitlesCheckBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        enableDashCheckBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        videoRb.setActionCommand(videoStr);
        audioRb.setActionCommand(audioStr);
        buttonGroup.add(videoRb);
        buttonGroup.add(audioRb);

        videoQualityList.setSelectedItem(config.getVideoQuality());
        containerList.setSelectedItem(config.getContainer());
        orderCheckBox.setSelected(config.isReversePlaylistOrder());
        subtitlesCheckBox.setSelected(config.isDownloadSubtitles());
        enableDashCheckBox.setSelected(config.isEnableDash());
        videoRb.setSelected(!config.isConvertToAudio());
        audioRb.setSelected(config.isConvertToAudio());
        if (videoRb.isSelected()) {
            videoQualityList.setEnabled(true);
            containerList.setEnabled(true);
            audioQualityList.setEnabled(false);
        } else {
            audioQualityList.setEnabled(true);
            videoQualityList.setEnabled(false);
            containerList.setEnabled(false);
        }
        audioQualityList.setSelectedItem(config.getAudioQuality());

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
        ActionListener rbListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (e.getActionCommand().equals(videoStr)) {
                    videoQualityList.setEnabled(true);
                    containerList.setEnabled(true);
                    audioQualityList.setEnabled(false);
                    config.setConvertToAudio(false);
                } else {
                    audioQualityList.setEnabled(true);
                    videoQualityList.setEnabled(false);
                    containerList.setEnabled(false);
                    config.setConvertToAudio(true);
                }
            }
        };
        videoRb.addActionListener(rbListener);
        audioRb.addActionListener(rbListener);
        audioQualityList.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                config.setAudioQuality((AudioQuality) audioQualityList.getSelectedItem());
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
        videoPanel.setLayout(new BoxLayout(videoPanel, BoxLayout.Y_AXIS));
        videoPanel.add(videoQualityLabel);
        videoPanel.add(videoQualityList);
        videoPanel.add(containerLabel);
        videoPanel.add(containerList);
        audioPanel.setLayout(new BoxLayout(audioPanel, BoxLayout.Y_AXIS));
        audioPanel.add(audioQualityLabel);
        audioPanel.add(audioQualityList);
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        add(videoRb);
        add(videoPanel);
        add(Box.createRigidArea(new Dimension(0, 15)));
        add(audioRb);
        add(audioPanel);
        add(Box.createRigidArea(new Dimension(0, 15)));
        add(orderCheckBox);
        add(subtitlesCheckBox);
        add(enableDashCheckBox);
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
    }

}