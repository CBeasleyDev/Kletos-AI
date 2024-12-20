// This project was developed by Christian Beasley



package org.example.kletos;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.json.JSONObject;
import org.json.JSONArray;


public class UIBuild extends Application {

    // Inserting Gemini API Key
    private static final String API_KEY = "AIzaSyC4Iu2sak8yZLBGibkgkaKWYGGDAgNdf3U";

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {

        // Initialize stage
        setupStage(primaryStage);

        // Create root layout
        BorderPane root = new BorderPane();

        // Set up logo in top right corner
        root.setTop(createLogoBox());

        // Create content area (WebView, Conversation Area, Input Box)
        root.setCenter(createContentArea());

        // Create scene and show the stage
        Scene scene = new Scene(root, 800, 600);


        URL cssResource = getClass().getResource("/org/example/kletos/K1.css");
        if (cssResource != null) {
            scene.getStylesheets().add(cssResource.toExternalForm());
        } else {
            System.out.println("Warning: K1.css not found.");
        }



        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void setupStage(Stage stage) {
        stage.setTitle("Kletos");
        stage.setWidth(800);
        stage.setHeight(600);
    }

    private HBox createLogoBox() {
        Image logo = new Image(getClass().getResourceAsStream("/kletos.png"));
        ImageView logoView = new ImageView(logo);
        logoView.setFitWidth(100);
        logoView.setPreserveRatio(true);

        HBox logoBox = new HBox(logoView);
        logoBox.setAlignment(Pos.TOP_LEFT);
        return logoBox;
    }

    private VBox createContentArea() {
        VBox contentBox = new VBox(10);
        contentBox.setPadding(new Insets(20));


        // WebView setup
        WebView webView = new WebView();
        webView.getEngine().loadContent(getHtmlContent());

        // Conversation display
        TextArea conversationArea = new TextArea();
        conversationArea.setEditable(false);
        conversationArea.setWrapText(true);

        // Input box
        TextField userInput = new TextField();
        userInput.setPromptText("Message Kletos");
        Button sendButton = new Button();
        sendButton.getStyleClass().add("send-button");

        // Adding an arrow icon to the button
        Label arrowIcon = new Label("\u2192"); // Unicode for right arrow
        arrowIcon.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: white;");
        sendButton.setGraphic(arrowIcon);


        //sendButton.getStyleClass().add("send-button");


        HBox inputBox = new HBox(10);
        inputBox.getChildren().addAll(userInput, sendButton);

        // Send button action

        sendButton.setOnAction(actionEvent -> {
            String userMessage = userInput.getText().trim();
            if (!userMessage.isEmpty()) {
                conversationArea.appendText("You: " + userMessage + "\n");
                userInput.clear();

                // Run the AI request in a separate thread
                new Thread(() -> {
                    String aiResponse = getAIResponse(userMessage);

                    // Update UI on the JavaFX Application Thread
                    Platform.runLater(() -> conversationArea.appendText("Kletos: " + aiResponse + "\n"));
                }).start();
            }
        });

        // Add components to content box
        contentBox.getChildren().addAll(webView, conversationArea, inputBox);
        return contentBox;
    }

    private String getHtmlContent() {
        return """
                    <html>
                    <head>
                        <link rel='stylesheet' href='https://use.typekit.net/zpb4wea.css'>
                        <style>body { font-family: 'bricolage-grotesque', sans-serif; }</style>
                    </head>
                    <body>
                        <h2>Kletos</h2>
                        <p>The everyday AI</p>
                    </body>
                    </html>
                """;
    }

    private String getAIResponse(String userMessage) {
        try {
            // Create an HTTP client
            HttpClient client = HttpClient.newHttpClient();

            // Set the Google Gemini API endpoint
            String uri = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash-latest:generateContent?key=" + API_KEY;

            // Create the HTTP request body for Google Gemini
            JSONObject requestBody = new JSONObject();
            JSONArray contentsArray = new JSONArray();
            JSONObject messagePart = new JSONObject().put("text", userMessage);
            JSONObject messageObj = new JSONObject().put("parts", new JSONArray().put(messagePart));
            contentsArray.put(messageObj);
            requestBody.put("contents", contentsArray);

            // Build the HTTP request
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(uri))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                    .build();

            // Send the request and get the response
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            // Print raw response to debug
            System.out.println("Response: " + response.body());

            // Check for successful response
            if (response.statusCode() != 200) {
                return "Error: Failed to connect to AI. Status code: " + response.statusCode();
            }

            // Parse the response
            JSONObject jsonResponse = new JSONObject(response.body());

            // Extract the AI response from the "candidates" array
            JSONArray candidates = jsonResponse.getJSONArray("candidates");
            if (candidates.length() == 0) {
                return "Error: No response from AI.";
            }

            // Get the content from the first candidate's parts
            JSONObject content = candidates.getJSONObject(0).getJSONObject("content");
            String aiResponse = content.getJSONArray("parts").getJSONObject(0).getString("text");

            return aiResponse.trim();
        } catch (Exception e) {
            e.printStackTrace();
            return "Error: Unable to communicate with the AI.";
        }
    }
}