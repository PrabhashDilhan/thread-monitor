package org.wso2.carbon.greeting.card.consumer;

import org.wso2.carbon.greeting.card.consumer.internal.GreetingCardConsumerComponent;

public class GreetingCardConsumer {

    public void requestGreetingCard(String cardName, String greeting){
        GreetingCardConsumerComponent.getGreetingCardProducer().createGreetingCard(cardName, greeting);
    }
}
