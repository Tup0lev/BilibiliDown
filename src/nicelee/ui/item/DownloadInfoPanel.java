package nicelee.ui.item;

import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import nicelee.bilibili.INeedAV;
import nicelee.ui.Global;
import nicelee.util.HttpRequestUtil;

public class DownloadInfoPanel extends JPanel implements ActionListener {
	
	String avTitle;
	String avid;
	String cid;
	String page;
	int qn;
	
	//下载相关
	public INeedAV iNeedAV;
	public String url;
	public String avid_qn;
	
	
	long lastCntTime = 0L;
	long lastCnt = 0L;
	/**
	 * 
	 */
	private static final long serialVersionUID = -752743062676819402L;
	String path;
	String fileName;
	long totalSize;
	long currentDown;
	boolean isdownloading = true;

	JButton btnRemove;
	JButton btnOpen;
	JButton btnOpenFolder;
	JButton btnControl;
	JLabel lbCurrentStatus;
	JLabel lbDownFile;
	JLabel lbFileName;

	public DownloadInfoPanel(String avTitle, String avid, String cid, String page, int qn) {
		this.avTitle = avTitle;
		this.avid = avid;
		this.cid = cid;
		this.page = page;
		this.qn = qn;
		path = "D:\\bilibiliDown\\";
		fileName = "timg.gif";
		totalSize = 0L;
		currentDown = 0L;
		initUI();
	}

