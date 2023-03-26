package lk.ijse.dep10.app.controller;

import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import lk.ijse.dep10.app.db.DBConnection;
import lk.ijse.dep10.app.model.Student;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageOutputStream;
import javax.sql.rowset.serial.SerialBlob;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.sql.*;

public class MainViewController {

    @FXML
    private Button btnBrowse;

    @FXML
    private Button btnClear;

    @FXML
    private Button btnDelete;

    @FXML
    private Button btnNewStudent;

    @FXML
    private Button btnSave;

    @FXML
    private Circle circleImage;

    @FXML
    private TableView<Student> tblStudents;

    @FXML
    private TextField txtId;

    @FXML
    private TextField txtName;

    @FXML
    private TextField txtSearch;

    private Image emptyImage = new javafx.scene.image.Image("/image/empty-pp.png");
    private Image selectedImage = null;
    private Student selectedStudent = null;
    public Image getEmptyImage() {
        return emptyImage;
    }

    public void initialize() {
        circleImage.setFill(new ImagePattern(emptyImage));
        tblStudents.getColumns().get(0).setCellValueFactory(new PropertyValueFactory<>("image"));
        tblStudents.getColumns().get(1).setCellValueFactory(new PropertyValueFactory<>("id"));
        tblStudents.getColumns().get(2).setCellValueFactory(new PropertyValueFactory<>("name"));

        loadStudents();

        btnDelete.setDisable(true);
        tblStudents.getSelectionModel().selectedItemProperty().addListener((ov,pre,current)->{
            btnDelete.setDisable(false);
            if(current == null){
                btnDelete.setDisable(true);
                selectedStudent = null;
                selectedImage = null;
                System.out.print("Selected Student:");
                System.out.println(selectedStudent);
                System.out.print("Selected image:");
                System.out.println(selectedImage);
                return;
            }
            selectedStudent = current;
            selectedImage = selectedStudent.getStoredImage();
            txtId.setText(current.getId());
            txtName.setText(current.getName());
            circleImage.setFill(current.getImagePattern()==null? new ImagePattern(emptyImage):current.getImagePattern());
            System.out.print("Selected Student:");
            System.out.println(selectedStudent);
            System.out.print("Selected image:");
            System.out.println(selectedImage);
        });

    }

