package eu.anasta.bm.notifier.receiver;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

import org.apache.log4j.Logger;

import eu.anasta.bm.notifier.ui.Notification;

public class NewMailReceiver {
	int port;


	String host;


	public NewMailReceiver(int port, String host) {
		super();
		this.port = port;
		this.host= host;
	}

	public void launch() {
		/* Indique si l'instance du programme est unique. */
		boolean unique;

		try {
			/* On crée une socket sur le port défini. */
			final ServerSocket server = new ServerSocket(port);

			/*
			 * Si la création de la socket réussit, c'est que l'instance du
			 * programme est unique, aucune autre n'existe.
			 */
			unique = true;

			/* On lance un Thread d'écoute sur ce port. */
			Thread portListenerThread = new Thread(
					"UniqueInstance-PortListenerThread") {

				{
					setDaemon(true);
				}

				@Override
				public void run() {
					/* Tant que l'application est lancée... */
					while (true) {
						try {
							/*
							 * On attend qu'une socket se connecte sur le
							 * serveur.
							 */
							final Socket socket = server.accept();

							/*
							 * Si une socket est connectée, on écoute le message
							 * envoyé dans un nouveau Thread.
							 */
							new Thread("UniqueInstance-SocketReceiver") {

								{
									setDaemon(true);
								}

								@Override
								public void run() {
									Logger.getLogger("BMNotifier-NewMailReceiver").info(
											"receive message");

									receive(socket);
								}
							}.start();
						} catch (IOException e) {
							Logger.getLogger("BMNotifier-NewMailReceiver").warn(
									"Attente de connexion de socket échouée.");
						}
					}
				}
			};

			/* On démarre le Thread. */
			portListenerThread.start();
		} catch (IOException e) {

		}
	}

	private synchronized void receive(Socket socket) {
		Scanner sc = null;

		try {
			/*
			 * On n'écoute que 5 secondes, si aucun message n'est reçu, tant
			 * pis...
			 */
			socket.setSoTimeout(5000);

			/* On définit un Scanner pour lire sur l'entrée de la socket. */
			sc = new Scanner(socket.getInputStream());

			/* On ne lit qu'une ligne. */
			String s = sc.nextLine();
			Notification.getInstance().sendMailTo(host, s);
		} catch (IOException e) {
			Logger.getLogger("UniqueInstance").warn(
					"Lecture du flux d'entrée de la socket échoué.");
		} finally {
			if (sc != null)
				sc.close();
		}

	}
}
