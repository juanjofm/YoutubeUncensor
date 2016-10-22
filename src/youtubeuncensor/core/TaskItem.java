package youtubeuncensor.core;

import java.io.BufferedReader;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author juanjo
 */
public class TaskItem implements Runnable {

    private int id;
    private String keyword;
    private int nvideos;
    private String status;

    private Thread thread;

    private String consoleLog;

    public static final String STATUS_RUNNING = "running";
    public static final String STATUS_STOPPED = "stopped";

    private static final String PREF_LOGFILE = "already_listed_log.log";

    //Runtime individual TaskItem preferences
    public int PREF_WAIT_TIME;
    public float PREF_MAX_FILESIZE;
    public boolean PREF_STOP_ON_ERROR;

    private File directory;

    public TaskItem(int id, String keyword) {
        this.id = id;
        this.keyword = keyword;
        this.status = TaskItem.STATUS_STOPPED;
        this.consoleLog = "";
        this.thread = new Thread(this);
        this.directory = new File(PreferencesHelper.PREF_DOWNLOAD_DIR + "/" + keyword);

        this.loadPreferences();
        this.checkDir();
        this.countVideos();

    }

    public void loadPreferences() {
        this.PREF_MAX_FILESIZE = PreferencesHelper.PREF_MAX_FILESIZE;
        this.PREF_STOP_ON_ERROR = PreferencesHelper.PREF_STOP_ON_ERROR;
        this.PREF_WAIT_TIME = PreferencesHelper.PREF_WAIT_TIME;
    }

    public void startNewThread() {

        this.status = TaskItem.STATUS_RUNNING;

        this.thread = new Thread(this);

        thread.setDaemon(true);

        thread.start();
    }

    public void stopThread() {
        this.thread.stop();
        this.status = TaskItem.STATUS_STOPPED;
    }

    public int getId() {
        return id;
    }

    public String getKeyword() {
        return keyword;
    }

    public int getNvideos() {
        return nvideos;
    }

    public String getStatus() {
        return status;
    }

    public Thread getThread() {
        return this.thread;
    }

    public String getConsoleLog() {
        return this.consoleLog;
    }

    public File getDirectory() {
        return this.directory;
    }

    public synchronized void checkDir() {
        if (!this.directory.exists() || !this.directory.isDirectory()) {
            this.directory.mkdir();
        }
    }

    public synchronized void countVideos() {

        this.deleteJSONandThumbnailwithNoVideos();

        this.nvideos = this.directory.listFiles(new FilenameFilter() {

            @Override
            public boolean accept(File dir, String name) {
                if (name.endsWith(".mp4")) {
                    return true;
                } else {
                    return false;
                }
            }
        }).length;
    }

    public File[] getVideoFiles() {
        return this.directory.listFiles(new FilenameFilter() {

            @Override
            public boolean accept(File dir, String name) {
                if (name.endsWith(".mp4")) {
                    return true;
                } else {
                    return false;
                }
            }
        });
    }

    public synchronized void deleteJSONandThumbnailwithNoVideos() {
        //Workaround for bug #8
        //Workaround para youtube-dl en el que descarga el json aunque no descargue el video por los filtros
        File[] files = this.directory.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                if (name.endsWith(".info.json")) {
                    return true;
                } else {
                    return false;
                }
            }
        });

        for (File file : files) {

            String nvidio = file.getName().split("\\.")[0];

            if (!(new File(this.directory.getAbsolutePath() + "/" + nvidio + ".mp4").exists())) {
                file.delete();

                //Delete thumbnail too
                String thumbnail = this.directory.getAbsolutePath() + "/" + nvidio + ".jpg";
                File fileThumb = new File(thumbnail);
                if (fileThumb.exists()) {
                    fileThumb.delete();
                }
            }

        }

    }

    public void deleteAllFiles() {
        File[] files = this.directory.listFiles();

        for (File file : files) {
            file.delete();
        }

        this.directory.delete();
    }

    @Override
    public void run() {

        while (true) {

            //Reset console log
            this.consoleLog = "";

            String youtubedl;
            if (System.getProperty("os.name").startsWith("Windows")) {
                // Windows
                youtubedl = "./bin/youtube-dl.exe";
            } else {
                // everything else
                youtubedl = "./bin/youtube-dl";
            }

            String youtubeURL = "https://www.youtube.com/results?search_sort=video_date_uploaded&filters=hour&search_query=" + this.keyword;
            String downloadDir = this.directory.getAbsolutePath() + "/" + "%(id)s.%(ext)s";

            String[] command = {youtubedl, youtubeURL, "-o", downloadDir, "--max-filesize", String.valueOf(this.PREF_MAX_FILESIZE) + "m", "--download-archive", TaskItem.PREF_LOGFILE, "--no-playlist", "--max-downloads", "4", "--write-info-json", "--write-thumbnail", "--recode-video", "mp4"};
            Runtime runtime = Runtime.getRuntime();

            try {
                Process process = runtime.exec(command);

                InputStream is = process.getInputStream();
                InputStreamReader isr = new InputStreamReader(is);
                BufferedReader br = new BufferedReader(isr);
                String line;

                while ((line = br.readLine()) != null) {
                    this.consoleLog += line + "\n";

                    //youtube-dl stops working well downloading live streaming video
                    if (this.PREF_STOP_ON_ERROR) {
                        if (line.contains("ERROR") && line.contains("SSL")) {
                            process.destroy();
                        }
                    }

                }

                //Wait to get exit value
                try {
                    int exitValue = process.waitFor();
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

            } catch (IOException ex) {
                Logger.getLogger(TaskItem.class.getName()).log(Level.SEVERE, null, ex);
            }

            this.countVideos();

            try {
                Thread.sleep(this.PREF_WAIT_TIME);
            } catch (InterruptedException ex) {
                Logger.getLogger(TaskItem.class.getName()).log(Level.SEVERE, null, ex);
            }

        }

    }

}
