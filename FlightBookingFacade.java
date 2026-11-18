public class FlightBookingFacade {

    // Encapsulated subsystem references
    private final SeatInventorySystem seatSystem;
    private final BaggagePricingEngine baggageEngine;
    private final CurrencyConverter currencyConverter;
    private final PaymentProcessor paymentProcessor;
    private final TicketIssuer ticketIssuer;

    public FlightBookingFacade() {
        this.seatSystem = new SeatInventorySystem();
        this.baggageEngine = new BaggagePricingEngine();
        this.currencyConverter = new CurrencyConverter();
        this.paymentProcessor = new PaymentProcessor();
        this.ticketIssuer = new TicketIssuer();
    }

    /**
     * Backward-compatible method. Existing callers use STANDARD loyalty tier.
     */
    public String bookFlight(String passengerName,
                             String flightCode,
                             String seatNumber,
                             double baseFareUSD,
                             int bagCount,
                             double bagWeightKg,
                             String currency,
                             String cardNumber) {
        return bookFlight(passengerName, flightCode, seatNumber, baseFareUSD,
                bagCount, bagWeightKg, currency, cardNumber, "STANDARD");
    }

    /**
     * Single entry point for booking a complete flight itinerary.
     * Coordinates seat locking, baggage pricing, currency conversion,
     * payment, rollback on failure, and ticket issuance.
     */
    public String bookFlight(String passengerName,
                             String flightCode,
                             String seatNumber,
                             double baseFareUSD,
                             int bagCount,
                             double bagWeightKg,
                             String currency,
                             String cardNumber,
                             String loyaltyTier) {
        String lockToken = null;

        try {
            seatSystem.connectToReservationServer();
            lockToken = seatSystem.lockSeat(flightCode, seatNumber, passengerName);

            double baggageFeeUSD;
            String tier = loyaltyTier == null ? "STANDARD" : loyaltyTier.trim().toUpperCase();

            if ("SILVER".equals(tier) || "GOLD".equals(tier)) {
                // SILVER and GOLD passengers do not pay checked baggage fees.
                // The baggage subsystem is intentionally not called.
                baggageFeeUSD = 0.0;
            } else {
                baggageFeeUSD = baggageEngine.calculateBaggageFeeUSD(bagCount, bagWeightKg);
            }

            double totalUSD = baseFareUSD + baggageFeeUSD;
            double convertedTotal = currencyConverter.convertFromUSD(totalUSD, currency);

            paymentProcessor.initializeGateway();

            // SILVER receives the promo after gateway initialization and before payment.
            if ("SILVER".equals(tier)) {
                paymentProcessor.applyPromoCode("SILVER-FREE-BAG");
            }

            String receiptId = paymentProcessor.chargeCard(cardNumber, convertedTotal, currency);
            return ticketIssuer.generateTicket(passengerName, flightCode, lockToken, receiptId);

        } catch (RuntimeException ex) {
            if (lockToken != null) {
                seatSystem.releaseSeatLock(lockToken);
            }
            System.out.println("[FlightBookingFacade] Booking failed: " + ex.getMessage());
            return "BOOKING_FAILED: " + ex.getMessage();

        } finally {
            seatSystem.disconnect();
        }
    }
}
