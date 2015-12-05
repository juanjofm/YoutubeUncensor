package youtubeuncensor;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ChoiceBox;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import youtubeuncensor.core.TaskItem;

/**
 * FXML Controller class
 *
 * @author juanjo
 */
public class ExplorerController implements Initializable {

    @FXML
    private ChoiceBox menu_choiceKeyword;
    @FXML
    private FlowPane flowpane;

    private ObservableList<String> keywordList;

    @Override
    public void initialize(URL url, ResourceBundle rb) {

        this.keywordList = FXCollections.observableArrayList();

        this.keywordList.add("ALL KEYWORDS");

        for (TaskItem item : Main.taskList) {
            this.keywordList.add(item.getKeyword());
        }

        menu_choiceKeyword.setItems(this.keywordList);

        menu_choiceKeyword.getSelectionModel().selectFirst();

        menu_choiceKeyword.valueProperty().addListener((observable, oldValue, newValue) -> {

            if (menu_choiceKeyword.getSelectionModel().getSelectedIndex() == 0) {
                listVideos(null);
            } else {
                listVideos(newValue.toString());
            }
        });

    }

    private void listVideos(String keyword) {

        System.out.println("listvideos");

        this.flowpane.getChildren().clear();

        if (keyword == null) {
            for (TaskItem item : Main.taskList) {

                //TODO BETTER
                File[] files = item.getVideoFiles();

                for (File file : files) {

                    String name = file.getName().replace(".mp4", ".jpg");
                    
                    System.out.println(item.getDirectory().getAbsolutePath() + "/" + name);

                    ImageView image = new ImageView(new File(item.getDirectory().getAbsolutePath() + "/" + name).toURI().toString());

                    this.flowpane.getChildren().add(image);
                }

            }
        } else {
            for (TaskItem item : Main.taskList) {
                if (item.getKeyword().equals(keyword)) {
                    //TODO
                    break;
                }
            }
        }
    }

}
