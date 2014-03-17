/*
 * MirrorChooserUI.java
 *
 * Created on 10. prosinec 2008, 13:34
 */

package cz.vity.freerapid.plugins.services.rapidshare;

import javax.swing.*;
import java.awt.*;

/**
 * @author Ludìk
 */
class MirrorChooserUI extends javax.swing.JPanel {
    // Variables declaration - do not modify
    private javax.swing.JComboBox jComboBox1;
    private javax.swing.DefaultComboBoxModel mirrors;

    // End of variables declaration
    /**
     * Creates new form MirrorChooser
     *
     * @param mirrors List of mirrors
     */
    public MirrorChooserUI(MirrorChooser mirrors) {
        this.mirrors = new DefaultComboBoxModel(mirrors.getArray());
        initComponents();
        final MirrorBean chosen = mirrors.getChosen();
        if (chosen != null)
            this.mirrors.setSelectedItem(chosen);
        this.setPreferredSize(new Dimension(40, 50));
    }

    private void initComponents() {

        jComboBox1 = new javax.swing.JComboBox();

        //jComboBox1.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        jComboBox1.setModel(mirrors);
        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                        .addGap(0, 0, 0)
                        .addComponent(jComboBox1, javax.swing.GroupLayout.PREFERRED_SIZE, 210, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addContainerGap(0, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                        .addGap(0, 0, 0)
                        .addComponent(jComboBox1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addContainerGap(0, Short.MAX_VALUE))
        );
    }

    public Object getChoosen() {
        return mirrors.getSelectedItem();
    }


}
