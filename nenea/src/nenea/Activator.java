package nenea;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import nenea.info.CpuInfo;

public class Activator implements BundleActivator {

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.
	 * BundleContext)
	 */
	public void start(BundleContext context) throws Exception {

		System.out.println("Client 시작");
		new Client().start();
		
		System.loadLibrary("sigar-amd64-winnt"); //.dll을 뺴고 쓴다
		
		new CpuInfo().monitor();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		System.out.println("Goodbye World!!");
	}

}
