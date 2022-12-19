package com.example.simplebrowser_7;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebHistory;
import javafx.scene.web.WebView;

import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.google.gson.Gson;

public class HelloController implements Initializable {
    @FXML
    public TabPane tabPanel;
    @FXML
    public Button newTabBtn;

    @FXML
    private ScrollPane scrollPane;

    @FXML
    private TextField addressBar;

    @FXML
    private MenuButton favouritesMenu;

    private final HashMap<Tab, javafx.scene.Parent> webViewMap = new HashMap<>();

    private final HashSet<String> urlHistoryExceptions  = new HashSet<>();

    private boolean isHistoryEnabled = true;

    private SingleSelectionModel<Tab> selectionModel;

    public void turnOnOfGlobalHistory() {
        isHistoryEnabled = !isHistoryEnabled;
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        selectionModel = tabPanel.getSelectionModel();
        tabPanel.setTabClosingPolicy(TabPane.TabClosingPolicy.ALL_TABS);
        createNewTab();
        addressBar.setOnAction(event -> {
            String address = addressBar.getText();
            Tab newTab = createNewTab();
            WebView newWebView = (WebView) webViewMap.get(newTab);
            if (newWebView == null) {
                return;
            }
            newWebView.getEngine().load(address);
            scrollPane.setContent(newWebView);
        });

        try (BufferedReader reader = new BufferedReader(new FileReader("C:\\Users\\Danon\\IdeaProjects\\SimpleBrowser_7\\Favourites\\Favourites.txt"))) {
            String line;
            while((line = reader.readLine()) != null) {
                int spaceIndex = line.indexOf(" ");
                String link = line.substring(0, spaceIndex);
                String title = line.substring(spaceIndex + 1);
                createNewFavouriteItem(title, link);
            }
        }
        catch (IOException e) {
            System.out.println("Error loading favourites");
        }

    }

    private void createNewFavouriteItem(String title, String location) {
        MenuItem item = new MenuItem(title);
        item.setOnAction(ev -> {
            Tab newTab = createNewTab();
            WebView currentView = (WebView) webViewMap.get(newTab);
            if (currentView == null) {
                return;
            }
            currentView.getEngine().load(location);
        });
        favouritesMenu.getItems().add(item);
    }

