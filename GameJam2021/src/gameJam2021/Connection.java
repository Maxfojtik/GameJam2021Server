package gameJam2021;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map.Entry;

import gameJam2021.Game.Ship;

public class Connection 
{
	OutputStream output;
	InputStream input;
	Socket socket;
	byte shipId;
	Game myGame;
	static final byte[] playerDisconnectedMessage = new byte[] {3,0};
	public Connection(Socket sock) throws IOException
	{
		socket = sock;
        output = socket.getOutputStream();
//        output = new BufferedWriter(new OutputStreamWriter(oStream));
        input = socket.getInputStream();
//        input = new BufferedReader(new InputStreamReader(iStream));
        lastMessageTime = System.currentTimeMillis();
	}
	boolean closed = false;
	void disconnect()
	{
		if(!closed)
		{
			if(myGame!=null)
			{
				myGame.connections.remove(this);
			}
			try {
				input.close();
				output.close();
				socket.close();
				closed = true;
			} catch (IOException e) {
				System.err.println("Can't close connection");
			}
			GameJam2021.removeConnection(this);
			System.out.println(this.socket.getInetAddress()+" disconnected");
		}
		if(myGame!=null)
		{
			myGame.playerDisconnected(this);
		}
	}
	byte[] TYPE_TO_LENGTH = new byte[] {1, 5, 13, 1, 1, 9, 1, 9, 1, 9};
	byte[] multiMessageData = null;
	long lastShipSendTime = 0;
	long lastPingTime = 0;
	long roundTripPing = 0;
	void logic() throws IOException
	{
		if(System.currentTimeMillis()-lastMessageTime>4000)
		{
			System.out.println("Disconnecting by inactivity");
			disconnect();
		}
		if(!closed)
		{
//			System.out.println(socket.isClosed());
			try {
				while(input.available()>0)
				{
					byte[] rawData = new byte[1024];
					int numRead = input.read(rawData);
					multiMessageData = new byte[numRead];
					System.arraycopy(rawData, 0, multiMessageData, 0, numRead);
					int index = 0;
					while(index!=multiMessageData.length)
					{
						int messageLength = TYPE_TO_LENGTH[multiMessageData[index]];
						byte[] messageData = new byte[messageLength];
						System.arraycopy(multiMessageData, index, messageData, 0, messageLength);
						parse(messageData);
						index += messageLength;
					}
				}
				if(System.currentTimeMillis()-lastShipSendTime>16)
				{
					lastShipSendTime = System.currentTimeMillis();
					sendEnemies();
				}
				if(System.currentTimeMillis()-lastPingTime>1000)
				{
					lastPingTime = System.currentTimeMillis();
					sendHeartBeat();
				}
				
			} catch (IOException e) {
				System.out.println("Disconnecting because of IO Error");
				disconnect();
//				e.printStackTrace();
			}
		}
		else
		{
			System.out.println("SOCKET CLOSED BUT IM STILL HERE");
		}
	}
	void sendEnemies()
	{
		if(myGame!=null)
		{
			Collection<Entry<Byte, Ship>> ships = myGame.ships.entrySet();
			for(Entry<Byte, Ship> shipEntry : ships)
			{
				Ship s = shipEntry.getValue();
				if(shipEntry.getKey()!=shipId)
				{
//					System.out.println("sending "+shipEntry.getKey()+" to "+shipId);
					sendShip(shipEntry.getKey(), s);
				}
			}
		}
	}
	void sendHeartBeat()
	{
		send(new byte[] {9});
	}
	void sendShip(int id, Ship s)
	{
		ByteBuffer bb = ByteBuffer.wrap(new byte[14]).order(ByteOrder.LITTLE_ENDIAN);
		bb.put((byte)2);//message id
		bb.put((byte)id);//ship id
		bb.putFloat(s.x);
		bb.putFloat(s.y);
		bb.putFloat(s.rot);
		send(bb.array());
	}
	void sendPlayerJoined(int id)
	{
		ByteBuffer bb = ByteBuffer.wrap(new byte[2]);
		bb.put((byte)1);//message id
		bb.put((byte)id);//ship id
		send(bb.array());
	}
	void send(byte[] bytes)
	{
		try {
			output.write(bytes);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	float getFloat(byte[] theArray, int start)
	{
		ByteBuffer bb = ByteBuffer.wrap(theArray).order(ByteOrder.LITTLE_ENDIAN);
		bb.position(start);
		return bb.getFloat();
	}
	String print(byte b)
	{
		return String.format("%02X", b);
	}
	long lastMessageTime = 0;
	long lastPingPrint = 0;
	void parse(byte[] messageData) throws UnsupportedEncodingException
	{
		if(messageData[0]!=2 && messageData[0]!=4)
		{
			//System.out.println(messageData[0]);
		}
		lastMessageTime = System.currentTimeMillis();
		if(messageData[0]==0)//create game message
		{
			Game g = GameJam2021.createGame();
			ByteBuffer buff = ByteBuffer.wrap(new byte[5]);
			buff.put((byte)0);
			buff.put(g.id.getBytes());
			send(buff.array());
		}
		else if(messageData[0]==1)//join game message
		{
			byte[] gameAsBytes = Arrays.copyOfRange(messageData, 1, 5);
			String gameId = new String(gameAsBytes, "ASCII");
			Game g = GameJam2021.getGame(gameId);
			if(g!=null)
			{
				myGame = g;
//				System.out.println("Connected to game "+g.id);
				ByteBuffer buff = ByteBuffer.wrap(new byte[2]);
				buff.put((byte)4);
				buff.put(shipId);
				send(buff.array());
		        shipId = g.playerJoined(this);
//				buff.put((byte)0);
//				buff.put(g.id.getBytes());
//				send(buff.array());
			}
			else
			{
				System.out.println("Tried and failed\t"+gameId);
				ByteBuffer buff = ByteBuffer.wrap(new byte[5]);
				buff.put((byte)0);
				buff.put("0000".getBytes());
				send(buff.array());
			}
		}
		else if(messageData[0]==2)//movement
		{
			float xPos = getFloat(messageData, 1);
			float yPos = getFloat(messageData, 5);
			float rot = getFloat(messageData, 9);
			myGame.setShipPosition(shipId, xPos, yPos, rot);
//			System.out.println("X: "+xPos+"\t Y: "+yPos+"\t R:"+rot);
		}
		else if(messageData[0]==3)//game closed
		{
			disconnect();
		}
		else if(messageData[0]==4)//game closed
		{
			roundTripPing = System.currentTimeMillis()-lastPingTime;
		}
		else if(messageData[0]==5)//explosion
		{
			float xPos = getFloat(messageData, 1);
			float yPos = getFloat(messageData, 5);
			ByteBuffer buff = ByteBuffer.wrap(new byte[9]).order(ByteOrder.LITTLE_ENDIAN);
			buff.put((byte)5);
			buff.putFloat(xPos);
			buff.putFloat(yPos);
			myGame.broadCast(buff.array(), this);
		}
		else if(messageData[0]==6)//died
		{
			System.out.println(socket.getInetAddress()+" Died");
			myGame.playerDisconnected(this);
		}
		else if(messageData[0]==7)//explosion
		{
			float xPos = getFloat(messageData, 1);
			float yPos = getFloat(messageData, 5);
			ByteBuffer buff = ByteBuffer.wrap(new byte[9]).order(ByteOrder.LITTLE_ENDIAN);
			buff.put((byte)7);
			buff.putFloat(xPos);
			buff.putFloat(yPos);
//			System.out.println("Ship Explosion");
			myGame.broadCast(buff.array(), this);
		}
		else if(messageData[0]==8)//respawn
		{
			
		}
		else if(messageData[0]==9)//enemy ping
		{
			float xPos = getFloat(messageData, 1);
			float yPos = getFloat(messageData, 5);
			ByteBuffer buff = ByteBuffer.wrap(new byte[9]).order(ByteOrder.LITTLE_ENDIAN);
			buff.put((byte)8);
			buff.putFloat(xPos);
			buff.putFloat(yPos);
			//System.out.println("Enemy Ping");
			myGame.broadCast(buff.array(), this);
		}
	}
}
