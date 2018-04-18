package com.example.ian.mycompass;

public class WeatherData {

    private Coord coord;
    private Weather[] weather;
    private String base;
    private Main main;
    private int visibility;
    private Wind wind;
    private Clouds clouds;
    private int dt;
    private Sys sys;
    private int id;
    private String name;
    private int cod;

    @Override
    public String toString() {
        return "-------------- Weather Details --------------"
                + "\n" + getCoord()
                + "\n" + getWeather()
                + "\n" + getBase()
                + "\n" + getMain()
                + "\n" + getVisibility()
                + "\n" + getWind()
                + "\n" + getClouds()
                + "\n" + getDt()
                + "\n" + getSys()
                + "\n" + getId()
                + "\n" + getName()
                + "\n" + getCod()
                + "\n" + "-------------------------------------------------------";
    }

    public Coord getCoord() {
        return coord;
    }

    public void setCoord(Coord coord) {
        this.coord = coord;
    }

    public Weather[] getWeather() {
        return weather;
    }

    public void setWeather(Weather[] weather) {
        this.weather = weather;
    }

    public String getBase() {
        return base;
    }

    public void setBase(String base) {
        this.base = base;
    }

    public Main getMain() {
        return main;
    }

    public void setMain(Main main) {
        this.main = main;
    }

    public int getVisibility() {
        return visibility;
    }

    public void setVisibility(int visibility) {
        this.visibility = visibility;
    }

    public Wind getWind() {
        return wind;
    }

    public void setWind(Wind wind) {
        this.wind = wind;
    }

    public Clouds getClouds() {
        return clouds;
    }

    public void setClouds(Clouds clouds) {
        this.clouds = clouds;
    }

    public int getDt() {
        return dt;
    }

    public void setDt(int dt) {
        this.dt = dt;
    }

    public Sys getSys() {
        return sys;
    }

    public void setSys(Sys sys) {
        this.sys = sys;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getCod() {
        return cod;
    }

    public void setCod(int cod) {
        this.cod = cod;
    }
}

class Coord {

    private float lon;

    private float lat;

    @Override
    public String toString() {
        return getLon() + ", " + getLat();
    }

    public void setLon(float lon) {
        this.lon = lon;
    }

    public void setLat(float lat) {
        this.lat = lat;
    }

    public float getLon() {
        return lon;
    }

    public float getLat() {
        return lat;
    }
}

class Weather {

    private int id;
    private String main;
    private String description;
    private String icon;

    @Override
    public String toString() {
        return getId() + ", " + getMain() + ", " + getDescription() + ", " + getIcon();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getMain() {
        return main;
    }

    public void setMain(String main) {
        this.main = main;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }
}

class Main {

    private float temp;
    private float pressure;
    private int humidity;
    private float temp_min;
    private float temp_max;

    @Override
    public String toString() {
        return getTemp() + "," + getPressure() + "," + getHumidity()  + "," + getTemp_min()  + "," + getTemp_max();
    }

    public float getTemp() {
        return temp;
    }

    public void setTemp(float temp) {
        this.temp = temp;
    }

    public float getPressure() {
        return pressure;
    }

    public void setPressure(float pressure) {
        this.pressure = pressure;
    }

    public int getHumidity() {
        return humidity;
    }

    public void setHumidity(int humidity) {
        this.humidity = humidity;
    }

    public float getTemp_min() {
        return temp_min;
    }

    public void setTemp_min(float temp_min) {
        this.temp_min = temp_min;
    }

    public float getTemp_max() {
        return temp_max;
    }

    public void setTemp_max(float temp_max) {
        this.temp_max = temp_max;
    }
}

class Wind {

    private float speed;
    private float deg;

    @Override
    public String toString() {
        return getSpeed() + "," + getDeg();
    }

    public float getSpeed() {
        return speed;
    }

    public void setSpeed(float speed) {
        this.speed = speed;
    }

    public float getDeg() {
        return deg;
    }

    public void setDeg(float deg) {
        this.deg = deg;
    }

    public String getDegString() {
        if (deg >= 348 || deg <= 11)
            return "N";
        else if (deg <= 33)
            return "NNE";
        else if (deg <= 56)
            return "NE";
        else if (deg <= 78)
            return "ENE";
        else if (deg <= 101)
            return "E";
        else if (deg <= 123)
            return "ESE";
        else if (deg <= 146)
            return "SE";
        else if (deg <= 168)
            return "SSE";
        else if (deg <= 191)
            return "S";
        else if (deg <= 213)
            return "SSW";
        else if (deg <= 236)
            return "SW";
        else if (deg <= 258)
            return "WSW";
        else if (deg <= 281)
            return "W";
        else if (deg <= 303)
            return "WNW";
        else if (deg <= 326)
            return "NW";
        else
            return "NNW";
        }
}

class Clouds {
    private int all;

    @Override
    public String toString() {
        return String.valueOf(getAll());
    }

    public int getAll() {
        return all;
    }

    public void setAll(int all) {
        this.all = all;
    }
}

class Sys {

    private int type;
    private int id;
    private float message;
    private String country;
    private int sunrise;
    private int sunset;

    @Override
    public String toString() {
        return getType() + "," + getId() + "," + getMessage() + "," + getCountry() + "," + getSunrise()  + "," +getSunset();
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public float getMessage() {
        return message;
    }

    public void setMessage(float message) {
        this.message = message;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public int getSunrise() {
        return sunrise;
    }

    public void setSunrise(int sunrise) {
        this.sunrise = sunrise;
    }

    public int getSunset() {
        return sunset;
    }

    public void setSunset(int sunset) {
        this.sunset = sunset;
    }
}