    private void loadStudents() {
        Connection connection = DBConnection.getInstance().getConnection();
        try {
            Statement stm = connection.createStatement();
            ResultSet rst = stm.executeQuery("SELECT * FROM Student");

            PreparedStatement stmPicture = connection.prepareStatement("SELECT * FROM Picture WHERE student_id=?");

            while (rst.next()) { // until meeting sentinel node
                String id = rst.getString("id");
                String name = rst.getString("name");


                Student student = new Student(id,name,null);

                stmPicture.setString(1,id);
                ResultSet rstPicture = stmPicture.executeQuery();

                if (rstPicture.next()){
                    student.setImage(new Image(rstPicture.getBlob("picture").getBinaryStream()));
                }else{
                    student.setImage(emptyImage);
                }
                tblStudents.getItems().add(student);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Failed to load items").show();
            System.exit(1);
        }
    }

    @FXML
    void btnBrowseOnAction(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image Files",
                "*.jpg","*.jpeg","*.png","*.gif"));
        File file = fileChooser.showOpenDialog(btnBrowse.getScene().getWindow());
        if(file==null) return;
        try {
            Image image = new Image(file.toURI().toURL().toString());
            circleImage.setFill(new ImagePattern(selectedImage = image));
            btnDelete.setDisable(false);
        } catch (MalformedURLException e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Error loading the image");
        }
    }

    @FXML
    void btnClearOnAction(ActionEvent event) {
        circleImage.setFill(new ImagePattern(emptyImage));
        btnClear.setDisable(true);
        selectedImage = null;
    }

    @FXML
    void btnDeleteOnAction(ActionEvent event) {
        String id = selectedStudent.getId();
        Connection connection = DBConnection.getInstance().getConnection();
        try {
            Statement stm = connection.createStatement();
            if (selectedStudent.getStoredImage()== emptyImage) {
                stm.executeUpdate(String.format("DELETE FROM Picture WHERE student_id=%s", id));
            }
            stm.executeUpdate(String.format("DELETE from Student WHERE id=%d",id));
            tblStudents.getItems().remove(selectedStudent);
            btnNewStudent.fire();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @FXML
    void btnNewStudentOnAction(ActionEvent event) {
        txtId.setText(generateId());
        txtName.clear();
        txtSearch.clear();
        tblStudents.getSelectionModel().clearSelection();
        circleImage.setFill(new ImagePattern(emptyImage));

        txtName.requestFocus();
    }

    private String generateId() {
        ObservableList<Student> studentsInTheTable = tblStudents.getItems();
        if(studentsInTheTable.size() == 0) return "DEP-10/S-001";
        Student lastStudent = studentsInTheTable.get(studentsInTheTable.size() - 1);
        String lastStudentId = lastStudent.getId();
        String numberString = lastStudentId.substring(9,12);
        return "DEP-10/S-" + String.format("%03d",Integer.parseInt(numberString)+1);
    }

    @FXML
    void btnSaveOnAction(ActionEvent event) {
        if(!isDataValid()) return;

        Connection connection = DBConnection.getInstance().getConnection();
        PreparedStatement stmStudent;
        PreparedStatement stmPicture;
        Blob selectedImageBlob = getSelectedImageAsBlob();
        System.out.print("Selected image blob:");
        System.out.println(selectedImageBlob);

        Student newStudent = new Student(txtId.getText(), txtName.getText(), selectedImage);

        try {
            connection.setAutoCommit(false);
            if (selectedStudent == null) {
                // add a record to Student table
                stmStudent = connection.prepareStatement("INSERT INTO Student (id,name) VALUES (?,?)");
                stmStudent.setString(1,txtId.getText());
                stmStudent.setString(2,txtName.getText());
                stmStudent.executeQuery();
                System.out.println("Added a new record to Student");
                // add a record to Picture table
                if (selectedImageBlob != null) {
                    stmPicture = connection.prepareStatement("INSERT INTO Picture(student_id, picture) VALUES (?,?)");
                    stmPicture.setString(1,txtId.getText());


                    stmPicture.setBlob(2, selectedImageBlob);
                    stmPicture.executeUpdate();
                    System.out.println("Added a new record to Picture ");
                }
                tblStudents.getItems().add(newStudent);
            } else {
                // update the Student table
                stmStudent =connection.prepareStatement("UPDATE Student SET name=? WHERE id=?");
                stmStudent.setString(1,txtName.getText());
                stmStudent.setString(2, txtId.getText());
                stmStudent.executeUpdate();
                System.out.println("updated the student name");
                // update the Picture table
                if (selectedImageBlob != null) {
                    Statement stm = connection.createStatement();
                    System.out.println(txtId.getText());
                    ResultSet rstPictures = stm.executeQuery(String.format("SELECT * FROM Picture WHERE student_id='%s'", txtId.getText()));

                    if (rstPictures.next()) {
                        stmPicture = connection.prepareStatement("UPDATE Picture SET picture=? WHERE student_id=?");
                        stmPicture.setBlob(1, selectedImageBlob);
                        stmPicture.setString(2, txtId.getText());
                    } else {
                        if (selectedImage != null) {

                        }
                        stmPicture = connection.prepareStatement("INSERT INTO Picture(STUDENT_ID, PICTURE) VALUES (?,?)");
                        stmPicture.setBlob(2, selectedImageBlob);
                        stmPicture.setString(1, txtId.getText());
                    }

                    stmPicture.executeUpdate();
                    System.out.println("updated the student's picture");
                }else{
                    deleteImage(txtId.getText(), connection);
                }
                Student selectedStudent = tblStudents.getSelectionModel().getSelectedItem();
                selectedStudent.setName(newStudent.getName());
                selectedStudent.setImage(newStudent.getStoredImage());
            }
            connection.commit();
            connection.setAutoCommit(true);
            tblStudents.refresh();

        } catch (Throwable e) {
            try {
                connection.rollback();
                connection.setAutoCommit(true);
            } catch (SQLException ex) {
                throw new RuntimeException(ex);
            }
            throw new RuntimeException(e);
        }
    }

    private void deleteImage(String id,Connection connection){
        try {
            Statement statement = connection.createStatement();
            String sql = "DELETE FROM Picture WHERE student_id=%s";
            statement.executeUpdate(String.format(sql,id));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

    }

    private Blob getSelectedImageAsBlob() {
        try {
            if(selectedImage==null) return null;
            BufferedImage bufferedImage = SwingFXUtils.fromFXImage(selectedImage, null);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(bufferedImage,"png", baos);
            byte[] bytes = baos.toByteArray();
            return new SerialBlob(bytes);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private boolean isDataValid() {
        if (!txtName.getText().matches("[A-Za-z]+")) {
            txtName.requestFocus();
            txtName.selectAll();
            return false;
        } else {
            return true;
        }
    }

    @FXML
    void tblStudentsOnKeyReleased(KeyEvent event) {

    }

}
