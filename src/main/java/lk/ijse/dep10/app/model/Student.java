package lk.ijse.dep10.app.model;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import lk.ijse.dep10.app.controller.MainViewController;

import java.sql.Blob;
import java.sql.SQLException;

public class Student {
    private String id;
    private String name;
    private Image image;
    public Image getStoredImage() {
        return image;
    }

    public Student(String id, String name, Image image) {
        this.id = id;
        this.name = name;
        this.image = image;
    }

    public Student() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Circle getImage() {
        Circle circle = new Circle(50);
        if(image ==null) circle.setFill(new ImagePattern(MainViewController.emptyImage));
        else circle.setFill(new ImagePattern(image));
        return circle;
    }

    public ImagePattern getImagePattern() {
        if(image == null) return null;
        return new ImagePattern(image);
    }

    public void setImage(Image image) {
        this.image = image;
    }
}
