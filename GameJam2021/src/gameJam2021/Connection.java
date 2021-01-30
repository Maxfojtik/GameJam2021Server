package gameJam2021;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
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
	BufferedReader input;
	Socket socket;
	int shipId;
	Game myGame;
	public Connection(Socket sock) throws IOException
	{
		socket = sock;
        output = socket.getOutputStream();
//        output = new BufferedWriter(new OutputStreamWriter(oStream));
        InputStream iStream = socket.getInputStream();
        input = new BufferedReader(new InputStreamReader(iStream));
        myGame = GameJam2021.games.get(0);
        shipId = myGame.playerJoined(this);
	}
	final byte[] ID_TO_LENGTH = new byte[] {1, 5, 13};
	boolean waitingForMessageId = true;
	byte[] messageData = null;
	int messageIndex = 0;
	int targetLength = 0;
	long lastShipSendTime = 0;
	void logic()
	{
		try {
			while(input.ready())
			{
				int code = input.read();
//				System.out.println(waitingForMessageId+":"+code);
				if(waitingForMessageId)
				{
					byte messageId = (byte)code;
					messageData = new byte[ID_TO_LENGTH[messageId]];
					messageData[0] = messageId;
					messageIndex = 1;
					waitingForMessageId = false;
				}
				else
				{
					messageData[messageIndex] = (byte)code;
					messageIndex++;
					if(messageIndex==messageData.length)
					{
						waitingForMessageId = true;
						parse();
					}
				}
			}
			if(System.currentTimeMillis()-lastShipSendTime>16)
			{
				lastShipSendTime = System.currentTimeMillis();
				sendEnemies();
			}
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	void sendEnemies()
	{
		Collection<Entry<Integer, Ship>> ships = myGame.ships.entrySet();
		for(Entry<Integer, Ship> shipEntry : ships)
		{
			Ship s = shipEntry.getValue();
			if(shipEntry.getKey()!=shipId)
			{
//				System.out.println("sending "+shipEntry.getKey()+" to "+shipId);
				sendShip(shipEntry.getKey(), s);
			}
		}
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
		float f = bb.getFloat();
		return f;
	}
	String print(byte b)
	{
		if(b<0)
		{
			return String.format("0x%02X", Math.abs(b) + 127);
		}
		return String.format("0x%02X", b);
	}
	void parse()
	{
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
			byte[] gameAsBytes = Arrays.copyOfRange(messageData, 1, 4);
			String gameId = new String(gameAsBytes);
			Game g = GameJam2021.getGame(gameId);
			if(g!=null)
			{
				ByteBuffer buff = ByteBuffer.wrap(new byte[5]);
				buff.put((byte)0);
				buff.put(g.id.getBytes());
				send(buff.array());
			}
			else
			{
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
			System.out.println(print(messageData[5]) + "\t" + print(messageData[6]) + "\t" + print(messageData[7]) + "\t" + print(messageData[8]));
			float rot = getFloat(messageData, 9);
			myGame.setShipPosition(shipId, xPos, yPos, rot);
//			System.out.println("X: "+xPos+"\t Y: "+yPos+"\t R:"+rot);
		}
	}
}