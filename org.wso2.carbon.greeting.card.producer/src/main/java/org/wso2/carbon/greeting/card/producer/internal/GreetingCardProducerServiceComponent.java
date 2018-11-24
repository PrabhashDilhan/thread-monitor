package org.wso2.carbon.greeting.card.producer.internal;


import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.wso2.carbon.greeting.card.producer.GreetingCardProducer;
import org.wso2.carbon.greeting.card.producer.GreetingCardProducerImpl;

import java.util.logging.Logger;

@Component(
        name = "greeting.card.producer",
        immediate = true
)
public class GreetingCardProducerServiceComponent {
    private static final Logger LOGGER = Logger.getLogger(GreetingCardProducerServiceComponent.class.getName());

    @Activate
    protected void activate(ComponentContext context) {
        BundleContext bundleContext = context.getBundleContext();
        bundleContext.registerService(GreetingCardProducer.class,
                GreetingCardProducerImpl.getInstance(), null);
        LOGGER.info("Greeting card producer bundle is activated");
    }

    @Deactivate
    protected void deactivate(ComponentContext context) {
        LOGGER.info("Greeting card producer bundle is deactivated");
    }

}
