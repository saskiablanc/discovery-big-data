package com.demo.model;

/**
 * Représente une personne récupérée depuis l'API randomuser.me.
 * Ce POJO est sérialisé en JSON et envoyé dans Kafka par le Generator,
 * puis désérialisé et inséré en base de données par le Consumer.
 */
public class PersonEvent implements java.io.Serializable {

    private String firstName;
    private String lastName;
    private String nationality;
    private int age;
    private String pictureUrl;

    // Constructeur vide requis par Jackson
    public PersonEvent() {}

    public PersonEvent(String firstName, String lastName, String nationality,
                       int age, String pictureUrl) {
        this.firstName   = firstName;
        this.lastName    = lastName;
        this.nationality = nationality;
        this.age         = age;
        this.pictureUrl  = pictureUrl;
    }

    // Getters & Setters
    public String getFirstName()                   { return firstName; }
    public void setFirstName(String firstName)     { this.firstName = firstName; }

    public String getLastName()                    { return lastName; }
    public void setLastName(String lastName)       { this.lastName = lastName; }

    public String getNationality()                 { return nationality; }
    public void setNationality(String nationality) { this.nationality = nationality; }

    public int getAge()                            { return age; }
    public void setAge(int age)                    { this.age = age; }

    public String getPictureUrl()                  { return pictureUrl; }
    public void setPictureUrl(String pictureUrl)   { this.pictureUrl = pictureUrl; }

    @Override
    public String toString() {
        return "PersonEvent{firstName='" + firstName + "', lastName='" + lastName
                + "', nationality='" + nationality + "', age=" + age + "}";
    }
}