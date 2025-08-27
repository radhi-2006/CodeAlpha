/* Save as: HotelReservationSystem.java
   Compile: javac HotelReservationSystem.java
   Run:     java HotelReservationSystem

   Notes:
   - Dates must be entered in ISO format: YYYY-MM-DD (example: 2025-09-05)
   - rooms.csv and bookings.csv will be created automatically if missing.
*/
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

/* ---------------------------
   Main application & menu
   --------------------------- */
public class HotelReservationSystem {

    private static final Scanner scanner = new Scanner(System.in);
    private static final Hotel hotel = new Hotel("Demo Hotel");
    private static final DateTimeFormatter DF = DateTimeFormatter.ISO_LOCAL_DATE;

    public static void main(String[] args) {
        System.out.println("Starting Hotel Reservation System...");
        try {
            hotel.loadData(); // creates sample rooms if none
        } catch (Exception e) {
            System.err.println("Failed to load data: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        boolean done = false;
        while (!done) {
            printMenu();
            String choice = prompt("Choice");
            switch (choice.trim()) {
                case "1": handleSearchAndBook(); break;
                case "2": handleCancel(); break;
                case "3": handleViewBooking(); break;
                case "4": hotel.listAllBookings(); break;
                case "5": hotel.listAllRooms(); break;
                case "0": done = true; break;
                default: System.out.println("Unknown choice. Try again.");
            }
        }

        System.out.println("Goodbye — data saved to CSV files.");
    }

    private static void printMenu() {
        System.out.println("\n--- Hotel Reservation Menu ---");
        System.out.println("1) Search rooms & Book");
        System.out.println("2) Cancel a reservation");
        System.out.println("3) View booking details (by booking id)");
        System.out.println("4) List all bookings");
        System.out.println("5) List all rooms");
        System.out.println("0) Exit");
    }

    private static void handleSearchAndBook() {
        System.out.println("\n--- Search Rooms ---");
        String typeStr = prompt("Room type (STANDARD, DELUXE, SUITE) or leave blank for any").trim().toUpperCase();
        RoomType type = null;
        if (!typeStr.isEmpty()) {
            try { type = RoomType.valueOf(typeStr); }
            catch (IllegalArgumentException e) { System.out.println("Unknown type. Searching all types."); }
        }
        LocalDate checkIn = promptForDate("Check-in date (YYYY-MM-DD)");
        LocalDate checkOut = promptForDate("Check-out date (YYYY-MM-DD)");
        if (checkIn == null || checkOut == null) return;
        if (!checkOut.isAfter(checkIn)) {
            System.out.println("Check-out must be after check-in.");
            return;
        }

        List<Room> avail = hotel.searchAvailableRooms(checkIn, checkOut.minusDays(1), type); // treat checkOut as exclusive
        if (avail.isEmpty()) {
            System.out.println("No rooms available for the selected dates and type.");
            return;
        }

        System.out.println("Available rooms:");
        for (int i = 0; i < avail.size(); i++) {
            Room r = avail.get(i);
            long nights = hotel.daysBetween(checkIn, checkOut);
            double total = r.getRatePerNight() * nights;
            System.out.printf("%d) Room %s (%s) - %.2f per night — total for %d nights = %.2f%n", i+1, r.getId(), r.getType(), r.getRatePerNight(), nights, total);
        }

        String want = prompt("Enter number to book or press Enter to cancel");
        if (want.trim().isEmpty()) {
            System.out.println("Booking cancelled by user.");
            return;
        }
        int idx;
        try { idx = Integer.parseInt(want.trim()) - 1; }
        catch (NumberFormatException e) { System.out.println("Invalid selection."); return; }
        if (idx < 0 || idx >= avail.size()) { System.out.println("Invalid selection."); return; }

        Room selected = avail.get(idx);
        String guest = prompt("Guest full name");
        String email = prompt("Contact email");

        long nights = hotel.daysBetween(checkIn, checkOut);
        double amount = selected.getRatePerNight() * nights;

        System.out.printf("Booking %s for %s from %s to %s — total %.2f%n", selected.getId(), guest, checkIn, checkOut, amount);
        String confirm = prompt("Proceed to payment? (yes/no)").trim().toLowerCase();
        if (!confirm.equals("yes") && !confirm.equals("y")) {
            System.out.println("Booking aborted.");
            return;
        }

        // simulate payment
        System.out.println("Simulating payment... enter card number (digits) or press Enter for dummy:");
        String card = scanner.nextLine().trim();
        PaymentSimulator.PaymentResult pr = PaymentSimulator.processPayment(amount, card);
        if (!pr.success) {
            System.out.println("Payment failed: " + pr.message);
            return;
        }

        Booking b = hotel.createBooking(selected.getId(), guest, email, checkIn, checkOut.minusDays(1), amount, "PAID");
        if (b != null) {
            System.out.printf("Booking successful! Booking ID: %s%n", b.getId());
            System.out.println("You can view details using option 3.");
        } else {
            System.out.println("Failed to create booking (concurrency or availability).");
        }
    }

    private static void handleCancel() {
        System.out.println("\n--- Cancel Reservation ---");
        String id = prompt("Enter Booking ID to cancel").trim();
        if (id.isEmpty()) { System.out.println("Booking ID required."); return; }
        Booking b = hotel.findBooking(id);
        if (b == null) { System.out.println("Booking not found."); return; }
        System.out.println("Booking found:");
        System.out.println(b.prettyPrint());
        String conf = prompt("Confirm cancellation? (yes/no)").trim().toLowerCase();
        if (!conf.equals("yes") && !conf.equals("y")) { System.out.println("Cancellation aborted."); return; }
        // simulate refund
        System.out.println("Processing refund...");
        PaymentSimulator.PaymentResult rr = PaymentSimulator.processRefund(b.getTotalAmount());
        if (!rr.success) {
            System.out.println("Refund failed: " + rr.message);
            return;
        }
        boolean cancelled = hotel.cancelBooking(id);
        if (cancelled) System.out.println("Booking cancelled and refund processed.");
        else System.out.println("Cancellation failed.");
    }

    private static void handleViewBooking() {
        System.out.println("\n--- View Booking ---");
        String id = prompt("Enter Booking ID").trim();
        if (id.isEmpty()) { System.out.println("Booking ID required."); return; }
        Booking b = hotel.findBooking(id);
        if (b == null) { System.out.println("Booking not found."); return; }
        System.out.println(b.prettyPrint());
    }

    private static String prompt(String label) {
        System.out.print(label + ": ");
        return scanner.nextLine();
    }

    private static LocalDate promptForDate(String promptText) {
        String in = prompt(promptText).trim();
        if (in.isEmpty()) { System.out.println("Date is required."); return null; }
        try {
            return LocalDate.parse(in, DF);
        } catch (DateTimeParseException e) {
            System.out.println("Invalid date format. Use YYYY-MM-DD.");
            return null;
        }
    }
}

/* ---------------------------
   Domain classes
   --------------------------- */
enum RoomType { STANDARD, DELUXE, SUITE }

class Room {
    private final String id;
    private final RoomType type;
    private final double ratePerNight;
    private final String description;

    Room(String id, RoomType type, double ratePerNight, String description) {
        this.id = id;
        this.type = type;
        this.ratePerNight = ratePerNight;
        this.description = description;
    }

    public String getId() { return id; }
    public RoomType getType() { return type; }
    public double getRatePerNight() { return ratePerNight; }
    public String getDescription() { return description; }

    public String toCSV() {
        return safe(id) + "," + safe(type.name()) + "," + ratePerNight + "," + safe(description);
    }

    public static Room fromCSV(String line) {
        String[] cols = HotelUtils.parseCSVLine(line);
        if (cols.length < 4) return null;
        String id = cols[0];
        RoomType type = RoomType.valueOf(cols[1]);
        double rate = Double.parseDouble(cols[2]);
        String desc = cols[3];
        return new Room(id, type, rate, desc);
    }

    private String safe(String s) {
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }

    @Override
    public String toString() {
        return String.format("Room %s: %s (%.2f/night) - %s", id, type, ratePerNight, description);
    }
}

class Booking {
    private final String id;
    private final String roomId;
    private final String guestName;
    private final String guestEmail;
    private final LocalDate start; // inclusive
    private final LocalDate end;   // inclusive
    private String status; // PAID, CANCELLED
    private final double totalAmount;

    Booking(String id, String roomId, String guestName, String guestEmail, LocalDate start, LocalDate end, double totalAmount, String status) {
        this.id = id;
        this.roomId = roomId;
        this.guestName = guestName;
        this.guestEmail = guestEmail;
        this.start = start;
        this.end = end;
        this.totalAmount = totalAmount;
        this.status = status;
    }

    public String getId() { return id; }
    public String getRoomId() { return roomId; }
    public String getGuestName() { return guestName; }
    public String getGuestEmail() { return guestEmail; }
    public LocalDate getStart() { return start; }
    public LocalDate getEnd() { return end; }
    public String getStatus() { return status; }
    public double getTotalAmount() { return totalAmount; }

    public void setStatus(String s) { this.status = s; }

    public String toCSV() {
        // id,roomId,guestName,guestEmail,start,end,status,totalAmount
        return concat(id, roomId, guestName, guestEmail, start.toString(), end.toString(), status, String.format("%.2f", totalAmount));
    }

    public static Booking fromCSV(String line) {
        String[] c = HotelUtils.parseCSVLine(line);
        if (c.length < 8) return null;
        String id = c[0], roomId = c[1], guestName = c[2], guestEmail = c[3];
        LocalDate start = LocalDate.parse(c[4]);
        LocalDate end = LocalDate.parse(c[5]);
        String status = c[6];
        double amt = Double.parseDouble(c[7]);
        return new Booking(id, roomId, guestName, guestEmail, start, end, amt, status);
    }

    private static String concat(String... parts) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (String p : parts) {
            if (!first) sb.append(",");
            sb.append("\"").append(p.replace("\"", "\"\"")).append("\"");
            first = false;
        }
        return sb.toString();
    }

    public boolean overlaps(LocalDate s, LocalDate e) {
        // both inclusive
        return !(e.isBefore(start) || s.isAfter(end));
    }

    public String prettyPrint() {
        return String.format("Booking ID: %s%nRoom: %s%nGuest: %s (%s)%nFrom: %s  To: %s%nStatus: %s%nTotal: %.2f%n",
                id, roomId, guestName, guestEmail, start.toString(), end.toString(), status, totalAmount);
    }
}

/* ---------------------------
   Hotel logic + persistence
   --------------------------- */
class Hotel {
    private final String name;
    private final Map<String, Room> rooms = new LinkedHashMap<>();
    private final Map<String, Booking> bookings = new LinkedHashMap<>();
    private final Path roomsFile = Paths.get("rooms.csv");
    private final Path bookingsFile = Paths.get("bookings.csv");

