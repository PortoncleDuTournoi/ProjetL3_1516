package client;


import java.awt.Point;
import java.rmi.RemoteException;
import java.util.HashMap;

import client.controle.Console;
import logger.LoggerProjet;
import serveur.IArene;
import serveur.element.Caracteristique;
import serveur.element.Personnage;
import utilitaires.Calculs;
import utilitaires.Constantes;

/**
 * Strategie d'un personnage. 
 */
public class StrategiePersonnage {
	
	/**
	 * Console permettant d'ajouter une phrase et de recuperer le serveur 
	 * (l'arene).
	 */
	protected Console console;
	
	// Contient le resultat de la clairvoyance
	protected HashMap<Caracteristique, Integer> statClair = new HashMap<Caracteristique, Integer>();
	
	protected StrategiePersonnage(LoggerProjet logger){
		logger.info("Lanceur", "Creation de la console...");
	}

	/**
	 * Cree un personnage, la console associe et sa strategie.
	 * @param ipArene ip de communication avec l'arene
	 * @param port port de communication avec l'arene
	 * @param ipConsole ip de la console du personnage
	 * @param nom nom du personnage
	 * @param groupe groupe d'etudiants du personnage
	 * @param nbTours nombre de tours pour ce personnage (si negatif, illimite)
	 * @param position position initiale du personnage dans l'arene
	 * @param logger gestionnaire de log
	 */
	public StrategiePersonnage(String ipArene, int port, String ipConsole, 
			String nom, String groupe, HashMap<Caracteristique, Integer> caracts,
			int nbTours, Point position, LoggerProjet logger) {
		this(logger);
		
		try {
			console = new Console(ipArene, port, ipConsole, this, 
					new Personnage(nom, groupe, caracts), 
					nbTours, position, logger);
			logger.info("Lanceur", "Creation de la console reussie");
			
		} catch (Exception e) {
			logger.info("Personnage", "Erreur lors de la creation de la console : \n" + e.toString());
			e.printStackTrace();
		}
	}

	// TODO etablir une strategie afin d'evoluer dans l'arene de combat
	// une proposition de strategie (simple) est donnee ci-dessous
	/** 
	 * Decrit la strategie.
	 * Les methodes pour evoluer dans le jeu doivent etre les methodes RMI
	 * de Arene et de ConsolePersonnage. 
	 * @param voisins element voisins de cet element (elements qu'il voit)
	 * @throws RemoteException
	 */
	public void executeStrategie(HashMap<Integer, Point> voisins) throws RemoteException {
		// arene
		IArene arene = console.getArene();
		
		// reference RMI de l'element courant
		int refRMI = 0;
		
		// position de l'element courant
		Point position = null;
		
		try {
			refRMI = console.getRefRMI();
			position = arene.getPosition(refRMI);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		
		
		
		/********************************
		 * 	 							*
		 *   STRATEGIE DU PERSONNAGE	*
		 *   							*
		 ********************************/
		/* Strategie 
	 * 
	 * ALGO NON A JOUR
	 * 
	 * 
	 *  si potion plus proche
	 *  	si potion aporte bonus
	 *  		aller vers elle
	 *  		ramasser
	 * 
	 *  Sinon ennemi plus proche
	 *  	clairvoyance
	 *  	si peut se faire defoncer
	 *  		si potion teleportation en vision
	 *  			aller vers potion de teleportation
	 *  	
	 *  
	 *  */
		if (voisins.isEmpty()) { // je n'ai pas de voisins, j'erre
			// si ma vie <60 ,  heal
			if(this.console.getPersonnage().getCaract(Caracteristique.VIE) < 60) arene.lanceAutoSoin(refRMI);
			
			// sinon j'erre
			else{ 
				console.setPhrase("J'erre...");
				arene.deplace(refRMI, 0); 
			}
		} 
		
		else {
			int refCible = Calculs.chercheElementProche(position, voisins);
			int distPlusProche = Calculs.distanceChebyshev(position, arene.getPosition(refCible));

			String elemPlusProche = arene.nomFromRef(refCible);
			
			if(distPlusProche <= Constantes.DISTANCE_MIN_INTERACTION) { // si suffisamment proches
				
				if(arene.estPotionFromRef(refCible)){ // potion
					// ramassage si ça vaut le coup
					// si tp : ramassage que si dans la merde
					
					console.setPhrase("Je ramasse une potion");
					arene.ramassePotion(refRMI, refCible);			
				} else { // personnage ou monstre
					// duel
					console.setPhrase("Je fais un duel avec " + elemPlusProche);
					arene.lanceAttaque(refRMI, refCible);
					arene.deplace(refRMI, refCible);
				}
				
			} else { // si voisins, mais plus eloignes
				
				if(arene.estPotionFromRef(refCible)){ // Potion a distance
					// Anduril : regarder si ça vaut le coup d'aller la prendre
					// Diablo Potion :
						// si ennemi dans champ vision
							// clairvoyance
							// si on va se faire defoncer
								// aller vers potion
							// sinon
								// aller vers personnage
				}
				else if(arene.estMonstreFromRef(refCible)){
					// si force > celle du monstre
					if(this.console.getPersonnage().getCaract(Caracteristique.FORCE) >= 10){
						console.setPhrase("Je vais vers mon voisin " + elemPlusProche);
						arene.deplace(refRMI, refCible);
						arene.lanceAttaque(refRMI, refCible);
					}
					else{
						console.setPhrase("Je fuis comme un homosexuel...");
						arene.deplace(refRMI, 0); 
					}
				}
				
				else{ // personnage
					// clairvoyance
					
					
					// si plus badass que nous, fuir
					/*if(1){
						console.setPhrase("Je fuis comme un homosexuel...");
						arene.deplace(refRMI, 0); 
					}*/
					
					/*else{*/
						console.setPhrase("Je vais vers mon voisin " + elemPlusProche);
						arene.deplace(refRMI, refCible);
						arene.lanceAttaque(refRMI, refCible);
					//}
				}
			}
		}
	}
	
	/********************************************
	 * 											*
	 *	 LISTE DES DIFFERENTES STRATEGIES		*
	 *											*
	 *  copier coller la strategie desiree		*
	 *  en dessous de "strategie du personnage"	*
	 *  	dans executeStrategie()				*
	 * 											*
	 ********************************************/
	
	
	/* Strategie de base
	  else {
			int refCible = Calculs.chercheElementProche(position, voisins);
			int distPlusProche = Calculs.distanceChebyshev(position, arene.getPosition(refCible));

			String elemPlusProche = arene.nomFromRef(refCible);

			if(distPlusProche <= Constantes.DISTANCE_MIN_INTERACTION) { // si suffisamment proches
				// j'interagis directement
				if(arene.estPotionFromRef(refCible)){ // potion
					// ramassage
					console.setPhrase("Je ramasse une potion");

					arene.ramassePotion(refRMI, refCible);			
				} else { // personnage
					// duel
					console.setPhrase("Je fais un duel avec " + elemPlusProche);
					arene.lanceAttaque(refRMI, refCible);
					arene.deplace(refRMI, refCible);
				}
				
			} else { // si voisins, mais plus eloignes
				// je vais vers le plus proche
				console.setPhrase("Je vais vers mon voisin " + elemPlusProche);
				arene.deplace(refRMI, refCible);
				arene.lanceAttaque(refRMI, refCible);
			}
		}
	 */
	
}
