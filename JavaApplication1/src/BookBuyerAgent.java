/*
 *
 *
 *
 *  Klasa agenta kupującego książki w imieniu właściciela
 *
 *  Argumenty projektu (NETBEANS: project properties/run/arguments):
 *  -agents seller1:BookSellerAgent();seller2:BookSellerAgent();buyer1:BookBuyerAgent(Zamek) -gui
 */
        
import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.*;
import jade.lang.acl.*;
import java.util.*;

// Przykładowa klasa zachowania:
class MyOwnBehaviour extends Behaviour
{
  protected MyOwnBehaviour()
  {  System.out.println("Konstruktor w MyOwnBehavior");

  }

  protected void setup()
  {
            System.out.println("Setup w MyOwnBehavior");

  }
  public void action()
  {
      System.out.println("Action w MyOwnBehavior");
  }
  public boolean done() {
            System.out.println("Done w MyOwnBehavior");

    return false;
  }
}

public class BookBuyerAgent extends Agent {

    private String targetBookTitle;    // tytuł kupowanej książki przekazywany poprzez argument wejściowy
    // lista znanych agentów sprzedających książki (w przypadku użycia żółtej księgi - usługi katalogowej, sprzedawcy
    // mogą być dołączani do listy dynamicznie!
    private AID[] sellerAgents = {
      new AID("seller1", AID.ISLOCALNAME),
      new AID("seller2", AID.ISLOCALNAME)};
    
    // Inicjalizacja klasy agenta:
    protected void setup()
    {
     
      //doWait(6000);   // Oczekiwanie na uruchomienie agentów sprzedających

      System.out.println("Witam! Agent-kupiec "+getAID().getName()+" (wersja d <2018/19>) jest gotów!");

      Object[] args = getArguments();  // lista argumentów wejściowych (tytuł książki)

      if (args != null && args.length > 0)   // jeśli podano tytuł książki
      {
        targetBookTitle = (String) args[0];
        System.out.println("Zamierzam kupić książkę zatytułowaną "+targetBookTitle);

        addBehaviour(new RequestPerformer());  // dodanie głównej klasy zachowań - kod znajduje się poniżej
       
      }
      else
      {
        // Jeśli nie przekazano poprzez argument tytułu książki, agent kończy działanie:
        System.out.println("Proszę podać tytuł lektury w argumentach wejściowych agenta kupującego!");
        doDelete();
      }
    }
    // Metoda realizująca zakończenie pracy agenta:
    protected void takeDown()
    {
      System.out.println("Agent-kupiec "+getAID().getName()+" kończy.");
    }

    /**
    Inner class RequestPerformer.
    This is the behaviour used by Book-buyer agents to request seller
    agents the target book.
    */
    private class RequestPerformer extends Behaviour
    {
       
      private AID bestSeller;     // agent sprzedający z najkorzystniejszą ofertą
      private int bestPrice;      // najlepsza cena
      private int repliesCnt = 0; // liczba odpowiedzi od agentów
      private MessageTemplate mt; // szablon odpowiedzi
      private int step = 0;       // krok
      private int negocjacja = 0;