    private void createCustomContextMenu(WebView webView) {
        webView.setContextMenuEnabled(false);

        ContextMenu contextMenu = new ContextMenu();
        MenuItem reload = new MenuItem("Reload");
        reload.setOnAction(e -> webView.getEngine().reload());

        MenuItem goBack = new MenuItem("Go Back");
        goBack.setOnAction(e -> {
            try {
                webView.getEngine().getHistory().go(-1);
            }
            catch (IndexOutOfBoundsException er) {
                return;
            }
        });
        MenuItem goForward = new MenuItem("Go Forward");
        goForward.setOnAction(e -> {
            try {
                webView.getEngine().getHistory().go(1);
            }
            catch (IndexOutOfBoundsException er) {
                return;
            }
        });


        MenuItem savePage = new MenuItem("Save Page");
        savePage.setOnAction(e -> {
            try (ZipOutputStream zip = new ZipOutputStream(new FileOutputStream("C:\\Users\\Danon\\IdeaProjects\\SimpleBrowser_7\\Downloads\\" + webView.getEngine().getTitle() +".zip"));
                 InputStream urlStream = new URL(webView.getEngine().getLocation()).openStream()) {
                zip.putNextEntry(new ZipEntry("Page.html"));
                urlStream.transferTo(zip);
            } catch (IOException ex) {
                System.out.println("BAD_URL: " + webView.getEngine().getLocation());
            }
        });
        MenuItem ShowHTML = new MenuItem("Show HTML");
        ShowHTML.setOnAction(e -> {
            TextArea htmlArea = new TextArea();
            htmlArea.setText((String) webView.getEngine().executeScript("document.documentElement.outerHTML"));
            HBox pageWithHTMLLayout = new HBox();

            htmlArea.textProperty().addListener((observable, oldValue, newValue) -> webView.getEngine().loadContent(newValue));
            pageWithHTMLLayout.getChildren().add(htmlArea);
            pageWithHTMLLayout.getChildren().add(webView);

            scrollPane.setContent(pageWithHTMLLayout);
        });

        MenuItem add2Favourites = new MenuItem("Add to Favorites");
        add2Favourites.setOnAction(e -> {
            String location = webView.getEngine().getLocation();
            String title = webView.getEngine().getTitle();
            try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter("C:\\Users\\Danon\\IdeaProjects\\SimpleBrowser_7\\Favourites\\Favourites.txt", true))) {
                bufferedWriter.write(location + " " + title + "\n");
                bufferedWriter.flush();
            }
            catch (IOException err) {
                System.out.println("Error writing to favourite");
            }
            createNewFavouriteItem(title, location);
        });

        MenuItem turnOnOfHistory = new MenuItem("On/of History (local)");
        turnOnOfHistory.setOnAction(e -> {
            String location = webView.getEngine().getLocation();
            if (urlHistoryExceptions.contains(location)) {
                urlHistoryExceptions.remove(location);
            }
            else {
                urlHistoryExceptions.add(location);
            }
        });

        contextMenu.getItems().addAll(reload, goBack, goForward, savePage, ShowHTML, add2Favourites, turnOnOfHistory);

        webView.setOnMousePressed(e -> {
            if (e.getButton() == MouseButton.SECONDARY) {
                contextMenu.show(webView, e.getScreenX(), e.getScreenY());
            } else {
                contextMenu.hide();
            }
        });
    }

    private void logHistory(WebEngine webEngine, String oldValue) {
        if (isHistoryEnabled && !urlHistoryExceptions.contains(oldValue) && !webEngine.getHistory().getEntries().isEmpty()) {
            int indexEntry = webEngine.getHistory().getCurrentIndex();
            List<WebHistory.Entry> webEntries = webEngine.getHistory().getEntries();
            Date currentDate = new Date();
            Date lastVisitedDate = webEntries.get(indexEntry).getLastVisitedDate();

            Gson gson = new Gson();
            HistoryEntry entry = new HistoryEntry(lastVisitedDate, (currentDate.getTime() - lastVisitedDate.getTime()) / 1000, oldValue);
            try (FileWriter writer = new FileWriter("C:\\Users\\Danon\\IdeaProjects\\SimpleBrowser_7\\History\\History.json", true)) {
                gson.toJson(entry, writer);
            }
            catch (IOException e) {
                System.out.println("Error while writing history");
            }
        }
    }

    public Tab createNewTab() {
        Tab newTab = new Tab("Google");
        newTab.setOnClosed(t -> {
            if (webViewMap.get(newTab) instanceof WebView) {
                WebEngine webEngine = ((WebView) webViewMap.get(newTab)).getEngine();
                logHistory(webEngine, webEngine.getLocation());
            }
            webViewMap.remove(newTab);
            if (webViewMap.isEmpty()) {
                scrollPane.setContent(null);
            }
        });
        tabPanel.getTabs().add(newTab);
        WebView newWebView = new WebView();
        createCustomContextMenu(newWebView);
        WebEngine webEngine = newWebView.getEngine();

        webEngine.load("https://www.google.com/");
        addressBar.setText("https://www.google.com/");
        webEngine.locationProperty().addListener((observable, oldValue, newValue) -> {
            logHistory(webEngine, oldValue);
            addressBar.setText(newValue);
        });
        webEngine.titleProperty().addListener((observable, oldValue, newValue) -> newTab.setText(newValue));
        newTab.selectedProperty().addListener((observable, oldValue, newValue) -> {
           if (newValue) {
               WebView currentView = (WebView) webViewMap.get(newTab);
               if (currentView == null) {
                   return;
               }
               scrollPane.setContent(currentView);
               addressBar.setText(currentView.getEngine().getLocation());
               newTab.setText(currentView.getEngine().getTitle());
           }
        });
        selectionModel.select(newTab);
        webViewMap.put(newTab, newWebView);
        scrollPane.setContent(newWebView);
        return newTab;
    }

    public void createDevTab() {
        Tab devTab = new Tab("DevTab");
        devTab.setOnClosed(t -> {
            webViewMap.remove(devTab);
            if (webViewMap.isEmpty()) {
                scrollPane.setContent(null);
            }
        });

        WebView webView = new WebView();
        WebEngine webEngine = webView.getEngine();

        VBox vbox = new VBox();
        TextArea htmlArea = new TextArea();
        htmlArea.textProperty().addListener((observable, oldValue, newValue) -> webEngine.loadContent(newValue));
        devTab.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                VBox currentView = (VBox) webViewMap.get(devTab);
                if (currentView == null) {
                    return;
                }
                scrollPane.setContent(currentView);
            }
        });
        tabPanel.getTabs().add(devTab);
        vbox.getChildren().add(htmlArea);
        vbox.getChildren().add(webView);
        webViewMap.put(devTab, vbox);
        scrollPane.setContent(vbox);
        selectionModel.select(devTab);
    }
}
