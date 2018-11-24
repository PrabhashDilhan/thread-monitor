package org.wso2.carbon.greeting.card.consumer.internal;

import org.wso2.carbon.greeting.card.producer.GreetingCardProducer;

import java.util.logging.Logger;

public class GreetingCardConsumerDataHolder {
    private static final Logger LOGGER = Logger.getLogger(GreetingCardConsumerDataHolder.class.getName());
    private static GreetingCardConsumerDataHolder instance = new GreetingCardConsumerDataHolder();
    private GreetingCardProducer greetingCardProducer;

    private GreetingCardConsumerDataHolder() {
    }

    public static GreetingCardConsumerDataHolder getInstance() {
        return instance;
    }

    public GreetingCardProducer getGreetingCardProducer() {
        return greetingCardProducer;
    }

    public void setGreetingCardProducer(GreetingCardProducer greetingCardProducer) {
        this.greetingCardProducer = greetingCardProducer;
    }
}