    Hotel(String name) {
        this.name = name;
    }

    public void loadData() throws IOException {
        if (!Files.exists(roomsFile)) createSampleRooms();
        loadRooms();
        if (!Files.exists(bookingsFile)) createEmptyBookingsFile();
        loadBookings();
    }

    private void createSampleRooms() throws IOException {
        List<Room> sample = Arrays.asList(
            new Room("R101", RoomType.STANDARD, 75.00, "Standard single bed"),
            new Room("R102", RoomType.STANDARD, 80.00, "Standard double bed"),
            new Room("R201", RoomType.DELUXE, 120.00, "Deluxe with city view"),
            new Room("R202", RoomType.DELUXE, 130.00, "Deluxe with balcony"),
            new Room("S301", RoomType.SUITE, 220.00, "Executive suite"),
            new Room("S302", RoomType.SUITE, 260.00, "Luxury suite with lounge")
        );
        try (BufferedWriter bw = Files.newBufferedWriter(roomsFile, StandardCharsets.UTF_8)) {
            bw.write("id,type,rate,description");
            bw.newLine();
            for (Room r : sample) {
                bw.write(r.toCSV());
                bw.newLine();
            }
        }
    }

    private void createEmptyBookingsFile() throws IOException {
        try (BufferedWriter bw = Files.newBufferedWriter(bookingsFile, StandardCharsets.UTF_8)) {
            bw.write("id,roomId,guestName,guestEmail,start,end,status,totalAmount");
            bw.newLine();
        }
    }

