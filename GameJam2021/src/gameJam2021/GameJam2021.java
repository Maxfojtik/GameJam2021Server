package gameJam2021;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;

public class GameJam2021 
{
	final static int port = 4162;
	static LinkedList<Connection> connections = new LinkedList<>();
	static LinkedList<Game> games = new LinkedList<Game>();
	public static void main(String args[]) throws InterruptedException
	{
		games.add(new Game());
        ConnectionListener cn = new ConnectionListener();
        new Thread(cn).start();
        long lastPingsPrint = 0;
        while(true)
        {
        	for(int i = 0; i < connections.size(); i++)
        	{
        		try
        		{
        			connections.get(i).logic();
        		}
        		catch(Exception e)
        		{e.printStackTrace();}
        	}
        	if(System.currentTimeMillis()-lastPingsPrint>1000 && connections.size()>0)
        	{
        		lastPingsPrint = System.currentTimeMillis();
        		System.out.println("Pings:");
            	for(int i = 0; i < connections.size(); i++)
            	{
            		System.out.println(connections.get(i).socket.getInetAddress()+"\t"+connections.get(i).roundTripPing);
            	}
        	}
        	Thread.sleep(1);
        }
	}
	static class ConnectionListener implements Runnable
	{
		@Override
		public void run() 
		{
			try (ServerSocket serverSocket = new ServerSocket(port)) {
				 
	            System.out.println("Server is listening on port " + port);
	            
	            while (true) {
	                Socket socket = serverSocket.accept();
	                System.out.println(socket.getInetAddress()+" connected");
	                connections.add(new Connection(socket));
	            }
	 
	        } catch (IOException ex) {
	            System.out.println("Server exception: " + ex.getMessage());
	            ex.printStackTrace();
	        }
		}
	}
	static void removeConnection(Connection con)
	{
		connections.remove(con);
	}
	static Game createGame()
	{
		Game g = new Game();
		games.add(g);
		return g;
	}
	static Game getGame(String id)
	{
		for(int i = 0; i < games.size(); i++)
		{
			Game g = games.get(i);
			if(g.id.equals(id))
			{
				return g;
			}
		}
		return null;
	}
}
