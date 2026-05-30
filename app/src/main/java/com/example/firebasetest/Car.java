package com.example.firebasetest;
import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class Car {
    public String carid;
    public String ownerId;
    public String team;
    public String name;
    public String engine;
    public int imageResId;
    public boolean published;
    public int likesCount;


    public Car(){}
    public Car(String carid, String team, String name, String engine, int imageResId){
        this.carid = carid;
        this.team = team;
        this.name = name;
        this.engine = engine;
        this.imageResId = imageResId;
    }

    public Car(String carid, String ownerId, String team, String name, String engine, int imageResId, boolean published, int likesCount){
        this.carid = carid;
        this.ownerId = ownerId;
        this.team = team;
        this.name = name;
        this.engine = engine;
        this.imageResId = imageResId;
        this.published = published;
        this.likesCount = likesCount;
    }
}