    private void loadRooms() throws IOException {
        rooms.clear();
        List<String> lines = Files.readAllLines(roomsFile, StandardCharsets.UTF_8);
        boolean first = true;
        for (String line : lines) {
            if (first) { first = false; continue; }
            if (line.trim().isEmpty()) continue;
            Room r = Room.fromCSV(line);
            if (r != null) rooms.put(r.getId(), r);
        }
    }

    private void loadBookings() throws IOException {
        bookings.clear();
        List<String> lines = Files.readAllLines(bookingsFile, StandardCharsets.UTF_8);
        boolean first = true;
        for (String line : lines) {
            if (first) { first = false; continue; }
            if (line.trim().isEmpty()) continue;
            Booking b = Booking.fromCSV(line);
            if (b != null) bookings.put(b.getId(), b);
        }
    }

    public void listAllRooms() {
        System.out.println("\n--- Rooms ---");
        rooms.values().forEach(r -> System.out.println(r.toString()));
    }

    public void listAllBookings() {
        System.out.println("\n--- Bookings ---");
        if (bookings.isEmpty()) { System.out.println("No bookings yet."); return; }
        bookings.values().forEach(b -> System.out.println(b.prettyPrint()));
    }

    public List<Room> searchAvailableRooms(LocalDate start, LocalDate end, RoomType type) {
        List<Room> result = new ArrayList<>();
        for (Room r : rooms.values()) {
            if (type != null && r.getType() != type) continue;
            boolean ok = true;
            for (Booking b : bookings.values()) {
                if (!b.getRoomId().equals(r.getId())) continue;
                if ("CANCELLED".equalsIgnoreCase(b.getStatus())) continue;
                if (b.overlaps(start, end)) { ok = false; break; }
            }
            if (ok) result.add(r);
        }
        return result;
    }

