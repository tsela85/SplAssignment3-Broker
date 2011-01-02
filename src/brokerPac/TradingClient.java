/**
 * 
 */
package brokerPac;

import java.util.Vector;

/**
 * @author tom
 * a object representing a on client of a broker 
 */
public class TradingClient {
	Vector<StockOrder> _buyOrders;
	Vector<StockOrder> _sellOrders;
	
	/**
	 * a {@link TradingClient} constructor
	 */
	public TradingClient() {
		_buyOrders = new Vector<StockOrder>();
		_sellOrders = new Vector<StockOrder>();
	}
	
	/**
	 * adds a buy order the clients orders data base
	 * @param order the new order
	 */
	public void addBuyOrder(StockOrder order) {
		_buyOrders.add(order);
	}
	
	/**
	 * adds a sell order the clients orders data base
	 * @param order the new order
	 */
	public void addSellOrder(StockOrder order) {
		_sellOrders.add(order);
	}

	/**
	 * returns all the buy orders of the client
	 * @return vector of orders
	 */
	public Vector<StockOrder> getBuyOrders() {
		return _buyOrders;
	}
	
	/**
	 * returns all the sell orders of the client
	 * @return vector of orders
	 */
	public Vector<StockOrder> getSellOrders() {
		return _sellOrders;
	}
	
	public void clearOrders() {
		_buyOrders.clear();
		_sellOrders.clear();
	}
}
