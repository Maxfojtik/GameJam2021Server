package gameJam2021;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map.Entry;



public class Game 
{

	static final String CONS = "BCDFGHJLMNPRSTVWXYZ";
	static final String VOWELS = "AEIOU";
	static SecureRandom rnd = new SecureRandom();
	LinkedList<Connection> connections = new LinkedList<>();
	String randomString( int len, String AB){
	   StringBuilder sb = new StringBuilder( len );
	   for( int i = 0; i < len; i++ ) 
	      sb.append( AB.charAt( rnd.nextInt(AB.length()) ) );
	   return sb.toString();
	}
	String generateId()
	{
		String potentialId = "";
		while(potentialId.equals(""))
		{
			potentialId = randomString(1, CONS)+randomString(1, VOWELS)+randomString(1, CONS)+randomString(1, VOWELS);
			if(GameJam2021.getGame(potentialId)!=null)
			{
				potentialId = "";
			}
		}
		return potentialId;
	}
	HashMap<Integer, Ship> ships = new HashMap<Integer, Ship>();
	String id;
	int nextShipId = 0;
	public Game()
	{
		id = generateId();
	}
	class Ship
	{
		float x;
		float y;
		float rot;
		void setPos(float x, float y, float rot)
		{
			this.x = x;
			this.y = y;
			this.rot = rot;
		}
	}
	int playerJoined(Connection con)
	{
		int id = nextShipId++;
		for(Entry<Integer, Ship> shipEntry : ships.entrySet())
		{
			con.sendPlayerJoined(shipEntry.getKey());
		}
		ships.put(id, new Ship());
		for(int i = 0; i < connections.size(); i++)
		{
			connections.get(i).sendPlayerJoined(id);
		}
		connections.add(con);
		return id;
	}
	void setShipPosition(int shipId, float xPos, float yPos, float rot)
	{
		ships.get(shipId).setPos(xPos, yPos, rot);
	}
}
