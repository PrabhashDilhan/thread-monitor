package org.wso2.carbon.greeting.card.producer;

import org.wso2.carbon.greeting.card.producer.model.GreetingCard;

import java.util.logging.Logger;

public class GreetingCardProducerImpl implements GreetingCardProducer {

    private static final Logger LOGGER = Logger.getLogger(GreetingCardProducerImpl.class.getName());

    private static volatile GreetingCardProducerImpl greetingCardProducer;


    private GreetingCardProducerImpl() {
    }

    public static GreetingCardProducerImpl getInstance() {

        if (greetingCardProducer == null) {
            synchronized (GreetingCardProducerImpl.class) {
                if (greetingCardProducer == null) {
                    greetingCardProducer = new GreetingCardProducerImpl();
                }
            }
        }
        return greetingCardProducer;
    }
    public void createGreetingCard(String cardName, String greeting) {
        //Logic of creating a greeting card goes here.
        GreetingCard greetingCard = new GreetingCard(cardName, greeting);
        LOGGER.info("Created a greeting card " + greetingCard.getCardName() + " with greeting: "+ greetingCard.getGreeting());
    }

    public void listGreetingCards() {

    }
}
