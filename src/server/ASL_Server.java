package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.sql.SQLException;

public class ASL_Server implements Runnable {

	private int portNumber;

	private ASL_ConnectionPool pool;

	private ASL_ClientHandler[] workers;
	private Thread[] workerThreads;
	private ASL_Listener listener;
	private Thread listenerThread;

	public boolean listening;
	public boolean useNew;

	public ASL_Server(int portNumber, String url, String user, String password,
			String driverClassName) throws ClassNotFoundException, SQLException {
		useNew = false;
		this.portNumber = portNumber;

		this.pool = new ASL_ConnectionPool(30, url, user, password,
				driverClassName);
	}

	/**
	 * Creates a new ASL_Server object connected to a database
	 * 
	 * @param portNumber
	 *            the port the server will listen on. If there are multiple
	 *            listeners they will choose the next higher ports
	 * @param url
	 *            the url of the database
	 * @param user
	 *            the user account to be used
	 * @param password
	 *            the password for the user
	 * @param driverClassName
	 *            the name of the JDBC driver
	 * @param poolSize
	 *            the size of the connection pool to the database
	 * @param nWorkers
	 *            the number of worker threads created
	 * @throws ClassNotFoundException
	 *             if the JDBC driver could not be found
	 * @throws IOException
	 *             if the port is not available
	 */
	public ASL_Server(int portNumber, String url, String user, String password,
			String driverClassName, int poolSize, int nWorkers)
			throws ClassNotFoundException, IOException {

		useNew = true;
		this.portNumber = portNumber;

		this.pool = new ASL_ConnectionPool(poolSize, url, user, password,
				driverClassName);

		this.listener = new ASL_Listener(portNumber);
		this.listenerThread = new Thread(this.listener, "ASL listener");
		this.listenerThread.setDaemon(true);
		
		this.workers = new ASL_ClientHandler[nWorkers];
		this.workerThreads = new Thread[nWorkers];
		for (int i = 0; i < this.workerThreads.length; i++) {
			this.workers[i] = new ASL_ClientHandler(this.listener.getQueue(),
					this.pool);
			this.workerThreads[i] = new Thread(this.workers[i], "ASL worker "
					+ i);
			this.workerThreads[i].setDaemon(true);
		}
	}

	@Override
	public void run() {
		if (useNew) {
			System.out.println("starting server...");
			try {
				this.pool.initPool();
			} catch (SQLException e1) {
				System.out.println("could not connect to database");
				return;
			}
			for (int i = 0; i < workerThreads.length; i++) {
				workerThreads[i].start();
			}
			//listenerThread.start();
			System.out.println("server is running");
			listener.run();
		} else {
			listening = true;
			try (ServerSocket serverSocket = new ServerSocket(portNumber);) {
				while (listening) {
					new Thread(new ASL_ClientHandler(serverSocket.accept(),
							pool), "ASL_ClientHandler").start();
				}
			} catch (IOException ex) {
				System.err.println("Could not listen on port " + portNumber);
			}
		}
	}

	public void shutdown() {
		System.out.println("server shutting down...");
		for (int i = 0; i < workers.length; i++) {
			workerThreads[i].interrupt();
		}
		for (int i = 0; i < workers.length; i++) {
			try {
				workerThreads[i].join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		listener.stop();
		System.out.println("server stopped");
	}

	public static void main(String[] args) {
		if (args.length < 4) {
			System.err
					.println("Usage: ASL_Server <port> <db-url> <db-user> <db-password>");
			System.exit(-1);
		}
		int port = Integer.parseInt(args[0]);
		String url = args[1];
		String user = args[2];
		String password = args[3];
		String driverName = "org.postgresql.Driver";

		try {
			ASL_Server server = new ASL_Server(port, url, user, password,
					driverName, 30, 10);
			
			Thread mainThread = Thread.currentThread();
			Runtime.getRuntime().addShutdownHook(new Thread() {
				public void run() {
					System.out.println("shutting down");
					server.shutdown();
					try {
						mainThread.join(4000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			});
			server.run();
			//server.shutdown();
		} catch (ClassNotFoundException ex) {
			System.err.println("JDBC Driver not found");
			System.exit(-1);
		} catch (IOException e) {
			System.err.println("Could not listen on port " + port);
			System.exit(-1);
		}
	}

}

// Runtime.getRuntime().addShutdownHook(new Thread() {
// public void run() {
// System.out.println("server shutting down...");
// for(int i = 0; i < workers.length; i++){
// workers[i].interrupt();
// }
// for(int i = 0; i < workers.length; i++){
// try {
// workers[i].join();
// } catch (InterruptedException e) {
// e.printStackTrace();
// }
// }
//
// listener.stop();
// try {
// listenerThread.join();
// } catch (InterruptedException e) {
// e.printStackTrace();
// }
// System.out.println("server stopped");
// }
// });
