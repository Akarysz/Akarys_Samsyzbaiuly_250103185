# Facade Pattern Lab — Java

## Files
- `SeatInventorySystem.java`
- `BaggagePricingEngine.java`
- `CurrencyConverter.java`
- `PaymentProcessor.java`
- `TicketIssuer.java`
- `FlightBookingFacade.java`
- `Main.java`

All files use the default package. Keep them in the same source folder.

## Run
From this directory:

```bash
javac *.java
java Main
```

In IntelliJ IDEA:
1. Open this folder as a project.
2. Make sure all `.java` files are in the same source folder.
3. Open `Main.java`.
4. Click the green Run icon next to `main`.

## What is tested
1. Successful STANDARD booking.
2. Blocked seat in row 13.
3. Invalid card number causes the seat lock to be released.
4. SILVER tier skips the baggage engine and applies `SILVER-FREE-BAG`.

`FlightBookingFacade` is the only subsystem entry point used by `Main`.
