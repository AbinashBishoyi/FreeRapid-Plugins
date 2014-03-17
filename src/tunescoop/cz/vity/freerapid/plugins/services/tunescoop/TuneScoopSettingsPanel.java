package cz.vity.freerapid.plugins.services.tunescoop;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * @author ntoskrnl
 */
public class TuneScoopSettingsPanel extends JPanel {
    private TuneScoopSettingsConfig config;

    public TuneScoopSettingsPanel(TuneScoopServiceImpl service) throws Exception {
        super(new BorderLayout());
        config = service.getConfig();
        initPanel();
    }

    private void initPanel() {
        final JLabel label = new JLabel("How should the downloads be named?");
        final JRadioButton buttonDefault = new JRadioButton("Use default filename", !config.getIsCustom());
        final JRadioButton buttonCustom = new JRadioButton("Custom filename:", config.getIsCustom());
        final JTextField textCustom = new JTextField();

        buttonDefault.setMnemonic('D');
        buttonCustom.setMnemonic('C');
        textCustom.setFont(new Font("Monospaced", Font.PLAIN, 12));
        textCustom.setToolTipText("<html>Write your desired custom filename here.<br>%ARTIST% and %SONG% are replaced with correct ones from the site.<br>The file extension is automatically added.</html>");

        final ButtonGroup group = new ButtonGroup();
        group.add(buttonDefault);
        group.add(buttonCustom);

        final JPanel radioPanel = new JPanel(new GridLayout(0, 1));
        radioPanel.add(buttonDefault);
        radioPanel.add(buttonCustom);

        textCustom.setText(config.getCustomName());
        textCustom.setEnabled(config.getIsCustom());

        buttonDefault.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                config.setIsCustom(buttonCustom.isSelected());
                textCustom.setEnabled(false);
            }
        });
        buttonCustom.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                config.setIsCustom(buttonCustom.isSelected());
                textCustom.setEnabled(true);
            }
        });
        textCustom.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                changed();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                changed();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
            }

            private void changed() {
                final String s = textCustom.getText();
                config.setCustomName(s == null ? "" : s);
            }
        });

        add(label, BorderLayout.NORTH);
        add(radioPanel, BorderLayout.CENTER);
        add(textCustom, BorderLayout.SOUTH);

        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
    }

}