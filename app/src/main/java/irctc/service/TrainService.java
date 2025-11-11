package irctc.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

// **NOTE:** आपको इन classes का सही पैकेज नाम उपयोग करना होगा!
// मान लीजिए कि ये irctc.entities पैकेज में हैं:
import irctc.entities.Train;
import irctc.entities.Ticket; // The Ticket import
// यदि आप ticket.booking.entities का उपयोग कर रहे हैं, तो इसे बदलें:
// import ticket.booking.entities.Train;
// import ticket.booking.entities.Ticket; 

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList; // यदि trainList को शुरू में initialize करने की आवश्यकता हो
import java.util.Optional;
import java.util.stream.IntStream;
import java.util.stream.Collectors;

public class TrainService {

    // Final variable का नाम UPPER_SNAKE_CASE में रखें
    private static final String TRAIN_DB_PATH = "./localDB/trains.json";

    private List<Train> trainList;
    private ObjectMapper objectMapper; 

    // Constructor 
    public TrainService() throws IOException {
        this.objectMapper = new ObjectMapper(); // Initialize ObjectMapper
        
        // Load data from file
        File file = new File(TRAIN_DB_PATH);
        if (file.exists() && file.length() != 0) {
             trainList = objectMapper.readValue(file, new TypeReference<List<Train>>() {});
        } else {
             // यदि फ़ाइल मौजूद नहीं है, तो एक खाली सूची (empty list) के साथ शुरू करें
             trainList = new ArrayList<>(); 
        }
    }

    public List<Train> searchTrains(String source, String destination) {
        return trainList.stream()
                .filter(train -> validTrain(train, source, destination))
                .collect(Collectors.toList());
    }

    public void addTrain(Train newTrain) {
        // Find if a train with the same trainId already exists
        Optional<Train> existingTrain = trainList.stream()
                .filter(train -> train.getTrainId().equalsIgnoreCase(newTrain.getTrainId()))
                .findFirst();

        if (existingTrain.isPresent()) {
            // If a train with the same trainId exists, update it instead of adding a new one
            updateTrain(newTrain);
        } else {
            // Otherwise, add the new train to the list
            trainList.add(newTrain);
            saveTrainListToFile();
        }
    }

    public void updateTrain(Train updatedTrain) {
        // Find the index of the train with the same trainId
        // IntStream.range(0, trainList.size()) 
        //  .filter(i -> trainList.get(i).getTrainId().equalsIgnoreCase(updatedTrain.getTrainId()))
        //  .findFirst() // <-- Note: findFirst() returns OptionalInt
        
        // Simpler implementation using a regular loop or map/replace
        
        // Find the index of the train with the same trainId
        Optional<Integer> index = IntStream.range(0, trainList.size())
            .filter(i -> trainList.get(i).getTrainId().equalsIgnoreCase(updatedTrain.getTrainId()))
            .boxed() // Convert IntStream to Stream<Integer>
            .findFirst();

        if (index.isPresent()) {
            // If found, replace the existing train with the updated one
            trainList.set(index.get(), updatedTrain);
            saveTrainListToFile();
        } else {
            // If not found, treat it as adding a new train
            addTrain(updatedTrain); 
        }
    }

    private void saveTrainListToFile() {
        try {
            // ObjectMapper is now initialized and TRAIN_DB_PATH is defined
            objectMapper.writeValue(new File(TRAIN_DB_PATH), trainList);
        } catch (IOException e) {
            // Handle the exception based on your application's requirements
            e.printStackTrace();
        }
    }

    private boolean validTrain(Train train, String source, String destination) {
        List<String> stationOrder = train.getStations();

        // Convert to lowercase for case-insensitive comparison
        int sourceIndex = stationOrder.indexOf(source.toLowerCase());
        int destinationIndex = stationOrder.indexOf(destination.toLowerCase());

        // A valid train must have both stations, and the source must come before the destination
        return sourceIndex != -1 && destinationIndex != -1 && sourceIndex < destinationIndex;
    }
}