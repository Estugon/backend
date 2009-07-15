/**
 * 
 */
package sc.plugin2010.renderer.twodimensional;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;

import sc.plugin2010.renderer.RendererUtil;

/**
 * @author ffi
 * 
 */
@SuppressWarnings("serial")
public class ErrorDialog extends JDialog
{
	private static final int	DEFAULTHEIGHT	= 100;
	private static final int	DEFAULTWIDTH	= 300;

	public ErrorDialog(String message)
	{
		setTitle("Fehler");

		setIconImage(RendererUtil.getImage("resource/hase_und_igel_icon.png"));

		setSize(DEFAULTWIDTH, DEFAULTHEIGHT);

		OkayListener awListener = new OkayListener();

		JPanel buttonPanel = new JPanel();

		JLabel imageLabel = new JLabel();

		FlowLayout buttonLayout = new FlowLayout();

		buttonPanel.setLayout(buttonLayout);

		// add okay Button
		JButton jbut = new JButton("OK");
		jbut.addMouseListener(awListener);
		buttonPanel.add(jbut);

		BorderLayout dialogLayout = new BorderLayout();
		setLayout(dialogLayout);

		imageLabel.setIcon(new ImageIcon(RendererUtil
				.getImage("resource/error.png")));

		this.add(new JLabel(message, JLabel.CENTER), BorderLayout.CENTER);
		this.add(imageLabel, BorderLayout.WEST);
		this.add(buttonPanel, BorderLayout.SOUTH);

		setLocationRelativeTo(null);

		setModal(true);
		setVisible(true);
	}

	public class OkayListener extends MouseAdapter
	{
		@Override
		public void mouseReleased(MouseEvent e)
		{
			if (e.getButton() == MouseEvent.BUTTON1)
			{
				dispose();
			}
		}
	}
}