      public void action()
      {
        switch (step) {
        case 0:      // wysłanie oferty kupna
          System.out.print(" Oferta kupna (CFP) jest wysyłana do: ");
          for (int i = 0; i < sellerAgents.length; ++i)
          {
            System.out.print(sellerAgents[i]+ " ");
          }
          System.out.println();

          // Tworzenie wiadomości CFP do wszystkich sprzedawców:
          ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
          for (int i = 0; i < sellerAgents.length; ++i)
          {
            cfp.addReceiver(sellerAgents[i]);                // dodanie adresata
          }
          cfp.setContent(targetBookTitle);                   // wpisanie zawartości - tytułu książki
          cfp.setConversationId("handel_ksiazkami");         // wpisanie specjalnego identyfikatora korespondencji
          cfp.setReplyWith("cfp"+System.currentTimeMillis()); // dodatkowa unikatowa wartość, żeby w razie odpowiedzi zidentyfikować adresatów
          myAgent.send(cfp);                           // wysłanie wiadomości

          // Utworzenie szablonu do odbioru ofert sprzedaży tylko od wskazanych sprzedawców:
          mt = MessageTemplate.and(MessageTemplate.MatchConversationId("handel_ksiazkami"),
                                   MessageTemplate.MatchInReplyTo(cfp.getReplyWith()));
          step = 1;     // przejście do kolejnego kroku
          break;
        case 1:      // odbiór ofert sprzedaży/odmowy od agentów-sprzedawców
          ACLMessage reply = myAgent.receive(mt);      // odbiór odpowiedzi
          if (reply != null)
          {
            if (reply.getPerformative() == ACLMessage.PROPOSE)   // jeśli wiadomość jest typu PROPOSE
            {
              int price = Integer.parseInt(reply.getContent());  // cena książki
              if (bestSeller == null || price < bestPrice)       // jeśli jest to najlepsza oferta
              {
                bestPrice = price;
                bestSeller = reply.getSender();
              }
            }
            repliesCnt++;                                        // liczba ofert
            if (repliesCnt >= sellerAgents.length)               // jeśli liczba ofert co najmniej liczbie sprzedawców
            {
              step = 2;
            }
          }
          else
          {
            block();
          }
          break;
        case 2:      // wysłanie zamówienia do sprzedawcy, który złożył najlepszą ofertę
          if (negocjacja == 0){
            bestPrice = bestPrice / 2;             
          }
          else if (negocjacja < 6){
            bestPrice = bestPrice + 5;
          }
          else {
              ACLMessage order = new ACLMessage(ACLMessage.REFUSE);
              order.addReceiver(bestSeller);
              order.setContent(targetBookTitle);
              order.setConversationId("handel_ksiazkami");
              order.setReplyWith(""+bestPrice); 
              myAgent.send(order);
              System.out.println("Seller nie chcial mnie, poradze sobie bez tej ksiazki");
              step = 4;
              break;
          }
          negocjacja++;
          
          ACLMessage order = new ACLMessage(ACLMessage.PROPOSE);
          order.addReceiver(bestSeller);
          order.setContent(targetBookTitle);
          order.setConversationId("handel_ksiazkami");
          order.setReplyWith(""+bestPrice); 
          myAgent.send(order);
          mt = MessageTemplate.and(MessageTemplate.MatchConversationId("handel_ksiazkami"),
                                   MessageTemplate.MatchInReplyTo(order.getReplyWith()));
          step = 3;
          break;
        case 3:      // odbiór odpowiedzi na zamównienie
          reply = myAgent.receive(mt);
          if (reply != null)
          {
            if (reply.getPerformative() == ACLMessage.INFORM)
            {
              System.out.println("Tytuł "+targetBookTitle+" zamówiony!");
              System.out.println("Po cenie: "+bestPrice);
              myAgent.doDelete();
            }
            else if (reply.getPerformative() == ACLMessage.PROPOSE)
            {
                int nowaCena = Integer.parseInt(reply.getInReplyTo());
                if (nowaCena >= bestPrice){
                    bestPrice = nowaCena;
                    order = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
                    order.addReceiver(bestSeller);
                    order.setContent(targetBookTitle);
                    order.setConversationId("handel_ksiazkami");
                    order.setReplyWith(""+bestPrice); 
                    myAgent.send(order);
                    mt = MessageTemplate.and(MessageTemplate.MatchConversationId("handel_ksiazkami"),
                                             MessageTemplate.MatchInReplyTo(order.getReplyWith()));
                    break;
                }
                else if (nowaCena < bestPrice){
                    step = 2;
                    break;
                }
            }
            step = 4;
          }
          else
          {
            block();
          }
          break;
        }  // switch
      } // action

      public boolean done() {
        return ((step == 2 && bestSeller == null) || step == 4);
      }
    } // Koniec wewnętrznej klasy RequestPerformer
}
