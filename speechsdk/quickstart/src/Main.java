import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.Dictionary;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import com.microsoft.cognitiveservices.speech.*;
import com.microsoft.cognitiveservices.speech.intent.*;

public class Main {
    public static void main(String[] args) throws InterruptedException, ExecutionException {
        IntentPatternMatchingWithMicrophone();
    }

    public static void IntentPatternMatchingWithMicrophone() throws InterruptedException, ExecutionException {
    	
    	Properties properties = new Properties();
    	String apiKey = null;
    	String subregion = null;
        try {
            // Load the properties file
            FileInputStream fis = new FileInputStream("D:\\Users\\Mouad\\speechsdk\\quickstart\\src\\config.properties");
            properties.load(fis);
            
            // Get the API key from the properties file
             apiKey  = properties.getProperty("api_key");
             subregion = properties.getProperty("sub_region");
            
            
        } catch (IOException e) {
            e.printStackTrace();
        }
         
        SpeechConfig config = SpeechConfig.fromSubscription(apiKey, subregion);
        try (IntentRecognizer recognizer = new IntentRecognizer(config)) {
            // Creates a Pattern Matching model and adds specific intents from your model. The
            // Id is used to identify this model from others in the collection.
            PatternMatchingModel model = new PatternMatchingModel("YourPatternMatchingModelId");

            // Creates a pattern that uses groups of optional words. "[Go | Take me]" will match either "Go", "Take me", or "".
            String patternWithOptionalWords = "[Go | Take me] to [floor|level] {floorName}";

            // Creates a pattern that uses an optional entity and group that could be used to tie commands together.
            String patternWithOptionalEntity = "Go to parking [{parkingLevel}]";

            // You can also have multiple entities of the same name in a single pattern by adding appending a unique identifier
            // to distinguish between the instances. For example:
            String patternWithTwoOfTheSameEntity = "Go to floor {floorName:1} [and then go to floor {floorName:2}]";
            // NOTE: Both floorName:1 and floorName:2 are tied to the same list of entries. The identifier can be a string
            // and is separated from the entity name by a ':'

            // Creates the pattern matching intents and adds them to the model
            model.getIntents().put(new PatternMatchingIntent("ChangeFloors", patternWithOptionalWords, patternWithOptionalEntity, patternWithTwoOfTheSameEntity));
            model.getIntents().put(new PatternMatchingIntent("DoorControl", "{action} the doors", "{action} doors", "{action} the door", "{action} door"));

            // Creates the "floorName" entity and set it to type list.
            // Adds acceptable values. NOTE the default entity type is Any and so we do not need
            // to declare the "action" entity.
            model.getEntities().put(PatternMatchingEntity.CreateListEntity("floorName", PatternMatchingEntity.EntityMatchMode.Strict, "ground floor", "lobby", "1st", "first", "one", "1", "2nd", "second", "two", "2"));

            // Creates the "parkingLevel" entity as a pre-built integer
            model.getEntities().put(PatternMatchingEntity.CreateIntegerEntity("parkingLevel"));

            ArrayList<LanguageUnderstandingModel> modelCollection = new ArrayList<LanguageUnderstandingModel>();
            modelCollection.add(model);

            recognizer.applyLanguageModels(modelCollection);

            System.out.println("Say something...");

            IntentRecognitionResult result = recognizer.recognizeOnceAsync().get();

            if (result.getReason() == ResultReason.RecognizedSpeech) {
                System.out.println("RECOGNIZED: Text= " + result.getText());
                System.out.println(String.format("%17s", "Intent not recognized."));
            }
            else if (result.getReason() == ResultReason.RecognizedIntent)
            {
                System.out.println("RECOGNIZED: Text= " + result.getText());
                System.out.println(String.format("%17s %s", "Intent Id=", result.getIntentId() + "."));
                Dictionary<String, String> entities = result.getEntities();

                switch (result.getIntentId())
                {
                    case "ChangeFloors":
                        if (entities.get("floorName") != null) {
                            System.out.println(String.format("%17s %s", "FloorName=", entities.get("floorName")));
                        }
                        if (entities.get("floorName:1") != null) {
                            System.out.println(String.format("%17s %s", "FloorName:1=", entities.get("floorName:1")));
                        }
                        if (entities.get("floorName:2") != null) {
                            System.out.println(String.format("%17s %s", "FloorName:2=", entities.get("floorName:2")));
                        }
                        if (entities.get("parkingLevel") != null) {
                            System.out.println(String.format("%17s %s", "ParkingLevel=", entities.get("parkingLevel")));
                        }
                        break;

                    case "DoorControl":
                        if (entities.get("action") != null) {
                            System.out.println(String.format("%17s %s", "Action=", entities.get("action")));
                        }
                        break;
                }
            }
            else if (result.getReason() == ResultReason.NoMatch) {
                System.out.println("NOMATCH: Speech could not be recognized.");
            }
            else if (result.getReason() == ResultReason.Canceled) {
                CancellationDetails cancellation = CancellationDetails.fromResult(result);
                System.out.println("CANCELED: Reason=" + cancellation.getReason());

                if (cancellation.getReason() == CancellationReason.Error)
                {
                    System.out.println("CANCELED: ErrorCode=" + cancellation.getErrorCode());
                    System.out.println("CANCELED: ErrorDetails=" + cancellation.getErrorDetails());
                    System.out.println("CANCELED: Did you update the subscription info?");
                }
            }
        }
    }
}