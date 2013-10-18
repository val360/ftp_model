/*
 * Created by JFormDesigner on Fri Mar 21 16:15:32 EDT 2008
 */

package com.prosc.ftpeek;

import java.awt.*;
import javax.swing.*;

public class TransferStatus extends JDialog {
	public TransferStatus(Frame owner) {
		super(owner, true);
		initComponents();
	}

	public TransferStatus(Dialog owner) {
		super(owner);
		initComponents();
	}

	private void initComponents() {
		// JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
		statusLabel = new JLabel();
		progressBar = new JProgressBar();
		percentLabel = new JLabel();

		//======== this ========
		setResizable(false);
		setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		Container contentPane = getContentPane();
		contentPane.setLayout(new GridBagLayout());
		((GridBagLayout)contentPane.getLayout()).columnWidths = new int[] {0, 0, 0};
		((GridBagLayout)contentPane.getLayout()).rowHeights = new int[] {0, 0, 0};
		((GridBagLayout)contentPane.getLayout()).columnWeights = new double[] {1.0, 0.0, 1.0E-4};
		((GridBagLayout)contentPane.getLayout()).rowWeights = new double[] {0.0, 1.0, 1.0E-4};

		//---- statusLabel ----
		statusLabel.setText(" ");
		contentPane.add(statusLabel, new GridBagConstraints(0, 0, 2, 1, 0.0, 0.0,
			GridBagConstraints.CENTER, GridBagConstraints.BOTH,
			new Insets(10, 10, 10, 0), 0, 0));

		//---- progressBar ----
		progressBar.setPreferredSize(new Dimension(300, 20));
		contentPane.add(progressBar, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
			GridBagConstraints.CENTER, GridBagConstraints.BOTH,
			new Insets(0, 10, 10, 0), 0, 0));

		//---- percentLabel ----
		percentLabel.setText("0%");
		contentPane.add(percentLabel, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0,
			GridBagConstraints.CENTER, GridBagConstraints.VERTICAL,
			new Insets(0, 10, 10, 10), 0, 0));
		pack();
		setLocationRelativeTo(getOwner());
		// JFormDesigner - End of component initialization  //GEN-END:initComponents
	}

	// JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
	private JLabel statusLabel;
	private JProgressBar progressBar;
	private JLabel percentLabel;
	// JFormDesigner - End of variables declaration  //GEN-END:variables

	public JProgressBar getProgressBar() {
		return progressBar;
	}

	public void setProgressBar(JProgressBar progressBar) {
		this.progressBar = progressBar;
	}

	public JLabel getPercentLabel() {
		return percentLabel;
	}

	public void setPercentLabel(JLabel percentLabel) {
		this.percentLabel = percentLabel;
	}

	public JLabel getStatusLabel() {
		return statusLabel;
	}

	public void setStatusLabel(JLabel statusLabel) {
		this.statusLabel = statusLabel;
	}
}