	void initUI() {
		//this.setOpaque(false);
		this.setBorder(BorderFactory.createLineBorder(Color.red));
		this.setPreferredSize(new Dimension(1100, 120));

		lbFileName = new JLabel(path + fileName);
		lbFileName.setPreferredSize(new Dimension(600, 45));
		lbFileName.setBorder(BorderFactory.createLineBorder(Color.red));
		this.add(lbFileName);

		btnOpen = new JButton("打开文件");
		btnOpen.setPreferredSize(new Dimension(100, 45));
		btnOpen.addActionListener(this);
		this.add(btnOpen);

		btnOpenFolder = new JButton("打开文件夹");
		btnOpenFolder.setPreferredSize(new Dimension(100, 45));
		btnOpenFolder.addActionListener(this);
		this.add(btnOpenFolder);

		btnRemove = new JButton("删除任务");
		btnRemove.setPreferredSize(new Dimension(100, 45));
		btnRemove.addActionListener(this);
		this.add(btnRemove);

		JLabel blank = new JLabel();
		blank.setPreferredSize(new Dimension(100, 45));
		this.add(blank);
		
		JLabel lbavName = new JLabel(avTitle);
		lbavName.setPreferredSize(new Dimension(500, 45));
		lbavName.setBorder(BorderFactory.createLineBorder(Color.red));
		this.add(lbavName);
		
		lbCurrentStatus = new JLabel("正在下载...");
		lbCurrentStatus.setPreferredSize(new Dimension(300, 45));
		lbCurrentStatus.setBorder(BorderFactory.createLineBorder(Color.red));
		this.add(lbCurrentStatus);

		lbDownFile = new JLabel(currentDown + "/" + totalSize);
		lbCurrentStatus.setPreferredSize(new Dimension(250, 45));
		lbDownFile.setBorder(BorderFactory.createLineBorder(Color.red));
		this.add(lbDownFile);
		this.setBackground(new Color(204, 255, 255));
		
		btnControl = new JButton("暂停");
		btnControl.setPreferredSize(new Dimension(100, 45));
		btnControl.addActionListener(this);
		this.add(btnControl);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == btnOpenFolder) {
			File file = new File(lbFileName.getText());
			String cmd = "explorer " + file.getParent();
			try {
				Runtime.getRuntime().exec(cmd);
			} catch (Exception e1) {
				//e1.printStackTrace();
				JOptionPane.showMessageDialog(null, "打开文件夹失败!", "失败", JOptionPane.INFORMATION_MESSAGE);
			}
		} else if (e.getSource() == btnOpen) {
			File file = new File(lbFileName.getText());
			try {
				Desktop.getDesktop().open(file);
			} catch (Exception e1) {
				//e1.printStackTrace();
				JOptionPane.showMessageDialog(null, "打开文件失败!", "失败", JOptionPane.INFORMATION_MESSAGE);
			}
		} else if (e.getSource() == btnRemove) {
//			if(Global.downloadTaskList.get(this).getStatus() == 0) {
//				JOptionPane.showMessageDialog(this, "当前正在文件下载中!", "警告", JOptionPane.WARNING_MESSAGE);
//			}
			removeTask(true);
		}else if (e.getSource() == btnControl) {
			//HttpRequestUtil util = Global.downloadTaskList.get(this);
			HttpRequestUtil util = iNeedAV.getUtil();
			// 0 正在下载; 1 下载完毕; -1 出现错误; -2 人工停止;-3队列中
			if(util.getStatus() == 0) {
				stopTask();
			}else if(util.getStatus() == 1) {
				JOptionPane.showMessageDialog(null, "文件已下载完成", "提示", JOptionPane.INFORMATION_MESSAGE);
			}else {
				startTask();
			}
		}
	}

	/**
	 * 停止任务(方法内包含状态判断)
	 */
	public void stopTask() {
		HttpRequestUtil util = iNeedAV.getUtil();
		//如果正在下载，或在队列， 则暂停
		if(util.getStatus() == 0 ) {
			util.stopDownload();
			btnControl.setText("继续下载");
		}else if(util.getStatus() == -3 ) {
			util.setStatus(-2);
			btnControl.setText("继续下载");
		}
	}

	/**
	 * 开始任务(方法内包含状态判断)
	 */
	public void startTask() {
		HttpRequestUtil util = iNeedAV.getUtil();
		//如果正在下载 或 下载完毕，则不需要下载
		if(util.getStatus() != 1 && util.getStatus() != 0) {
			util.setStatus(-3);
			Global.downLoadThreadPool.execute(new Runnable() {
				@Override
				public void run() {
					if(util.getStatus() == -2) {
						System.out.println("已经人工停止,无需再下载");
						return;
					}
					util.startDownload();
					iNeedAV.downloadClip(url, avid_qn, page);
				}
			});
		}
	}

	/**
	 * 删除任务
	 */
	public void removeTask(boolean deleteAll) {
		HttpRequestUtil util = iNeedAV.getUtil();
		//删除所有 或 删除已完成的任务
		if(deleteAll || util.getStatus() == 1) {
			//停止下载
			Global.downloadTaskList.get(this).stopDownload();
			//全局监控撤销
			Global.downloadTaskList.remove(this);
			//当前页面控件删除
			Global.downloadTab.getJpContent().remove(this);
			//大小重新适配
			Global.downloadTab.getJpContent().setPreferredSize(new Dimension(1100, 128 * Global.downloadTaskList.size()));
			Global.downloadTab.getJpContent().updateUI();
			Global.downloadTab.getJpContent().repaint();
//		删除未完成的下载文件
//		File file = new File(lbFileName.getText() + ".part");
//		if( file.exists()) {
//			file.delete();
//		}
		}
	}

	public JLabel getLbCurrentStatus() {
		return lbCurrentStatus;
	}

	public void setLbCurrentStatus(JLabel lbCurrentStatus) {
		this.lbCurrentStatus = lbCurrentStatus;
	}

	public JLabel getLbDownFile() {
		return lbDownFile;
	}

	public void setLbDownFile(JLabel lbDownFile) {
		this.lbDownFile = lbDownFile;
	}

	public JLabel getLbFileName() {
		return lbFileName;
	}

	public void setLbFileName(JLabel lbFileName) {
		this.lbFileName = lbFileName;
	}

	public JButton getBtnControl() {
		return btnControl;
	}

	public void setBtnControl(JButton btnControl) {
		this.btnControl = btnControl;
	}
	
	@Override
	public int hashCode() {
		return (avid + page).hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		//System.out.println("DownloadInfoPanel - equals:");
		if(obj instanceof DownloadInfoPanel) {
			DownloadInfoPanel down = (DownloadInfoPanel)obj;
			return (avid.equals(down.avid) 
					&& page.equals(down.page));
		}
		return false;
	}

	public long getLastCntTime() {
		return lastCntTime;
	}

	public void setLastCntTime(long lastCntTime) {
		this.lastCntTime = lastCntTime;
	}

	public long getLastCnt() {
		return lastCnt;
	}

	public void setLastCnt(long lastCnt) {
		this.lastCnt = lastCnt;
	}

	public String getAvid() {
		return avid;
	}

	public void setAvid(String avid) {
		this.avid = avid;
	}

	public int getQn() {
		return qn;
	}

	public void setQn(int qn) {
		this.qn = qn;
	}

}
