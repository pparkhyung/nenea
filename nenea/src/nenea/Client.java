package nenea;

import nenea.client.operation.UptimeClient;

public class Client {

	public void start() throws Exception {
		System.out.println("nenea booting");
		//new OperationClient().start();
		UptimeClient.start();
		//new FileClient().start();
		//new TelnetClient().start();
	}

}
