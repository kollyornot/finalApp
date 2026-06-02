package com.example.firebasetest;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;

public class CarAdapter extends ArrayAdapter<Car> {
    Context context;
    ArrayList<Car> cars;
    private boolean showPublishButton = false;
    private boolean showDeleteButton = false;
    private boolean showLikeButton = false;
    private boolean showCreatorName = false;
    private OnCarActionListener publishListener;
    private OnCarActionListener deleteListener;
    private OnCarActionListener likeListener;

    public interface OnCarActionListener {
        void onCarAction(Car car);
    }

    public  CarAdapter(@NonNull Context context, ArrayList<Car> cars){
        super(context, 0, cars);
        this.context = context;
        this.cars = cars;
    }

    public void setShowPublishButton(boolean showPublishButton) {
        this.showPublishButton = showPublishButton;
    }

    public void setShowLikeButton(boolean showLikeButton) {
        this.showLikeButton = showLikeButton;
    }
    public void setShowDeleteButton(boolean showDeleteButton) {
        this.showDeleteButton = showDeleteButton;
    }

    public void setPublishListener(OnCarActionListener publishListener) {
        this.publishListener = publishListener;
    }

    public void setDeleteListener(OnCarActionListener deleteListener) {
        this.deleteListener = deleteListener;
    }

    public void setLikeListener(OnCarActionListener likeListener) {
        this.likeListener = likeListener;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent){
        if(convertView == null){
            convertView = LayoutInflater.from(context).inflate(R.layout.item_car, parent, false);
        }
        ImageView imageView = convertView.findViewById(R.id.imageView);
        TextView CarNameTxtInfo = convertView.findViewById(R.id.CarNameTxtInfo);
        TextView TeamNameTxtInfo = convertView.findViewById(R.id.TeamNameTxtInfo);
        TextView EngineNameTxtInfo = convertView.findViewById(R.id.EngineNameTxtInfo);
        TextView LikesTxtInfo = convertView.findViewById(R.id.LikesTxtInfo);
        Button publishBtn = convertView.findViewById(R.id.publishBtn);
        Button deleteBtn = convertView.findViewById(R.id.deleteBtn);
        Button likeBtn = convertView.findViewById(R.id.likeBtn);

        Car car = cars.get(position);
        CarNameTxtInfo.setText(car.name);
        TeamNameTxtInfo.setText(car.team);
        EngineNameTxtInfo.setText(car.engine);
        LikesTxtInfo.setText("Likes: " + car.likesCount);

        imageView.setImageResource(CarImageHelper.getImageResIdForTeam(car.team));
        if (showPublishButton) {
            publishBtn.setVisibility(View.VISIBLE);
            publishBtn.setEnabled(!car.published);
            publishBtn.setText(car.published ? "Published" : "Publish");
            publishBtn.setOnClickListener(v -> {
                if (publishListener != null) {
                    publishListener.onCarAction(car);
                }
            });
        } else {
            publishBtn.setVisibility(View.GONE);
            publishBtn.setOnClickListener(null);
        }

        if (showDeleteButton) {
            deleteBtn.setVisibility(View.VISIBLE);
            deleteBtn.setOnClickListener(v -> {
                if (deleteListener != null) {
                    deleteListener.onCarAction(car);
                }
            });
        } else {
            deleteBtn.setVisibility(View.GONE);
            deleteBtn.setOnClickListener(null);
        }

        if (showLikeButton) {
            likeBtn.setVisibility(View.VISIBLE);
            likeBtn.setOnClickListener(v -> {
                if (likeListener != null) {
                    likeListener.onCarAction(car);
                }
            });
        } else {
            likeBtn.setVisibility(View.GONE);
            likeBtn.setOnClickListener(null);
        }

        return convertView;
    }
}
