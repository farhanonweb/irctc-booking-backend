package irctc.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

// **आवश्यक Imports:** आपको अपनी entities और utility classes का सही पैकेज नाम उपयोग करना होगा!
// **NOTE:** मैंने अनुमान लगाया है कि ये classes 'irctc.entities', 'irctc.util', और 'irctc.service' में हैं।

import irctc.entities.Ticket;  // The Ticket class
import irctc.entities.Train;   // The Train class
import irctc.entities.User;    // The User class
import irctc.util.UserServiceUtil;      // The UserUtil class (जिसमें checkPassword है)
import irctc.service.TrainService; // TrainService class

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Optional;
import java.util.Scanner; // For Scanner in cancelBooking (if used)

public class UserBookingService {

    private final ObjectMapper objectMapper;
    
    // User data file path (मैंने आपके द्वारा दी गई absolute path को './' relative path में बदला है)
    private static final String USER_FILE_PATH = "./localDB/users.json";
    
    private List<User> userList;
    private User loggedInUser; // For the currently logged-in user

    // Constructor
    public UserBookingService() throws IOException {
        this.objectMapper = new ObjectMapper();
        loadUserListFromFile(); // Call the loading method
    }

    // Overloaded Constructor (आपके Screenshot 52 की लाइन 27 के अनुसार)
    public UserBookingService(User user) throws IOException {
        this(); // Calls the default constructor to load data and initialize objectMapper
        this.loggedInUser = user; // Assign the passed user
    }

    // Loads user data from JSON file
    private void loadUserListFromFile() throws IOException {
        File file = new File(USER_FILE_PATH);
        if (file.exists() && file.length() != 0) {
            userList = objectMapper.readValue(file, new TypeReference<List<User>>() {});
        } else {
            userList = new ArrayList<>();
        }
    }
    
    // Saves user data to JSON file
    private void saveUserListToFile() throws IOException {
        File userFile = new File(USER_FILE_PATH);
        objectMapper.writeValue(userFile, userList);
    }
    
    // Finds and returns a user based on name and password
    public Optional<User> login(String userName, String password) {
        return userList.stream()
            .filter(user -> user.getUserName().equals(userName) && UserUtil.checkPassword(password, user.getHashedPassword()))
            .findFirst();
    }
    
    // Adds a new user
    public boolean signUp(User user1) {
        try {
            userList.add(user1);
            saveUserListToFile();
            return true;
        } catch (IOException ex) {
            ex.printStackTrace();
            return false; 
        }
    }
    
    // Fetches bookings for the logged-in user
    public void fetchBookings(String userName, String password) {
        Optional<User> fetchedUser = userList.stream()
            .filter(user -> user.getUserName().equals(userName) && UserUtil.checkPassword(password, user.getHashedPassword()))
            .findFirst();

        if (fetchedUser.isPresent()) {
            fetchedUser.get().printTickets(); // Assuming User class has a printTickets method
        }
    }

    // Fetches available trains based on source and destination (Screenshot 54)
    public List<Train> getTrains(String source, String destination) {
        try {
            // TrainService को initialize करने के लिए IOException हैंडल करना आवश्यक है
            TrainService trainService = new TrainService(); 
            return trainService.searchTrains(source, destination);
        } catch (IOException ex) {
            System.err.println("Error accessing train data: " + ex.getMessage());
            return new ArrayList<>(); // Return an empty list on failure
        }
    }
    
    // Books a ticket (Screenshot 55)
    public boolean bookTrainSeat(Train train, int row, int seat) {
        // trainService को एक बार class level पर भी declare किया जा सकता है, 
        // लेकिन यहाँ local initialization भी काम करेगा।
        TrainService trainService;
        try {
            trainService = new TrainService();
        } catch (IOException e) {
            System.err.println("Failed to initialize TrainService: " + e.getMessage());
            return false;
        }
        
        // ट्रेन से सीटों की लिस्ट प्राप्त करें
        List<List<Integer>> seats = train.getSeats(); 

        try {
            // Check for valid row and seat indices
            // seats.size() rows की संख्या है (0 to size-1), seats.get(row).size() columns की संख्या है
            if (row >= 0 && row < seats.size() && seat >= 0 && seat < seats.get(row).size()) {
                
                // Check if the seat is available (assuming 0 means available, 1 means booked)
                if (seats.get(row).get(seat) == 0) { 
                    
                    // Book the seat (Set to 1)
                    seats.get(row).set(seat, 1);
                    train.setSeats(seats);
                    
                    // Update the train in the data store
                    trainService.addTrain(train); 
                    
                    // Create and add the new ticket to the loggedInUser's tickets
                    // **Note:** आपको ticket ID, source, destination, date आदि के लिए logic जोड़ना होगा।
                    // loggedInUser.addTicket(new Ticket(...));
                    
                    System.out.println("Booking successful!");
                    return true;
                } else {
                    System.out.println("Seat is already booked");
                    return false;
                }
            } else {
                System.out.println("Invalid row or seat index");
                return false;
            }
        } catch (Exception ex) { // Catch any unexpected exceptions
            ex.printStackTrace();
            return false; 
        }
    }
    
    // Cancels a booking (Screenshot 54)
    public boolean cancelBooking(String ticketId) {
        // यह मानकर चल रहा हूँ कि ticketId input लेने का लॉजिक (Scanner) UI layer में है
        
        if (ticketId == null || ticketId.isEmpty()) {
            System.out.println("Ticket ID cannot be null or empty.");
            return false;
        }

        // यदि आप `loggedInUser` का उपयोग कर रहे हैं:
        if (loggedInUser == null) {
            System.out.println("User is not logged in.");
            return false;
        }

        // Ticket को हटाएँ
        // यह मानकर कि User क्लास में getTickets() और removeTicket(Ticket) method है
        boolean removed = loggedInUser.getTickets().removeIf(ticket -> ticket.getTicketId().equals(ticketId));

        if (removed) {
            // **Note:** आपको यहाँ TrainService का उपयोग करके train seat को भी unbook करना होगा।
            // TrainService trainService = new TrainService();
            // trainService.unbookSeat(...);
            
            // User की जानकारी को file में save करें
            try {
                saveUserListToFile(); 
            } catch (IOException e) {
                System.err.println("Failed to save user data after cancellation.");
                e.printStackTrace();
            }

            System.out.println("Ticket with ID " + ticketId + " has been canceled.");
            return true;
        } else {
            System.out.println("No ticket found with ID " + ticketId);
            return false;
        }
    }
}