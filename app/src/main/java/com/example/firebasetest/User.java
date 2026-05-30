package com.example.firebasetest;

import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class User {
    public String uid;
    public String key;
    public String name;
    public String email;
    public String avatarBase64;
    public boolean canCreateCars;

    public User(){}
    public User(String uid, String email, String name){
        this.uid = uid;
        this.email = email;
        this.name = name;
        this.canCreateCars = false;
    }

}
