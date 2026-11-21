package cz.boredclawd.samsungmessages;

/**
 * Result of the roaming and charge evaluation for a recipient.
 */
public final class ChargeAssessment {

    public enum Reason {
        NONE,
        INTERNATIONAL_FROM_HOME,
        ROAMING_OUTSIDE_EU,
        ROAMING_DEST_OUTSIDE_EU,
        ALL_ROAMING_POLICY
    }

    private final boolean charged;
    private final Reason reason;
    private final String recipientNumber;
    private final String destCountry;
    private final String homeCountry;
    private final String networkCountry;

    public ChargeAssessment(boolean charged, Reason reason, String recipientNumber,
                            String destCountry, String homeCountry, String networkCountry) {
        this.charged = charged;
        this.reason = reason;
        this.recipientNumber = recipientNumber != null ? recipientNumber : "";
        this.destCountry = destCountry != null ? destCountry : "";
        this.homeCountry = homeCountry != null ? homeCountry : "";
        this.networkCountry = networkCountry != null ? networkCountry : "";
    }

    public static ChargeAssessment free(String recipientNumber, String destCountry,
                                        String homeCountry, String networkCountry) {
        return new ChargeAssessment(false, Reason.NONE, recipientNumber, destCountry, homeCountry, networkCountry);
    }

    public static ChargeAssessment charged(Reason reason, String recipientNumber, String destCountry,
                                           String homeCountry, String networkCountry) {
        return new ChargeAssessment(true, reason, recipientNumber, destCountry, homeCountry, networkCountry);
    }

    public boolean isCharged() {
        return charged;
    }

    public Reason getReason() {
        return reason;
    }

    public String getRecipientNumber() {
        return recipientNumber;
    }

    public String getDestCountry() {
        return destCountry;
    }

    public String getHomeCountry() {
        return homeCountry;
    }

    public String getNetworkCountry() {
        return networkCountry;
    }
}
