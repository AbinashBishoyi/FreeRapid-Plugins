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
        final String[] qualityStrings = {"Highest available", "1080p (HD)", "720p (HD)", "480p", "360p", "240p", "Lowest available"};
        final int[] qualityIndexMap = {4, 6, 5, 3, 2, 1, 0}; //Due to quality settings in older versions, 4 is Highest available
        final String[] containerStrings = {"Any container", "MP4", "FLV", "WebM", "3GP"};

        final JLabel qualityLabel = new JLabel("Preferred quality level:");
        final JComboBox<String> qualityList = new JComboBox<String>(qualityStrings);
        final JLabel containerLabel = new JLabel("Preferred container:");
        final JComboBox<String> containerList = new JComboBox<String>(containerStrings);
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
        qualityLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        qualityList.setAlignmentX(Component.LEFT_ALIGNMENT);
        containerLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        containerList.setAlignmentX(Component.LEFT_ALIGNMENT);
        audioQualityLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        audioQualityList.setAlignmentX(Component.LEFT_ALIGNMENT);
        orderCheckBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        subtitlesCheckBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        videoRb.setActionCommand(videoStr);
        audioRb.setActionCommand(audioStr);
        buttonGroup.add(videoRb);
        buttonGroup.add(audioRb);
        int qs = config.getQualitySetting();
        for (int i = 0; i < qualityIndexMap.length; i++) {
            if (qualityIndexMap[i] == qs) {
                qualityList.setSelectedIndex(i);
                break;
            }
        }
        containerList.setSelectedIndex(config.getContainer());
        orderCheckBox.setSelected(config.isReversePlaylistOrder());
        subtitlesCheckBox.setSelected(config.isDownloadSubtitles());
        videoRb.setSelected(!config.isConvertToAudio());
        audioRb.setSelected(config.isConvertToAudio());
        if (videoRb.isSelected()) {
            qualityList.setEnabled(true);
            containerList.setEnabled(true);
            audioQualityList.setEnabled(false);
        } else {
            audioQualityList.setEnabled(true);
            qualityList.setEnabled(false);
            containerList.setEnabled(false);
        }
        audioQualityList.setSelectedItem(config.getAudioQuality());

        qualityList.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                config.setQualitySetting(qualityIndexMap[qualityList.getSelectedIndex()]);
            }
        });
        containerList.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                config.setContainer(containerList.getSelectedIndex());
            }
        });
        ActionListener rbListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (e.getActionCommand().equals(videoStr)) {
                    qualityList.setEnabled(true);
                    containerList.setEnabled(true);
                    audioQualityList.setEnabled(false);
                    config.setConvertToAudio(false);
                } else {
                    audioQualityList.setEnabled(true);
                    qualityList.setEnabled(false);
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
        videoPanel.setLayout(new BoxLayout(videoPanel, BoxLayout.Y_AXIS));
        videoPanel.add(qualityLabel);
        videoPanel.add(qualityList);
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
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
    }

}