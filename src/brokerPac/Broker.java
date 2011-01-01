/**
 * 
 */
package brokerPac;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;

import javax.security.auth.login.LoginException;

import Stomp.Client;
import Stomp.Listener;

/**
 * @author tom
 *
 */

public class Broker implements Listener {
	static final int N=4;				 //Max number of clients the broker can deal with
	private Client _brokerStompClient;	// Stomp Client
	private final String _name;
	private final double _commission;
	private double _cash;
	private int _day;
	private Map<String,TradingClient> _clients;
	private boolean _connected;
	private int _numClosedCliends;


	public Broker(String name,String server, int port, String login, String pass) throws FileNotFoundException, IOException, LoginException {
		Properties p = new Properties();
		p.load(new FileInputStream(name+".ini"));
		_name= p.getProperty("brokerName");
		_commission= Double.parseDouble(p.getProperty("commissionRate"));
		_cash=0;
		_connected=false;
		_day=0;
		_clients = new HashMap<String, TradingClient>();
		_numClosedCliends=0;
		_brokerStompClient = new Client(server,port,login,pass);
	}

	public Broker(String name,String server, int port) throws FileNotFoundException, LoginException, IOException {
		this(name,server,port,"login","pass");
	}

	public void connectToStockExcange() {
		//connect to StockExcange	
		_brokerStompClient.send("/topic/bConnect","connect "+ _name +"\n");
		//listen to the StockExcange connection acknowledge
		_brokerStompClient.subscribe("/topic/bConnected", this);   				

	}

	/* (non-Javadoc)
	 * @see Stomp.Listener#message(java.util.Map, java.lang.String)
	 */
	@Override
	public void message(Map headers, String body,String origin) {
		body=body.replace("\n","");
		body=body.replace("\r","");
		Vector<String> parts = new Vector<String>();
		for (String s : body.split(" "))
			parts.add(s);
		// connected to stockExcange
		if ((parts.elementAt(0).equals("connected")) && 
				(parts.size() == 2) && 
				(parts.elementAt(1).equals(_name))) { 
			connectedToStockExcange();
			return;
		}
		if (_connected) {
			// client connected to broker
			if ((parts.elementAt(0).equals("connected")) && (parts.size() == 3) && (parts.elementAt(2).equals(_name))) {
				clientConnected(parts.elementAt(1));
				return;
			}
			// client disconnected
			if ((parts.elementAt(0).equals("disconnect"))  && (parts.size() == 2)) {
				clientDisconnected(parts.elementAt(1));
				return;
			}
			// new day
			if ((parts.elementAt(0).equals("newDay")) && (parts.size() == 2)) {
				newDay(Integer.parseInt(parts.elementAt(1)));
				return;
			} 
			// the buyer in a deal
			if ((parts.elementAt(0).equals("deal")) && (parts.size() == 8) && (parts.elementAt(2).equals(_name)))  {
				deal("dealBought",parts.elementAt(1),parts.elementAt(5),Integer.parseInt(parts.elementAt(6)),Double.parseDouble(parts.elementAt(7)));
				return;
			}
			// the seller in a deal
			if ((parts.elementAt(0).equals("deal")) && (parts.size() == 8) && (parts.elementAt(4).equals(_name)))  {
				deal("dealSold",parts.elementAt(3),parts.elementAt(5),Integer.parseInt(parts.elementAt(6)),Double.parseDouble(parts.elementAt(7)));
				return;
			} 
			// buyOrder
			if ((parts.elementAt(0).equals("buyOrder"))  && (parts.size() == 6) && (parts.elementAt(2).equals(_name))) {
				buyOrder(parts.elementAt(1),parts.elementAt(3),parts.elementAt(4),parts.elementAt(5));
				return;
			}
			// sellOrder
			if ((parts.elementAt(0).equals("sellOrder"))  && (parts.size() == 6) && (parts.elementAt(2).equals(_name))) {
				sellOrder(parts.elementAt(1),parts.elementAt(3),parts.elementAt(4),parts.elementAt(5));
				return;
			}
			// close day
			if ((parts.elementAt(0).equals("closeDay")) && (parts.size() == 3)) {
				ClientclosedDay(parts.elementAt(1),Integer.parseInt(parts.elementAt(2)));
				return;
			} 

		}
	} 


	private void deal(String type,String clientName,String stockName,int amount,double price) {
		_cash+=(price*amount)*_commission;
		_brokerStompClient.send("/topic/cDeals-"+clientName,
				type+" "+clientName+" "+stockName+" "+amount+" "+price+" "+_commission+"\n");
	}

	public void connectedToStockExcange() {
		_brokerStompClient.subscribe("/topic/cConnected", this);
		_brokerStompClient.subscribe("/topic/bConnected", this);
		_brokerStompClient.subscribe("/topic/cDisconnected", this);
		_brokerStompClient.subscribe("/topic/bOrders-"+_name, this);
		_brokerStompClient.subscribe("/topic/bDeals-"+_name, this);
		_brokerStompClient.subscribe("/topic/Calendar", this);
		_connected=true;
	}

	private void ClientclosedDay(String clientName, int day) {
		_numClosedCliends++;
		if (_clients.size() == _numClosedCliends) 
			brokerCloseDay();
	}

	private void brokerCloseDay() {
		for (TradingClient client : _clients.values()) {
			for(StockOrder order : client.getBuyOrders())
				_brokerStompClient.send("/topic/Orders",order.toString()+"\n");
			for(StockOrder order : client.getSellOrders())
				_brokerStompClient.send("/topic/Orders",order.toString()+"\n");
		}
		_brokerStompClient.send("/topic/Orders", "closeDay "+_name+" "+_day +"\n");		
	}

	private void sellOrder(String clientName, String shares,String stockName, String price) {
		StockOrder order= new StockOrder("sellOrder",clientName,_name,Integer.parseInt(shares),stockName,Double.parseDouble(price));
		TradingClient test= _clients.get(clientName);
		test.addSellOrder(order);
	}

	private void buyOrder(String clientName, String shares,String stockName, String price) {
		StockOrder order= new StockOrder("buyOrder",clientName,_name,Integer.parseInt(shares),stockName,Double.parseDouble(price));
		_clients.get(clientName).addBuyOrder(order);
	}

	private void newDay(int day) {
		_numClosedCliends=0;
		_day=day;
	}

	private void clientDisconnected(String clientName) {
		_clients.remove(clientName);
		if (_clients.size() == _numClosedCliends) 
			brokerCloseDay();
	}

	private void clientConnected(String clientName) {
		if (_clients.size() < N) 
			_clients.put(clientName, new TradingClient());
	}

	public double getCash() {
		return _cash;
	}
	
}
