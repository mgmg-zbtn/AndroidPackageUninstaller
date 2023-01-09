import java.awt.AWTException;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.SwingUtilities;


/**
 * adb shell pm list packages | sort > G:\Eclipse\dat\AndroidPackageUninstaller\list.txt
 * パッケージリストを出力する（手動）
 * 
 * adb devices
 * コマンドプロンプトからUSB接続のデバイスにアクセスする
 * 
 * コマンドプロンプトは左側、GUIは右側に並べる
 * 
 * 
 * adb shell pm uninstall -k --user 0 [package name]
 * 
 * [LOAD]		出力されたテキストを読み込む
 * [DO]			選択したパッケージをコマンドに成形してコピペ
 * [REMOVE]		選択したパッケージをリストから除外
 * [TextField]	読み込んだパッケージリストから指定した正規表現に従って絞り込む（Enter実行）
 * 
 * @since 2022/12/10
 */
public class Main extends JFrame {
	
	JPanel headerPane;

	JPanel addressPane;
	JTextField addressField;
	JButton addressButton;
	
	JPanel controlPane;
	JButton doButton;
	JButton removeButton;
	
	JPanel searchPane;
	JTextField searchField;
	
	JList packageList;
	DefaultListModel model;
	
	//--------------------------------------------------
	
	Robot robot;
	Clipboard clip;
	
	//--------------------------------------------------
	
	List<String> dataList;
	
	public Main() {
		
		try {
			robot = new Robot();
		} catch (AWTException e) {
			e.printStackTrace();
		}
		
		// hard code!!
		String test = "G:\\Eclipse\\tools\\AndroidPackageUninstaller\\list2.txt";
		
		addressPane = new JPanel();
		addressButton = new JButton("LOAD");
		addressField = new JTextField(test);
		
		addressPane.setLayout(new BoxLayout(addressPane, BoxLayout.LINE_AXIS));
		addressField.setMaximumSize(new Dimension(Short.MAX_VALUE, addressButton.getMaximumSize().height));
		
		addressPane.add(addressButton);
		addressPane.add(addressField);
		
		controlPane = new JPanel();
		doButton = new JButton("DO");
		removeButton = new JButton("REMOVE");
		
		controlPane.setLayout(new GridLayout(1, 2));
		controlPane.add(doButton);
		controlPane.add(removeButton);
		
		searchPane = new JPanel();
		searchField = new JTextField();

		searchPane.setLayout(new BoxLayout(searchPane, BoxLayout.LINE_AXIS));
		searchField.setMaximumSize(new Dimension(Short.MAX_VALUE, addressButton.getMaximumSize().height));
		searchPane.add(searchField);
		
		headerPane = new JPanel();
		headerPane.setLayout(new GridLayout(3, 1));
		headerPane.add(addressPane);
		headerPane.add(controlPane);
		headerPane.add(searchPane);
		
		model = new DefaultListModel();
		packageList = new JList(model);
		packageList.setCellRenderer(new TestCellRenderer());
		
		
		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setPreferredSize(new Dimension(400, 600));
		scrollPane.setViewportView(packageList);
		
		// スクロールの間接パネル
		JPanel wrapPane = new JPanel();
		wrapPane.add(scrollPane);
		
		getContentPane().add(headerPane, BorderLayout.NORTH);
		getContentPane().add(wrapPane, BorderLayout.CENTER);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setResizable(true);
		setMinimumSize(new Dimension(400, 500));
		pack();
		setVisible(true);
		
		dataList = new LinkedList<String>();
		
		addressButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				addressButton.setEnabled(false);
				
				model.clear();
				dataList.clear();
				searchField.setText("");
				
				Path path = Paths.get(addressField.getText());
				try {
					List<String> list = Files.readAllLines(path);
					for (String str : list) {
						if (str.length() != 0) {
							str = str.replace("package:", "");
							dataList.add(str);
							model.addElement(str);
						}
					}
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				addressButton.setEnabled(true);
			}
		});
		
		//------------------------------------------------------------
		
		JFrame tf = this;
		clip = Toolkit.getDefaultToolkit().getSystemClipboard();
		
		doButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				doButton.setEnabled(false);
				
				String temp = (String) packageList.getSelectedValue();
				String cmd = "adb shell pm uninstall -k --user 0 " + temp;
				StringSelection ss = new StringSelection(cmd);
				clip.setContents(ss, ss);

				Rectangle ra = tf.getBounds();
				robot.mouseMove(ra.x - ra.width / 2, ra.y + ra.height / 2);
				robot.mousePress(MouseEvent.BUTTON1_DOWN_MASK);
				robot.mouseRelease(MouseEvent.BUTTON1_DOWN_MASK);
				
				robot.delay(100);

				robot.keyPress(KeyEvent.VK_CONTROL);
				robot.keyPress(KeyEvent.VK_V);
				robot.keyRelease(KeyEvent.VK_V);
				robot.keyRelease(KeyEvent.VK_CONTROL);

				robot.delay(100);

				robot.keyPress(KeyEvent.VK_ENTER);
				robot.keyRelease(KeyEvent.VK_ENTER);
				
				robot.mouseMove(ra.x + ra.width / 2, ra.y + ra.height / 2);	
				
				ss = new StringSelection("");
				clip.setContents(ss, ss);
				
				doButton.setEnabled(true);
			}
		});
		
		//------------------------------------------------------------

		removeButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (!packageList.isSelectionEmpty()) {
					int index = packageList.getSelectedIndex();
			        model.remove(index);
			        dataList.remove(index);
				}
			}
		});
		
		//------------------------------------------------------------

		searchField.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				model.clear();
				String regx = searchField.getText();
				Pattern p = Pattern.compile(regx);
				for (String dat : dataList) {
					Matcher m = p.matcher(dat);
					if (m.find()) {
						model.addElement(dat);
					}
				}
			}
		});
	}
	
	/**
	 * JListのセルはラベルで出来ている
	 * セルのフォントや表示方法を細かく再定義する
	 */
	class TestCellRenderer extends JLabel implements ListCellRenderer {
		
		public TestCellRenderer() {
			super();
			setOpaque(true);
			setFont(new Font("MS ゴシック", Font.PLAIN, 16));
		}

		@Override
		public Component getListCellRendererComponent(
				JList list,
				Object value,
				int index,
				boolean isSelected,
				boolean cellHasFocus
				) {
			
			if (isSelected) {
				setBackground(Color.GRAY);
				setForeground(Color.WHITE);
			} else {
				setBackground(Color.WHITE);
				setForeground(Color.BLACK);
			}
			
			setText(value.toString());
			
			return this;
		}
	}
	
	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				new Main();
			}
		});
	}
}

