/*
 *  Klasa agenta sprzedawcy książek.
 *  Sprzedawca dysponuje katalogiem książek oraz dwoma klasami zachowań:
 *  - OfferRequestsServer - obsługa odpowiedzi na oferty klientów
 *  - PurchaseOrdersServer - obsługa zamówienia klienta
 *
 *  Argumenty projektu (NETBEANS: project properties/run/arguments):
 *  -agents seller1:BookSellerAgent();seller2:BookSellerAgent();buyer1:BookBuyerAgent(Zamek) -gui
 */
import jade.core.Agent;
import jade.core.behaviours.*;
import jade.lang.acl.*;
import java.util.*;
import java.lang.*;


public class BookSellerAgent extends Agent
{
    
  public int staraCena = 0;
  // Katalog lektur na sprzedaż:
  private Hashtable catalogue;

  // Inicjalizacja klasy agenta:
  protected void setup()
  {
    // Tworzenie katalogu lektur jako tablicy rozproszonej
    catalogue = new Hashtable();

    Random randomGenerator = new Random();    // generator liczb losowych

    catalogue.put("Zamek", 90+randomGenerator.nextInt(500));       // nazwa lektury jako klucz, cena jako wartość
    catalogue.put("Opowiadania", 110+randomGenerator.nextInt(200));
    catalogue.put("Ameryka", 300+randomGenerator.nextInt(70));
    catalogue.put("Proces", 250+randomGenerator.nextInt(250));

    doWait(2019);                     // czekaj 2 sekundy

    System.out.println("Witam! Agent-sprzedawca (wersja d <2018/19>) "+getAID().getName()+" jest gotów do działania!");

    // Dodanie zachowania obsługującego odpowiedzi na oferty klientów (kupujących książki):
    addBehaviour(new OfferRequestsServer());

    // Dodanie zachowania obsługującego zamówienie klienta:
    addBehaviour(new PurchaseOrdersServer());
  }

  // Metoda realizująca zakończenie pracy agenta:
  protected void takeDown()
  {
    System.out.println("Agent-sprzedawca (wersja d <2018/19>) "+getAID().getName()+" zakończył się.");
  }


  /**
    Inner class OfferRequestsServer.
    This is the behaviour used by Book-seller agents to serve incoming requests
    for offer from buyer agents.
    If the requested book is in the local catalogue the seller agent replies
    with a PROPOSE message specifying the price. Otherwise a REFUSE message is sent back.
    */
    class OfferRequestsServer extends CyclicBehaviour
    {
      public void action()
      {
        // Tworzenie szablonu wiadomości (wstępne określenie tego, co powinna zawierać wiadomość)
        MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
        // Próba odbioru wiadomości zgodnej z szablonem:
        ACLMessage msg = myAgent.receive(mt);
        if (msg != null) {  // jeśli nadeszła wiadomość zgodna z ustalonym wcześniej szablonem
          String title = msg.getContent();  // odczytanie tytułu zamawianej książki

          System.out.println("Agent-sprzedawca  "+getAID().getName()+" otrzymał komunikat: "+
                   title);
          ACLMessage reply = msg.createReply();               // tworzenie wiadomości - odpowiedzi
          Integer price = (Integer) catalogue.get(title);     // ustalenie ceny dla podanego tytułu
          staraCena = price;
          if (price != null) {                                // jeśli taki tytuł jest dostępny
            reply.setPerformative(ACLMessage.PROPOSE);            // ustalenie typu wiadomości (propozycja)
            reply.setContent(String.valueOf(price.intValue()));   // umieszczenie ceny w polu zawartości (content)
            System.out.println("Agent-sprzedawca "+getAID().getName()+" odpowiada: "+
                   price.intValue());
          }
          else {                                              // jeśli tytuł niedostępny
            // The requested book is NOT available for sale.
            reply.setPerformative(ACLMessage.REFUSE);         // ustalenie typu wiadomości (odmowa)
            reply.setContent("tytuł niestety niedostępny");                  // treść wiadomości
          }
          myAgent.send(reply);                                // wysłanie odpowiedzi
        }
        else                         // jeśli wiadomość nie nadeszła, lub była niezgodna z szablonem
        {
          block();                   // blokada metody action() dopóki nie pojawi się nowa wiadomość
        }
      }
    } // Koniec klasy wewnętrznej będącej rozszerzeniem klasy CyclicBehaviour


    class PurchaseOrdersServer extends CyclicBehaviour
    {int oldPrice = 0;
        
        
      public void action()
      {
        ACLMessage msg = myAgent.receive();
        
        if (msg != null)
        {
            if(msg.getPerformative() == ACLMessage.ACCEPT_PROPOSAL)
            {
              // Message received. Process it          
              ACLMessage reply = msg.createReply();
              String title = msg.getContent();
              reply.setPerformative(ACLMessage.INFORM);
              System.out.println("Agent sprzedający (wersja d <2018/19>) "+getAID().getName()+" sprzedał książkę: "+title);
              myAgent.send(reply);
            }
            if (msg.getPerformative() == ACLMessage.PROPOSE)
            {
                System.out.println("getinReplyto: " + msg.getReplyWith());
                int proposedPrice = Integer.parseInt(msg.getReplyWith());
                
                Integer newPrice = (Integer)(3 * staraCena / 4 + proposedPrice / 4);
                staraCena = newPrice;
                ACLMessage reply = msg.createReply();               // tworzenie wiadomości - odpowiedzi
                reply.setPerformative(ACLMessage.PROPOSE);            // ustalenie typu wiadomości (propozycja)
                
                reply.setReplyWith(String.valueOf(newPrice));   // umieszczenie ceny w polu zawartości (content)
                System.out.println("Agent-sprzedawca "+getAID().getName()+" odpowiada: "+ newPrice);
                myAgent.send(reply);
            }
            
            if(msg.getPerformative() == ACLMessage.REFUSE)
            {
                System.out.println("Agent-sprzedawca otrzymał: Odmowa dalszych negocjacji");
                block(); 
            }
        }
      }
    } // Koniec klasy wewnętrznej będącej rozszerzeniem klasy CyclicBehaviour
} // Koniec klasy będącej rozszerzeniem klasy Agent