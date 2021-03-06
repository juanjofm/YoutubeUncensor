package youtubeuncensor;

import youtubeuncensor.core.Constants;
import youtubeuncensor.core.TaskItem;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import youtubeuncensor.core.PreferencesHelper;
import youtubeuncensor.core.Utils;

/**
 *
 * @author juanjo
 */
public class Main implements Initializable {

    @FXML
    private Button btnStartAll;
    @FXML
    private Button btnStart;
    @FXML
    private Button btnStopAll;
    @FXML
    private Button btnStop;
    @FXML
    private Button btnAddTask;
    @FXML
    private Button btnRemoveTask;
    @FXML
    private Button btnShowLog;
    @FXML
    private Button btnPreferences;
    @FXML
    private Button btnAbout;
    @FXML
    private Button btnExplorer;

    @FXML
    private TableView tableView_tasks;

    public static ObservableList<TaskItem> taskList;

    private static Thread updateThread;

    @FXML
    private void handleButtonAction(ActionEvent event) {
        Object source = event.getSource();

        if (source == this.btnAbout) {
            try {
                Parent root;
                root = FXMLLoader.load(getClass().getResource("About.fxml"));
                Stage stage = new Stage();
                stage.setTitle("About");
                stage.setScene(new Scene(root));
                stage.setResizable(false);
                stage.show();
            } catch (IOException ex) {
                Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else if (source == this.btnStartAll) {

            startUpdateListThread();

            for (TaskItem taskItem : taskList) {
                if (!taskItem.getThread().isAlive()) {
                    taskItem.startNewThread();
                }
            }

        } else if (source == this.btnStart) {
            TaskItem ti = (TaskItem) tableView_tasks.getSelectionModel().getSelectedItem();

            if (ti != null) {
                startUpdateListThread();
                ti.startNewThread();
            }
        } else if (source == this.btnStop) {
            TaskItem ti = (TaskItem) tableView_tasks.getSelectionModel().getSelectedItem();

            if (ti != null) {
                ti.stopThread();
            }
        } else if (source == this.btnStopAll) {
            stopAllTasks();
        } else if (source == this.btnAddTask) {
            try {
                Parent root;
                root = FXMLLoader.load(getClass().getResource("AddTask.fxml"));
                Stage stage = new Stage();
                stage.setTitle("Add new task");
                stage.setScene(new Scene(root));
                stage.setResizable(false);
                stage.show();
            } catch (IOException ex) {
                Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
            }

        } else if (source == this.btnRemoveTask) {

            TaskItem ti = (TaskItem) tableView_tasks.getSelectionModel().getSelectedItem();

            if (ti != null) {

                Alert alert = new Alert(AlertType.CONFIRMATION);
                alert.setTitle("Are you sure?");
                alert.setHeaderText("This will delete the task and THE FILES.");
                alert.setContentText("Are you ok with this?");

                Optional<ButtonType> result = alert.showAndWait();
                if (result.isPresent() && result.get() == ButtonType.OK) {

                    ti.deleteAllFiles();

                    taskList.remove(ti);
                }  //cancel


            }

        } else if (source == this.btnShowLog) {
            TaskItem ti = (TaskItem) tableView_tasks.getSelectionModel().getSelectedItem();
            if (ti != null) {
                try {
                    ConsoleLogController.ti = ti;
                    Parent root;
                    root = FXMLLoader.load(getClass().getResource("ConsoleLog.fxml"));
                    Stage stage = new Stage();
                    stage.setTitle("Console log for " + ti.getKeyword());
                    stage.setScene(new Scene(root));
                    //stage.setResizable(false);
                    stage.show();

                    stage.setOnCloseRequest(e -> ConsoleLogController.th.stop());
                } catch (IOException ex) {
                    Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

        } else if (source == this.btnPreferences) {

            try {
                Parent root;
                root = FXMLLoader.load(getClass().getResource("GlobalConfig.fxml"));
                Stage stage = new Stage();
                stage.setTitle("Preferences");
                stage.setScene(new Scene(root));
                stage.setResizable(false);
                stage.show();
            } catch (IOException ex) {
                Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
            }

        } else if (source == this.btnExplorer) {

            try {
                Parent root;
                root = FXMLLoader.load(getClass().getResource("Explorer.fxml"));
                Stage stage = new Stage();
                stage.setTitle("Explorer");
                stage.setScene(new Scene(root));
                stage.show();
            } catch (IOException ex) {
                Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
            }

        }
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {

        taskList = FXCollections.observableArrayList();

        tableView_tasks.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn colId = new TableColumn("#");
        TableColumn colKeyword = new TableColumn("keyword");
        TableColumn colNvideos = new TableColumn("number of videos");
        TableColumn colStatus = new TableColumn("status");
        TableColumn colLastError = new TableColumn("last error");

        colId.setCellValueFactory(
                new PropertyValueFactory<TaskItem, String>("id")
        );
        colKeyword.setCellValueFactory(
                new PropertyValueFactory<TaskItem, String>("keyword")
        );
        colNvideos.setCellValueFactory(
                new PropertyValueFactory<TaskItem, String>("nvideos")
        );
        colStatus.setCellValueFactory(
                new PropertyValueFactory<TaskItem, String>("status")
        );
        colLastError.setCellValueFactory(
                new PropertyValueFactory<TaskItem, String>("lasterror")
        );

        tableView_tasks.getColumns().addAll(colId, colKeyword, colNvideos, colStatus, colLastError);

        tableView_tasks.setItems(taskList);

        tableView_tasks.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {

            if (newValue != null) {
                TaskItem tr = (TaskItem) newValue;

                //btns
                btnStart.setDisable(tr.getThread().isAlive());
                btnStop.setDisable(!tr.getThread().isAlive());
                btnRemoveTask.setDisable(tr.getThread().isAlive());
                btnShowLog.setDisable(false);

            } else {
                //btns
                btnStart.setDisable(true);
                btnStop.setDisable(true);
                btnRemoveTask.setDisable(true);
                btnShowLog.setDisable(true);
            }

        });

        checkFiles();
        startListDir();
    }

    public void startListDir() {

        File downloadDir = new File(PreferencesHelper.PREF_DOWNLOAD_DIR);
        if (!downloadDir.exists() || !downloadDir.isDirectory()) {
            try {
                downloadDir.mkdir();
            } catch (Exception e) {
                //e.printStackTrace();

                Alert alert = new Alert(AlertType.ERROR);
                alert.setTitle("Error");
                alert.setHeaderText("Error creating the keyword directory.");
                alert.setContentText(e.getMessage());

                alert.showAndWait();
            }
        }

        File[] keywordListDirs = downloadDir.listFiles();

        int j = 0;
        for (File keywordListDir : keywordListDirs) {
            if (keywordListDir.isDirectory()) {
                String keyword = keywordListDir.getName();

                TaskItem tk = new TaskItem(j, keyword);

                taskList.add(tk);

                j++;
            }
        }

    }

    public void checkFiles() {
        File youtubedl = new File(Utils.getYoutubedlPath());

        if (youtubedl.exists()) {
            if (!youtubedl.canExecute()) {
                youtubedl.setExecutable(true);
            }
        } else {

            Alert alert = new Alert(AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText("Error: youtube-dl files not found or cannot execute!");

            alert.showAndWait();

            System.err.println("ERROR: some youtube-dl executables do not exist or do not have execution privileges. (In "+ Constants.rutaBin +" folder)");
            System.exit(-1);
        }
    }

    public void startUpdateListThread() {
        if (updateThread == null || !updateThread.isAlive()) {
            updateThread = new Thread(() -> {
                while (true) {
                    updateList();

                    try {
                        Thread.sleep(1500);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
                    }

                }

            });

            updateThread.setDaemon(true);
            updateThread.start();
        }
    }

    public synchronized void updateList() {

        boolean somethingRunning = false;
        for (int i = 0; i < taskList.size(); i++) {
            taskList.set(i, taskList.get(i));
            if (taskList.get(i).getThread().isAlive()) {
                somethingRunning = true;
            }
        }

        this.btnStopAll.setDisable(!somethingRunning);

        if (!somethingRunning) {
            updateThread.stop();
        }
    }

    //Stop all Tasks (no updater thread)
    private static void stopAllTasks() {
        for (TaskItem taskItem : taskList) {
            if (taskItem.getThread().isAlive()) {
                taskItem.stopThread();
            }
        }
    }

    public static void properExit() {

        stopAllTasks();

        if (updateThread != null && updateThread.isAlive()) {
            updateThread.stop();
        }

    }

}
