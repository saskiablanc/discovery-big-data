package com.demo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "person")
public class Person {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "nationality")
    private String nationality;

    @Column(name = "age")
    private int age;

    @Column(name = "age_group")
    private String ageGroup;

    @Column(name = "picture_url")
    private String pictureUrl;

    public Long getId()                          { return id; }
    public void setId(Long id)                   { this.id = id; }

    public String getFirstName()                 { return firstName; }
    public void setFirstName(String firstName)   { this.firstName = firstName; }

    public String getLastName()                  { return lastName; }
    public void setLastName(String lastName)     { this.lastName = lastName; }

    public String getNationality()               { return nationality; }
    public void setNationality(String n)         { this.nationality = n; }

    public int getAge()                          { return age; }
    public void setAge(int age)                  { this.age = age; }

    public String getAgeGroup()                  { return ageGroup; }
    public void setAgeGroup(String ageGroup)     { this.ageGroup = ageGroup; }

    public String getPictureUrl()                { return pictureUrl; }
    public void setPictureUrl(String pictureUrl) { this.pictureUrl = pictureUrl; }
}