package server;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;

import javax.imageio.ImageIO;

import objects.ClientStatus;
import objects.ClientStatusMessage;
import objects.FileMessage;
import objects.ImageWindow;
import objects.InstructionMessage;
import objects.Message;
import objects.ScreenshotMessage;

public class ServerClientThread extends Thread implements Comparable<ServerClientThread>
{
	
	private InetAddress 		address;
	public  InstructionMessage 	lastInstructionSent = null;
	private Socket 				con;
	private ClientStatus 		status;
	private Server 				server;
	
	private ObjectInputStream 	ois = null;
	private ObjectOutputStream 	oos = null;
	private boolean	 			run = true;
	
	ImageWindow					imageWindow;

	public ServerClientThread(Socket con, Server man) throws SocketException
	{
		address = con.getInetAddress();
		this.con = con;
		con.setKeepAlive(true);
		server = man;
		status = ClientStatus.READY;
	}

	@Override
	public void run()
	{
		setupConnectionStreams();

		while (run)
		{
			try
			{
				Message m = (Message) ois.readObject();
				handleClientMessage(m);
			}
			catch (Exception e)
			{
				server.log("Excpetion in ManagerClientThread, removing client\n");
				e.printStackTrace();
				server.removeClient(this);
				run = false;
			}
		}
	}
	
	private void setupConnectionStreams()
	{
		try
		{
			oos = new ObjectOutputStream(con.getOutputStream());
			ois = new ObjectInputStream(con.getInputStream());
		}
		catch (Exception e)
		{
			server.log("ManagerClientThread Object Streams could not initialize\n");
			e.printStackTrace();
		}
	}
	
	private void handleClientMessage(Message msg) throws IOException
	{
		if (msg != null)
		{
			server.log(msg+" received from client "+server.getClientIndex(this)+"\n");
			
			if (msg instanceof ClientStatusMessage)
			{
				updateClientStatus((ClientStatusMessage)msg);
			}
			else if (msg instanceof FileMessage)
			{
				((FileMessage) msg).write(server.env);
			}
			else if (msg instanceof ScreenshotMessage)
			{
				InputStream in = new ByteArrayInputStream(((ScreenshotMessage) msg).data);
				BufferedImage screenshot = ImageIO.read(in);
				
				if (imageWindow == null)
					imageWindow = new ImageWindow(this.getAddress().toString(), screenshot);
				else
					imageWindow.refresh(screenshot);
			}
		}
	}
	
	private void updateClientStatus(ClientStatusMessage m)
	{
		status = m.status;
		server.updateClient(this);
		
		// if the status is sending, grab the replay
		if (status == ClientStatus.SENDING)
		{
			server.receiveGameResults(m.game);
		}
		
		if (m.status == ClientStatus.RUNNING && m.gameState != null)
		{
			server.updateRunningStats(this.toString(), m.gameState, m.isHost);
		}
		
		if (m.status == ClientStatus.STARTING && m.startingTime > 0)
		{
			server.updateStartingStats(this.toString(), m.startingTime);
		}
	}

	public synchronized void sendMessage(Message m) throws IOException
	{
		server.log(m+" sending to Client " + server.getClientIndex(this) + "\n");
		
		oos.writeObject(m);
		oos.flush();
		oos.reset();
		
		if (m instanceof InstructionMessage)
		{
			lastInstructionSent = (InstructionMessage)m;
		}
	}
	
	@Override
	public String toString()
	{
		return getAddress().toString().replaceAll("/", "");
	}
	
	public synchronized InetAddress getAddress()
	{
		return address;
	}

	public synchronized ClientStatus getStatus()
	{
		return status;
	}

	public void stopThread()
	{
		interrupt();
		try
		{
			con.close();
		}
		catch
		(IOException e)
		{
			e.printStackTrace();
		}
		
		run = false;
	}

	public synchronized void setStatus(ClientStatus status)
	{
		this.status = status;
	}

	@Override
	public int compareTo(ServerClientThread arg0)
	{
		return address.equals((arg0).getAddress()) ? 0 : 1;
	}
}
