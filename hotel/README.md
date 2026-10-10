# Hotel Management System (Java Swing + MySQL)

Upgraded from the single-file console program. Data now lives in MySQL instead of the
serialized `backup` file, and the console menu is replaced by a Swing desktop UI.

## Features
- **Dashboard**: floor plan of all 60 rooms, colour-coded (green = free, red = occupied),
  with room details and quick actions. Double-click a room to start its check-in or food order.
- **Check-in**: book a free room for one guest (single rooms) or two guests (double rooms).
- **Food orders**: add menu items (Sandwich, Pasta, Noodles, Coke) to an occupied room.
- **Checkout & bill**: preview the bill, check out, and open the bill in its own window.
- **Bill window**: separate window with Print, Save as text, and the same layout on screen and paper.

Business rules are kept from the original project: room numbers 1-10 Luxury Double,
11-30 Deluxe Double, 31-40 Luxury Single, 41-60 Deluxe Single; rates Rs. 4000 / 3000 / 2200 / 1200
per day; food prices unchanged. One change: the room charge is now per night (minimum one night),
calculated from the check-in and check-out times.

## Setup
1. Install **MySQL 8.0+** and **JDK 17+**.
2. Edit `db.properties` with your MySQL user and password. The database `hotel_db` and all tables
   are created automatically on first run.
3. Build and run:
   ```
   mvn package
   java -jar target/hotel-management-2.0.0.jar
   ```
   Or run `com.hotel.HotelApp` from your IDE. Keep `db.properties` in the folder you run from.

## Project layout
```
src/main/resources/schema.sql        tables and menu/room-type seed data
src/main/java/com/hotel/
  HotelApp.java                      entry point
  db/Database.java                   connections, schema setup, transactions
  db/HotelRepository.java            all SQL
  model/                             records: Room, Booking, Guest, Bill, ...
  service/HotelService.java          booking, food, billing and checkout rules
  billing/BillFormatter.java         bill text layout
  billing/BillPrinter.java           printing through the system print dialog
  ui/                                Swing screens (MainFrame, Dashboard, Check-in, Food, Checkout, BillWindow)
```

## Notes
- Rooms that are booked are locked during check-in, food and checkout (`SELECT ... FOR UPDATE`),
  so two staff members cannot book the same room at once.
- The old `backup` file is not imported. Start with an empty database, or ask for an importer.
