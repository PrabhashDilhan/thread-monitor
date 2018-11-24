package org.wso2.carbon.greeting.card.producer;

public interface GreetingCardProducer {

    void createGreetingCard(String cardName, String greeting);

    void listGreetingCards();

}
