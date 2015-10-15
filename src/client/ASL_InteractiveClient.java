package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class ASL_InteractiveClient {

	public static void main(String[] args) {
		if (args.length < 2) {
			System.err.println("Usage: ASL_Client <host> <port>");
			System.exit(-1);
		}
		String host = args[0];
		int port = Integer.parseInt(args[1]);
		ASL_Client client = new ASL_Client(host, port);
		BufferedReader reader = new BufferedReader(new InputStreamReader(
				System.in));
		String input;
		while (true) {
			try {
				input = reader.readLine();
				String[] tokens = input.split(" ");
				if (tokens[0].toLowerCase().equals("stop")) {
					break;
				} else {
					int q, r, s;
					int[] qs;
					String m;
					ASL_Message msg;
					switch (tokens[0].toLowerCase()) {
					case "register":
						client.register();
						System.out.println("registered with id: "
								+ client.getId());
						break;
					case "cqueue":
						q = client.createQueue();
						System.out.println("created queue: " + q);
						break;
					case "dqueue":
						if (tokens.length > 1) {
							q = Integer.parseInt(tokens[1]);
							client.deleteQueue(q);
							System.out.println("deleted queue");
						} else {
							System.err.println("usage: dqueue <qID>");
						}
						break;
					case "push":
						if (tokens.length > 2) {
							q = Integer.parseInt(tokens[1]);
							if (tokens.length > 3) {
								r = Integer.parseInt(tokens[2]);
								m = tokens[3];
								client.push(q, r, m);
							} else {
								m = tokens[2];
								client.push(q, m);
							}
							System.out.println("sent message");
						} else {
							System.err.println("usage: push <qID> [sID] <msg>");
						}
						break;
					case "poll":
						if (tokens.length > 1) {
							q = Integer.parseInt(tokens[1]);
							if (tokens.length > 2) {
								s = Integer.parseInt(tokens[2]);
								msg = client.poll(q, s);
							} else {
								msg = client.poll(q);
							}
							System.out.println("polled message:\n"
									+ msg.toString());
						} else {
							System.err.println("usage: poll <qID> [sID]");
						}
						break;
					case "peek":
						if (tokens.length > 1) {
							q = Integer.parseInt(tokens[1]);
							if (tokens.length > 2) {
								s = Integer.parseInt(tokens[2]);
								msg = client.poll(q, s);
							} else {
								msg = client.poll(q);
							}
							System.out.println("peeked message:\n"
									+ msg.toString());
						} else {
							System.err.println("usage: poll <qID> [sID]");
						}
						break;
					case "gqueues":
						qs = client.getQueues();
						if (qs.length > 0) {
							System.out.print("messages on queues: ");
							for (int i = 0; i < qs.length; i++) {
								if (i == qs.length - 2) {
									System.out.print(qs[i] + " and ");
								} else if (i == qs.length - 1) {
									System.out.println(qs[i]);
								} else {
									System.out.print(qs[i] + ", ");
								}
							}
						} else {
							System.out
									.println("no messages waiting for you :(");
						}
						break;
					case "gmessage":
						if (tokens.length > 1) {
							s = Integer.parseInt(tokens[1]);
							msg = client.getMessage(s);
							System.out.println("got message:\n"
									+ msg.toString());
						} else {
							System.err.println("usage: gmessage <sID>");
						}
						break;
					case "help":
						System.out
								.println("commands:\n"
										+ "\t-register                  register as new user\n"
										+ "\t-cqueue                    create a new queue\n"
										+ "\t-dqueue <qID>              delete the queue with id=qID\n"
										+ "\t-push <qID> [rID] <msg>    push a message on queue with id=qID with an optional receiver\n"
										+ "\t-poll <qID> [sID]          poll a message from queue with id=qID with an  optional sender\n"
										+ "\t-peek <qID> [sID]          peek a message from queue with id=qID with an  optional sender\n"
										+ "\t-gqueues                   query for queues with messages waiting for you\n"
										+ "\t-gmessage <sID>            query for a message from a particular sender");
						break;
					default:
						System.err.println("unknown command: " + tokens[0]);
						break;
					}
				}
			} catch (ASL_Exception e) {
				System.err.println(e.getLocalizedMessage());
			} catch (IOException e) {
				e.printStackTrace();
				break;
			}
		}
	}

}
