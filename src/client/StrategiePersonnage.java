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
	
	/**
	 *  Contient le resultat de la clairvoyance
	 */
	protected HashMap<Caracteristique, Integer> statClair = new HashMap<Caracteristique, Integer>();
	
	/**
	 * Reference du personnage sur lequel la clairvoyance a ete effectue
	 */
	int refClair;
	
	/**
	 * Boolean pour savoir si le personnage doit attaquer ou non
	 */
	boolean attacAdv;
	
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
	 *  si potion plus proche
	 *  	si Aduril
	 *  		si potion aporte bonus
	 *  			aller vers elle
	 *  			ramasser
	 *  	si Diablo
	 *  		si ennemi autour
	 * 
	 *  Sinon ennemi plus proche
	 *  	clairvoyance
	 *  	si on peut tuer l'adverssaire
	 *  		si potion teleportation en vision
	 *  			aller vers potion de teleportation
	 *  	sinon fuir
	 *  */
		
		
		
		/* Je n'ai pas de voisin, j'erre */
		if (voisins.isEmpty()) { 
			// si ma vie <60 ,  heal
			if(this.console.getPersonnage().getCaract(Caracteristique.VIE) < 60) 
				arene.lanceAutoSoin(refRMI);
			// sinon j'erre
			else{ 
				console.setPhrase("J'erre...");
				arene.deplace(refRMI, 0); 
			}
		} 
		/* Je vois des choses */
		else {
			/*Recherhce du plus proche voisins*/
			int refCible = Calculs.chercheElementProche(position, voisins);
			int distPlusProche = Calculs.distanceChebyshev(position, arene.getPosition(refCible));

			String elemPlusProche = arene.nomFromRef(refCible);
			
			/* Si une element est suffisamment proche */
			if(distPlusProche <= Constantes.DISTANCE_MIN_INTERACTION) { 
				/* Si l'element est une POTION */
				if(arene.estPotionFromRef(refCible)){ 
					// Anduril
					if(elemPlusProche.equals("Anduril")){
						 // regarder si �a vaut le coup d'aller la prendre
						if(goodPotion(arene, refCible)){
							console.setPhrase("Je ramasse une potion");
							arene.ramassePotion(refRMI, refCible);
						}
						// sinon errer
						else{
							console.setPhrase("J'erre...");
							arene.deplace(refRMI, 0);
						}
					}
					// Diablo
					else{
						/* Si plus d'un ennemi est a proximite */
						if(this.nbEnnemis(voisins, arene) > 1)
						{
							console.setPhrase("Je ramasse une potion");
							arene.ramassePotion(refRMI, refCible);
						}
						else
						{
							/* S'il y a un ennemi */
							if(this.ennemiPlusProche(voisins, arene) != -1)
							{
								stratAttaque(refRMI,refCible,elemPlusProche,arene);
							}
							/* Sinon on erre, en priant pour rester pas loin de la potion */
							else
							{
								console.setPhrase("J'erre...");
								arene.deplace(refRMI, 0);
							}
						}
					}
				/* Si l'element est un etre vivant */
				}else {
					// duel
					console.setPhrase("Je fais un duel avec " + elemPlusProche);
					arene.lanceAttaque(refRMI, refCible);
					arene.deplace(refRMI, refCible);
				}
				
			/* Si l'element est trop eloigne */
			} else { 
				/* L'element est une POTION */
				if(arene.estPotionFromRef(refCible)){
					
					/* ANDURIL */
					if(elemPlusProche.equals("Anduril")){
						 /* Si bonne potion, deplacer */
						if(goodPotion(arene, refCible)){
							console.setPhrase("Je vais vers une potion");
							arene.deplace(refRMI, refCible);
						}
						/* Errer sinon */
						else{
							console.setPhrase("J'erre...");
							arene.deplace(refRMI, 0);
						}
					}					
					/* DIABLO */
					else{
						/* Si plus d'un ennemi est a proximite */
						if(this.nbEnnemis(voisins, arene) > 1)
						{
							console.setPhrase("Je vais vers une potion");
							arene.deplace(refRMI, refCible);
						}
						else
						{
							/* S'il y a un ennemi */
							if(this.ennemiPlusProche(voisins, arene) != -1)
							{
								stratAttaque(refRMI,refCible,elemPlusProche,arene);
							}
							/* Sinon on erre, en priant pour rester pas loin de la potion */
							else
							{
								console.setPhrase("J'erre...");
								arene.deplace(refRMI, 0);
							}
						}
					}	
				}
				/* Si l'element le plus proche est un monstre */
				else if(arene.estMonstreFromRef(refCible)){
					// si force > celle du monstre
					if(this.console.getPersonnage().getCaract(Caracteristique.FORCE) >= 10){
						console.setPhrase("Je vais vers mon voisin " + elemPlusProche);
						arene.deplace(refRMI, refCible);
						arene.lanceAttaque(refRMI, refCible);
					}
					else{
						console.setPhrase("Je fuis...");
						arene.deplace(refRMI, 0); 
					}
				}
				/*Si l'element est un personnage */
				else{ 
					stratAttaque(refRMI,refCible,elemPlusProche,arene);
				}
			}
		}
	}
	/*Strategie a appliquer contre un autre personnage*/
	public void stratAttaque(int refRMI,int refCible,String elemPlusProche,IArene arene) throws RemoteException{
		
		/*Si clairvoyance n'a pas deja ete utilise sur ce personnage*/
		if(refCible != refClair){
			statClair = arene.lanceClairvoyance(refRMI, refCible);
			refClair = refCible;
			attacAdv = this.gagnable(statClair);
		}	
		// si le personnage est plus "fort" on fuit
		if(!attacAdv){
			console.setPhrase("Je fuis ...");
			arene.deplace(refRMI,0); 
		}
		/*Si il a choisi d'attaquer l'autre personnage*/
		else{
			console.setPhrase("Je vais vers mon voisin " + elemPlusProche);
			arene.deplace(refRMI, refCible);
			arene.lanceAttaque(refRMI, refCible);
		}
	}
	
	/**
	 * Fonction qui calcul si le personnage doit attaquer ou non
	 */
	public boolean gagnable(HashMap<Caracteristique, Integer> caractAdv) throws RemoteException{
		boolean gagne = true;
		boolean fin = false;
		int t = 1;
		/*Initialisation des degats*/
		int nosDegats = this.console.getPersonnage().getCaract(Caracteristique.FORCE)-((this.console.getPersonnage().getCaract(Caracteristique.FORCE)*((caractAdv.get(Caracteristique.DEFENSE)-(10*t))/100))*t);
		int sesDegats = caractAdv.get(Caracteristique.FORCE)-(caractAdv.get(Caracteristique.FORCE)*((this.console.getPersonnage().getCaract(Caracteristique.DEFENSE)-(10*t))/100)*t);
		
		/*Si les Cararteristique sont les memes on fuit*/
		if((this.console.getPersonnage().getCaract(Caracteristique.FORCE) == caractAdv.get(Caracteristique.FORCE)) && (this.console.getPersonnage().getCaract(Caracteristique.VIE) == caractAdv.get(Caracteristique.VIE)) && (this.console.getPersonnage().getCaract(Caracteristique.INITIATIVE) == caractAdv.get(Caracteristique.INITIATIVE)) && (this.console.getPersonnage().getCaract(Caracteristique.DEFENSE) == caractAdv.get(Caracteristique.DEFENSE))){
			gagne = false;
			fin = true;
			System.out.println("Yo");
		}
		
		/*Tant qu'un des deux personnages n'a pas tue l'autre*/
		while(!fin){
			/*Si l'adverssaire peut nous tuer au tour t*/
			if((this.console.getPersonnage().getCaract(Caracteristique.VIE) - sesDegats) <= 0){
				/*Si l'adverssaire attaque en premier*/
				if(this.console.getPersonnage().getCaract(Caracteristique.INITIATIVE) < caractAdv.get(Caracteristique.INITIATIVE)){
					gagne = false;
					fin = true;
				}
				/*Si on peut tuer l'adverssaire au tour t*/
				else if((caractAdv.get(Caracteristique.VIE) - nosDegats) <= 0){
					fin = true;
				}else{
					gagne = false;
					fin = true;
				}
			/*Si on peut tuer l'adverssaire au tour t*/
			}else if((caractAdv.get(Caracteristique.VIE) - sesDegats) <= 0){
				fin = true;
			}
			t++;
			/*Calcul des degats pour un tour de plus*/
			nosDegats = this.console.getPersonnage().getCaract(Caracteristique.FORCE)-((this.console.getPersonnage().getCaract(Caracteristique.FORCE)*((caractAdv.get(Caracteristique.DEFENSE)-(10*t))/100))*t);
			sesDegats = caractAdv.get(Caracteristique.FORCE)-(caractAdv.get(Caracteristique.FORCE)*((this.console.getPersonnage().getCaract(Caracteristique.DEFENSE)-(10*t))/100)*t);
		}
		
		return gagne;
	}
	
	/* Verifie la legitimite de la prise d'une potion Anduril */
	public boolean goodPotion(IArene arene, int refCible) throws RemoteException {
		boolean nonAbsolu = false;
		int fav = 0;
		
		if(arene.caractFromRef(refCible, Caracteristique.DEFENSE) + this.console.getPersonnage().getCaract(Caracteristique.DEFENSE) > 0){
			if(arene.caractFromRef(refCible, Caracteristique.DEFENSE) >= 0) fav++; 
		}else nonAbsolu = true;
		
		if(arene.caractFromRef(refCible, Caracteristique.FORCE) + this.console.getPersonnage().getCaract(Caracteristique.FORCE) > 0 && nonAbsolu == false){
			if(arene.caractFromRef(refCible, Caracteristique.FORCE) >= 0) fav++; 
		}else nonAbsolu = true;
		
		if(arene.caractFromRef(refCible, Caracteristique.VIE) + this.console.getPersonnage().getCaract(Caracteristique.VIE) > 0 && nonAbsolu == false){
			if(arene.caractFromRef(refCible, Caracteristique.VIE) >= 0) fav++; 
		}else nonAbsolu = true;

		if(arene.caractFromRef(refCible, Caracteristique.INITIATIVE) + this.console.getPersonnage().getCaract(Caracteristique.INITIATIVE) > 0 && nonAbsolu == false){
			if(arene.caractFromRef(refCible, Caracteristique.INITIATIVE) >= 0) fav++; 
		}else nonAbsolu = true;
		
		
		if(nonAbsolu == true || fav < 3) return false;
		else return true;
	}
	
	/* Renvoie le nombre d'ennemis en vue */
	public int nbEnnemis(HashMap<Integer, Point> voisins, IArene arene) throws RemoteException
	{
		int nbEnnemis = 0;
		for(Integer refVoisin : voisins.keySet())
		{
			if(arene.estPersonnageFromRef(refVoisin))
			{
				nbEnnemis++;
			}
		}
		
		return nbEnnemis;
	}
	
	
	/* Renvoie l'ennemi le plus proche S'IL N'Y A QU'UN SEUL ENNEMI EN VU !!! */
	public int ennemiPlusProche(HashMap<Integer, Point> voisins, IArene arene) throws RemoteException
	{
		for(Integer refEnnemi : voisins.keySet())
		{
			if(arene.estPersonnageFromRef(refEnnemi))
			{
				return refEnnemi;
			}
		}
		return -1;
	}
	
}


