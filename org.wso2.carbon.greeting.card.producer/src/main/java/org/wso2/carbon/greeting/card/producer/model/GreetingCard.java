package org.wso2.carbon.greeting.card.producer.model;

public class GreetingCard {

    private String cardName;
    private String greeting;

    public GreetingCard(String cardName, String greeting){
        this.cardName = cardName;
        this.greeting = greeting;
    }

    public void setCardName(String cardName) {
        this.cardName = cardName;
    }

    public String getCardName() {
        return cardName;
    }

    public void setGreeting(String greeting) {
        this.greeting = greeting;
    }

    public String getGreeting() {
        return greeting;
    }

}
