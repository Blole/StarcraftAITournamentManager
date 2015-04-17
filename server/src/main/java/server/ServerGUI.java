
package server;

import java.awt.Color;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import common.Game;
import common.protocols.RemoteClient;

public class ServerGUI
{
	Server		server;
	
    JFrame		mainFrame;
    private 	JTable		mainTable;
    private 	JTextArea	bottomText;
    private 	JPanel		bottomPanel;
    private 	JMenuBar	menuBar;
    private 	JMenu		fileMenu;
    private 	JMenu		actionsMenu;
    private		JMenuItem	exitMenuItem;
    private 	JMenuItem	sendClientCommandMenuItem;
    private		JMenuItem	viewClientScreenMenuItem;
    
    private String [] 		columnNames = {"Client", "Status", "Game #", "Self", "Enemy", "Map", "Duration", "Win"};
	private Object [][] 	data = 	{ };

	public ServerGUI(Server server)
	{
		this.server = server;
		CreateGUI();
	}
	
	public void CreateGUI()
	{
		mainTable = new JTable(new DefaultTableModel(data, columnNames));
		mainTable.setDefaultRenderer(Object.class, new MyRenderer());
		bottomText = new JTextArea();
		mainFrame = new JFrame("StarCraft AI Tournament Manager - Server");
		mainFrame.setLayout(new GridLayout(2,0));
		
		bottomPanel = new JPanel();
		bottomPanel.setLayout(new GridLayout(1,0));
		bottomPanel.add(new JScrollPane(bottomText));
	
        mainTable.setFillsViewportHeight(true);
	
        menuBar = new JMenuBar();
        fileMenu = new JMenu("File");
        fileMenu.setMnemonic(KeyEvent.VK_F);
        menuBar.add(fileMenu);
        actionsMenu = new JMenu("Actions");
        actionsMenu.setMnemonic(KeyEvent.VK_A);
        menuBar.add(actionsMenu);
        
        exitMenuItem = new JMenuItem("Quit Server", KeyEvent.VK_Q);
        exitMenuItem.addActionListener(new ActionListener()
        {
            @Override
			public void actionPerformed(ActionEvent e)
            {
            	//int confirmed = JOptionPane.showConfirmDialog(mainFrame, "Shutdown Server: This will stop the entire tournament.", "Shutdown Confirmation", JOptionPane.YES_NO_OPTION);
    			//if (confirmed == JOptionPane.YES_OPTION)
    			{
    				server.thread.interrupt();
    			}
            }
        });
        fileMenu.add(exitMenuItem);
        
        sendClientCommandMenuItem = new JMenuItem("Send Command to all Clients", KeyEvent.VK_C);
        sendClientCommandMenuItem.addActionListener(new ActionListener()
        {
            @Override
			public void actionPerformed(ActionEvent e)
            {
            	String command = (String)JOptionPane.showInputDialog(mainFrame,
            				"Enter Windows command to be executed on all Client machines.\n\n"
            				+ "Will run as if typed into the client's Windows command line.\n\n"
            				+ "Execution is asynchronous to client, no error on failure.\n\n"
            				+ "Example:     notepad.exe\n"
            				+ "Example:     taskkill /im notepad.exe\n\n",
            				"Send Command to Clients", JOptionPane.PLAIN_MESSAGE, null, null, "");
            	
            	if (command != null && command.trim().length() > 0)
            	{
            		server.sendCommandToAllClients(command);
            	}
            }
        });
        actionsMenu.add(sendClientCommandMenuItem);
        
        viewClientScreenMenuItem = new JMenuItem("Take Client Screenshot", KeyEvent.VK_S);
        viewClientScreenMenuItem.addActionListener(new ActionListener()
        {
            @Override
			public void actionPerformed(ActionEvent e)
            {
            	String client = (String)JOptionPane.showInputDialog(mainFrame,
        				"Enter Address of Client\n\n"
        				+ "Will match if substring of address in client window\n\n"
        				+ "If multiple match, multiple will display\n\n"
        				+ "If Client screenshot already open, it will refresh\n\n",
        				"Take Screenshot of Client", JOptionPane.PLAIN_MESSAGE, null, null, "");
            	
            	if (client != null && client.trim().length() > 0)
            	{
            		//TODO: server.sendScreenshotRequestToClient(client);
            	}
            }
        });
        actionsMenu.add(viewClientScreenMenuItem);
        
        
		mainFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
	    mainFrame.setSize(800,600);
	    mainFrame.setJMenuBar(menuBar);
	    mainFrame.add(new JScrollPane(mainTable));
	    
		mainFrame.add(bottomPanel);
	    mainFrame.setVisible(true);
	    
	    
	    
	    mainFrame.addWindowListener(new WindowAdapter()
	    {
	    	@Override
			public void windowClosing(WindowEvent e)
	    	{
    			//int confirmed = JOptionPane.showConfirmDialog(mainFrame, "Shutdown Server: Are you sure?", "Shutdown Confirmation", JOptionPane.YES_NO_OPTION);
    			//if (confirmed == JOptionPane.YES_OPTION)
    			{
    				server.thread.interrupt();
    			}
            }
	    });
	}
	
