package com.arnav.floorplan;

import android.graphics.Path;
import android.graphics.Region;

import java.util.List;

/**
 * Created by Arnav on 21/02/2017.
 */

public class DatasetStoreDetails {

    String shape;
    List<Float> coordinates;
    Path path;
    String name;
    String id;
    Region region;

    public DatasetStoreDetails(String shape, List<Float> coordinates, Path path, String name, String id, Region region){
        this.shape = shape;
        this.coordinates = coordinates;
        this.path = path;
        this.name = name;
        this.id = id;
        this.region = region;
    }

    public String getShape() {
        return shape;
    }

    public void setShape(String shape) {
        this.shape = shape;
    }

    public List<Float> getCoordinates() {
        return coordinates;
    }

    public void setCoordinates(List<Float> coordinates) {
        this.coordinates = coordinates;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Region getRegion() {
        return region;
    }

    public void setRegion(Region region) {
        this.region = region;
    }

    public Path getPath() {
        return path;
    }

    public void setPath(Path path) {
        this.path = path;
    }
}
