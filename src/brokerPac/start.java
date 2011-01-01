/**
 * 
 */
package brokerPac;

import java.io.FileNotFoundException;
import java.io.IOException;

import javax.security.auth.login.LoginException;

/**
 * @author tom
 *
 */
public class start {

	/**
	 * @param args
	 * @throws IOException 
	 * @throws LoginException 
	 * @throws FileNotFoundException 
	 * @throws NumberFormatException 
	 */
	public static void main(String[] args) throws NumberFormatException, FileNotFoundException, LoginException, IOException {
		Broker broker1=new Broker(args[0],args[1],Integer.parseInt(args[3]));;
		broker1.connectToStockExcange();
	}

}
