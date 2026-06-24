package com.demo.model;

public class PersonEvent implements java.io.Serializable {

    private long id;
    private String firstName;
    private String lastName;
    private String nationality;
    private int age;
    private String pictureUrl;

    public PersonEvent() {}

    public PersonEvent(long id, String firstName, String lastName,
                       String nationality, int age, String pictureUrl) {
        this.id          = id;
        this.firstName   = firstName;
        this.lastName    = lastName;
        this.nationality = nationality;
        this.age         = age;
        this.pictureUrl  = pictureUrl;
    }

    public long getId()                            { return id; }
    public void setId(long id)                     { this.id = id; }
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
        return "PersonEvent{id=" + id + ", firstName='" + firstName
                + "', lastName='" + lastName + "', nationality='" + nationality
                + "', age=" + age + "}";
    }
}