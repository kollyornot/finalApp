package com.example.firebasetest;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.reflect.Constructor;
import java.util.ArrayList;

public class UserAdapter extends ArrayAdapter<User> {
    Context context;
    ArrayList<User> users;

    public  UserAdapter(@NonNull Context context, ArrayList<User> users){
        super(context, 0, users);
        this.context = context;
        this.users = users;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent){
        if(convertView == null){
            convertView = LayoutInflater.from(context).inflate(R.layout.item_user, parent, false);
        }

        TextView nameView = convertView.findViewById(R.id.nameView);
        TextView emailView = convertView.findViewById(R.id.emailView);

        User u = users.get(position);
        nameView.setText(u.name);
        emailView.setText(u.email);

        return convertView;
    }
}
