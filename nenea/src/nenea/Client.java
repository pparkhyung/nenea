package nenea;

import nenea.client.file.FileClient;
import nenea.client.operation.OperationClient;
import nenea.client.telnet.TelnetClient;

public class Client {

	public void start() throws Exception {
		System.out.println("Client Start");
		new OperationClient().start();
		//new FileClient().start();
		//new TelnetClient().start();
	}

}
