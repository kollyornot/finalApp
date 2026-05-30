package com.example.firebasetest;

public class CarImageHelper {
    public static int getImageResIdForTeam(String team) {
        if (team == null) {
            return R.drawable.unknown_car;
        }

        switch (team) {
            case "McLaren":
                return R.drawable.mclaren_car;
            case "Scuderia Ferrari":
                return R.drawable.ferrari_car;
            case "Red Bull Racing":
                return R.drawable.redbull_car;
            case "Mercedes":
                return R.drawable.mercedes_car;
            case "Aston Martin":
                return R.drawable.aston_martin_car;
            case "Audi":
                return R.drawable.audi_car;
            case "Haas":
                return R.drawable.haas_car;
            case "Alpine":
                return R.drawable.alpine_car;
            case "Racing Bulls":
                return R.drawable.vcarb_car;
            case "Williams":
                return R.drawable.williams_car;
            default:
                return R.drawable.unknown_car;
        }
    }
}