	public static String getTimeStamp()
	{
		return new SimpleDateFormat("[HH:mm:ss]").format(Calendar.getInstance().getTime());
	}
	
	public synchronized String getHTML() throws Exception
	{
		String table = "<table cellpadding=2 rules=all style=\"font: 12px/1.5em Verdana\">\n";
		table += "  <tr>\n";
		table += "    <td colspan=11 bgcolor=#CCCCCC align=center style=\"font: 16px/1.5em Verdana\">Current Real-Time Client Scheduler / Status</td>\n";
		table += "  </tr>\n";
		table += "  <tr>\n";
		for (int c=0; c<columnNames.length; ++c)
		{
			table += "    <td bgcolor=#cccccc width=67><center>";
			table += columnNames[c];
			table += "    </center></td>\n";
		}
		table += "  </tr>\n";
		
		for (int r=0; r<mainTable.getRowCount(); ++r)
		{
			String colour = "#FFFFFF";
			if (mainTable.getValueAt(r,1).equals("RUNNING")) colour = "#00FF00";
			if (mainTable.getValueAt(r,1).equals("STARTING")) colour = "#FFFF00";
			if (mainTable.getValueAt(r,1).equals("SENDING")) colour = "#FFA500";
			
			table += "  <tr bgcolor=" + colour + ">\n";
			for (int c=0; c<mainTable.getColumnCount(); ++c)
			{
				table += "    <td><center>";
				String s = (String)mainTable.getValueAt(r,c);
				table += s.substring(0, Math.min(8, s.length()));
				table += "    </center></td>\n";
			}
			table += "  </tr>\n";
		}
		table += "</table><br>\n";
		
		return table;
	}
	
	public synchronized void UpdateClient(String name, String status, String num, String host, String join)
	{
		int row = GetClientRow(name);
		if (row != -1)
		{
			getModel().setValueAt(status, row, 1);
			getModel().setValueAt(name, row, 0);
			getModel().setValueAt(num, row, 2);
			
			if (!status.equals("READY") && !status.equals("SENDING"))
			{
				getModel().setValueAt("", row, 3);
				getModel().setValueAt("", row, 4);
				getModel().setValueAt("", row, 5);
				getModel().setValueAt("", row, 6);
				getModel().setValueAt("", row, 7);
			}
			else
			{
				for (int i=3; i<columnNames.length; ++i)
				{
					getModel().setValueAt(mainTable.getValueAt(row, i), row, i);
				}
			}
		}
		else
		{
			getModel().addRow(new Object[]{name, status, num, host, join, "", "", ""});
		}
	}
	
	public synchronized void UpdateRunningStats(RemoteClient client, Game game, int i, long startTime)
	{
		int row = GetClientRow(client.toString());
		
		if (row != -1)
		{
			String time = new SimpleDateFormat("mm:ss").format(new Date(System.currentTimeMillis() - startTime));
			getModel().setValueAt(game.bots[i], row, 3);
			getModel().setValueAt(game.bots[i%2], row, 4);
			getModel().setValueAt(game.map, row, 5);
			getModel().setValueAt(time, row, 6);
		}
	}
	
	public synchronized int GetClientRow(String name)
	{
		for (int r=0; r<getModel().getRowCount(); ++r)
		{
			String rowName = (String)(getModel().getValueAt(r,0));
			if (rowName.equalsIgnoreCase(name))
			{
				return r;
			}
		}
		
		return -1;
	}
	
	public void RemoveClient(String name)
	{
		int row = GetClientRow(name);
		
		if (row != -1)
		{
			getModel().removeRow(row);
		}
		else
		{
			
		}
	}
	
	public void logText(String s)
	{
		bottomText.append(s);
		bottomText.setCaretPosition(bottomText.getDocument().getLength());
	}
	
	private DefaultTableModel getModel()
	{
		return (DefaultTableModel)(mainTable.getModel());
	}
	
	class MyRenderer extends DefaultTableCellRenderer
	{
		private static final long serialVersionUID = -6642925623417572930L;

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
		{
			Component cell = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
		
			String status = (String)table.getValueAt(row, 1);
			
			if(status.equals("RUNNING"))
			{
				cell.setBackground(Color.green);
			}
			else if (status.equals("STARTING"))
			{
				cell.setBackground(Color.yellow);
			}
			else if (status.equals("READY"))
			{
				cell.setBackground(Color.white);
			}
			else if (status.equals("SENDING"))
			{
				cell.setBackground(Color.orange);
			}
			else
			{
				 //this shouldn't happen
				cell.setBackground(Color.red);
			}
			
			return cell;
		}
	}
}