    public long daysBetween(LocalDate in, LocalDate outExclusive) {
        return java.time.temporal.ChronoUnit.DAYS.between(in, outExclusive);
    }

    public synchronized Booking createBooking(String roomId, String guestName, String guestEmail, LocalDate start, LocalDate end, double amount, String status) {
        // double-check availability
        Room r = rooms.get(roomId);
        if (r == null) return null;
        for (Booking b : bookings.values()) {
            if (!b.getRoomId().equals(roomId)) continue;
            if ("CANCELLED".equalsIgnoreCase(b.getStatus())) continue;
            if (b.overlaps(start, end)) return null; // not available
        }
        String id = generateBookingId();
        Booking newB = new Booking(id, roomId, guestName, guestEmail, start, end, amount, status);
        bookings.put(id, newB);
        try {
            persistBookings();
        } catch (IOException e) {
            System.err.println("Warning: failed to persist booking: " + e.getMessage());
        }
        return newB;
    }

    public Booking findBooking(String bookingId) { return bookings.get(bookingId); }

    public synchronized boolean cancelBooking(String bookingId) {
        Booking b = bookings.get(bookingId);
        if (b == null) return false;
        if ("CANCELLED".equalsIgnoreCase(b.getStatus())) return false;
        b.setStatus("CANCELLED");
        try {
            persistBookings();
            return true;
        } catch (IOException e) {
            System.err.println("Failed to persist cancellation: " + e.getMessage());
            return false;
        }
    }

    private void persistBookings() throws IOException {
        try (BufferedWriter bw = Files.newBufferedWriter(bookingsFile, StandardCharsets.UTF_8)) {
            bw.write("id,roomId,guestName,guestEmail,start,end,status,totalAmount");
            bw.newLine();
            for (Booking b : bookings.values()) {
                bw.write(b.toCSV());
                bw.newLine();
            }
        }
    }

    private String generateBookingId() {
        return "BKG-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}

/* ---------------------------
   Payment simulator (fake)
   --------------------------- */
class PaymentSimulator {
    static class PaymentResult {
        final boolean success;
        final String message;
        PaymentResult(boolean success, String message) { this.success = success; this.message = message; }
    }

    static PaymentResult processPayment(double amount, String cardNumber) {
        if (amount <= 0) return new PaymentResult(false, "Invalid amount.");
        // minimal validation: accept if card digits length >= 8, else allow dummy
        if (cardNumber == null || cardNumber.isEmpty()) {
            // dummy success
            return new PaymentResult(true, "Dummy payment accepted.");
        }
        String digits = cardNumber.replaceAll("[^0-9]", "");
        if (digits.length() < 8) return new PaymentResult(false, "Card number too short.");
        // simulate random failure 10%
        if (new Random().nextInt(10) == 0) return new PaymentResult(false, "Network/decline (simulated).");
        return new PaymentResult(true, "Payment accepted.");
    }

    static PaymentResult processRefund(double amount) {
        if (amount <= 0) return new PaymentResult(false, "Invalid refund amount.");
        // simulate refund success majority of the time
        if (new Random().nextInt(20) == 0) return new PaymentResult(false, "Refund failed (simulated).");
        return new PaymentResult(true, "Refund completed (simulated).");
    }
}

/* ---------------------------
   Small CSV & utility helper
   --------------------------- */
class HotelUtils {
    // parse CSV line with quoted fields (supports "" escaping)
    static String[] parseCSVLine(String line) {
        if (line == null) return new String[0];
        List<String> cols = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;
        for (int i=0; i<line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuotes && i+1 < line.length() && line.charAt(i+1) == '"') {
                    cur.append('"'); i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                cols.add(cur.toString());
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        cols.add(cur.toString());
        return cols.toArray(new String[0]);
    }
}
