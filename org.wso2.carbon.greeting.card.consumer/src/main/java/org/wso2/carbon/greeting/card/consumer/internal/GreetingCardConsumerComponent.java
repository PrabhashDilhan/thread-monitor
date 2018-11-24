package org.wso2.carbon.greeting.card.consumer.internal;


import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.*;
import org.wso2.carbon.greeting.card.consumer.GreetingCardConsumer;
import org.wso2.carbon.greeting.card.producer.GreetingCardProducer;

import java.util.logging.Logger;

@Component(
        name = "greeting.card.consumer",
        immediate = true
)
public class GreetingCardConsumerComponent {
    private static final Logger LOGGER = Logger.getLogger(GreetingCardConsumerComponent.class.getName());

    @Activate
    protected void activate(ComponentContext context) {
        LOGGER.info("Greeting card consumer bundle is activated");
        GreetingCardConsumer greetingCardConsumer = new GreetingCardConsumer();
        greetingCardConsumer.requestGreetingCard("Birthday", "Happy Birthday!");
    }

    @Reference(
            name = "greeting.card.producer",
            service = GreetingCardProducer.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetGreetingCardProducer"
    )
    protected void setGreetingCardProducer(GreetingCardProducer greetingCardProducer) {
        LOGGER.info("Greeting card producer is set to Greeting card consumer bundle.");
        GreetingCardConsumerDataHolder.getInstance().setGreetingCardProducer(greetingCardProducer);
    }
    protected void unsetGreetingCardProducer(GreetingCardProducer greetingCardProducer){
        LOGGER.info("Greeting card producer is unset to Greeting card consumer bundle.");
        GreetingCardConsumerDataHolder.getInstance().setGreetingCardProducer(null);
    }
    public static GreetingCardProducer getGreetingCardProducer() {
        return GreetingCardConsumerDataHolder.getInstance().getGreetingCardProducer();
    }

}
